package edouard.testjava.services;

import edouard.testjava.Config;
import edouard.testjava.TestJava;
import edouard.testjava.classes.CustomEntity;
import edouard.testjava.helpers.CustomName;
import edouard.testjava.models.VillageModel;
import edouard.testjava.models.VillagerModel;
import edouard.testjava.repositories.VillageRepository;
import edouard.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class VillagerService {
    public void testIfGrowIsEatable(BlockGrowEvent e) {
        Ageable age = (Ageable) e.getNewState().getBlockData();
        if (
                e.getBlock().getType() == Material.WHEAT_SEEDS ||
                e.getBlock().getType() == Material.BEETROOT_SEEDS ||
                e.getBlock().getType() == Material.CARROTS ||
                e.getBlock().getType() == Material.POTATOES
        ) {
            if (age.getAge() == age.getMaximumAge()) {
                // Block is eatable
                VillageModel nearby = VillageRepository.getNearestOf(e.getBlock().getLocation());
                double distance = VillageRepository.getBellLocation(nearby).distance(e.getBlock().getLocation());
                if(distance > Config.VILLAGE_PROTECTION_RADIUS) {
                    return;
                }
                Collection<CustomEntity> villagers = CustomName.whereVillage(nearby.getId());
                Villager v = null;
                int oldFood = 11;
                for (CustomEntity selecting : villagers) {
                    if (selecting.getEntity() instanceof Villager villager) {
                        VillagerModel current = VillagerRepository.find(villager.getUniqueId());
                        if (current.getFood() < oldFood) {
                            v = (Villager) selecting.getEntity();
                            oldFood = current.getFood();
                        }
                    }
                }
                if (v == null) {
                    return;
                }
                Location newLoc = e.getBlock().getLocation();
                newLoc.setY(newLoc.getY() + 1);
                v.getPathfinder().moveTo(newLoc);
                Villager finalV = v;
                Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin, new BukkitRunnable() {
                    @Override
                    public void run() {
                        int first =
                                finalV.getLocation().getBlockX() +
                                        (finalV.getLocation().getBlockY() - 1) +
                                        finalV.getLocation().getBlockZ();
                        int second =
                                e.getBlock().getLocation().getBlockX() +
                                        e.getBlock().getLocation().getBlockY() +
                                        e.getBlock().getLocation().getBlockZ();
                        if (first == second) {
                            VillagerModel villager = VillagerRepository.find(finalV.getUniqueId());
                            if (villager.getFood() < 10) {
                                age.setAge(1);
                                e.getBlock().setBlockData(age);
                                System.out.println("Un villageois a mangé");

                                // Increment food
                                villager.setFood(villager.getFood() + 1);
                                VillagerRepository.update(villager);
                            }
                            this.cancel();
                        }
                    }
                }, 0, 20);
            }
        }
    }
}