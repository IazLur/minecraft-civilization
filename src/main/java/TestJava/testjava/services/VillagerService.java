package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.TestJava;
import TestJava.testjava.classes.CustomEntity;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.EatableModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EatableRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.Collection;
import java.util.UUID;

public class VillagerService {
    public void testIfGrowIsEatable(BlockGrowEvent e) {
        try {
            Ageable age = (Ageable) e.getNewState().getBlockData();
            System.out.println(e.getBlock().getType().name());
            if (
                    e.getBlock().getType() == Material.WHEAT ||
                            e.getBlock().getType() == Material.BEETROOTS ||
                            e.getBlock().getType() == Material.CARROTS ||
                            e.getBlock().getType() == Material.POTATOES
            ) {
                if (age.getAge() == age.getMaximumAge()) {
                    // Block is eatable
                    VillageModel nearby = VillageRepository.getNearestOf(e.getBlock().getLocation());
                    double distance = VillageRepository.getBellLocation(nearby).distance(e.getBlock().getLocation());
                    if (distance > Config.VILLAGE_PROTECTION_RADIUS) {
                        return;
                    }
                    EatableModel eatable = new EatableModel();
                    eatable.setId(e.getBlock().getLocation());
                    eatable.setVillage(nearby.getId());
                    EatableRepository.update(eatable);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}