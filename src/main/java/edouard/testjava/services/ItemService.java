package edouard.testjava.services;

import org.bukkit.event.entity.ItemSpawnEvent;

import java.util.Random;

public class ItemService {
    public void reduceSpawnChanceIfSapling(ItemSpawnEvent e) {
        Random rand = new Random();
        if (e.getEntity().getType().name().contains("SAPLING") &&
                rand.nextInt(100) < 80) {
            e.setCancelled(true);
        }
    }
}
