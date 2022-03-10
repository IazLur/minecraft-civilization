package edouard.testjava.commands;

import edouard.testjava.classes.CustomEntity;
import edouard.testjava.helpers.Colorize;
import edouard.testjava.helpers.CustomName;
import edouard.testjava.models.VillageModel;
import edouard.testjava.models.VillagerModel;
import edouard.testjava.repositories.VillageRepository;
import edouard.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class RenameCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        VillageModel village = VillageRepository.getCurrentVillageConstructibleIfOwn((Player) sender);
        VillageModel old = VillageRepository.getCurrentVillageConstructibleIfOwn((Player) sender);
        if (village == null) {
            sender.sendMessage(ChatColor.RED + "Vous devez être dans un de vos villages");
            return false;
        }

        if (Arrays.stream(args).count() > 1) {
            sender.sendMessage(ChatColor.RED + "Le nouveau nom ne doit pas contenir d'espace");
            return false;
        }

        if (VillageRepository.get(args[0]) != null) {
            sender.sendMessage(ChatColor.RED + "Ce nom de village est déjà pris");
            return false;
        }

        village.setId(args[0]);
        VillageRepository.update(village);
        VillageRepository.remove(old);

        Collection<CustomEntity> entities = CustomName.whereVillage(old.getId());
        for (CustomEntity entity : entities) {
            entity.setVillage(village);
        }

        Bukkit.getServer().broadcastMessage(Colorize.name(((Player) sender).getDisplayName()) +
                " a renommé son village " + Colorize.name(old.getId()) + " par " + Colorize.name(args[0]));

        // Updating model
        Collection<VillagerModel> villagers = VillagerRepository.getAll();
        for (VillagerModel villager : villagers) {
            if (Objects.equals(villager.getVillageName(), old.getId())) {
                villager.setVillageName(args[0]);
                VillagerRepository.update(villager);
            }
        }

        return true;
    }
}