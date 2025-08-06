package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.HistoryService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service pour synchroniser les villageois existants dans le monde avec la base de données
 * Détecte les villageois avec customName qui ne sont pas en base et les ajoute automatiquement
 */
public class VillagerSynchronizationService {



    /**
     * Synchronise tous les villageois du monde avec la base de données
     * À appeler au démarrage du plugin
     */
    public static SynchronizationResult synchronizeWorldVillagersWithDatabase() {
        Bukkit.getLogger().info("[VillagerSync] ===============================================");
        Bukkit.getLogger().info("[VillagerSync] Démarrage de la synchronisation villageois...");
        
        SynchronizationResult result = new SynchronizationResult();
        
        if (TestJava.world == null) {
            Bukkit.getLogger().warning("[VillagerSync] Monde non disponible - synchronisation annulée");
            return result;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Récupère tous les villageois existants en base
            Collection<VillagerModel> dbVillagers = VillagerRepository.getAll();
            Set<UUID> dbVillagerIds = new HashSet<>();
            for (VillagerModel villager : dbVillagers) {
                dbVillagerIds.add(villager.getId());
            }
            
            result.existingInDB = dbVillagers.size();
            Bukkit.getLogger().info("[VillagerSync] Villageois en base de données: " + result.existingInDB);
            
            // Parcourt tous les villageois du monde avec customName
            Map<String, Integer> villagePopulationUpdates = new HashMap<>();
            
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Villager villager && villager.getCustomName() != null) {
                    result.worldVillagersWithName++;
                    
                    // Vérifie si ce villageois existe déjà en base
                    if (!dbVillagerIds.contains(villager.getUniqueId())) {
                        // Villageois non synchronisé trouvé !
                        try {
                            VillagerModel newVillagerModel = createVillagerModelFromEntity(villager);
                            if (newVillagerModel != null) {
                                // Sauvegarde en base
                                VillagerRepository.update(newVillagerModel);
                                
                                // Initialise la classe sociale
                                SocialClassService.evaluateAndUpdateSocialClass(newVillagerModel);
                                SocialClassService.updateVillagerDisplayName(newVillagerModel);
                                
                                result.syncedCount++;
                                
                                // Compte pour la mise à jour de population
                                String villageName = newVillagerModel.getVillageName();
                                villagePopulationUpdates.put(villageName, 
                                    villagePopulationUpdates.getOrDefault(villageName, 0) + 1);
                                
                                Bukkit.getLogger().info("[VillagerSync] ✅ Synchronisé: " + 
                                    villager.getUniqueId() + " (" + newVillagerModel.getVillageName() + ")");
                                
                            } else {
                                result.errors++;
                                Bukkit.getLogger().warning("[VillagerSync] ❌ Impossible de créer le modèle pour " + 
                                    villager.getUniqueId());
                            }
                            
                        } catch (Exception e) {
                            result.errors++;
                            Bukkit.getLogger().severe("[VillagerSync] ❌ Erreur synchronisation " + 
                                villager.getUniqueId() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            // Met à jour les populations des villages
            updateVillagePopulations(villagePopulationUpdates, result);
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            result.duration = duration;
            
            // Logs de résumé
            Bukkit.getLogger().info("[VillagerSync] ===============================================");
            Bukkit.getLogger().info("[VillagerSync] ✅ Synchronisation terminée en " + 
                String.format("%.2f", duration) + " secondes");
            Bukkit.getLogger().info("[VillagerSync] Villageois en base: " + result.existingInDB);
            Bukkit.getLogger().info("[VillagerSync] Villageois dans le monde: " + result.worldVillagersWithName);
            Bukkit.getLogger().info("[VillagerSync] Nouveaux synchronisés: " + result.syncedCount);
            Bukkit.getLogger().info("[VillagerSync] Villages mis à jour: " + result.villagesUpdated);
            Bukkit.getLogger().info("[VillagerSync] Erreurs: " + result.errors);
            
            if (result.syncedCount > 0) {
                // Broadcast informatif
                Bukkit.getServer().broadcastMessage(
                    ChatColor.AQUA + "🔄 Synchronisation: " + ChatColor.YELLOW + result.syncedCount + 
                    ChatColor.AQUA + " villageois ajoutés à la base de données"
                );
            }
            
            Bukkit.getLogger().info("[VillagerSync] ===============================================");
            
        } catch (Exception e) {
            result.errors++;
            Bukkit.getLogger().severe("[VillagerSync] ❌ Erreur générale de synchronisation: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * Crée un VillagerModel à partir d'une entité Villager existante
     */
    private static VillagerModel createVillagerModelFromEntity(Villager villager) {
        try {
            String customName = villager.getCustomName();
            if (customName == null) {
                return null;
            }
            
            // Extrait le nom du village depuis le customName
            String villageName = CustomName.extractVillageName(customName);
            if (villageName == null) {
                Bukkit.getLogger().warning("[VillagerSync] Impossible d'extraire le village de: " + customName);
                return null;
            }
            
            // Vérifie que le village existe
            VillageModel village = VillageRepository.get(villageName);
            if (village == null) {
                Bukkit.getLogger().warning("[VillagerSync] Village inexistant: " + villageName);
                return null;
            }
            
            // Crée le modèle
            VillagerModel villagerModel = new VillagerModel();
            villagerModel.setId(villager.getUniqueId());
            villagerModel.setVillageName(villageName);
            villagerModel.setFood(10); // Valeur par défaut (évite la mort immédiate)
            villagerModel.setSocialClass(0); // Classe 0 par défaut
            villagerModel.setEating(false);
            villagerModel.setRichesse(0.0f); // Richesse par défaut
            
            // Enregistrer la naissance dans l'historique
            HistoryService.recordVillagerBirth(villagerModel, villageName);
            
            return villagerModel;
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[VillagerSync] Erreur création modèle: " + e.getMessage());
            return null;
        }
    }
    

    
    /**
     * Met à jour les populations des villages
     */
    private static void updateVillagePopulations(Map<String, Integer> updates, SynchronizationResult result) {
        for (Map.Entry<String, Integer> entry : updates.entrySet()) {
            try {
                String villageName = entry.getKey();
                int additionalPopulation = entry.getValue();
                
                VillageModel village = VillageRepository.get(villageName);
                if (village != null) {
                    int oldPopulation = village.getPopulation();
                    int newPopulation = oldPopulation + additionalPopulation;
                    
                    village.setPopulation(newPopulation);
                    VillageRepository.update(village);
                    
                    result.villagesUpdated++;
                    
                    Bukkit.getLogger().info("[VillagerSync] Population " + villageName + ": " + 
                        oldPopulation + " → " + newPopulation + " (+" + additionalPopulation + ")");
                }
            } catch (Exception e) {
                result.errors++;
                Bukkit.getLogger().severe("[VillagerSync] Erreur mise à jour population " + 
                    entry.getKey() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Résultat de la synchronisation
     */
    public static class SynchronizationResult {
        public int existingInDB = 0;
        public int worldVillagersWithName = 0;
        public int syncedCount = 0;
        public int villagesUpdated = 0;
        public int errors = 0;
        public double duration = 0.0;
        
        public boolean wasSuccessful() {
            return errors == 0;
        }
        
        public boolean foundUnsynchronized() {
            return syncedCount > 0;
        }
    }
}