package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.BuildingDistanceConfig;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.enums.SocialClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour l'assignation automatique d'emplois aux villageois inactifs
 * S'exécute toutes les minutes pour chercher des opportunités d'emploi
 */
public class AutomaticJobAssignmentService {
    
    private static final int MAX_SEARCH_RADIUS = 256; // Rayon max de recherche de villageois
    
    /**
     * Exécute l'assignation automatique d'emplois pour tous les villages
     * Cette méthode est appelée toutes les minutes par le thread
     */
    public static void executeAutomaticJobAssignment() {
        if (TestJava.world == null) {
            Bukkit.getLogger().warning("[AutoJobAssignment] Monde non disponible");
            return;
        }
        
        int totalAssignments = 0;
        int villagesProcessed = 0;
        int villagesWithAssignments = 0;
        
        try {
            // Récupérer tous les villages
            Collection<VillageModel> allVillages = VillageRepository.getAll();
            
            for (VillageModel village : allVillages) {
                int villageAssignments = processVillageForJobAssignment(village);
                if (villageAssignments > 0) {
                    totalAssignments += villageAssignments;
                    villagesWithAssignments++;
                }
                villagesProcessed++;
            }
            
            // Un seul log de résumé
            if (totalAssignments > 0) {
                Bukkit.getLogger().info("[AutoJobAssignment] ✅ Résumé: " + totalAssignments + " emplois assignés dans " + 
                                      villagesWithAssignments + "/" + villagesProcessed + " villages");
                
                // Broadcast informatif
                Bukkit.getServer().broadcastMessage(
                    org.bukkit.ChatColor.GREEN + "💼 " + org.bukkit.ChatColor.AQUA + totalAssignments +
                    org.bukkit.ChatColor.GREEN + " villageois ont trouvé un emploi automatiquement !"
                );
            } else {
                Bukkit.getLogger().info("[AutoJobAssignment] ℹ️ Aucun emploi assigné (vérifié " + villagesProcessed + " villages)");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[AutoJobAssignment] ❌ Erreur lors de l'assignation automatique: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Traite un village spécifique pour l'assignation d'emplois
     * @param village Le village à traiter
     * @return Nombre d'emplois assignés
     */
    private static int processVillageForJobAssignment(VillageModel village) {
        int assignments = 0;
        
        try {
            // 1. Récupérer tous les villageois inactifs du village
            List<VillagerModel> inactiveVillagers = getInactiveVillagersForVillage(village.getId());
            
            if (inactiveVillagers.isEmpty()) {
                return 0; // Pas de villageois inactifs
            }
            
            // 2. Récupérer tous les bâtiments actifs du village
            Collection<BuildingModel> villageBuildings = BuildingRepository.getBuildingsForVillage(village.getId());
            List<BuildingModel> activeBuildings = villageBuildings.stream()
                .filter(BuildingModel::isActive)
                .collect(Collectors.toList());
            
            if (activeBuildings.isEmpty()) {
                return 0; // Pas de bâtiments actifs
            }
            
            // 3. Pour chaque villageois inactif, chercher un emploi disponible
            for (VillagerModel villager : inactiveVillagers) {
                BuildingModel availableJob = findAvailableJobForVillager(villager, activeBuildings);
                
                if (availableJob != null) {
                    if (assignVillagerToBuilding(villager, availableJob)) {
                        assignments++;
                        
                        // Si on a assigné un emploi, on peut arrêter pour ce villageois
                        // (on ne veut pas qu'un villageois prenne plusieurs emplois)
                        continue;
                    }
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AutoJobAssignment] Erreur traitement village " + village.getId() + 
                                      ": " + e.getMessage());
        }
        
        return assignments;
    }
    
    /**
     * Récupère tous les villageois inactifs d'un village
     */
    private static List<VillagerModel> getInactiveVillagersForVillage(String villageName) {
        return VillagerRepository.getAll().stream()
            .filter(villager -> villageName.equals(villager.getVillageName()))
            .filter(villager -> villager.getSocialClassEnum() == SocialClass.INACTIVE)
            .filter(villager -> !villager.hasJob()) // Pas de métier natif ou custom
            .collect(Collectors.toList());
    }
    
    /**
     * Trouve un emploi disponible pour un villageois
     */
    private static BuildingModel findAvailableJobForVillager(VillagerModel villager, List<BuildingModel> buildings) {
        for (BuildingModel building : buildings) {
            if (hasAvailableJobPosition(building)) {
                return building;
            }
        }
        return null;
    }
    
    /**
     * Vérifie si un bâtiment a des postes disponibles
     */
    private static boolean hasAvailableJobPosition(BuildingModel building) {
        // Compter les employés actuels
        int currentEmployees = CustomJobAssignmentService.countBuildingEmployees(building);
        
        // Obtenir la configuration du bâtiment
        BuildingDistanceConfig config = DistanceConfigService.getBuildingConfig(building.getBuildingType());
        if (config == null) {
            return false;
        }
        
        // Calculer le nombre d'employés requis
        int requiredEmployees = building.getLevel();
        if ("bergerie".equals(building.getBuildingType())) {
            requiredEmployees = Math.max(1, building.getLevel());
        }
        
        // Ne pas dépasser le maximum configuré
        requiredEmployees = Math.min(requiredEmployees, config.getNombreEmployesMax());
        
        // Vérifier s'il y a des postes disponibles
        return currentEmployees < requiredEmployees;
    }
    
    /**
     * Assigne un villageois à un bâtiment
     */
    private static boolean assignVillagerToBuilding(VillagerModel villagerModel, BuildingModel building) {
        try {
            // Trouver l'entité villageois dans le monde
            Villager villager = findVillagerEntity(villagerModel.getId());
            if (villager == null) {
                Bukkit.getLogger().warning("[AutoJobAssignment] Villageois introuvable dans le monde: " + villagerModel.getId());
                return false;
            }
            
            // Vérifier que le villageois est toujours inactif et sans emploi
            if (villagerModel.getSocialClassEnum() != SocialClass.INACTIVE || villagerModel.hasJob()) {
                return false;
            }
            
            // Téléporter le villageois près du bâtiment
            Location buildingLocation = new Location(TestJava.world, building.getX(), building.getY() + 1, building.getZ());
            villager.teleport(buildingLocation);
            
            // Arrêter tout pathfinding en cours
            villager.getPathfinder().stopPathfinding();
            
            // Attribuer le métier custom
            villagerModel.assignCustomJob(building.getBuildingType(), building.getId());
            
            // Promotion vers classe ouvrière
            SocialClassService.promoteToWorkerOnJobAssignment(villagerModel);
            
            // Équiper l'armure de cuir
            CustomJobArmorService.equipLeatherArmor(villager);
            villagerModel.setHasLeatherArmor(true);
            
            // Programmer une vérification supplémentaire de l'armure
            CustomJobSynchronizationService.equipArmorOnJobAssignment(villagerModel);
            
            // Sauvegarder
            VillagerRepository.update(villagerModel);
            
            // Message de confirmation
            String villagerName = extractVillagerName(villager);
            Bukkit.getServer().broadcastMessage(
                org.bukkit.ChatColor.AQUA + villagerName + 
                org.bukkit.ChatColor.WHITE + " a trouvé un emploi automatiquement dans la " + 
                org.bukkit.ChatColor.YELLOW + building.getBuildingType() + 
                org.bukkit.ChatColor.WHITE + " !"
            );
            
            return true;
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AutoJobAssignment] Erreur lors de l'assignation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Trouve l'entité villageois dans le monde
     */
    private static Villager findVillagerEntity(java.util.UUID villagerId) {
        for (Entity entity : TestJava.world.getEntities()) {
            if (entity instanceof Villager villager && entity.getUniqueId().equals(villagerId)) {
                return villager;
            }
        }
        return null;
    }
    
    /**
     * Extrait le nom du villageois depuis son nom personnalisé
     */
    private static String extractVillagerName(Villager villager) {
        try {
            if (villager.getCustomName() != null) {
                String customName = villager.getCustomName();
                String cleanName = customName.replaceAll("§[0-9a-fk-or]", ""); // Enlève les codes couleur
                
                // Format: {X} [VillageName] Prénom Nom
                int bracketEnd = cleanName.indexOf(']');
                if (bracketEnd != -1 && bracketEnd + 2 < cleanName.length()) {
                    return cleanName.substring(bracketEnd + 2); // +2 pour "] "
                }
            }
            return "Villageois " + villager.getUniqueId().toString().substring(0, 8);
        } catch (Exception e) {
            return "Villageois Inconnu";
        }
    }
}
