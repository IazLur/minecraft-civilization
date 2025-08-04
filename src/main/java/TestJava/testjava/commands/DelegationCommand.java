package TestJava.testjava.commands;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class DelegationCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        // Vérification des arguments
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /delegation <pseudo>");
            return true;
        }

        Player receiver = Bukkit.getPlayer(args[0]);
        Player player = ((Player) sender);
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Le joueur " + args[0] + " n'est pas connecté ou n'existe pas");
            return false;
        }

        VillageModel village = VillageRepository.getCurrentVillageConstructibleIfOwn(player);
        if (village == null) {
            sender.sendMessage(ChatColor.RED + "Vous devez être dans un de vos villages");
            return false;
        }

        ItemStack book = TestJava.inventoryService.get(player.getInventory(), Material.WRITTEN_BOOK);
        if(book == null) {
            sender.sendMessage(ChatColor.RED + "Vous devez avoir une lettre écrite dans votre inventaire");
            return false;
        }

        Animals delegator = player.getWorld().spawn(VillageRepository.getBellLocation(village), Llama.class);
        delegator.setRemoveWhenFarAway(false);
        delegator.setCustomNameVisible(true);
        delegator.setCustomName("[" + player.getName() + "] Délégateur");
        delegator.setCanPickupItems(false);
        delegator.damage(0D, receiver);
        delegator.setVelocity(new Vector());
        delegator.setTarget(receiver);
        InventoryHolder holder = (InventoryHolder) delegator;
        holder.getInventory().addItem(book.clone());
        book.setAmount(0);

        try {
            delegator.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(1D);
            delegator.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1D);
        } catch (NullPointerException ignored) {
        }

        return true;
    }
}