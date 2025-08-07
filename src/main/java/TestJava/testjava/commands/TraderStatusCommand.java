package TestJava.testjava.commands;

import TestJava.testjava.TestJava;
import TestJava.testjava.threads.TraderThread;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.WanderingTrader;

/**
 * Commande pour vérifier l'état des marchands spéciaux
 * Usage: /traderstatus
 */
public class TraderStatusCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande ne peut être exécutée que par un joueur");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Vous devez être administrateur pour utiliser cette commande");
            return true;
        }

        if (TestJava.world == null) {
            player.sendMessage(ChatColor.RED + "Le monde n'est pas disponible");
            return true;
        }

        // Compter les entités dans le monde
        int worldTraders = TestJava.world.getEntitiesByClass(WanderingTrader.class).size();
        int worldLlamas = TestJava.world.getEntitiesByClass(TraderLlama.class).size();
        
        // Obtenir les statistiques du système de tracking
        int trackedTraders = TraderThread.getActiveTraderCount();

        // Afficher les statistiques
        player.sendMessage(ChatColor.GOLD + "=== État des Marchands Spéciaux ===");
        player.sendMessage(ChatColor.WHITE + "Marchands dans le monde: " + ChatColor.YELLOW + worldTraders);
        player.sendMessage(ChatColor.WHITE + "Lamas dans le monde: " + ChatColor.YELLOW + worldLlamas);
        player.sendMessage(ChatColor.WHITE + "Marchands trackés: " + ChatColor.GREEN + trackedTraders);
        
        if (worldTraders > trackedTraders) {
            int orphans = worldTraders - trackedTraders;
            player.sendMessage(ChatColor.RED + "⚠ " + orphans + " marchand(s) orphelin(s) détecté(s)!");
        } else if (worldTraders == 0 && trackedTraders == 0) {
            player.sendMessage(ChatColor.GREEN + "✅ Aucun marchand actif - Système propre");
        } else {
            player.sendMessage(ChatColor.GREEN + "✅ Système de tracking cohérent");
        }

        // Option pour forcer le nettoyage
        if (args.length > 0 && args[0].equalsIgnoreCase("cleanup")) {
            // Utiliser le service de nettoyage
            TestJava.playerService.killAllWanderingTraders();
            player.sendMessage(ChatColor.GREEN + "Nettoyage forcé des marchands effectué!");
        } else if (worldTraders > 0 || trackedTraders > 0) {
            player.sendMessage(ChatColor.GRAY + "Utilisez '/traderstatus cleanup' pour forcer le nettoyage");
        }

        return true;
    }
}
