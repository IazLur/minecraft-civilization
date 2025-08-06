package TestJava.testjava.commands;

import TestJava.testjava.Config;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.BuildingDistanceConfig;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.services.CustomJobAssignmentService;
import TestJava.testjava.services.DistanceConfigService;
import TestJava.testjava.services.DistanceValidationService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class BuildCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seulement un joueur peut utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("Usage: /build <buildingType>");
            return true;
        }

        String buildingType = args[0].toLowerCase();

        // Vérifier si le type de bâtiment est configuré
        if (!DistanceConfigService.isBuildingConfigured(buildingType)) {
            Collection<String> availableTypes = DistanceConfigService.getConfiguredBuildingTypes();
            player.sendMessage(ChatColor.RED + "Type de bâtiment inconnu: " + buildingType);
            player.sendMessage(ChatColor.YELLOW + "Types disponibles: " + ChatColor.WHITE + String.join(", ", availableTypes));
            return true;
        }

        VillageModel village = VillageRepository
                .getNearestVillageOfPlayer(player.getName(), Config.VILLAGE_CONSTRUCTION_RADIUS);

        if (village == null) {
            player.sendMessage("Vous devez être dans votre village pour construire un bâtiment.");
            return true;
        }

        // Valider la distance avant de procéder à la construction
        Location buildLocation = player.getLocation();
        buildLocation.setY(buildLocation.getBlockY() - 1);
        
        DistanceValidationService.ValidationResult validation = 
            DistanceValidationService.validateBuildingPlacement(player, buildLocation, buildingType);
        
        if (!validation.isValid()) {
            player.sendMessage(validation.getMessage());
            return true;
        }

        EmpireModel empire = EmpireRepository.getForPlayer(player.getName());
        if (empire == null) {
            player.sendMessage("Vous n'avez pas d'empire.");
            return true;
        }

        // Récupérer la configuration du bâtiment
        BuildingDistanceConfig config = DistanceConfigService.getBuildingConfig(buildingType);
        if (config == null) {
            player.sendMessage(ChatColor.RED + "Configuration manquante pour le bâtiment: " + buildingType);
            return true;
        }

        // Créer le bâtiment avec les valeurs de configuration
        BuildingModel building = new BuildingModel();
        building.setId(UUID.randomUUID());
        building.setBuildingType(buildingType);
        building.setVillageName(village.getId());
        building.setX(buildLocation.getBlockX());
        building.setY(buildLocation.getBlockY());
        building.setZ(buildLocation.getBlockZ());
        building.setLevel(1);
        building.setActive(true);
        building.setCostToBuild(config.getCostToBuild());
        building.setCostPerDay(config.getCostPerDay());
        building.setCostPerUpgrade(config.getCostPerUpgrade());
        building.setCostUpgradeMultiplier(config.getCostUpgradeMultiplier());

        player.sendMessage(Colorize.name(building.getBuildingType()) + " va vous coûter " + Colorize.name(building.getCostToBuild() + "µ"));

        if (empire.getJuridictionCount() < building.getCostToBuild()) {
            player.sendMessage("Vous n'avez pas assez d'argent pour construire ce bâtiment.");
            return true;
        }

        empire.setJuridictionCount(empire.getJuridictionCount() - building.getCostToBuild());
        EmpireRepository.update(empire);

        BuildingRepository.update(building);
        buildLocation.getBlock().setType(Material.BEDROCK);
        buildLocation.setY(buildLocation.getY() + 1);
        ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(buildLocation, EntityType.ARMOR_STAND);

        // Nom avec statut actif/inactif
        String statusText = building.isActive() ? ChatColor.GREEN + "{actif}" : ChatColor.RED + "{inactif}";
        armorStand.setCustomName(statusText + " " + ChatColor.BLUE + "[" + village.getId() + "] " + ChatColor.WHITE
                + building.getBuildingType());
        armorStand.setVisible(true);
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setVisible(false);
        armorStand.setCustomNameVisible(true);

        Bukkit.getServer().broadcastMessage(Colorize.name(player.getName()) + " a construit le bâtiment " + Colorize.name(building.getBuildingType()));
        player.sendMessage("Bâtiment construit avec succès!");
        
        // Afficher la confirmation de distance
        player.sendMessage(validation.getMessage());
        
        // Attribuer automatiquement des employés au nouveau bâtiment custom
        CustomJobAssignmentService.assignEmployeesToBuilding(building);

        return true;
    }
}