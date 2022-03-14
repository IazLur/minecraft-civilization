package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.EatableModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EatableRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Villager;

import java.util.*;

public class VillagerGoEatThread implements Runnable {
    @Override
    public void run() {
        Collection<EatableModel> eatables = EatableRepository.getAll();
        HashMap<String, Collection<EatableModel>> map = new HashMap<>();
        String jxQuery = String.format("/.[food<'%s']", "20");
        Collection<VillagerModel> villagers = TestJava.database.find(jxQuery, VillagerModel.class);
        for (EatableModel eatable : eatables) {
            if (!map.containsKey(eatable.getVillage()))
                map.put(eatable.getVillage(), new ArrayList<>());
            map.get(eatable.getVillage()).add(eatable);
        }
        for (VillagerModel villager : villagers) {
            Villager eVillager = (Villager) TestJava.world.getEntity(villager.getId());
            if (eVillager == null) {
                continue;
            }
            if (!map.containsKey(villager.getVillageName())
                    || map.get(villager.getVillageName()).size() == 0) {
                Bukkit.getServer().broadcastMessage(Colorize.name(eVillager.getCustomName()) + " n'a rien à manger");
                continue;
            }
            EatableModel first = null;
            Location loc = null;
            VillageModel village = VillageRepository.get(villager.getVillageName());
            UUID uuid = UUID.randomUUID();
            Block block = null;
            for (int a = 0; a < map.get(villager.getVillageName()).size(); a++) {
                first = (EatableModel) map.get(villager.getVillageName()).toArray()[a];
                loc = new Location(eVillager.getWorld(), first.getX(), first.getY(), first.getZ());
                block = loc.getBlock();
                if (block.getBlockData() instanceof Ageable age &&
                        age.getMaximumAge() == age.getAge()) {
                    break;
                }
            }
            if (first == null)
                return;
            map.get(villager.getVillageName()).remove(first);
            if (eVillager.isSleeping())
                eVillager.wakeup();
            Random rand = new Random();
            int delay = rand.nextInt(20 * 25);
            delay += 20 * 5;
            Location finalLoc = loc;
            EatableModel finalFirst = first;
            Block finalBlock = block;
            TestJava.threads.put(uuid,
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin,
                            () -> {
                                if (eVillager.isSleeping())
                                    eVillager.wakeup();
                                eVillager.getPathfinder().moveTo(finalLoc);
                                if (!(finalBlock.getBlockData() instanceof Ageable age)
                                        || age.getAge() != age.getMaximumAge()) {
                                    EatableRepository.remove(finalFirst);
                                    Bukkit.getServer().broadcastMessage(Colorize.name(eVillager.getCustomName())
                                            + " n'a rien à manger");
                                    Bukkit.getScheduler().cancelTask(TestJava.threads.get(uuid));
                                    TestJava.threads.remove(uuid);
                                }
                                if (eVillager.getLocation().distance(finalLoc) <= 2) {
                                    System.out.println(eVillager.getCustomName() + " a mangé");
                                    Ageable age = (Ageable) finalBlock.getBlockData();
                                    age.setAge(1);
                                    finalBlock.setBlockData(age);
                                    villager.setFood(villager.getFood() + 1);
                                    VillagerRepository.update(villager);
                                    EatableRepository.remove(finalFirst);
                                    if (villager.getFood() >= 19) {
                                        village.setProsperityPoints(village.getProsperityPoints() + 1);
                                    }
                                    Bukkit.getScheduler().cancelTask(TestJava.threads.get(uuid));
                                    TestJava.threads.remove(uuid);
                                }
                            }, delay, 20 * 3));
        }
    }
}
