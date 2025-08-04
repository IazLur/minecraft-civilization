package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.models.BuildingDistanceConfig;
import TestJava.testjava.models.JobDistanceConfig;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Service pour valider les distances de placement des métiers et bâtiments
 */
public class DistanceValidationService {

    /**
     * Résultat de la validation de distance
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final double currentDistance;
        private final int minDistance;
        private final int maxDistance;

        public ValidationResult(boolean valid, String message, double currentDistance, int minDistance, int maxDistance) {
            this.valid = valid;
            this.message = message;
            this.currentDistance = currentDistance;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public double getCurrentDistance() { return currentDistance; }
        public int getMinDistance() { return minDistance; }
        public int getMaxDistance() { return maxDistance; }
    }

    /**
     * Valide la distance pour la pose d'un bloc de métier
     */
    public static ValidationResult validateJobBlockPlacement(Player player, Location blockLocation, Material material) {
        JobDistanceConfig config = DistanceConfigService.getJobConfig(material);
        if (config == null) {
            return new ValidationResult(true, "Bloc non configuré - placement autorisé", 0, 0, 0);
        }

        VillageModel village = VillageRepository.getNearestVillageOfPlayer(player.getName(), Config.VILLAGE_PROTECTION_RADIUS);
        if (village == null) {
            return new ValidationResult(false, 
                ChatColor.RED + "Vous devez être dans votre village pour placer ce bloc de métier !", 
                0, config.getDistanceMin(), config.getDistanceMax());
        }

        return validateDistanceToVillageCenter(blockLocation, village, config.getDistanceMin(), 
                                             config.getDistanceMax(), config.getJobName());
    }

    /**
     * Valide la distance pour la construction d'un bâtiment
     */
    public static ValidationResult validateBuildingPlacement(Player player, Location buildLocation, String buildingType) {
        BuildingDistanceConfig config = DistanceConfigService.getBuildingConfig(buildingType);
        if (config == null) {
            return new ValidationResult(true, "Bâtiment non configuré - construction autorisée", 0, 0, 0);
        }

        VillageModel village = VillageRepository.getNearestVillageOfPlayer(player.getName(), Config.VILLAGE_CONSTRUCTION_RADIUS);
        if (village == null) {
            return new ValidationResult(false, 
                ChatColor.RED + "Vous devez être dans votre village pour construire ce bâtiment !", 
                0, config.getDistanceMin(), config.getDistanceMax());
        }

        return validateDistanceToVillageCenter(buildLocation, village, config.getDistanceMin(), 
                                             config.getDistanceMax(), config.getBuildingType());
    }

    /**
     * Valide la distance par rapport au centre du village
     */
    private static ValidationResult validateDistanceToVillageCenter(Location targetLocation, VillageModel village, 
                                                                   int minDistance, int maxDistance, String itemName) {
        
        Location villageCenter = new Location(targetLocation.getWorld(), village.getX(), village.getY(), village.getZ());
        double distance = targetLocation.distance(villageCenter);

        if (distance < minDistance) {
            String message = ChatColor.RED + "❌ " + itemName + " trop proche du centre !\n" +
                           ChatColor.YELLOW + "Distance actuelle: " + ChatColor.WHITE + String.format("%.1f", distance) + " blocs\n" +
                           ChatColor.YELLOW + "Distance minimum: " + ChatColor.GREEN + minDistance + " blocs\n" +
                           ChatColor.GRAY + "Éloignez-vous de " + String.format("%.1f", minDistance - distance) + " blocs du centre";
            
            return new ValidationResult(false, message, distance, minDistance, maxDistance);
        }

        if (distance > maxDistance) {
            String message = ChatColor.RED + "❌ " + itemName + " trop loin du centre !\n" +
                           ChatColor.YELLOW + "Distance actuelle: " + ChatColor.WHITE + String.format("%.1f", distance) + " blocs\n" +
                           ChatColor.YELLOW + "Distance maximum: " + ChatColor.RED + maxDistance + " blocs\n" +
                           ChatColor.GRAY + "Rapprochez-vous de " + String.format("%.1f", distance - maxDistance) + " blocs du centre";
            
            return new ValidationResult(false, message, distance, minDistance, maxDistance);
        }

        // Distance valide
        String message = ChatColor.GREEN + "✅ " + itemName + " placé correctement !\n" +
                       ChatColor.YELLOW + "Distance: " + ChatColor.WHITE + String.format("%.1f", distance) + " blocs " +
                       ChatColor.GRAY + "(zone autorisée: " + minDistance + "-" + maxDistance + " blocs)";
        
        return new ValidationResult(true, message, distance, minDistance, maxDistance);
    }

    /**
     * Obtient la distance entre une location et le centre du village le plus proche
     */
    public static double getDistanceToNearestVillageCenter(Location location, String playerName) {
        VillageModel village = VillageRepository.getNearestVillageOfPlayer(playerName, Config.VILLAGE_PROTECTION_RADIUS);
        if (village == null) {
            return -1; // Pas de village
        }

        Location villageCenter = new Location(location.getWorld(), village.getX(), village.getY(), village.getZ());
        return location.distance(villageCenter);
    }

    /**
     * Vérifie si une location est dans la zone de construction d'un village
     */
    public static boolean isInVillageConstructionZone(Location location, String playerName) {
        VillageModel village = VillageRepository.getNearestVillageOfPlayer(playerName, Config.VILLAGE_CONSTRUCTION_RADIUS);
        return village != null;
    }

    /**
     * Obtient le village le plus proche pour la construction
     */
    public static VillageModel getNearestVillageForConstruction(String playerName) {
        return VillageRepository.getNearestVillageOfPlayer(playerName, Config.VILLAGE_CONSTRUCTION_RADIUS);
    }

    /**
     * Formate un message d'information sur les distances autorisées
     */
    public static String formatDistanceInfo(JobDistanceConfig config) {
        return ChatColor.AQUA + "🔧 " + config.getJobName() + "\n" +
               ChatColor.YELLOW + "Matériau: " + ChatColor.WHITE + config.getMaterial() + "\n" +
               ChatColor.YELLOW + "Distance requise: " + ChatColor.GREEN + config.getDistanceMin() + 
               ChatColor.GRAY + " à " + ChatColor.RED + config.getDistanceMax() + ChatColor.GRAY + " blocs du centre\n" +
               ChatColor.GRAY + config.getDescription();
    }

    /**
     * Formate un message d'information sur les distances autorisées pour un bâtiment
     */
    public static String formatDistanceInfo(BuildingDistanceConfig config) {
        return ChatColor.AQUA + "🏗️ " + config.getBuildingType() + "\n" +
               ChatColor.YELLOW + "Distance requise: " + ChatColor.GREEN + config.getDistanceMin() + 
               ChatColor.GRAY + " à " + ChatColor.RED + config.getDistanceMax() + ChatColor.GRAY + " blocs du centre\n" +
               ChatColor.YELLOW + "Coût: " + ChatColor.GOLD + config.getCostToBuild() + "µ\n" +
               ChatColor.GRAY + config.getDescription();
    }
}