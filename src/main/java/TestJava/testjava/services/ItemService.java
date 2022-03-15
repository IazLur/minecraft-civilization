package TestJava.testjava.services;

import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Random;

public class ItemService {
    public void reduceSpawnChanceIfSapling(ItemSpawnEvent e) {
        Random rand = new Random();
        if (e.getEntity().getType().name().contains("SAPLING") &&
                rand.nextInt(100) < 80) {
            e.setCancelled(true);
        }
    }

    public void testIfVillagerPickupFood(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Villager villager)) {
            return;
        }
        ItemStack item = e.getItem().getItemStack();
        if (!item.getType().isEdible()) {
            return;
        }

        HashMap<String, Integer> map = new HashMap<>() {{
            put(Material.BREAD.name(), 3);
            put(Material.CARROTS.name(), 1);
            put(Material.WHEAT.name(), 1);
            put(Material.POTATO.name(), 1);
            put(Material.BEETROOT.name(), 1);
        }};

        if (!map.containsKey(item.getType().name())) {
            return;
        }

        Integer food = map.get(item.getType().name());
        Bukkit.getServer().broadcastMessage(Colorize.name(villager.getCustomName()) + " a été nourri");
        item.setAmount(0);
        VillagerModel villagerModel = VillagerRepository.find(villager.getUniqueId());
        villagerModel.setFood(villagerModel.getFood() + food);
        VillagerRepository.update(villagerModel);
    }
}
