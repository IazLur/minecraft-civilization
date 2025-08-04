package TestJava.testjava.commands;

import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.helpers.JuridictionHelper;
import TestJava.testjava.helpers.ResourceHelper;
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

        // Vérification des arguments
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /marketprice <buy|sell> <resourceName>");
            return true;
        }

        boolean isBuy = args[0].equals("buy");
        boolean isSell = args[0].equals("sell");

        if (!isBuy && !isSell) {
            sender.sendMessage(ChatColor.RED + "Le premier argument doit être 'buy' ou 'sell'");
            return false;
        }

        // Recherche intelligente avec suggestions
        ResourceHelper.ResourceSearchResult searchResult = ResourceHelper.findResourceWithSuggestions(args[1]);
        
        if (!searchResult.isFound()) {
            sender.sendMessage(ResourceHelper.formatResourceNotFoundMessage(args[1], searchResult.getSuggestions()));
            sender.sendMessage(ResourceHelper.formatAllResourcesList());
            return true; // ✅ Éviter l'affichage d'usage automatique
        }
        
        ResourceModel match = searchResult.getResource();

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
