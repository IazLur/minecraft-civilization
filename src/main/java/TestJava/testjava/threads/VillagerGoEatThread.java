package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.EatableModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EatableRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.*;

public class VillagerGoEatThread implements Runnable {

    private static final int MIN_DELAY = 20;
    private static final int RANGE_DELAY = 20 * 10;
    private static final int MAX_FOOD = 19;
    private static final double MOVE_SPEED = 1D;

    private final Set<UUID> targetedEatables = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void run() {
        Collection<EatableModel> eatables = EatableRepository.getAll();
        HashMap<String, Collection<EatableModel>> villageEatablesMap = prepareEatablesMap(eatables);

        String query = String.format("/.[food<'%s']", MAX_FOOD);
        Collection<VillagerModel> hungryVillagers = TestJava.database.find(query, VillagerModel.class);

        for (VillagerModel villager : hungryVillagers) {
            Bukkit.getLogger().info("Testing food for " + villager.getId());
            Bukkit.getScheduler().runTask(TestJava.plugin, () -> handleHungryVillager(villager, villageEatablesMap));
        }
    }

    private HashMap<String, Collection<EatableModel>> prepareEatablesMap(Collection<EatableModel> eatables) {
        HashMap<String, Collection<EatableModel>> map = new HashMap<>();
        for (EatableModel eatable : eatables) {
            map.computeIfAbsent(eatable.getVillage(), k -> new ArrayList<>()).add(eatable);
        }
        return map;
    }

    private void handleHungryVillager(VillagerModel villager, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        Villager eVillager = fetchEntityVillager(villager.getId(), villager);

        EatableModel targetEatable = findEatable(villager, villageEatablesMap);

        if(eVillager == null) {
            Bukkit.getServer().getLogger().info("Impossible de trouver l'entité de " + villager.getId());
            return;
        }

        if (targetEatable != null) {
            targetedEatables.add(targetEatable.getId());
            moveVillagerToFood(eVillager, villager, targetEatable, villageEatablesMap);
        } else {
            broadcastNoFoodMessage(eVillager);
        }
    }

    private Villager fetchEntityVillager(UUID uuid, VillagerModel villager) {
        Entity entity = TestJava.plugin.getServer().getEntity(uuid);

        if (entity == null) {
            Location location = VillageRepository.getBellLocation(VillageRepository.get(villager.getVillageName()));
            World world = location.getWorld();

            // Récupère les coordonnées du chunk central
            int chunkX = location.getChunk().getX();
            int chunkZ = location.getChunk().getZ();

            // Boucle pour charger les chunks dans un carré de 3x3
            for (int x = chunkX - 1; x <= chunkX + 1; x++) {
                for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                    Chunk chunk = world.getChunkAt(x, z);

                    if (!chunk.isLoaded()) {
                        boolean success = chunk.load(true);

                        if (success) {
                            entity = TestJava.plugin.getServer().getEntity(uuid);
                            if (entity != null) {
                                break;
                            }
                        } else {
                            Bukkit.getServer().getLogger().warning("Impossible de charger le chunk en [" + x + ", " + z + "].");
                        }
                    }
                }
                if (entity != null) {
                    break;
                }
            }
        }

        return (entity instanceof Villager) ? (Villager) entity : null;
    }

    private boolean eatableExistsInWorld(EatableModel eatable) {
        Location loc = new Location(TestJava.world, eatable.getX(), eatable.getY(), eatable.getZ());
        Block block = loc.getBlock();
        boolean exists = (block.getBlockData() instanceof Ageable age) && (age.getMaximumAge() == age.getAge());

        if (block.getBlockData().getMaterial() != Material.WHEAT) {
            EatableRepository.remove(eatable);
            return false;
        }

        return exists;
    }

    private EatableModel findEatable(VillagerModel villager, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        Villager eVillager = fetchEntityVillager(villager.getId(), villager);
        if (eVillager == null) {
            Bukkit.getServer().broadcastMessage("ERROR A1");
            return null;
        }

        Location villagerLocation = eVillager.getLocation();
        Collection<EatableModel> eatables = villageEatablesMap.getOrDefault(villager.getVillageName(), Collections.emptyList());

        return eatables.stream().filter(this::eatableExistsInWorld).filter(eatable -> !targetedEatables.contains(eatable.getId())).min(Comparator.comparingDouble(eatable -> calculateDistance(villagerLocation, eatable))).orElse(null);
    }

    private double calculateDistance(Location villagerLocation, EatableModel eatable) {
        Location eatableLocation = new Location(TestJava.world, eatable.getX(), eatable.getY(), eatable.getZ());
        return villagerLocation.distance(eatableLocation);
    }

    private void moveVillagerToFood(Villager eVillager, VillagerModel villager, EatableModel targetEatable, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        Random rand = new Random();
        int delay = MIN_DELAY + rand.nextInt(RANGE_DELAY);
        UUID uuid = UUID.randomUUID();

        Location loc = new Location(TestJava.world, targetEatable.getX(), targetEatable.getY(), targetEatable.getZ());
        Block block = loc.getBlock();

        final int[] attempts = {0};
        final double[] increasedDistance = {2.0};

        TestJava.threads.put(uuid, Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin, () -> {
            attempts[0] += 1;

            if (attempts[0] % 3 == 0) {
                increasedDistance[0] += 1.0;
            }

            performScheduledTask(eVillager, villager, targetEatable, block, loc, uuid, increasedDistance[0], villageEatablesMap);
        }, delay, 10));
    }

    private void performScheduledTask(Villager eVillager, VillagerModel villager, EatableModel targetEatable, Block block, Location loc, UUID uuid, double increasedDistance, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        if (eVillager.isSleeping()) {
            eVillager.wakeup();
        }

        eVillager.getPathfinder().moveTo(loc, MOVE_SPEED);

        if (isFoodGone(block)) {
            handleFoodGone(targetEatable, uuid, villager, villageEatablesMap);
            return;
        }

        if (eVillager.getLocation().distance(loc) <= increasedDistance) {
            handleEating(villager, block, targetEatable, uuid);
        }
    }

    private boolean isFoodGone(Block block) {
        return !(block.getBlockData() instanceof Ageable age) || age.getAge() != age.getMaximumAge();
    }

    private void handleFoodGone(EatableModel targetEatable, UUID uuid, VillagerModel villager, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        targetedEatables.remove(targetEatable.getId());
        handleHungryVillager(villager, villageEatablesMap);
        cancelTask(uuid);
    }

    private void handleEating(VillagerModel villager, Block block, EatableModel targetEatable, UUID uuid) {
        Ageable age = (Ageable) block.getBlockData();
        age.setAge(1);
        block.setBlockData(age);

        villager.setFood(villager.getFood() + 1);
        VillagerRepository.update(villager);

        VillageModel village = VillageRepository.get(villager.getVillageName());
        if (villager.getFood() >= MAX_FOOD) {
            village.setProsperityPoints(village.getProsperityPoints() + 1);
        }

        targetedEatables.remove(targetEatable.getId());

        VillageRepository.update(village);
        EatableRepository.remove(targetEatable);
        cancelTask(uuid);
    }

    private void cancelTask(UUID uuid) {
        Bukkit.getScheduler().cancelTask(TestJava.threads.get(uuid));
        TestJava.threads.remove(uuid);
    }

    private void broadcastNoFoodMessage(Villager eVillager) {
        if (eVillager != null) {
            Bukkit.getServer().broadcastMessage(Colorize.name(eVillager.getCustomName()) + " n'a rien à manger");
        }
    }
}