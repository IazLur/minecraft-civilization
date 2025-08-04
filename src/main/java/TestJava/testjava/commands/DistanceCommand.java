package TestJava.testjava.commands;

import TestJava.testjava.models.BuildingDistanceConfig;
import TestJava.testjava.models.JobDistanceConfig;
import TestJava.testjava.services.DistanceConfigService;
import TestJava.testjava.services.DistanceValidationService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class DistanceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seulement un joueur peut utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "metiers":
            case "jobs":
                return handleJobsCommand(player);
            
            case "batiments":
            case "buildings":
                return handleBuildingsCommand(player);
            
            case "check":
                return handleCheckCommand(player, args);
            
            case "reload":
                return handleReloadCommand(player);
            
            case "info":
                return handleInfoCommand(player, args);
            
            default:
                showUsage(player);
                return true;
        }
    }

    private void showUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Commandes Distance ===");
        player.sendMessage(ChatColor.YELLOW + "/distance metiers" + ChatColor.WHITE + " - Liste des métiers et leurs distances");
        player.sendMessage(ChatColor.YELLOW + "/distance batiments" + ChatColor.WHITE + " - Liste des bâtiments et leurs distances");
        player.sendMessage(ChatColor.YELLOW + "/distance check" + ChatColor.WHITE + " - Vérifie votre position actuelle");
        player.sendMessage(ChatColor.YELLOW + "/distance info <type>" + ChatColor.WHITE + " - Info détaillée sur un métier/bâtiment");
        player.sendMessage(ChatColor.YELLOW + "/distance reload" + ChatColor.WHITE + " - Recharge les configurations (admin)");
    }

    private boolean handleJobsCommand(Player player) {
        Map<Material, JobDistanceConfig> jobConfigs = DistanceConfigService.getAllJobConfigs();
        
        if (jobConfigs.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Aucun métier configuré.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Métiers Configurés (" + jobConfigs.size() + ") ===");
        
        for (Map.Entry<Material, JobDistanceConfig> entry : jobConfigs.entrySet()) {
            JobDistanceConfig config = entry.getValue();
            player.sendMessage(ChatColor.AQUA + "🔧 " + config.getJobName() + 
                             ChatColor.GRAY + " (" + config.getMaterial() + ")");
            player.sendMessage(ChatColor.YELLOW + "   Distance: " + ChatColor.GREEN + config.getDistanceMin() + 
                             ChatColor.GRAY + " à " + ChatColor.RED + config.getDistanceMax() + 
                             ChatColor.GRAY + " blocs");
        }
        
        return true;
    }

    private boolean handleBuildingsCommand(Player player) {
        Map<String, BuildingDistanceConfig> buildingConfigs = DistanceConfigService.getAllBuildingConfigs();
        
        if (buildingConfigs.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Aucun bâtiment configuré.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Bâtiments Configurés (" + buildingConfigs.size() + ") ===");
        
        for (Map.Entry<String, BuildingDistanceConfig> entry : buildingConfigs.entrySet()) {
            BuildingDistanceConfig config = entry.getValue();
            player.sendMessage(ChatColor.AQUA + "🏗️ " + config.getBuildingType());
            player.sendMessage(ChatColor.YELLOW + "   Distance: " + ChatColor.GREEN + config.getDistanceMin() + 
                             ChatColor.GRAY + " à " + ChatColor.RED + config.getDistanceMax() + 
                             ChatColor.GRAY + " blocs");
            player.sendMessage(ChatColor.YELLOW + "   Coût: " + ChatColor.GOLD + config.getCostToBuild() + "µ" +
                             ChatColor.GRAY + " (maintenance: " + config.getCostPerDay() + "µ/jour)");
        }
        
        return true;
    }

    private boolean handleCheckCommand(Player player, String[] args) {
        double distance = DistanceValidationService.getDistanceToNearestVillageCenter(player.getLocation(), player.getName());
        
        if (distance < 0) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes pas dans la zone d'un village.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Vérification Position ===");
        player.sendMessage(ChatColor.YELLOW + "Distance du centre: " + ChatColor.WHITE + String.format("%.1f", distance) + " blocs");
        
        // Afficher les métiers/bâtiments possibles à cette distance
        player.sendMessage(ChatColor.GREEN + "Métiers possibles à cette distance:");
        boolean foundJob = false;
        for (Map.Entry<Material, JobDistanceConfig> entry : DistanceConfigService.getAllJobConfigs().entrySet()) {
            JobDistanceConfig config = entry.getValue();
            if (distance >= config.getDistanceMin() && distance <= config.getDistanceMax()) {
                player.sendMessage(ChatColor.AQUA + "  ✅ " + config.getJobName() + 
                                 ChatColor.GRAY + " (" + config.getMaterial() + ")");
                foundJob = true;
            }
        }
        if (!foundJob) {
            player.sendMessage(ChatColor.GRAY + "  Aucun métier possible");
        }

        player.sendMessage(ChatColor.GREEN + "Bâtiments possibles à cette distance:");
        boolean foundBuilding = false;
        for (Map.Entry<String, BuildingDistanceConfig> entry : DistanceConfigService.getAllBuildingConfigs().entrySet()) {
            BuildingDistanceConfig config = entry.getValue();
            if (distance >= config.getDistanceMin() && distance <= config.getDistanceMax()) {
                player.sendMessage(ChatColor.AQUA + "  ✅ " + config.getBuildingType());
                foundBuilding = true;
            }
        }
        if (!foundBuilding) {
            player.sendMessage(ChatColor.GRAY + "  Aucun bâtiment possible");
        }
        
        return true;
    }

    private boolean handleInfoCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /distance info <nom_metier_ou_batiment>");
            return true;
        }

        String itemName = args[1].toLowerCase();
        
        // Chercher dans les métiers d'abord
        for (Map.Entry<Material, JobDistanceConfig> entry : DistanceConfigService.getAllJobConfigs().entrySet()) {
            JobDistanceConfig config = entry.getValue();
            if (config.getJobName().toLowerCase().contains(itemName) || 
                config.getMaterial().toLowerCase().contains(itemName)) {
                player.sendMessage(DistanceValidationService.formatDistanceInfo(config));
                return true;
            }
        }
        
        // Chercher dans les bâtiments
        for (Map.Entry<String, BuildingDistanceConfig> entry : DistanceConfigService.getAllBuildingConfigs().entrySet()) {
            BuildingDistanceConfig config = entry.getValue();
            if (config.getBuildingType().toLowerCase().contains(itemName)) {
                player.sendMessage(DistanceValidationService.formatDistanceInfo(config));
                return true;
            }
        }
        
        player.sendMessage(ChatColor.RED + "Métier ou bâtiment non trouvé: " + itemName);
        return true;
    }

    private boolean handleReloadCommand(Player player) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Vous devez être administrateur pour utiliser cette commande.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Rechargement des configurations...");
        DistanceConfigService.reloadConfigurations();
        player.sendMessage(ChatColor.GREEN + "✅ Configurations rechargées avec succès !");
        
        return true;
    }
}