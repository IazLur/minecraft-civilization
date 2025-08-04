package TestJava.testjava.threads;

import TestJava.testjava.listeners.SocialClassJobListener;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.SocialClassService;
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
            Bukkit.getLogger().info("[SocialClassEnforcement] Démarrage de la vérification périodique...");
            
            // Enforce les restrictions de métier
            SocialClassJobListener.enforceJobRestrictions();
            
            // Met à jour les noms d'affichage pour tous les villageois
            updateAllVillagerDisplayNames();
            
            // Vérifie la cohérence des classes sociales
            validateSocialClassConsistency();
            
            Bukkit.getLogger().info("[SocialClassEnforcement] Vérification terminée.");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[SocialClassEnforcement] Erreur lors de l'enforcement: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Met à jour les noms d'affichage de tous les villageois
     */
    private void updateAllVillagerDisplayNames() {
        Collection<VillagerModel> villagers = VillagerRepository.getAll();
        int updated = 0;
        
        for (VillagerModel villager : villagers) {
            try {
                SocialClassService.updateVillagerDisplayName(villager);
                updated++;
            } catch (Exception e) {
                Bukkit.getLogger().warning("[SocialClassEnforcement] Erreur mise à jour nom villageois " + 
                                         villager.getId() + ": " + e.getMessage());
            }
        }
        
        if (updated > 0) {
            Bukkit.getLogger().info("[SocialClassEnforcement] " + updated + " noms de villageois mis à jour");
        }
    }
    
    /**
     * Vérifie la cohérence des classes sociales et corrige si nécessaire
     */
    private void validateSocialClassConsistency() {
        Collection<VillagerModel> villagers = VillagerRepository.getAll();
        int corrected = 0;
        
        for (VillagerModel villager : villagers) {
            try {
                // S'assure que tous les villageois ont une classe sociale définie
                if (villager.getSocialClass() == null) {
                    villager.setSocialClass(0); // Misérable par défaut
                    VillagerRepository.update(villager);
                    corrected++;
                    Bukkit.getLogger().info("[SocialClassEnforcement] Classe sociale assignée par défaut à " + villager.getId());
                }
                
                // Réévalue la classe sociale basée sur la nourriture actuelle
                SocialClassService.evaluateAndUpdateSocialClass(villager);
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[SocialClassEnforcement] Erreur validation villageois " + 
                                         villager.getId() + ": " + e.getMessage());
            }
        }
        
        if (corrected > 0) {
            Bukkit.getLogger().info("[SocialClassEnforcement] " + corrected + " classes sociales corrigées");
        }
    }
}