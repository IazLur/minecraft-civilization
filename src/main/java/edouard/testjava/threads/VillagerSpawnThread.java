package edouard.testjava.threads;

import edouard.testjava.TestJava;
import edouard.testjava.models.EmpireModel;
import edouard.testjava.models.VillageModel;
import edouard.testjava.repositories.EmpireRepository;
import edouard.testjava.repositories.VillageRepository;
import org.bukkit.Location;
import org.bukkit.entity.Villager;

import java.util.Collection;
import java.util.Random;


public class VillagerSpawnThread implements Runnable {
    @Override
    public void run() {
        Random rand = new Random();
        Collection<VillageModel> all = VillageRepository.getAll();
        for (VillageModel village : all) {
            EmpireModel empire = EmpireRepository.get(village.getPlayerName());
            Location bell = VillageRepository.getBellLocation(village);
            // Spawn villager
            if (rand.nextInt(60 * 30) == 1 &&
                    village.getPopulation() < village.getBedsCount()) {
                village.setPopulation(village.getPopulation() + 1);
                VillageRepository.update(village);
                bell.setY(bell.getY() + 1);
                TestJava.world.spawn(bell, Villager.class);
            }
        }
    }
}
