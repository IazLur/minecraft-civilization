package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;

/**
 * Service pour synchroniser les vraies stats des villages avec les entités réelles
 */
public class VillageStatsService {
    
    /**
     * Recalcule et corrige toutes les stats des villages
     */
    public static void synchronizeAllVillageStats() {
        if (TestJava.world == null) {
            return;
        }
        
        try {
            Bukkit.getLogger().info("[VillageStats] Début synchronisation stats villages...");
            
            for (VillageModel village : VillageRepository.getAll()) {
                synchronizeVillageStats(village);
            }
            
            Bukkit.getLogger().info("[VillageStats] Synchronisation terminée.");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[VillageStats] Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Synchronise les stats d'un village spécifique
     */
    public static void synchronizeVillageStats(VillageModel village) {
        try {
            // Compter les vraies entités dans le monde
            int realPopulation = countRealVillagers(village.getId());
            int realGarrison = countRealGarrison(village.getId());
            int realGroundArmy = countRealGroundArmy(village.getId());
            
            // Comparer avec les stats en base
            boolean needsUpdate = false;
            String changes = "";
            
            if (village.getPopulation() != realPopulation) {
                changes += String.format("Population: %d → %d ", village.getPopulation(), realPopulation);
                village.setPopulation(realPopulation);
                needsUpdate = true;
            }
            
            if (village.getGarrison() != realGarrison) {
                changes += String.format("Garnison: %d → %d ", village.getGarrison(), realGarrison);
                village.setGarrison(realGarrison);
                needsUpdate = true;
            }
            
            if (village.getGroundArmy() != realGroundArmy) {
                changes += String.format("Armée: %d → %d ", village.getGroundArmy(), realGroundArmy);
                village.setGroundArmy(realGroundArmy);
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                VillageRepository.update(village);
                Bukkit.getLogger().info("[VillageStats] " + village.getId() + " - " + changes);
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VillageStats] Erreur sync " + village.getId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Compte les vrais villageois d'un village
     */
    private static int countRealVillagers(String villageName) {
        int count = 0;
        
        try {
            // Compter depuis la base de données (plus fiable)
            for (VillagerModel villagerModel : VillagerRepository.getAll()) {
                if (villageName.equals(villagerModel.getVillageName())) {
                    // Vérifier que l'entité existe encore dans le monde
                    Entity entity = Bukkit.getServer().getEntity(villagerModel.getId());
                    if (entity instanceof Villager && entity.getCustomName() != null) {
                        try {
                            String entityVillage = CustomName.extractVillageName(entity.getCustomName());
                            if (villageName.equals(entityVillage)) {
                                count++;
                            }
                        } catch (Exception e) {
                            // Ignorer les erreurs d'extraction
                        }
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VillageStats] Erreur count villagers: " + e.getMessage());
        }
        
        return count;
    }
    
    /**
     * Compte les vrais gardes (squelettes) d'un village
     */
    private static int countRealGarrison(String villageName) {
        int count = 0;
        
        try {
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Skeleton && entity.getCustomName() != null) {
                    try {
                        String entityVillage = CustomName.extractVillageName(entity.getCustomName());
                        if (villageName.equals(entityVillage)) {
                            count++;
                        }
                    } catch (Exception e) {
                        // Ignorer les erreurs d'extraction
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VillageStats] Erreur count garrison: " + e.getMessage());
        }
        
        return count;
    }
    
    /**
     * Compte les vrais soldats (pillagers) d'un village
     */
    private static int countRealGroundArmy(String villageName) {
        int count = 0;
        
        try {
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Pillager && entity.getCustomName() != null) {
                    try {
                        String entityVillage = CustomName.extractVillageName(entity.getCustomName());
                        if (villageName.equals(entityVillage)) {
                            count++;
                        }
                    } catch (Exception e) {
                        // Ignorer les erreurs d'extraction
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VillageStats] Erreur count army: " + e.getMessage());
        }
        
        return count;
    }
}