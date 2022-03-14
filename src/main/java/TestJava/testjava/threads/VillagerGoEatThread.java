package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.EatableModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EatableRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

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
            if (!map.containsKey(villager.getVillageName())
                    || map.get(villager.getVillageName()).size() == 0) {
                Bukkit.getServer().broadcastMessage(Colorize.name(eVillager.getCustomName()) + " n'a rien à manger");
                continue;
            }
            EatableModel first = map.get(villager.getVillageName()).stream().findFirst().get();
            map.get(villager.getVillageName()).remove(first);
            if (eVillager.isSleeping())
                eVillager.wakeup();
            eVillager.getPathfinder().moveTo(first.getId());
            UUID uuid = UUID.randomUUID();
            Block block = first.getId().getBlock();
            TestJava.threads.put(uuid,
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin,
                            () -> {
                                if (!(block.getBlockData() instanceof Ageable age)
                                        || age.getAge() != age.getMaximumAge()) {
                                    Bukkit.getServer().broadcastMessage(Colorize.name(eVillager.getCustomName())
                                            + " n'a rien à manger");
                                    Bukkit.getScheduler().cancelTask(TestJava.threads.get(uuid));
                                    TestJava.threads.remove(uuid);
                                }
                                if (eVillager.getLocation().distance(first.getId()) <= 2) {
                                    Ageable age = (Ageable) block.getBlockData();
                                    age.setAge(1);
                                    block.setBlockData(age);
                                    villager.setFood(villager.getFood() + 1);
                                    VillagerRepository.update(villager);
                                    EatableRepository.remove(first);
                                    Bukkit.getScheduler().cancelTask(TestJava.threads.get(uuid));
                                    TestJava.threads.remove(uuid);
                                }
                            }, 0, 20));
        }
    }
}
