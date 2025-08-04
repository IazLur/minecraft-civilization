package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.*;

/**
 * Service pour détecter et nettoyer les villageois fantômes
 * (présents en base de données mais pas dans le monde)
 */
public class GhostVillagerCleanupService {

    /**
     * Nettoie tous les villageois fantômes
     */
    public static CleanupResult cleanupGhostVillagers() {
        Bukkit.getLogger().info("[GhostCleanup] Démarrage du nettoyage des villageois fantômes...");
        
        CleanupResult result = new CleanupResult();
        
        if (TestJava.world == null) {
            Bukkit.getLogger().warning("[GhostCleanup] Monde non disponible - abandon");
            return result;
        }
        
        try {
            // Récupère tous les UUIDs des villageois dans le monde
            Set<UUID> worldVillagerIds = getWorldVillagerIds();
            
            // Récupère tous les villageois en base
            Collection<VillagerModel> dbVillagers = VillagerRepository.getAll();
            
            result.totalInDB = dbVillagers.size();
            result.totalInWorld = worldVillagerIds.size();
            
            Bukkit.getLogger().info("[GhostCleanup] Villageois en DB: " + result.totalInDB + 
                                   ", dans le monde: " + result.totalInWorld);
            
            // Trouve les villageois fantômes
            List<VillagerModel> ghostVillagers = new ArrayList<>();
            for (VillagerModel villager : dbVillagers) {
                if (!worldVillagerIds.contains(villager.getId())) {
                    ghostVillagers.add(villager);
                }
            }
            
            result.ghostsDetected = ghostVillagers.size();
            
            if (ghostVillagers.isEmpty()) {
                Bukkit.getLogger().info("[GhostCleanup] ✅ Aucun villageois fantôme détecté");
                return result;
            }
            
            Bukkit.getLogger().warning("[GhostCleanup] " + ghostVillagers.size() + " villageois fantômes détectés");
            
            // Nettoie chaque villageois fantôme
            Map<String, Integer> villageUpdates = new HashMap<>();
            
            for (VillagerModel ghostVillager : ghostVillagers) {
                try {
                    // Supprime de la base
                    VillagerRepository.remove(ghostVillager.getId());
                    result.ghostsRemoved++;
                    
                    // Met à jour le compteur du village
                    String villageName = ghostVillager.getVillageName();
                    villageUpdates.put(villageName, villageUpdates.getOrDefault(villageName, 0) + 1);
                    
                    Bukkit.getLogger().info("[GhostCleanup] Supprimé: " + ghostVillager.getId() + 
                                           " du village " + villageName);
                    
                } catch (Exception e) {
                    result.errors++;
                    Bukkit.getLogger().severe("[GhostCleanup] Erreur suppression " + 
                                             ghostVillager.getId() + ": " + e.getMessage());
                }
            }
            
            // Met à jour les populations des villages
            for (Map.Entry<String, Integer> entry : villageUpdates.entrySet()) {
                try {
                    VillageModel village = VillageRepository.get(entry.getKey());
                    if (village != null) {
                        int oldPopulation = village.getPopulation();
                        int removedCount = entry.getValue();
                        int newPopulation = Math.max(0, oldPopulation - removedCount);
                        
                        village.setPopulation(newPopulation);
                        VillageRepository.update(village);
                        
                        result.villagesUpdated++;
                        
                        Bukkit.getLogger().info("[GhostCleanup] Village " + entry.getKey() + 
                                               " population: " + oldPopulation + " → " + newPopulation + 
                                               " (-" + removedCount + ")");
                    }
                } catch (Exception e) {
                    result.errors++;
                    Bukkit.getLogger().severe("[GhostCleanup] Erreur mise à jour village " + 
                                             entry.getKey() + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            result.errors++;
            Bukkit.getLogger().severe("[GhostCleanup] Erreur générale: " + e.getMessage());
            e.printStackTrace();
        }
        
        Bukkit.getLogger().info("[GhostCleanup] ✅ Nettoyage terminé - " + 
                               result.ghostsRemoved + "/" + result.ghostsDetected + " supprimés, " +
                               result.villagesUpdated + " villages mis à jour, " +
                               result.errors + " erreurs");
        
        return result;
    }
    
    /**
     * Récupère tous les UUIDs des villageois présents dans le monde
     */
    private static Set<UUID> getWorldVillagerIds() {
        Set<UUID> worldIds = new HashSet<>();
        
        for (Entity entity : TestJava.world.getEntities()) {
            if (entity instanceof Villager && entity.getCustomName() != null) {
                // Seulement les villageois avec noms personnalisés (du plugin)
                worldIds.add(entity.getUniqueId());
            }
        }
        
        return worldIds;
    }
    
    /**
     * Résultat du nettoyage
     */
    public static class CleanupResult {
        public int totalInDB = 0;
        public int totalInWorld = 0;
        public int ghostsDetected = 0;
        public int ghostsRemoved = 0;
        public int villagesUpdated = 0;
        public int errors = 0;
        
        public boolean hasGhosts() {
            return ghostsDetected > 0;
        }
        
        public boolean wasSuccessful() {
            return errors == 0 && ghostsRemoved == ghostsDetected;
        }
    }
}