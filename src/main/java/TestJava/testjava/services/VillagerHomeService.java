package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Villager;

import java.util.Collection;

/**
 * Service pour gérer la vérification et correction des "Home" des villageois
 * Empêche les villageois de retourner automatiquement à leur village d'origine
 */
public class VillagerHomeService {

    /**
     * Vérifie et corrige les "Home" de tous les villageois
     * S'exécute périodiquement dans SocialClassEnforcementThread
     */
    public static void validateAndCorrectAllVillagerHomes() {
        Collection<VillagerModel> villagers = VillagerRepository.getAll();
        int corrected = 0;
        
        for (VillagerModel villagerModel : villagers) {
            try {
                if (validateAndCorrectVillagerHome(villagerModel)) {
                    corrected++;
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[VillagerHome] Erreur validation villageois " + 
                                         villagerModel.getId() + ": " + e.getMessage());
            }
        }
        
        if (corrected > 0) {
            Bukkit.getLogger().info("[VillagerHome] ✅ " + corrected + " villageois corrigés");
        }
    }

    /**
     * Vérifie et corrige le "Home" d'un villageois spécifique
     * @param villagerModel Le modèle du villageois
     * @return true si une correction a été effectuée
     */
    public static boolean validateAndCorrectVillagerHome(VillagerModel villagerModel) {
        // Récupérer l'entité villageois
        Villager villager = (Villager) Bukkit.getEntity(villagerModel.getId());
        if (villager == null) {
            return false; // Villageois fantôme
        }

        // Récupérer le village du villageois
        VillageModel village = VillageRepository.get(villagerModel.getVillageName());
        if (village == null) {
            Bukkit.getLogger().warning("[VillagerHome] Village inexistant pour " + villagerModel.getId());
            return false;
        }

        // Obtenir la position de la cloche du village
        Location bellLocation = VillageRepository.getBellLocation(village);
        
        // Vérifier si le villageois est dans le rayon de protection de son village
        double distanceToBell = villager.getLocation().distance(bellLocation);
        
        if (distanceToBell > Config.VILLAGE_PROTECTION_RADIUS) {
            Bukkit.getLogger().info("[VillagerHome] 🚨 Villageois " + villagerModel.getId() + 
                                   " hors de son village (distance: " + String.format("%.1f", distanceToBell) + 
                                   " > " + Config.VILLAGE_PROTECTION_RADIUS + ")");
            
            // Corriger le "Home" du villageois
            correctVillagerHome(villager, village);
            return true;
        }
        
        return false;
    }

    /**
     * Corrige le "Home" d'un villageois en le forçant à rester dans son village
     * @param villager L'entité villageois
     * @param village Le village d'appartenance
     */
    public static void correctVillagerHome(Villager villager, VillageModel village) {
        try {
            Bukkit.getLogger().info("[VillagerHome] Correction du Home pour " + villager.getUniqueId() + 
                                   " vers " + village.getId());
            
            // Arrêter tous les mouvements en cours
            villager.getPathfinder().stopPathfinding();
            
            // Retirer temporairement la profession pour réinitialiser les données de navigation
            Villager.Profession currentProfession = villager.getProfession();
            villager.setProfession(Villager.Profession.NONE);
            
            // Programmer la restauration de la profession après quelques ticks
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                villager.setProfession(currentProfession);
                Bukkit.getLogger().info("[VillagerHome] ✅ Home corrigé pour " + villager.getUniqueId());
            }, 5L);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VillagerHome] Erreur correction Home: " + e.getMessage());
        }
    }

    /**
     * Réinitialise complètement les données de navigation d'un villageois
     * Utilisé lors de la téléportation pour famine
     * @param villager L'entité villageois
     */
    public static void resetVillagerNavigation(Villager villager) {
        try {
            Bukkit.getLogger().info("[VillagerHome] Réinitialisation navigation pour " + villager.getUniqueId());
            
            // Arrêter tous les mouvements en cours
            villager.getPathfinder().stopPathfinding();
            
            // Retirer la profession temporairement puis la remettre pour réinitialiser
            Villager.Profession currentProfession = villager.getProfession();
            villager.setProfession(Villager.Profession.NONE);
            
            // Programmer la restauration de la profession après quelques ticks
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                villager.setProfession(currentProfession);
                Bukkit.getLogger().info("[VillagerHome] ✅ Navigation réinitialisée pour " + villager.getUniqueId());
            }, 5L);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VillagerHome] Erreur réinitialisation navigation: " + e.getMessage());
        }
    }
}
