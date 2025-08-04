package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.SocialClassService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

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
        nVillager.setFood(1);
        nVillager.setId(villager.getUniqueId());
        VillagerRepository.update(nVillager);
        
        // Mise à jour du nom avec le tag de classe sociale
        SocialClassService.updateVillagerDisplayName(nVillager);
    }

    public void testDeathIfVillager(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Villager)) {
            return;
        }
        if (!e.getEntity().isCustomNameVisible()) {
            return;
        }

        VillageModel village = VillageRepository.get(CustomName.extractVillageName(e.getEntity().getCustomName()));
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
        skeleton.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0D);
        skeleton.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(10D);
        skeleton.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1D);
        skeleton.setCustomNameVisible(true);
        skeleton.setRemoveWhenFarAway(false);
        skeleton.setPersistent(true);
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
        pillager.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1D);
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

        VillageModel village = VillageRepository.get(CustomName.extractVillageName(e.getEntity().getCustomName()));
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

        VillageModel village = VillageRepository.get(CustomName.extractVillageName(e.getEntity().getCustomName()));
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

    public void testIfMobTargetSkeleton(EntityTargetLivingEntityEvent e) {
        if (
                !(e.getEntity() instanceof Mob) ||
                        e.getEntity() instanceof Pillager
        ) {
            return;
        }
        if (e.getTarget() != null && e.getTarget().isCustomNameVisible()) {
            if (e.getTarget() instanceof Skeleton) {
                e.setTarget(null);
                e.setCancelled(true);
            }
        }
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
            if (village.getPlayerName().equals(((Player) e.getTarget()).getName())) {
                e.setCancelled(true);
                return;
            }
            return;
        }
        if (!e.getEntity().isCustomNameVisible()) {
            return;
        }

        String tVillage = CustomName.extractVillageName(e.getEntity().getCustomName());

        if (sVillage.equals(tVillage)) {
            e.setCancelled(true);
        }
    }

    public void testIfPlaceBandit(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.COPPER_BLOCK) {
            return;
        }

        Player player = e.getPlayer();
        VillageModel village = VillageRepository.getNearestOf(player);
        e.getBlockPlaced().setType(Material.AIR);
        Zombie bandit = e.getBlockPlaced().getWorld().spawn(e.getBlockPlaced().getLocation(), Zombie.class);
        bandit.setCustomNameVisible(true);
        bandit.setCustomName("[" + village.getId() + "] Mercenaire");
        bandit.setRemoveWhenFarAway(false);
        bandit.setPersistent(true);
        bandit.setAdult();
        bandit.setCanBreakDoors(true);
        bandit.setSwimming(true);
        bandit.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
        bandit.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
        bandit.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
        bandit.getEquipment().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        bandit.getEquipment().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        bandit.getEquipment().setBoots(new ItemStack(Material.LEATHER_BOOTS));
        Player enemy = TestJava.playerService.getNearestPlayerWhereNot(bandit, player.getName());
        TestJava.banditTargets.put(bandit.getUniqueId(), enemy.getName());
        bandit.setTarget(enemy);
    }

    public void testIfBanditTargetRight(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof Zombie zombie)) {
            return;
        }

        if (!zombie.isCustomNameVisible()) {
            return;
        }

        if (!(e.getTarget() instanceof Player player)) {
            e.setCancelled(true);
            return;
        }

        if (TestJava.banditTargets.get(zombie.getUniqueId()).equals(player.getName())) {
            return;
        }
        e.setCancelled(true);
    }

    public void preventFireForCustom(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            return;
        }

        if (!e.getEntity().isCustomNameVisible()) {
            return;
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            if (e.getEntity() instanceof Skeleton skeleton) {
                skeleton.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET, 1));
            }
            e.setCancelled(true);
            e.setDamage(0D);
        }
    }

    public void testSpawnIfGolem(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof IronGolem golem)) {
            return;
        }

        VillageModel village = VillageRepository.getNearestOf(golem);
        golem.setCustomNameVisible(true);
        String name = CustomName.generate();
        golem.setCustomName(
                ChatColor.AQUA + "[" + village.getId() + "] " + ChatColor.WHITE + name
        );
        Bukkit.getServer().broadcastMessage(ChatColor.GRAY + Colorize.name(name) +
                " a rejoint la garde nationale à " + Colorize.name(village.getId()));
    }

    public void testIfGolemDamageSameVillage(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof IronGolem golem)) {
            return;
        }
        testIfEntityDamageSameVillage(e, golem.isCustomNameVisible(), golem.getCustomName());
    }

    public void testIfPlaceLocust(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.COAL_BLOCK) {
            return;
        }

        e.getBlockPlaced().setType(Material.AIR);
        Location position = e.getBlockPlaced().getLocation();
        VillageModel village = VillageRepository.getNearestOf(position);
        VillageModel enemy = VillageRepository.getNearestPopulatedOfWhereNot(position, village);
        Bat locust = TestJava.world.spawn(position, Bat.class);
        locust.setCustomNameVisible(true);
        locust.setPersistent(true);
        locust.setCustomName(ChatColor.DARK_RED + "[" + village.getId() + "] Criquet");
        TestJava.locustTargets.put(locust.getUniqueId(), enemy);
    }

    public void preventVillageEntityTransform(EntityTransformEvent e) {
        if (!(e.getEntity() instanceof Zombie) &&
                !(e.getEntity() instanceof Skeleton)) {
            return;
        }
        if (e.getEntity().isCustomNameVisible()) {
            e.setCancelled(true);
        }
    }

    public void testAnimalSpawn(CreatureSpawnEvent event) {
        // Obtenez la raison de l'apparition de la créature
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

        switch (reason) {
            // Les raisons acceptables pour lesquelles un animal peut apparaître
            case SPAWNER_EGG: // Apparu à partir d'un œuf d'apparition
            case BREEDING:    // Résultat de l'accouplement
            case CUSTOM:      // Instancié par un plugin ou un code
                // Ne faites rien, laissez l'animal apparaître
                break;
            default:
                // Pour toutes les autres raisons, annulez l'événement
                event.setCancelled(true);
                break;
        }
    }
}
