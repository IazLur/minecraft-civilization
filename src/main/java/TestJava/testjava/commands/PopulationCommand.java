package TestJava.testjava.commands;

import TestJava.testjava.services.VillagePopulationCorrectionService;
import TestJava.testjava.services.VillagerInventoryService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande pour gérer les populations de villages
 * Usage: /population [check|fix|stats]
 */
public class PopulationCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "❌ Cette commande est réservée aux joueurs");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /population <check|fix|stats|diagnose> [village]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "check":
                VillagePopulationCorrectionService.displayPopulationStatistics();
                player.sendMessage(ChatColor.GREEN + "✅ Vérification des populations terminée. Consultez les logs du serveur.");
                break;

            case "fix":
                int corrected = VillagePopulationCorrectionService.correctAllVillagePopulations();
                player.sendMessage(ChatColor.GREEN + "✅ Correction terminée: " + corrected + " villages corrigés.");
                break;

            case "stats":
                VillagePopulationCorrectionService.displayPopulationStatistics();
                player.sendMessage(ChatColor.GREEN + "✅ Statistiques affichées dans les logs du serveur.");
                break;
                
            case "diagnose":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /population diagnose <village>");
                    return true;
                }
                String villageName = args[1];
                TestJava.testjava.services.VillagerInventoryService.diagnoseVillageVillagers(villageName);
                player.sendMessage(ChatColor.GREEN + "✅ Diagnostic du village " + villageName + " terminé. Consultez les logs du serveur.");
                break;

            default:
                player.sendMessage(ChatColor.RED + "❌ Commande inconnue. Usage: /population <check|fix|stats|diagnose>");
                break;
        }

        return true;
    }
    
    private void showUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Commande Population ===");
        player.sendMessage(ChatColor.YELLOW + "/population check" + ChatColor.WHITE + " - Vérifier les populations");
        player.sendMessage(ChatColor.YELLOW + "/population fix" + ChatColor.WHITE + " - Corriger les populations");
        player.sendMessage(ChatColor.YELLOW + "/population stats" + ChatColor.WHITE + " - Afficher les statistiques");
    }
    
    private boolean handleCheckCommand(Player player) {
        player.sendMessage(ChatColor.AQUA + "🔍 Vérification des populations de villages...");
        
        // Affiche les statistiques dans les logs
        VillagePopulationCorrectionService.displayPopulationStatistics();
        
        player.sendMessage(ChatColor.GREEN + "✅ Vérification terminée. Consultez les logs du serveur pour les détails.");
        return true;
    }
    
    private boolean handleFixCommand(Player player) {
        player.sendMessage(ChatColor.YELLOW + "🔧 Démarrage de la correction des populations...");
        
        int correctedVillages = VillagePopulationCorrectionService.correctAllVillagePopulations();
        
        if (correctedVillages > 0) {
            player.sendMessage(ChatColor.GREEN + "✅ Correction terminée: " + ChatColor.YELLOW + correctedVillages + 
                ChatColor.GREEN + " villages ont été corrigés");
        } else {
            player.sendMessage(ChatColor.GREEN + "✅ Aucun village nécessite une correction");
        }
        
        return true;
    }
    
    private boolean handleStatsCommand(Player player) {
        player.sendMessage(ChatColor.AQUA + "📊 Affichage des statistiques de population...");
        
        // Affiche les statistiques dans les logs
        VillagePopulationCorrectionService.displayPopulationStatistics();
        
        player.sendMessage(ChatColor.GREEN + "✅ Statistiques affichées dans les logs du serveur");
        return true;
    }
}
