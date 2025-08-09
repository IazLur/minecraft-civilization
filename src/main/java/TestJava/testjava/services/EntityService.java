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
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("deprecation")
public class EntityService {
    public void testSpawnIfVillager(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Villager villager)) {
            return;
        }
        VillageModel village = VillageRepository.getNearestOf(villager);
        if (village == null) {
            // No village context; cancel spawn for consistency
            e.setCancelled(true);
            return;
        }
        
        // VÉRIFICATION CRITIQUE: Empêcher le spawn si le village a atteint sa limite de lits
        if (village.getPopulation() >= village.getBedsCount()) {
            Bukkit.getLogger().warning("[EntityService] Spawn villageois bloqué: Village " + 
                village.getId() + " a atteint sa limite (" + village.getPopulation() + "/" + village.getBedsCount() + " lits)");
            e.setCancelled(true);
            return;
        }
        
        villager.setCustomNameVisible(true);
        String customName = CustomName.generate();
        
        // Creating new model entity
        VillagerModel nVillager = new VillagerModel();
        nVillager.setVillageName(village.getId());
        nVillager.setFood(1);
        nVillager.setId(villager.getUniqueId());
        VillagerRepository.update(nVillager);
        
        // Mise à jour de la population du village
        village.setPopulation(village.getPopulation() + 1);
        VillageRepository.update(village);
        
        // Appliquer le nouveau format centralisé
        String display = VillagerNameService.buildDisplayName(nVillager, villager, customName);
        villager.setCustomName(display);
        
        Bukkit.getServer().broadcastMessage(Colorize.name(customName) + " est né à " + Colorize.name(village.getId()));

        // Mise à jour du nom avec la classe sociale (réapplique le format centralisé si besoin)
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
        if (village == null) {
            skeleton.remove();
            return;
        }
        skeleton.setCanPickupItems(false);
        skeleton.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        skeleton.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0D);
        skeleton.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(10D);
        skeleton.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1D);
        skeleton.setCustomNameVisible(true);
        skeleton.setRemoveWhenFarAway(false);
        skeleton.setPersistent(true);
        String customName = CustomName.generate();
        
        // CORRECTION BUG: Incrémenter la garnison lors de la création
        village.setGarrison(village.getGarrison() + 1);
        VillageRepository.update(village);
        
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
        if (village == null) {
            pillager.remove();
            return;
        }
        pillager.setCanPickupItems(false);
        pillager.setCanJoinRaid(false);
        pillager.setPatrolLeader(false);
        pillager.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        pillager.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        pillager.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        
        // Ajouter des bottes de givre pour la mobilité sur glace
        ItemStack frostBoots = new ItemStack(Material.IRON_BOOTS);
        frostBoots.addEnchantment(org.bukkit.enchantments.Enchantment.FROST_WALKER, 2);
        pillager.getEquipment().setBoots(frostBoots);
        pillager.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1D);
        pillager.setCustomNameVisible(true);
        pillager.setRemoveWhenFarAway(false);
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
        if (village == null) {
            return;
        }
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
        
        // Ajouter des bottes de givre pour la mobilité sur glace
        ItemStack frostBoots = new ItemStack(Material.LEATHER_BOOTS);
        frostBoots.addEnchantment(org.bukkit.enchantments.Enchantment.FROST_WALKER, 2);
        bandit.getEquipment().setBoots(frostBoots);
        Player enemy = TestJava.playerService.getNearestPlayerWhereNot(bandit, player.getName());
        if (enemy != null) {
            TestJava.banditTargets.put(bandit.getUniqueId(), enemy.getName());
            bandit.setTarget(enemy);
        }
    }

    public void testSpawnIfGolem(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof IronGolem golem)) {
            return;
        }

        Bukkit.getLogger().info("[GardeNationale] Golem de fer détecté en spawn");
        VillageModel village = VillageRepository.getNearestOf(golem);
        if (village == null) {
            return;
        }
        golem.setCustomNameVisible(true);
        String name = CustomName.generate();
        golem.setCustomName(
                ChatColor.AQUA + "[" + village.getId() + "] " + ChatColor.WHITE + name
        );
        Bukkit.getServer().broadcastMessage(ChatColor.GRAY + Colorize.name(name) +
                " a rejoint la garde nationale à " + Colorize.name(village.getId()));
        Bukkit.getLogger().info("[GardeNationale] " + name + " a rejoint la garde nationale à " + village.getId());
    }

    public void testIfGolemDamageSameVillage(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof IronGolem golem)) {
            return;
        }
        if (!golem.isCustomNameVisible() || golem.getCustomName() == null) {
            return;
        }
        String golemVillage = CustomName.squareBrackets(golem.getCustomName(), 0);
        VillageModel village = VillageRepository.get(golemVillage);
        LivingEntity target = e.getTarget();
        if (target == null) {
            return;
        }
        // Cancel if target is a player from the same village
        if (target instanceof Player player) {
            if (village.getPlayerName().equals(player.getName())) {
                e.setCancelled(true);
                return;
            }
        }
        // Cancel if target is a mob with the same village name
        if (target.isCustomNameVisible() && target.getCustomName() != null) {
            String targetVillage = CustomName.squareBrackets(target.getCustomName(), 0);
            if (golemVillage.equals(targetVillage)) {
                e.setCancelled(true);
                return;
            }
        }
        // Otherwise, allow attack (hostile mobs, other villages, etc.)
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
        
        // Traitement spécial pour les golems de fer (garde nationale)
        if (event.getEntityType() == EntityType.IRON_GOLEM) {
            Bukkit.getLogger().info("[GardeNationale] Tentative de spawn golem de fer avec raison: " + reason);
            switch (reason) {
                case SPAWNER_EGG: // Apparu à partir d'un œuf d'apparition
                case BREEDING:    // Résultat de l'accouplement
                case CUSTOM:      // Instancié par un plugin ou un code
                case VILLAGE_DEFENSE:  // Spawn naturel pour défendre un village
                case VILLAGE_INVASION: // Spawn lors d'invasions
                case BUILD_IRONGOLEM:  // Construit par les joueurs
                case NATURAL:     // Spawn naturel dans les villages
                    // Laisser les golems de fer apparaître pour la garde nationale
                    Bukkit.getLogger().info("[GardeNationale] ✅ Spawn golem de fer autorisé (raison: " + reason + ")");
                    break;
                default:
                    // Pour toutes les autres raisons, annulez l'événement
                    Bukkit.getLogger().info("[GardeNationale] ❌ Spawn golem de fer bloqué (raison: " + reason + ")");
                    event.setCancelled(true);
                    break;
            }
            return;
        }
        
        // Ne traiter que les autres animaux passifs, pas les monstres
        if (event.getEntityType() == EntityType.SHEEP ||
            event.getEntityType() == EntityType.COW ||
            event.getEntityType() == EntityType.PIG ||
            event.getEntityType() == EntityType.CHICKEN ||
            event.getEntityType() == EntityType.RABBIT ||
            event.getEntityType() == EntityType.HORSE ||
            event.getEntityType() == EntityType.DONKEY ||
            event.getEntityType() == EntityType.MULE ||
            event.getEntityType() == EntityType.LLAMA ||
            event.getEntityType() == EntityType.GOAT ||
            event.getEntityType() == EntityType.FOX ||
            event.getEntityType() == EntityType.WOLF ||
            event.getEntityType() == EntityType.CAT ||
            event.getEntityType() == EntityType.OCELOT ||
            event.getEntityType() == EntityType.PARROT ||
            event.getEntityType() == EntityType.BEE ||
            event.getEntityType() == EntityType.DOLPHIN ||
            event.getEntityType() == EntityType.SQUID ||
            event.getEntityType() == EntityType.GLOW_SQUID ||
            event.getEntityType() == EntityType.TURTLE ||
            event.getEntityType() == EntityType.PANDA ||
            event.getEntityType() == EntityType.POLAR_BEAR) {
            
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
        // Pour tous les autres types d'entités (monstres, etc.), laisser faire
    }

    public void testIfBanditTargetRight(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof Zombie bandit)) {
            return;
        }
        // Only manage our custom mercenaries (named and tracked)
        if (!bandit.isCustomNameVisible() || bandit.getCustomName() == null) {
            return;
        }
        String lockedName = TestJava.banditTargets.get(bandit.getUniqueId());
        if (lockedName == null || lockedName.isEmpty()) {
            return;
        }
        LivingEntity current = e.getTarget();
        if (current instanceof Player player) {
            if (!player.getName().equals(lockedName)) {
                Player desired = Bukkit.getPlayer(lockedName);
                if (desired != null && desired.isOnline()) {
                    bandit.setTarget(desired);
                    e.setTarget(desired);
                } else {
                    // No valid target available, cancel retargeting
                    e.setCancelled(true);
                }
            }
        } else {
            Player desired = Bukkit.getPlayer(lockedName);
            if (desired != null && desired.isOnline()) {
                bandit.setTarget(desired);
                e.setTarget(desired);
            } else {
                e.setCancelled(true);
            }
        }
    }

    public void preventFireForCustom(EntityDamageEvent e) {
        // Protect custom, named guards from sunlight by re-equipping helmets and cancelling burning
        if (!(e.getEntity() instanceof LivingEntity living)) {
            return;
        }
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE &&
            cause != EntityDamageEvent.DamageCause.FIRE_TICK &&
            cause != EntityDamageEvent.DamageCause.HOT_FLOOR &&
            cause != EntityDamageEvent.DamageCause.LAVA) {
            return;
        }
        if (!living.isCustomNameVisible() || living.getCustomName() == null) {
            return;
        }
        // Ensure this is one of our managed entities (has a [Village] tag)
        try {
            CustomName.extractVillageName(living.getCustomName());
        } catch (Exception ex) {
            return; // Not a managed custom entity
        }
        if (living instanceof Skeleton skeleton) {
            var equipment = skeleton.getEquipment();
            if (equipment != null && (equipment.getHelmet() == null || equipment.getHelmet().getType() == Material.AIR)) {
                equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
                equipment.setHelmetDropChance(0.0f);
            }
            skeleton.setFireTicks(0);
            e.setCancelled(true);
        }
    }
}
