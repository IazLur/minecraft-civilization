package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.ChatColor;
import org.bukkit.entity.Villager;

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
