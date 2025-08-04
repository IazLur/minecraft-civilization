package TestJava.testjava.commands;

import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VillageCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        // Vérification des arguments
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /village <villageName>");
            return true;
        }

        VillageModel village = VillageRepository.get(args[0]);
        if (village == null) {
            sender.sendMessage(ChatColor.RED + "Ce village n'existe pas");
            return false;
        }
        sender.sendMessage(
                ChatColor.GOLD + village.getId() + "\n" +
                        ChatColor.DARK_PURPLE + "Lits: " + ChatColor.GRAY + village.getBedsCount() + "\n" +
                        ChatColor.DARK_PURPLE + "Armée: " + ChatColor.GRAY + village.getGroundArmy() + "\n" +
                        ChatColor.DARK_PURPLE + "Garnison: " + ChatColor.GRAY + village.getGarrison() + "\n" +
                        ChatColor.DARK_PURPLE + "Population: " + ChatColor.GRAY + village.getPopulation() + "\n" +
                        ChatColor.DARK_PURPLE + "PDP: " + ChatColor.GRAY + village.getProsperityPoints()
        );
        return true;
    }
}