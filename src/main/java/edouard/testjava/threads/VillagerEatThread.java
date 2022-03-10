package edouard.testjava.threads;

import edouard.testjava.TestJava;
import edouard.testjava.classes.CustomEntity;
import edouard.testjava.helpers.Colorize;
import edouard.testjava.helpers.CustomName;
import edouard.testjava.models.VillageModel;
import edouard.testjava.models.VillagerModel;
import edouard.testjava.repositories.VillageRepository;
import edouard.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.Collection;

public class VillagerEatThread implements Runnable {
    @Override
    public void run() {
        Collection<VillagerModel> villagers = VillagerRepository.getAll();
        villagers.forEach(villagerModel -> {
            villagerModel.setFood(villagerModel.getFood() - 1);
            if(villagerModel.getFood() == 0) {
                ((Villager) TestJava.plugin.getServer().getEntity(villagerModel.getId()))
                        .setHealth(0D);
                TestJava.plugin.getServer().broadcastMessage(
                        ChatColor.GRAY + "La famine sévit à " + Colorize.name(villagerModel.getVillageName())
                );
            }
        });
    }
}
