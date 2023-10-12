package TestJava.testjava.commands;

import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.helpers.JuridictionHelper;
import TestJava.testjava.models.ResourceModel;
import TestJava.testjava.repositories.ResourceRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class MarketPriceCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        boolean isBuy = args[0].equals("buy");
        boolean isSell = args[0].equals("sell");

        if (!isBuy && !isSell) {
            sender.sendMessage(ChatColor.RED + "Le premier argument doit être 'buy' ou 'sell'");
            return false;
        }

        Collection<ResourceModel> resources = ResourceRepository.getAll();
        ResourceModel match = null;

        for (ResourceModel resource : resources) {
            if (resource.getName().toLowerCase().contains(args[1].toLowerCase())) {
                match = resource;
                break;
            }
        }

        if (match == null) {
            sender.sendMessage(ChatColor.RED + "Impossible de trouver la resource '" + args[1] + "'");
            return false;
        }

        if (isBuy) {
            float sellPrice = JuridictionHelper.calculatePriceForBuy(match.getName());
            sellPrice = Math.round(sellPrice * 100.0f) / 100.0f;
            sender.sendMessage(Colorize.name("La banque mondiale") + " vend " + Colorize.name(match.getName().toLowerCase()) + " pour "
                    + Colorize.name(sellPrice + "µ"));
        }

        if (isSell) {
            float buyPrice = JuridictionHelper.calculatePriceForSell(match);
            buyPrice = Math.round(buyPrice * 100.0f) / 100.0f;
            sender.sendMessage(Colorize.name("La banque mondiale") + " achète " + Colorize.name(match.getName().toLowerCase()) + " pour "
                    + Colorize.name(buyPrice + "µ"));
        }

        return true;
    }
}
