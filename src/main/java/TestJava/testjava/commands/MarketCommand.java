package TestJava.testjava.commands;

import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.helpers.JuridictionHelper;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.ResourceModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.ResourceRepository;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
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

        for (ResourceModel resource : resources) {
            if (resource.getName().toLowerCase().contains(args[1].toLowerCase())) {
                match = resource;
                break;
            }
        }

        int quantity = Integer.parseInt(args[2]);
        EmpireModel empire = EmpireRepository.getForPlayer(Objects.requireNonNull(((Player) sender).getPlayer()).getName());
        if (empire == null) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas d'empire");
            return false;
        }

        if (match == null) {
            sender.sendMessage(ChatColor.RED + "Impossible de trouver la resource '" + args[1] + "'");
            return false;
        }

        if (isBuy) {
            float buyPrice = JuridictionHelper.calculatePriceForSell(match) * quantity;
            buyPrice = Math.round(buyPrice * 100.0f) / 100.0f;
            if (empire.getJuridictionCount() < buyPrice) {
                sender.sendMessage(ChatColor.RED + "Vous n'avez pas assez d'argent (acheter " + buyPrice + " avec " + Colorize.name(empire.getJuridictionCount() + "µ") + ")");
                return false;
            }
            empire.setJuridictionCount(empire.getJuridictionCount() - buyPrice);
            EmpireRepository.update(empire);
            match.setQuantity(match.getQuantity() - quantity);
            ResourceRepository.update(match);
            ((Player) sender).getPlayer().getInventory().addItem(new ItemStack(Objects.requireNonNull(Material.matchMaterial(match.getName())), quantity));
            sender.sendMessage(Colorize.name("La banque mondiale") + " vous a vendu " + Colorize.name(match.getName().toLowerCase()) + " pour "
                    + Colorize.name(buyPrice + "µ") + ", vous avez " + Colorize.name(empire.getJuridictionCount() + "µ"));
        }

        if (isSell) {
            float sellPrice = JuridictionHelper.calculatePriceForBuy(match.getName()) * quantity;
            sellPrice = Math.round(sellPrice * 100.0f) / 100.0f;

            Material material = Material.matchMaterial(match.getName());

            Inventory inv = ((Player) sender).getPlayer().getInventory();
            int totalQuantity = 0;

            // Parcourir l'inventaire pour compter les articles
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() == material) {
                    totalQuantity += item.getAmount();
                }
            }

            if (totalQuantity < quantity) {
                sender.sendMessage(ChatColor.RED + "Vous n'avez pas " + quantity + " " + match.getName().toLowerCase() + " en main");
                return false;
            }

            inv.remove(material);

            // Donner le total - quantityToRemove au joueur
            int newQuantity = totalQuantity - quantity;
            if (newQuantity > 0) {
                inv.addItem(new ItemStack(material, newQuantity));
            }

            empire.setJuridictionCount(empire.getJuridictionCount() + sellPrice);
            EmpireRepository.update(empire);
            match.setQuantity(match.getQuantity() + quantity);
            ResourceRepository.update(match);
            sender.sendMessage(Colorize.name("La banque mondiale") + " vous a acheté " + Colorize.name(match.getName().toLowerCase()) + " pour "
                    + Colorize.name(sellPrice + "µ") + ", il vous reste " + Colorize.name(empire.getJuridictionCount() + "µ"));
        }

        return true;
    }
}
