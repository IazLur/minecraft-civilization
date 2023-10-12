package TestJava.testjava.commands;

import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.helpers.JuridictionHelper;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.ResourceModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.ResourceRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Objects;

public class MarketCommand implements CommandExecutor {
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
        int quantity = Integer.parseInt(args[1]);
        EmpireModel empire = EmpireRepository.getForPlayer(Objects.requireNonNull(((Player) sender).getPlayer()).getName());
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas d'empire");
            return false;
        }
        ItemStack currentItem = Objects.requireNonNull(((Player) sender).getPlayer()).getItemInHand();

        for (ResourceModel resource : resources) {
            if (resource.getName().toLowerCase().contains(currentItem.getType().name().toLowerCase())) {
                match = resource;
                break;
            }
        }

        if (match == null) {
            sender.sendMessage(ChatColor.RED + "Impossible de trouver la resource '" + currentItem.getType().name().toLowerCase() + "'");
            return false;
        }

        if (isBuy) {
            float buyPrice = JuridictionHelper.calculatePriceForSell(match) * quantity;
            buyPrice = Math.round(buyPrice * 100.0f) / 100.0f;
            if (empire.getJuridictionCount() < buyPrice) {
                sender.sendMessage(ChatColor.RED + "Vous n'avez pas assez d'argent (acheter " + buyPrice + " avec " + empire.getJuridictionCount() + ")");
                return false;
            }
            if (match.getQuantity() - 1 < quantity) {
                sender.sendMessage(ChatColor.RED + "La banque n'a que " + (match.getQuantity() - 1) + " " + match.getName().toLowerCase());
                return false;
            }
            empire.setJuridictionCount(empire.getJuridictionCount() - buyPrice);
            EmpireRepository.update(empire);
            match.setQuantity(match.getQuantity() - quantity);
            ResourceRepository.update(match);
            currentItem.setAmount(currentItem.getAmount() + quantity);
            ((Player) sender).getPlayer().setItemInHand(currentItem);
            sender.sendMessage(Colorize.name("La banque mondiale") + " vous a vendu " + Colorize.name(match.getName().toLowerCase()) + " pour "
                    + Colorize.name(buyPrice + "µ") + ", vous avez " + Colorize.name(empire.getJuridictionCount() + "µ"));
        }

        if (isSell) {
            float sellPrice = JuridictionHelper.calculatePriceForBuy(match.getName()) * quantity;
            sellPrice = Math.round(sellPrice * 100.0f) / 100.0f;
            if (currentItem.getAmount() < quantity) {
                sender.sendMessage(ChatColor.RED + "Vous n'avez pas " + quantity + " " + match.getName().toLowerCase() + " en main");
                return false;
            }
            empire.setJuridictionCount(empire.getJuridictionCount() + sellPrice);
            EmpireRepository.update(empire);
            currentItem.setAmount(currentItem.getAmount() - quantity);
            match.setQuantity(match.getQuantity() + quantity);
            ResourceRepository.update(match);
            ((Player) sender).getPlayer().setItemInHand(currentItem);
            sender.sendMessage(Colorize.name("La banque mondiale") + " vous a acheté " + Colorize.name(match.getName().toLowerCase()) + " pour "
                    + Colorize.name(sellPrice + "µ") + ", il vous reste " + Colorize.name(empire.getJuridictionCount() + "µ"));
        }

        return true;
    }
}
