package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.enums.SocialClass;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.CustomJobArmorService;
import TestJava.testjava.services.CustomJobAssignmentService;
import TestJava.testjava.services.SocialClassService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;

import java.util.Collection;

/**
 * Thread de maintenance pour le système de métiers custom
 * - Vérifie que les employés portent leur armure
 * - Ajuste le nombre d'employés selon les besoins des bâtiments  
 * - Nettoie les incohérences
 */
public class CustomJobMaintenanceThread implements Runnable {

    @Override
    public void run() {
        try {
            int totalActions = 0;
            int armorFixed = 0;
            int buildingAdjustments = 0;
            int orphansFixed = 0;
            int inconsistenciesFixed = 0;
            
            // 1. Maintenance des armures des employés custom
            armorFixed = maintainCustomJobArmor();
            totalActions += armorFixed;
            
            // 2. Ajustement automatique des employés selon les besoins des bâtiments
            buildingAdjustments = adjustBuildingEmployees();
            totalActions += buildingAdjustments;
            
            // 3. Nettoyage des incohérences (villageois avec métier custom mais bâtiment inexistant)
            orphansFixed = cleanupOrphanedCustomJobs();
            totalActions += orphansFixed;
            
            // 4. Vérification de la cohérence des classes sociales vs métiers custom
            inconsistenciesFixed = verifyCustomJobConsistency();
            totalActions += inconsistenciesFixed;
            
            // Un seul log de résumé
            if (totalActions > 0) {
                Bukkit.getLogger().info("[CustomJobMaintenance] ✅ Résumé: " + totalActions + " actions effectuées " +
                                       "(armures: " + armorFixed + ", ajustements: " + buildingAdjustments + 
                                       ", orphelins: " + orphansFixed + ", incohérences: " + inconsistenciesFixed + ")");
            } else {
                Bukkit.getLogger().info("[CustomJobMaintenance] ℹ️ Aucune maintenance nécessaire");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobMaintenance] ❌ Erreur lors de la maintenance: " + e.getMessage());
        }
    }
    
    /**
     * S'assure que tous les employés custom portent leur armure de cuir
     */
    private int maintainCustomJobArmor() {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        int armorFixed = 0;
        
        for (VillagerModel villager : allVillagers) {
            if (!villager.hasCustomJob()) {
                continue;
            }
            
            try {
                Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villager.getId());
                if (entity == null) {
                    continue; // Villageois fantôme
                }
                
                // Vérifier si l'armure est présente
                if (!CustomJobArmorService.isWearingLeatherArmor(entity)) {
                    CustomJobArmorService.equipLeatherArmor(entity);
                    villager.setHasLeatherArmor(true);
                    VillagerRepository.update(villager);
                    armorFixed++;
                }
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[CustomJobMaintenance] Erreur maintenance armure pour " + villager.getId() + ": " + e.getMessage());
            }
        }
        
        return armorFixed;
    }
    
    /**
     * Ajuste automatiquement le nombre d'employés pour chaque bâtiment
     */
    private int adjustBuildingEmployees() {
        Collection<BuildingModel> allBuildings = BuildingRepository.getAll();
        int adjustments = 0;
        
        for (BuildingModel building : allBuildings) {
            try {
                int beforeCount = CustomJobAssignmentService.countBuildingEmployees(building);
                CustomJobAssignmentService.adjustBuildingEmployees(building);
                int afterCount = CustomJobAssignmentService.countBuildingEmployees(building);
                
                if (beforeCount != afterCount) {
                    adjustments++;
                }
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[CustomJobMaintenance] Erreur ajustement employés pour " + building.getId() + ": " + e.getMessage());
            }
        }
        
        return adjustments;
    }
    
    /**
     * Nettoie les villageois avec métier custom mais sans bâtiment associé
     */
    private int cleanupOrphanedCustomJobs() {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        int orphansFixed = 0;
        
        for (VillagerModel villager : allVillagers) {
            if (!villager.hasCustomJob()) {
                continue;
            }
            
            try {
                // Vérifier si le bâtiment associé existe encore
                BuildingModel building = BuildingRepository.getBuildingById(villager.getCurrentBuildingId());
                if (building == null) {
                    // Bâtiment n'existe plus : retirer le métier custom
                    CustomJobAssignmentService.removeCustomJobFromVillager(villager);
                    orphansFixed++;
                }
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[CustomJobMaintenance] Erreur nettoyage orphelin pour " + villager.getId() + ": " + e.getMessage());
            }
        }
        
        return orphansFixed;
    }
    
    /**
     * Vérifie la cohérence entre les classes sociales et les métiers custom
     * Corrige les incohérences détectées
     */
    private int verifyCustomJobConsistency() {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        int inconsistenciesFixed = 0;
        
        for (VillagerModel villager : allVillagers) {
            try {
                // Cas 1: Villageois avec métier custom mais classe inappropriée
                if (villager.hasCustomJob() && villager.getSocialClassEnum() != SocialClass.OUVRIERE) {
                    // Corriger la classe sociale
                    villager.setSocialClassEnum(SocialClass.OUVRIERE);
                    VillagerRepository.update(villager);
                    
                    // Mettre à jour le nom du villageois
                    SocialClassService.updateVillagerDisplayName(villager);
                    
                    inconsistenciesFixed++;
                }
                
                // Cas 2: Villageois classe ouvrière sans aucun métier (ni natif ni custom)
                else if (villager.getSocialClassEnum() == SocialClass.OUVRIERE && !villager.hasJob()) {
                    // Vérifier si il a vraiment un métier natif
                    Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villager.getId());
                    if (entity != null && entity.getProfession() == Villager.Profession.NONE) {
                        SocialClassService.demoteToInactiveOnJobLoss(villager);
                        inconsistenciesFixed++;
                    }
                }
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[CustomJobMaintenance] Erreur vérification cohérence pour " + villager.getId() + ": " + e.getMessage());
            }
        }
        
        return inconsistenciesFixed;
    }
}
