package edouard.testjava.services;

import edouard.testjava.TestJava;
import edouard.testjava.helpers.Colorize;
import edouard.testjava.helpers.CustomName;
import edouard.testjava.models.VillageModel;
import edouard.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;

public class EntityService {
    public void testSpawnIfVillager(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Villager)) {
            return;
        }
        Villager villager = (Villager) e.getEntity();
        VillageModel village = VillageRepository.getNearestOf(villager);
        villager.setCustomNameVisible(true);
        assert village != null;
        String customName = CustomName.generate();
        villager.setCustomName(ChatColor.BLUE + "[" + village.getId() + "] " + ChatColor.WHITE
                + customName);
        Bukkit.getServer().broadcastMessage(Colorize.name(customName) + " est né à " + Colorize.name(village.getId()));
    }

    public void testDeathIfVillager(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Villager)) {
            return;
        }
        if (!e.getEntity().isCustomNameVisible()) {
            return;
        }

        VillageModel village = VillageRepository.get(CustomName.squareBrackets(e.getEntity().getCustomName(), 0));
        village.setPopulation(village.getPopulation() - 1);
        VillageRepository.update(village);

        Bukkit.getServer().broadcastMessage(Colorize.name(e.getEntity().getCustomName()) + " est malheureusement décédé");
    }

    public void testIfPlaceDefender(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.IRON_BLOCK) {
            return;
        }
        e.getBlockPlaced().setType(Material.AIR);
        Location location = e.getBlockPlaced().getLocation();
        Skeleton skeleton = TestJava.world.spawn(location, Skeleton.class);
        VillageModel village = VillageRepository.getNearestOf(skeleton);
        skeleton.setCanPickupItems(false);
        skeleton.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0D);
        skeleton.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(10D);
        skeleton.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1D);
        skeleton.setCustomNameVisible(true);
        skeleton.setRemoveWhenFarAway(false);
        assert village != null;
        String customName = CustomName.generate();
        skeleton.setCustomName(ChatColor.BLUE + "[" + village.getId() + "] " + ChatColor.WHITE
                + customName);
        Bukkit.getServer().broadcastMessage(Colorize.name(customName) + " a rejoins la milice à " + Colorize.name(village.getId()));
    }

    public void testDeathIfSkeleton(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Skeleton)) {
            return;
        }
        if (!e.getEntity().isCustomNameVisible()) {
            return;
        }

        VillageModel village = VillageRepository.get(CustomName.squareBrackets(e.getEntity().getCustomName(), 0));
        village.setGarrison(village.getGarrison() - 1);
        VillageRepository.update(village);

        Bukkit.getServer().broadcastMessage(Colorize.name(e.getEntity().getCustomName()) + " est mort bravement");
    }

    public void testIfSkeletonDamageSameVillage(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof Skeleton)) {
            return;
        }
        Skeleton skeleton = (Skeleton) e.getEntity();
        if (!skeleton.isCustomNameVisible()) {
            return;
        }
        String sVillage = CustomName.squareBrackets(skeleton.getCustomName(), 0);
        VillageModel village = VillageRepository.get(sVillage);
        if (e.getTarget() instanceof Player) {
            if (village.getPlayerName().equals(((Player) e.getTarget()).getDisplayName())) {
                e.setCancelled(true);
                return;
            }
            return;
        }
        if (!e.getEntity().isCustomNameVisible()) {
            return;
        }

        String tVillage = CustomName.squareBrackets(e.getEntity().getCustomName(), 0);

        if (sVillage.equals(tVillage)) {
            e.setCancelled(true);
        }
    }
}
