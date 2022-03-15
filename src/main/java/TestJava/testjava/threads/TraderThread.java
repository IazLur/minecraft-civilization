package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class TraderThread implements Runnable {

    Material[] buys = new Material[]{
            Material.DIAMOND,
            Material.GOLD_BLOCK,
            Material.IRON_BLOCK,
            Material.TNT,
            Material.COPPER_BLOCK,
    };

    @Override
    public void run() {
        Random rand = new Random();
        Collection<VillageModel> villages = VillageRepository.getAll();

        for (VillageModel village : villages) {
            if (rand.nextInt(45) > 1) {
                continue;
            }
            Bukkit.getServer().broadcastMessage(ChatColor.GRAY + "Un marchand spécial est apparu à " +
                    Colorize.name(village.getId()) + " pour 5 minutes");
            List<MerchantRecipe> trader = new ArrayList<>();
            WanderingTrader wander = TestJava.world.spawn(VillageRepository.getBellLocation(village), WanderingTrader.class);
            TraderLlama llama1 = TestJava.world.spawn(VillageRepository.getBellLocation(village), TraderLlama.class);
            TraderLlama llama2 = TestJava.world.spawn(VillageRepository.getBellLocation(village), TraderLlama.class);
            llama1.setLeashHolder(wander);
            llama2.setLeashHolder(wander);
            MerchantRecipe trade = new MerchantRecipe(new ItemStack(buys[rand.nextInt(buys.length)]), 1);
            ItemStack ing = new ItemStack(Material.EMERALD);
            ing.setAmount(rand.nextInt(5) + 4);
            trade.addIngredient(ing);
            trade.setMaxUses(6);
            trade.setDemand(6);
            trade.setIgnoreDiscounts(true);
            trade.setExperienceReward(false);
            trader.add(trade);
            wander.setRecipes(trader);
            wander.setDespawnDelay(20 * 60 * 5);
            wander.getTrackedPlayers().removeAll(wander.getTrackedPlayers());
            wander.getTrackedPlayers().add(Bukkit.getPlayer(village.getPlayerName()));
        }
    }
}
