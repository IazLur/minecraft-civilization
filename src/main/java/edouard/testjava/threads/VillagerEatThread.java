package edouard.testjava.threads;

import edouard.testjava.classes.CustomEntity;
import edouard.testjava.helpers.CustomName;
import edouard.testjava.models.VillageModel;
import edouard.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collection;

public class VillagerEatThread implements Runnable {
    @Override
    public void run() {
        Collection<VillageModel> villages = VillageRepository.getAll();
        for (VillageModel village : villages) {
            Location bell = VillageRepository.getBellLocation(village);
            Collection<Block> eatable = new ArrayList<>();
            Collection<CustomEntity> villagers = CustomName.whereVillage(village.getId());
            for (CustomEntity villager : villagers) {

            }
        }
    }
}
