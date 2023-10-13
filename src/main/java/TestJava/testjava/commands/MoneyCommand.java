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

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class MoneyCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        boolean isMe = Arrays.stream(args).findAny().isEmpty();

        EmpireModel me = EmpireRepository.getForPlayer(Objects.requireNonNull(((Player) sender).getPlayer()).getName());
        if (me == null) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas d'empire");
            return false;
        }

        if (isMe) {
            sender.sendMessage(Colorize.name(sender.getName() + " a " + Colorize.name(me.getJuridictionCount() + "µ")));
        } else {
            EmpireModel e = EmpireRepository.getForPlayer(args[0]);
            if (e == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' does not exists or haven't an empire");
                return false;
            }
            sender.sendMessage(Colorize.name(args[0]) + " a " + Colorize.name(e.getJuridictionCount() + "µ"));
        }


        return true;
    }
}
