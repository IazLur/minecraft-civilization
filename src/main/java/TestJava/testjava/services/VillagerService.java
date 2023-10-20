package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.models.EatableModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.EatableRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.UUID;

public class VillagerService {
    public void testIfGrowIsEatable(BlockGrowEvent e) {
        if (!(e.getNewState().getBlockData() instanceof Ageable age)) {
            return;
        }
        if (e.getNewState().getBlock().getBlockData().getMaterial() == Material.WHEAT) {
            if (age.getAge() == age.getMaximumAge()) {
                // Block is eatable
                VillageModel nearby = VillageRepository.getNearestOf(e.getBlock().getLocation());
                double distance = VillageRepository.getBellLocation(nearby).distance(e.getBlock().getLocation());
                if (distance > Config.VILLAGE_PROTECTION_RADIUS) {
                    return;
                }
                EatableModel eatable = new EatableModel();
                UUID uniq = UUID.randomUUID();
                eatable.setId(uniq);
                eatable.setVillage(nearby.getId());
                eatable.setX(e.getBlock().getX());
                eatable.setY(e.getBlock().getY());
                eatable.setZ(e.getBlock().getZ());
                EatableRepository.update(eatable);
            }
        }
    }
}