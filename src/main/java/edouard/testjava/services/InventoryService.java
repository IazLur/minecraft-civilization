package edouard.testjava.services;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class InventoryService {
    public ItemStack get(Inventory inventory, Material material) {
        HashMap<Integer, ? extends ItemStack> all = inventory.all(material);
        for (ItemStack value : all.values()) {
            return value;
        }
        return null;
    }
}
