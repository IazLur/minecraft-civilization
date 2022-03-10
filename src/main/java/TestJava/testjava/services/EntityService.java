package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;

public class EntityService {
    public void testSpawnIfVillager(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Villager villager)) {
            return;
        }
        VillageModel village = VillageRepository.getNearestOf(villager);
        villager.setCustomNameVisible(true);
        assert village != null;
        String customName = CustomName.generate();
        villager.setCustomName(ChatColor.BLUE + "[" + village.getId() + "] " + ChatColor.WHITE
                + customName);
        Bukkit.getServer().broadcastMessage(Colorize.name(customName) + " est né à " + Colorize.name(village.getId()));

        // Creating new model entity
        VillagerModel nVillager = new VillagerModel();
        nVillager.setVillageName(village.getId());
        nVillager.setFood(10);
        nVillager.setId(villager.getUniqueId());
        VillagerRepository.update(nVillager);
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
        VillagerRepository.remove(e.getEntity().getUniqueId());

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
        skeleton.setCustomName(ChatColor.DARK_GREEN + "[" + village.getId() + "] " + ChatColor.WHITE
                + customName);
        Bukkit.getServer().broadcastMessage(Colorize.name(customName) + " a rejoint la milice à " + Colorize.name(village.getId()));
    }

    public void testIfPlaceAttacker(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.EMERALD_BLOCK) {
            return;
        }
        e.getBlockPlaced().setType(Material.AIR);
        Location location = e.getBlockPlaced().getLocation();
        Pillager pillager = TestJava.world.spawn(location, Pillager.class);
        VillageModel village = VillageRepository.getNearestOf(pillager);
        pillager.setCanPickupItems(false);
        pillager.setCanJoinRaid(false);
        pillager.setPatrolLeader(false);
        pillager.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        pillager.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
        pillager.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        pillager.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        pillager.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1D);
        pillager.setCustomNameVisible(true);
        pillager.setRemoveWhenFarAway(false);
        assert village != null;
        String customName = CustomName.generate();
        village.setGroundArmy(village.getGroundArmy() + 1);
        VillageRepository.update(village);
        pillager.setCustomName(ChatColor.DARK_RED + "[" + village.getId() + "] " + ChatColor.WHITE
                + customName);
        Bukkit.getServer().broadcastMessage(Colorize.name(customName) + " a rejoint l'armée de terre à " + Colorize.name(village.getId()));
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

    public void testDeathIfPillager(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Pillager)) {
            return;
        }
        if (!e.getEntity().isCustomNameVisible()) {
            return;
        }

        VillageModel village = VillageRepository.get(CustomName.squareBrackets(e.getEntity().getCustomName(), 0));
        village.setGroundArmy(village.getGroundArmy() - 1);
        VillageRepository.update(village);

        Bukkit.getServer().broadcastMessage(Colorize.name(e.getEntity().getCustomName()) + " est mort bravement");
    }

    public void testIfSkeletonDamageSameVillage(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof Skeleton skeleton)) {
            return;
        }
        testIfEntityDamageSameVillage(e, skeleton.isCustomNameVisible(), skeleton.getCustomName());
    }

    public void testIfPillagerDamageSameVillage(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof Pillager pillager)) {
            return;
        }
        testIfEntityDamageSameVillage(e, pillager.isCustomNameVisible(), pillager.getCustomName());
    }

    private void testIfEntityDamageSameVillage(EntityTargetLivingEntityEvent e, boolean customNameVisible, String customName) {
        if (!customNameVisible) {
            return;
        }
        String sVillage = CustomName.squareBrackets(customName, 0);
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