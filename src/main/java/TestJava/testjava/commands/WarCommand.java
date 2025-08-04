package TestJava.testjava.commands;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.threads.WarThread;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WarCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        VillageModel village = VillageRepository.getCurrentVillageConstructibleIfOwn((Player) sender);
        EmpireModel empire = EmpireRepository.get(((Player) sender).getName());
        if (village == null) {
            sender.sendMessage(ChatColor.RED + "Vous devez être dans un de vos villages");
            return false;
        }

        if (empire.getIsInWar()) {
            sender.sendMessage(ChatColor.RED + "Vous êtes déjà en guerre");
            return false;
        }

        // Vérification des arguments
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /war <villageName>");
            return true;
        }

        VillageModel enemy = VillageRepository.get(args[0]);
        if (enemy == null) {
            sender.sendMessage(ChatColor.RED + "Ce village est n'existe pas");
            return false;
        }

        empire.setIsInWar(true);
        empire.setEnemyName(enemy.getId());
        EmpireRepository.update(empire);
        Bukkit.getServer().broadcastMessage(Colorize.name(village.getId()) + " a déclaré la guerre à "
                + Colorize.name(enemy.getId()));

        UUID uuid = UUID.randomUUID();
        TestJava.threads.put(uuid,
                Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin,
                        new WarThread(village.getId(), uuid, enemy, empire, ((Player) sender).getName()), 0, 20 * 10));

        return true;
    }
}