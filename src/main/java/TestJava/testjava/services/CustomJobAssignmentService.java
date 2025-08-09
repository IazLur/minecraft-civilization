package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.BuildingDistanceConfig;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.enums.SocialClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service pour gérer l'attribution automatique des métiers custom aux villageois inactifs
 * quand un nouveau bâtiment custom est construit
 */
@SuppressWarnings("deprecation")
public class CustomJobAssignmentService {
    
    private static final int MAX_SEARCH_RADIUS = 256; // Rayon max de recherche de villageois
    
    /**
     * Trouve et attribue des employés à un nouveau bâtiment custom
     * 
     * @param building Le bâtiment qui a besoin d'employés
     */
    public static void assignEmployeesToBuilding(BuildingModel building) {
        if (TestJava.world == null) {
            Bukkit.getLogger().warning("[CustomJobAssignment] Monde non disponible");
            return;
        }
        
        // Obtenir la configuration du bâtiment
        BuildingDistanceConfig config = DistanceConfigService.getBuildingConfig(building.getBuildingType());
        if (config == null) {
            Bukkit.getLogger().warning("[CustomJobAssignment] Configuration manquante pour " + building.getBuildingType());
            return;
        }
        
        // Pour la bergerie : au moins 1 employé par niveau (mouton)
        int requiredEmployees = building.getLevel();
        if ("bergerie".equals(building.getBuildingType())) {
            requiredEmployees = Math.max(1, building.getLevel());
        }
        
        // Ne pas dépasser le maximum configuré
        requiredEmployees = Math.min(requiredEmployees, config.getNombreEmployesMax());
        
        Bukkit.getLogger().info("[CustomJobAssignment] Attribution de " + requiredEmployees + 
                               " employés pour " + building.getBuildingType() + " " + building.getId());
        
        Location buildingLocation = new Location(TestJava.world, building.getX(), building.getY(), building.getZ());
        
        // 1. Trouver tous les villageois inactifs dans un rayon donné
        List<VillagerCandidate> inactiveVillagers = findInactiveVillagersNearby(buildingLocation);
        
        if (inactiveVillagers.isEmpty()) {
            Bukkit.getLogger().info("[CustomJobAssignment] Aucun villageois inactif trouvé dans un rayon de " + 
                                   MAX_SEARCH_RADIUS + " blocs");
            return;
        }
        
        // 2. Trier par distance (le plus proche en premier)
        inactiveVillagers.sort(Comparator.comparingDouble(VillagerCandidate::getDistance));
        
        // 3. Attribuer les métiers aux villageois les plus proches
        int assignedCount = 0;
        for (VillagerCandidate candidate : inactiveVillagers) {
            if (assignedCount >= requiredEmployees) {
                break;
            }
            
            if (assignCustomJobToVillager(candidate, building, config)) {
                assignedCount++;
            }
        }
        
        Bukkit.getLogger().info("[CustomJobAssignment] ✅ " + assignedCount + "/" + requiredEmployees + 
                               " employés attribués pour " + building.getBuildingType());
    }
    
    /**
     * Trouve tous les villageois inactifs dans un rayon donné
     */
    private static List<VillagerCandidate> findInactiveVillagersNearby(Location center) {
        List<VillagerCandidate> candidates = new ArrayList<>();
        
        for (Entity entity : TestJava.world.getEntities()) {
            if (!(entity instanceof Villager villager)) {
                continue;
            }
            
            // Vérifier la distance
            double distance = entity.getLocation().distance(center);
            if (distance > MAX_SEARCH_RADIUS) {
                continue;
            }
            
            // Vérifier si le villageois est dans la base de données
            VillagerModel villagerModel = VillagerRepository.find(villager.getUniqueId());
            if (villagerModel == null) {
                continue;
            }
            
            // Vérifier si le villageois est inactif, sans métier ET sans métier custom
            if (villagerModel.getSocialClassEnum() == SocialClass.INACTIVE && 
                villager.getProfession() == Villager.Profession.NONE &&
                !villagerModel.hasJob()) {
                
                candidates.add(new VillagerCandidate(villager, villagerModel, distance));
                
                Bukkit.getLogger().info("[CustomJobAssignment] Candidat trouvé: " + villager.getUniqueId() + 
                                       " (distance: " + String.format("%.1f", distance) + ")");
            }
        }
        
        return candidates;
    }
    
