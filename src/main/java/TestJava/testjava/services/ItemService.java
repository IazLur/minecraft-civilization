package TestJava.testjava.services;

import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.SocialClassService;
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

        Integer foodValue = map.get(item.getType().name());
        VillagerModel villagerModel = VillagerRepository.find(villager.getUniqueId());
        
        // CORRECTION BUG: Limiter la nourriture à 20 max et garder le surplus
        int currentFood = villagerModel.getFood();
        int maxFood = 20;
        int totalItemAmount = item.getAmount();
        int itemsNeeded = Math.max(0, (maxFood - currentFood + foodValue - 1) / foodValue); // Calcul arrondi vers le haut
        int itemsToConsume = Math.min(itemsNeeded, totalItemAmount);
        int surplus = totalItemAmount - itemsToConsume;
        
        if (currentFood >= maxFood) {
            // Villageois rassasié, garde tout dans l'inventaire
            Bukkit.getServer().broadcastMessage(Colorize.name(villager.getCustomName()) + " est rassasié et garde la nourriture");
            return; // Ne pas consommer, laisser dans l'inventaire
        }
        
        // Consommer seulement ce qui est nécessaire
        int foodToAdd = itemsToConsume * foodValue;
        int newFoodTotal = Math.min(currentFood + foodToAdd, maxFood);
        
        villagerModel.setFood(newFoodTotal);
        VillagerRepository.update(villagerModel);
        
        // Mettre à jour l'item : consommer la quantité nécessaire, garder le surplus
        if (surplus > 0) {
            item.setAmount(surplus);
            Bukkit.getServer().broadcastMessage(Colorize.name(villager.getCustomName()) + " a mangé " + 
                                              itemsToConsume + " " + item.getType().name().toLowerCase() + 
                                              " et garde " + surplus + " en réserve");
        } else {
            item.setAmount(0);
            Bukkit.getServer().broadcastMessage(Colorize.name(villager.getCustomName()) + " a été nourri");
        }
        
        // Évaluation de la classe sociale après l'alimentation
        SocialClassService.evaluateAndUpdateSocialClass(villagerModel);
    }
}
