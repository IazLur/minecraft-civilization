package TestJava.testjava.threads;

import TestJava.testjava.listeners.SocialClassJobListener;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.SocialClassService;
import TestJava.testjava.services.VillagerHomeService;
import org.bukkit.Bukkit;

import java.util.Collection;

/**
 * Thread qui s'exécute périodiquement pour enforcer les règles de classes sociales
 * - Retire les métiers des villageois classe 0
 * - Met à jour les noms d'affichage si nécessaire
 * - Vérifie la cohérence du système
 */
public class SocialClassEnforcementThread implements Runnable {

    @Override
    public void run() {
        try {
            int totalActions = 0;
            int namesUpdated = 0;
            int socialClassesCorrected = 0;
            int homesValidated = 0;
            
            // Enforce les restrictions de métier
            enforceJobRestrictions();
            totalActions += 1; // Une action globale
            
            // CORRECTION BUG: Vérification supplémentaire des villageois misérables avec métier
            int strictRestrictions = enforceStrictJobRestrictions();
            totalActions += strictRestrictions;
            
            // Met à jour les noms d'affichage pour tous les villageois
            namesUpdated = updateAllVillagerDisplayNames();
            totalActions += namesUpdated;
            
            // Vérifie la cohérence des classes sociales
            socialClassesCorrected = validateSocialClassConsistency();
            totalActions += socialClassesCorrected;
            
            // NOUVELLE FONCTIONNALITÉ: Vérification et correction des "Home" des villageois
            homesValidated = validateAndCorrectVillagerHomes();
            totalActions += homesValidated;
            
            // Un seul log de résumé
            if (totalActions > 0) {
                Bukkit.getLogger().info("[SocialClassEnforcement] ✅ Résumé: " + totalActions + " actions effectuées " +
                                       "(restrictions: 1, strict: " + strictRestrictions + 
                                       ", noms: " + namesUpdated + ", classes: " + socialClassesCorrected + 
                                       ", homes: " + homesValidated + ")");
            } else {
                Bukkit.getLogger().info("[SocialClassEnforcement] ℹ️ Aucune action nécessaire");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[SocialClassEnforcement] ❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Met à jour les noms d'affichage de tous les villageois
     */
    private int updateAllVillagerDisplayNames() {
        Collection<VillagerModel> villagers = VillagerRepository.getAll();
        int updated = 0;
        
        for (VillagerModel villager : villagers) {
            try {
                SocialClassService.updateVillagerDisplayName(villager);
                updated++;
            } catch (Exception e) {
                Bukkit.getLogger().warning("[SocialClassEnforcement] Erreur mise à jour nom " + villager.getId() + ": " + e.getMessage());
            }
        }
        
        return updated;
    }
    
    /**
     * Vérifie la cohérence des classes sociales et corrige si nécessaire
     */
    private int validateSocialClassConsistency() {
        Collection<VillagerModel> villagers = VillagerRepository.getAll();
        int corrected = 0;
        
        for (VillagerModel villager : villagers) {
            try {
                // S'assure que tous les villageois ont une classe sociale définie
                if (villager.getSocialClass() == null) {
                    villager.setSocialClass(0); // Misérable par défaut
                    VillagerRepository.update(villager);
                    corrected++;
                }
                
                // Réévalue la classe sociale basée sur la nourriture actuelle
                SocialClassService.evaluateAndUpdateSocialClass(villager);
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[SocialClassEnforcement] Erreur validation villageois " + 
                                         villager.getId() + ": " + e.getMessage());
            }
        }
        
        return corrected;
    }
    
    /**
     * Vérifie et corrige les "Home" des villageois
     */
    private int validateAndCorrectVillagerHomes() {
        try {
            VillagerHomeService.validateAndCorrectAllVillagerHomes();
            return 1; // Une seule action globale
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SocialClassEnforcement] Erreur validation homes: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Enforce les restrictions de métier strictes
     */
    private int enforceStrictJobRestrictions() {
        Collection<VillagerModel> villagers = VillagerRepository.getAll();
        int corrected = 0;
        
        for (VillagerModel villager : villagers) {
            try {
                // Vérifier les villageois misérables avec métier (natifs ET custom)
                if (villager.getSocialClassEnum().getLevel() == 0 && villager.hasJob()) {
                    // Retirer le métier custom s'il en a un
                    if (villager.hasCustomJob()) {
                        villager.setCurrentJobType(null);
                        villager.setCurrentJobName(null);
                        villager.setCurrentBuildingId(null);
                        villager.setHasLeatherArmor(false);
                        VillagerRepository.update(villager);
                        corrected++;
                        Bukkit.getLogger().info("[SocialClassEnforcement] Métier custom retiré pour villageois misérable " + villager.getId());
                    }
                    // Retirer le métier natif s'il en a un
                    else if (villager.hasNativeJob()) {
                        villager.setCurrentJobType(null);
                        villager.setCurrentJobName(null);
                        villager.setCurrentBuildingId(null);
                        villager.setHasLeatherArmor(false);
                        VillagerRepository.update(villager);
                        corrected++;
                        Bukkit.getLogger().info("[SocialClassEnforcement] Métier natif retiré pour villageois misérable " + villager.getId());
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[SocialClassEnforcement] Erreur restriction stricte " + villager.getId() + ": " + e.getMessage());
            }
        }
        
        return corrected;
    }
    
    /**
     * Enforce les restrictions de métier de base
     */
    private void enforceJobRestrictions() {
        SocialClassJobListener.enforceJobRestrictions();
    }
}