    /**
     * Attribue un métier custom à un villageois
     */
    private static boolean assignCustomJobToVillager(VillagerCandidate candidate, BuildingModel building, 
                                                   BuildingDistanceConfig config) {
        Villager villager = candidate.getVillager();
        VillagerModel villagerModel = candidate.getVillagerModel();
        
        try {
            // Téléporter le villageois près du bâtiment
            Location buildingLocation = new Location(TestJava.world, building.getX(), building.getY() + 1, building.getZ());
            villager.teleport(buildingLocation);
            
            // Arrêter tout pathfinding en cours pour éviter le retour au village d'origine
            villager.getPathfinder().stopPathfinding();
            
            // Attribuer le métier custom dans la base de données
            villagerModel.assignCustomJob(building.getBuildingType(), building.getId());
            
            Bukkit.getLogger().info("[CustomJobAssignment] 📋 Métier custom assigné: " + building.getBuildingType() + 
                                   " à villageois " + villager.getUniqueId() + 
                                   " (classe actuelle: " + villagerModel.getSocialClassEnum().getName() + ")");
            
            // Promotion vers classe ouvrière
            SocialClassService.promoteToWorkerOnJobAssignment(villagerModel);
            
            // Équiper l'armure de cuir immédiatement et programmer une vérification
            CustomJobArmorService.equipLeatherArmor(villager);
            villagerModel.setHasLeatherArmor(true);
            
            // Programmer une vérification supplémentaire de l'armure après 2 secondes
            CustomJobSynchronizationService.equipArmorOnJobAssignment(villagerModel);
            
            // Sauvegarder
            VillagerRepository.update(villagerModel);
            // MAJ du nom en temps réel (afficher le métier custom)
            SocialClassService.updateVillagerDisplayName(villagerModel);
            
            // Message de confirmation
            String villagerName = extractVillagerName(villager);
            Bukkit.getServer().broadcastMessage(
                org.bukkit.ChatColor.AQUA + villagerName + 
                org.bukkit.ChatColor.WHITE + " travaille maintenant dans la " + 
                org.bukkit.ChatColor.YELLOW + building.getBuildingType() + 
                org.bukkit.ChatColor.WHITE + " (métier custom)"
            );
            
            Bukkit.getLogger().info("[CustomJobAssignment] ✅ Métier custom attribué: " + 
                                   building.getBuildingType() + " pour " + villager.getUniqueId());
            
            return true;
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobAssignment] Erreur lors de l'attribution: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Retire le métier custom d'un villageois (en cas de perte de classe sociale ou destruction du bâtiment)
     */
    public static void removeCustomJobFromVillager(VillagerModel villagerModel) {
        try {
            // Trouver l'entité villageois
            Villager villager = (Villager) TestJava.plugin.getServer().getEntity(villagerModel.getId());
            if (villager == null) {
                Bukkit.getLogger().warning("[CustomJobAssignment] Villageois introuvable: " + villagerModel.getId());
                return;
            }
            
            Bukkit.getLogger().info("[CustomJobAssignment] 🚫 Retrait métier custom: " + villagerModel.getCurrentJobName() + 
                                   " pour villageois " + villager.getUniqueId() + 
                                   " (classe actuelle: " + villagerModel.getSocialClassEnum().getName() + ")");
            
            // Retirer l'armure de cuir
            if (villagerModel.hasLeatherArmor()) {
                CustomJobArmorService.removeLeatherArmor(villager);
            }
            
            // Nettoyer les données de métier
            villagerModel.clearJob();
            
            // Rétrogradation vers inactive si était ouvrière
            if (villagerModel.getSocialClassEnum() == SocialClass.OUVRIERE) {
                Bukkit.getLogger().info("[CustomJobAssignment] ⬇️ Rétrogradation Ouvrière → Inactive suite perte métier custom");
                SocialClassService.demoteToInactiveOnJobLoss(villagerModel);
            }
            
            // Sauvegarder
            VillagerRepository.update(villagerModel);
            // MAJ du nom en temps réel (métier custom retiré)
            SocialClassService.updateVillagerDisplayName(villagerModel);
            
            String villagerName = extractVillagerName(villager);
            Bukkit.getLogger().info("[CustomJobAssignment] ✅ Métier custom retiré pour " + villagerName);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobAssignment] Erreur lors du retrait du métier: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie et ajuste le nombre d'employés d'un bâtiment selon ses besoins
     */
    public static void adjustBuildingEmployees(BuildingModel building) {
        if (!building.isActive()) {
            // Bâtiment inactif : retirer tous les employés
            removeAllEmployeesFromBuilding(building);
            return;
        }
        
        // Compter les employés actuels
        int currentEmployees = countBuildingEmployees(building);
        
        // Calculer le nombre requis (pour bergerie : 1 par niveau)
        int requiredEmployees = building.getLevel();
        if ("bergerie".equals(building.getBuildingType())) {
            requiredEmployees = Math.max(1, building.getLevel());
        }
        
        BuildingDistanceConfig config = DistanceConfigService.getBuildingConfig(building.getBuildingType());
        if (config != null) {
            requiredEmployees = Math.min(requiredEmployees, config.getNombreEmployesMax());
        }
        
        if (currentEmployees < requiredEmployees) {
            // Besoin de plus d'employés
            Bukkit.getLogger().info("[CustomJobAssignment] Recrutement nécessaire pour " + building.getBuildingType() + 
                                   ": " + currentEmployees + "/" + requiredEmployees);
            assignEmployeesToBuilding(building);
        } else if (currentEmployees > requiredEmployees) {
            // Trop d'employés : licencier les plus récents
            removeExcessEmployees(building, currentEmployees - requiredEmployees);
        }
    }
    
    /**
     * Compte le nombre d'employés actuels d'un bâtiment
     */
    public static int countBuildingEmployees(BuildingModel building) {
        String query = String.format("/.[currentBuildingId='%s']", building.getId().toString());
        return TestJava.database.find(query, VillagerModel.class).size();
    }
    
    /**
     * Retire tous les employés d'un bâtiment
     */
    private static void removeAllEmployeesFromBuilding(BuildingModel building) {
        String query = String.format("/.[currentBuildingId='%s']", building.getId().toString());
        var employees = TestJava.database.find(query, VillagerModel.class);
        
        for (VillagerModel employee : employees) {
            removeCustomJobFromVillager(employee);
        }
        
        Bukkit.getLogger().info("[CustomJobAssignment] Tous les employés retirés du " + building.getBuildingType());
    }
    
    /**
     * Retire un nombre donné d'employés en excès
     */
    private static void removeExcessEmployees(BuildingModel building, int excessCount) {
        String query = String.format("/.[currentBuildingId='%s']", building.getId().toString());
        var employees = TestJava.database.find(query, VillagerModel.class);
        
        int removed = 0;
        for (VillagerModel employee : employees) {
            if (removed >= excessCount) break;
            
            removeCustomJobFromVillager(employee);
            removed++;
        }
        
        Bukkit.getLogger().info("[CustomJobAssignment] " + removed + " employés licenciés du " + building.getBuildingType());
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
    
    /**
     * Classe interne pour stocker les candidats villageois
     */
    private static class VillagerCandidate {
        private final Villager villager;
        private final VillagerModel villagerModel;
        private final double distance;
        
        public VillagerCandidate(Villager villager, VillagerModel villagerModel, double distance) {
            this.villager = villager;
            this.villagerModel = villagerModel;
            this.distance = distance;
        }
        
        public Villager getVillager() { return villager; }
        public VillagerModel getVillagerModel() { return villagerModel; }
        public double getDistance() { return distance; }
    }
}
