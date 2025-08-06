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
            // 1. Maintenance des armures des employés custom
            maintainCustomJobArmor();
            
            // 2. Ajustement automatique des employés selon les besoins des bâtiments
            adjustBuildingEmployees();
            
            // 3. Nettoyage des incohérences (villageois avec métier custom mais bâtiment inexistant)
            cleanupOrphanedCustomJobs();
            
            // 4. Vérification de la cohérence des classes sociales vs métiers custom
            verifyCustomJobConsistency();
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobMaintenance] Erreur lors de la maintenance: " + e.getMessage());
        }
    }
    
    /**
     * S'assure que tous les employés custom portent leur armure de cuir
     */
    private void maintainCustomJobArmor() {
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
        
        if (armorFixed > 0) {
            Bukkit.getLogger().info("[CustomJobMaintenance] Armures réparées: " + armorFixed + " employés custom");
        }
    }
    
    /**
     * Ajuste automatiquement le nombre d'employés pour chaque bâtiment
     */
    private void adjustBuildingEmployees() {
        Collection<BuildingModel> allBuildings = BuildingRepository.getAll();
        int adjustments = 0;
        
        for (BuildingModel building : allBuildings) {
            try {
                int beforeCount = CustomJobAssignmentService.countBuildingEmployees(building);
                CustomJobAssignmentService.adjustBuildingEmployees(building);
                int afterCount = CustomJobAssignmentService.countBuildingEmployees(building);
                
                if (beforeCount != afterCount) {
                    adjustments++;
                    Bukkit.getLogger().info("[CustomJobMaintenance] Ajustement employés " + building.getBuildingType() + 
                                           ": " + beforeCount + " → " + afterCount);
                }
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[CustomJobMaintenance] Erreur ajustement employés pour " + building.getId() + ": " + e.getMessage());
            }
        }
        
        if (adjustments > 0) {
            Bukkit.getLogger().info("[CustomJobMaintenance] Ajustements d'employés effectués: " + adjustments + " bâtiments");
        }
    }
    
    /**
     * Nettoie les villageois avec métier custom mais sans bâtiment associé
     */
    private void cleanupOrphanedCustomJobs() {
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
                    
                    Bukkit.getLogger().info("[CustomJobMaintenance] Métier custom orphelin retiré: " + villager.getId() + 
                                           " (bâtiment " + villager.getCurrentBuildingId() + " n'existe plus)");
                }
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[CustomJobMaintenance] Erreur nettoyage orphelin pour " + villager.getId() + ": " + e.getMessage());
            }
        }
        
        if (orphansFixed > 0) {
            Bukkit.getLogger().info("[CustomJobMaintenance] Métiers custom orphelins nettoyés: " + orphansFixed + " villageois");
        }
    }
    
    /**
     * Vérifie la cohérence entre les classes sociales et les métiers custom
     * Corrige les incohérences détectées
     */
    private void verifyCustomJobConsistency() {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        int inconsistenciesFixed = 0;
        
        for (VillagerModel villager : allVillagers) {
            try {
                // Cas 1: Villageois avec métier custom mais classe inappropriée
                if (villager.hasCustomJob() && villager.getSocialClassEnum() != SocialClass.OUVRIERE) {
                    Bukkit.getLogger().warning("[CustomJobMaintenance] ⚠️ INCOHÉRENCE DÉTECTÉE: Villageois " + villager.getId() + 
                                             " a métier custom (" + villager.getCurrentJobName() + 
                                             ") mais classe " + villager.getSocialClassEnum().getName() + 
                                             " - CORRECTION AUTOMATIQUE");
                    
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
                        Bukkit.getLogger().warning("[CustomJobMaintenance] ⚠️ INCOHÉRENCE DÉTECTÉE: Villageois " + villager.getId() + 
                                                 " classe Ouvrière mais AUCUN métier (ni natif ni custom) " + 
                                                 " - RÉTROGRADATION vers Inactive");
                        
                        SocialClassService.demoteToInactiveOnJobLoss(villager);
                        inconsistenciesFixed++;
                    }
                }
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[CustomJobMaintenance] Erreur vérification cohérence pour " + villager.getId() + ": " + e.getMessage());
            }
        }
        
        if (inconsistenciesFixed > 0) {
            Bukkit.getLogger().info("[CustomJobMaintenance] ✅ Incohérences corrigées: " + inconsistenciesFixed + " villageois");
        }
    }
}
