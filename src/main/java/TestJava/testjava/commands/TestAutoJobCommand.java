package TestJava.testjava.commands;

import TestJava.testjava.services.AutomaticJobAssignmentService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande de test pour déclencher manuellement l'assignation automatique d'emplois
 * Usage: /testautojob
 */
public class TestAutoJobCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "❌ Cette commande est réservée aux joueurs");
            return true;
        }
        
        Player player = (Player) sender;
        
        player.sendMessage(ChatColor.YELLOW + "🔧 Déclenchement manuel de l'assignation automatique d'emplois...");
        
        // Exécuter l'assignation automatique
        AutomaticJobAssignmentService.executeAutomaticJobAssignment();
        
        player.sendMessage(ChatColor.GREEN + "✅ Assignation automatique d'emplois terminée. Consultez les logs du serveur pour les détails.");
        
        return true;
    }
}
