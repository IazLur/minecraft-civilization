package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Villager;

import java.util.UUID;

import static org.bukkit.Bukkit.getScheduler;

public class VillagerGoEatThread implements Runnable {

    private final Villager finalV;
    private final Block block;
    private Ageable age;
    private UUID uniq;

    public VillagerGoEatThread(Villager finalV, Block block, Ageable age, UUID uniq) {
        this.finalV = finalV;
        this.block = block;
        this.age = age;
        this.uniq = uniq;
    }

    @Override
    public void run() {
        this.age = (Ageable) block.getBlockData();
        if (age.getAge() != age.getMaximumAge()) {
            getScheduler().cancelTask(TestJava.threads.get(uniq));
            TestJava.threads.remove(uniq);
            return;
        }
        VillagerModel villager = VillagerRepository.find(finalV.getUniqueId());
        if(finalV.isDead() || villager == null || villager.getFood() >= 20) {
            getScheduler().cancelTask(TestJava.threads.get(uniq));
            TestJava.threads.remove(uniq);
            return;
        }
        if (finalV.getLocation().distance(block.getLocation()) <= 2) {
            age.setAge(1);
            block.setBlockData(age);

            // Increment food
            villager.setFood(villager.getFood() + 1);
            VillagerRepository.update(villager);

            getScheduler().cancelTask(TestJava.threads.get(uniq));
            TestJava.threads.remove(uniq);
        } else {
            if (finalV.isSleeping()) {
                finalV.wakeup();
            }
            finalV.getPathfinder().moveTo(block.getLocation());
        }
    }

    public UUID getUniq() {
        return uniq;
    }

    public void setUniq(UUID uniq) {
        this.uniq = uniq;
    }
}
