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
            "§b" + villagerName + 
            "§f se dirige vers le bloc de métier pour devenir " + 
            "§e" + jobName
        );
        
        // CORRECTION BUG: Forcer l'attribution immédiate du métier
        // Au lieu de laisser le villageois "décider" naturellement, nous forçons l'attribution
        forceJobAssignment(villager, villagerModel, jobBlockType, jobBlockLocation);
    }
    
    /**
     * CORRECTION BUG: Force l'attribution immédiate du métier au villageois
     * Cette méthode résout le problème où le villageois se déplace vers le bloc mais ne prend pas le métier
     */
    private static void forceJobAssignment(Villager villager, VillagerModel villagerModel, 
                                         Material jobBlockType, Location jobBlockLocation) {
        try {
            // Étape 1: Téléporter le villageois près du bloc pour garantir la proximité
            Location targetLocation = jobBlockLocation.clone().add(0.5, 1, 0.5);
            villager.teleport(targetLocation);
            
            Bukkit.getLogger().info("[JobAssignment] 🔧 Téléportation forcée du villageois vers " + 
                                   locationToString(jobBlockLocation));
            
            // Étape 2: Forcer l'attribution du métier avec un délai court
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                try {
                    // Déterminer la profession correspondante au bloc
                    Villager.Profession targetProfession = getProfessionFromJobBlock(jobBlockType);
                    
                    if (targetProfession != null) {
                        // Forcer la profession directement
                        villager.setProfession(targetProfession);
                        
                        Bukkit.getLogger().info("[JobAssignment] ✅ ATTRIBUTION FORCÉE: " + 
                                               villager.getUniqueId() + " → " + targetProfession);
                        
                        // Programmer une vérification finale
                        Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                            verifyFinalJobAssignment(villager, villagerModel, targetProfession);
                        }, 20L); // 1 seconde de délai pour la vérification
                        
                    } else {
                        Bukkit.getLogger().warning("[JobAssignment] ❌ Impossible de déterminer la profession pour " + jobBlockType);
                    }
                    
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[JobAssignment] Erreur lors de l'attribution forcée: " + e.getMessage());
                }
            }, 10L); // 0.5 seconde de délai
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[JobAssignment] Erreur lors de la téléportation: " + e.getMessage());
        }
    }
    
    /**
     * Détermine la profession Minecraft correspondante à un bloc de métier
     */
    private static Villager.Profession getProfessionFromJobBlock(Material blockType) {
        return switch (blockType) {
            case COMPOSTER -> Villager.Profession.FARMER;
            case BLAST_FURNACE -> Villager.Profession.ARMORER;
            case SMOKER -> Villager.Profession.BUTCHER;
            case CARTOGRAPHY_TABLE -> Villager.Profession.CARTOGRAPHER;
            case BREWING_STAND -> Villager.Profession.CLERIC;
            case SMITHING_TABLE -> Villager.Profession.TOOLSMITH;
            case FLETCHING_TABLE -> Villager.Profession.FLETCHER;
            case LOOM -> Villager.Profession.SHEPHERD;
            case STONECUTTER -> Villager.Profession.MASON;
            case CAULDRON -> Villager.Profession.LEATHERWORKER;
            case LECTERN -> Villager.Profession.LIBRARIAN;
            case GRINDSTONE -> Villager.Profession.WEAPONSMITH;
            case BARREL -> Villager.Profession.FISHERMAN;
            default -> null;
        };
    }
    
    /**
     * Vérification finale que le villageois a bien obtenu le métier
     */
    private static void verifyFinalJobAssignment(Villager villager, VillagerModel villagerModel, 
                                               Villager.Profession expectedProfession) {
        try {
            if (villager.getProfession() == expectedProfession) {
                // Succès ! Le SocialClassJobListener va maintenant gérer la promotion à la classe Ouvrière
                Bukkit.getLogger().info("[JobAssignment] ✅ SUCCESS: Villageois " + villager.getUniqueId() + 
                                       " a obtenu le métier " + expectedProfession);
                
                String villagerName = extractVillagerName(villager);
                String jobName = getJobNameFromBlock(getMaterialFromProfession(expectedProfession));
                
                Bukkit.getServer().broadcastMessage(
                    "§a✅ " + villagerName + 
                    "§f est maintenant " + 
                    "§e" + jobName
                );
                
            } else {
                Bukkit.getLogger().warning("[JobAssignment] ❌ ÉCHEC FINAL: Villageois " + villager.getUniqueId() + 
                                         " devrait être " + expectedProfession + " mais est " + villager.getProfession());
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[JobAssignment] Erreur lors de la vérification finale: " + e.getMessage());
        }
    }
    
    /**
     * Obtient le matériau correspondant à une profession (pour les messages)
     */
    private static Material getMaterialFromProfession(Villager.Profession profession) {
        if (profession == Villager.Profession.FARMER) return Material.COMPOSTER;
        if (profession == Villager.Profession.ARMORER) return Material.BLAST_FURNACE;
        if (profession == Villager.Profession.BUTCHER) return Material.SMOKER;
        if (profession == Villager.Profession.CARTOGRAPHER) return Material.CARTOGRAPHY_TABLE;
        if (profession == Villager.Profession.CLERIC) return Material.BREWING_STAND;
        if (profession == Villager.Profession.TOOLSMITH) return Material.SMITHING_TABLE;
        if (profession == Villager.Profession.FLETCHER) return Material.FLETCHING_TABLE;
        if (profession == Villager.Profession.SHEPHERD) return Material.LOOM;
        if (profession == Villager.Profession.MASON) return Material.STONECUTTER;
        if (profession == Villager.Profession.LEATHERWORKER) return Material.CAULDRON;
        if (profession == Villager.Profession.LIBRARIAN) return Material.LECTERN;
        if (profession == Villager.Profession.WEAPONSMITH) return Material.GRINDSTONE;
        if (profession == Villager.Profession.FISHERMAN) return Material.BARREL;
        return Material.STONE;
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