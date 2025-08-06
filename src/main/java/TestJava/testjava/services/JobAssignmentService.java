package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.enums.SocialClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service pour gérer l'attribution automatique des métiers aux villageois inactifs
 * quand un nouveau bloc de métier est placé
 */
public class JobAssignmentService {
    
    private static final int MAX_SEARCH_RADIUS = 100; // Rayon max de recherche de villageois
    
    /**
     * Trouve et dirige un villageois inactif vers un nouveau bloc de métier
     * 
     * @param jobBlockLocation Position du bloc de métier
     * @param jobBlockType Type du bloc de métier
     */
    public static void assignJobToNearestInactiveVillager(Location jobBlockLocation, Material jobBlockType) {
        if (TestJava.world == null) {
            Bukkit.getLogger().warning("[JobAssignment] Monde non disponible");
            return;
        }
        
        Bukkit.getLogger().info("[JobAssignment] Recherche villageois inactif pour bloc " + 
                               jobBlockType + " à " + locationToString(jobBlockLocation));
        
        // 1. Trouver tous les villageois inactifs dans un rayon donné
        List<VillagerCandidate> inactiveVillagers = findInactiveVillagersNearby(jobBlockLocation);
        
        if (inactiveVillagers.isEmpty()) {
            Bukkit.getLogger().info("[JobAssignment] Aucun villageois inactif trouvé dans un rayon de " + 
                                   MAX_SEARCH_RADIUS + " blocs");
            return;
        }
        
        // 2. Trier par distance (le plus proche en premier)
        inactiveVillagers.sort(Comparator.comparingDouble(VillagerCandidate::getDistance));
        
        // 3. Prendre le plus proche et lui faire prendre le métier
        VillagerCandidate nearestCandidate = inactiveVillagers.get(0);
        directVillagerToJobBlock(nearestCandidate, jobBlockLocation, jobBlockType);
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
            
            // Vérifier si le villageois est inactif et sans métier
            if (villagerModel.getSocialClassEnum() == SocialClass.INACTIVE && 
                villager.getProfession() == Villager.Profession.NONE) {
                
                candidates.add(new VillagerCandidate(villager, villagerModel, distance));
                
                Bukkit.getLogger().info("[JobAssignment] Candidat trouvé: " + villager.getUniqueId() + 
                                       " (distance: " + String.format("%.1f", distance) + ")");
            }
        }
        
        return candidates;
    }
    
    /**
     * Dirige un villageois vers un bloc de métier pour qu'il le prenne
     */
    private static void directVillagerToJobBlock(VillagerCandidate candidate, Location jobBlockLocation, Material jobBlockType) {
        Villager villager = candidate.getVillager();
        VillagerModel villagerModel = candidate.getVillagerModel();
        
        Bukkit.getLogger().info("[JobAssignment] ✅ Attribution du métier " + jobBlockType + 
                               " au villageois " + villager.getUniqueId() + 
                               " (distance: " + String.format("%.1f", candidate.getDistance()) + ")");
        
        // Broadcast du message
        String villagerName = extractVillagerName(villager);
        String jobName = getJobNameFromBlock(jobBlockType);
        
        Bukkit.getServer().broadcastMessage(
            org.bukkit.ChatColor.AQUA + villagerName + 
            org.bukkit.ChatColor.WHITE + " se dirige vers le bloc de métier pour devenir " + 
            org.bukkit.ChatColor.YELLOW + jobName
        );
        
        // Faire se déplacer le villageois vers le bloc
        try {
            // Déplacer légèrement au-dessus du bloc pour éviter les problèmes de pathfinding
            Location targetLocation = jobBlockLocation.clone().add(0.5, 1, 0.5);
            villager.getPathfinder().moveTo(targetLocation, 1.0); // Vitesse normale
            
            // Programmer une vérification pour s'assurer que le villageois prend bien le métier
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                verifyJobAssignment(villager, villagerModel, jobBlockType, jobBlockLocation);
            }, 100L); // 5 secondes de délai
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[JobAssignment] Erreur lors du déplacement: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie que le villageois a bien pris le métier après le déplacement
     */
    private static void verifyJobAssignment(Villager villager, VillagerModel villagerModel, 
                                          Material jobBlockType, Location jobBlockLocation) {
        try {
            if (villager.getProfession() != Villager.Profession.NONE) {
                // Le villageois a pris le métier avec succès
                Bukkit.getLogger().info("[JobAssignment] ✅ Métier attribué avec succès: " + 
                                       villager.getProfession() + " pour " + villager.getUniqueId());
            } else {
                // Le villageois n'a pas pris le métier, réessayer s'il est encore proche
                double currentDistance = villager.getLocation().distance(jobBlockLocation);
                if (currentDistance <= 5.0) {
                    Bukkit.getLogger().info("[JobAssignment] ⚠️ Réessai d'attribution pour " + villager.getUniqueId());
                    // Réessayer de le diriger vers le bloc
                    Location targetLocation = jobBlockLocation.clone().add(0.5, 1, 0.5);
                    villager.getPathfinder().moveTo(targetLocation, 1.0);
                } else {
                    Bukkit.getLogger().warning("[JobAssignment] ❌ Échec d'attribution pour " + villager.getUniqueId() + 
                                             " (distance: " + String.format("%.1f", currentDistance) + ")");
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[JobAssignment] Erreur lors de la vérification: " + e.getMessage());
        }
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
     * Obtient le nom du métier depuis le type de bloc
     */
    private static String getJobNameFromBlock(Material blockType) {
        return switch (blockType) {
            case COMPOSTER -> "Fermier";
            case BLAST_FURNACE -> "Armurier";
            case SMOKER -> "Boucher";
            case CARTOGRAPHY_TABLE -> "Cartographe";
            case BREWING_STAND -> "Clerc";
            case SMITHING_TABLE -> "Forgeron d'Outils";
            case FLETCHING_TABLE -> "Fabricant d'Arcs";
            case LOOM -> "Berger";
            case STONECUTTER -> "Maçon";
            case CAULDRON -> "Travailleur du Cuir";
            case LECTERN -> "Bibliothécaire";
            case GRINDSTONE -> "Forgeron d'Armes";
            case BARREL -> "Pêcheur";
            default -> "Artisan";
        };
    }
    
    /**
     * Convertit une location en chaîne lisible
     */
    private static String locationToString(Location location) {
        return String.format("(%d, %d, %d)", 
                           location.getBlockX(), 
                           location.getBlockY(), 
                           location.getBlockZ());
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