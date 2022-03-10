package edouard.testjava.services;

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
                e.getBlock().getType() == Material.WHEAT_SEEDS
        ) {
            if (age.getAge() == age.getMaximumAge()) {
                // Block is eatable
                VillageModel nearby = VillageRepository.getNearestOf(e.getBlock().getLocation());
                Collection<CustomEntity> villagers = CustomName.whereVillage(nearby.getId());
                for (CustomEntity villager : villagers) {
                    if (villager.getEntity() instanceof Villager v) {
                        Location newLoc = e.getBlock().getLocation();
                        newLoc.setY(newLoc.getY() + 1);
                        v.getPathfinder().moveTo(newLoc);
                        Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin, new BukkitRunnable() {
                            @Override
                            public void run() {
                                int first =
                                        v.getLocation().getBlockX() +
                                                (v.getLocation().getBlockY() - 1) +
                                                v.getLocation().getBlockZ();
                                int second =
                                        e.getBlock().getLocation().getBlockX() +
                                                e.getBlock().getLocation().getBlockY() +
                                                e.getBlock().getLocation().getBlockZ();
                                if (first == second) {
                                    VillagerModel villager = VillagerRepository.find(v.getUniqueId());
                                    if(villager.getFood() < 10) {
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
    }
}
