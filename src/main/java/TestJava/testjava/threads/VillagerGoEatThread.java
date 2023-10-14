package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.EatableModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EatableRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.*;

public class VillagerGoEatThread implements Runnable {

    private static final int MIN_DELAY = 20 * 5;
    private static final int RANGE_DELAY = 20 * 25;
    private static final int MAX_FOOD = 19;
    private static final double MOVE_SPEED = 1D;

    @Override
    public void run() {
        Collection<EatableModel> eatables = EatableRepository.getAll();
        HashMap<String, Collection<EatableModel>> villageEatablesMap = prepareEatablesMap(eatables);

        String query = String.format("/.[food<'%s']", MAX_FOOD);
        Collection<VillagerModel> hungryVillagers = TestJava.database.find(query, VillagerModel.class);

        for (VillagerModel villager : hungryVillagers) {
            handleHungryVillager(villager, villageEatablesMap);
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
        Villager eVillager = fetchEntityVillager(villager.getId());
        if (eVillager == null || !villageEatablesMap.containsKey(villager.getVillageName())) {
            broadcastNoFoodMessage(eVillager);
            return;
        }

        EatableModel targetEatable = findEatable(villager, villageEatablesMap);
        if (targetEatable == null) {
            return;
        }

        moveVillagerToFood(eVillager, villager, targetEatable);
    }

    private Villager fetchEntityVillager(UUID uuid) {
        Entity entity = TestJava.world.getEntity(uuid);
        return (entity instanceof Villager) ? (Villager) entity : null;
    }

    private EatableModel findEatable(VillagerModel villager, HashMap<String, Collection<EatableModel>> villageEatablesMap) {
        Villager eVillager = fetchEntityVillager(villager.getId());
        if (eVillager == null) {
            return null;
        }

        Location villagerLocation = eVillager.getLocation();
        Collection<EatableModel> eatables = villageEatablesMap.getOrDefault(villager.getVillageName(), Collections.emptyList());

        return eatables.stream()
                .filter(this::isAtMaxAge)
                .min(Comparator.comparingDouble(eatable -> calculateDistance(villagerLocation, eatable)))
                .orElse(null);
    }

    private boolean isAtMaxAge(EatableModel eatable) {
        Location loc = new Location(TestJava.world, eatable.getX(), eatable.getY(), eatable.getZ());
        Block block = loc.getBlock();
        return (block.getBlockData() instanceof Ageable age) && (age.getMaximumAge() == age.getAge());
    }

    private double calculateDistance(Location villagerLocation, EatableModel eatable) {
        Location eatableLocation = new Location(TestJava.world, eatable.getX(), eatable.getY(), eatable.getZ());
        return villagerLocation.distance(eatableLocation);
    }

    private void moveVillagerToFood(Villager eVillager, VillagerModel villager, EatableModel targetEatable) {
        Random rand = new Random();
        int delay = MIN_DELAY + rand.nextInt(RANGE_DELAY);
        UUID uuid = UUID.randomUUID();

        Location loc = new Location(TestJava.world, targetEatable.getX(), targetEatable.getY(), targetEatable.getZ());
        Block block = loc.getBlock();

        TestJava.threads.put(uuid, Bukkit.getScheduler().scheduleSyncRepeatingTask(TestJava.plugin, () -> {
            performScheduledTask(eVillager, villager, targetEatable, block, loc, uuid);
        }, delay, 10));
    }

    private void performScheduledTask(Villager eVillager, VillagerModel villager, EatableModel targetEatable, Block block, Location loc, UUID uuid) {
        if (eVillager.isSleeping()) {
            eVillager.wakeup();
        }

        eVillager.getPathfinder().moveTo(loc, MOVE_SPEED);

        if (isFoodGone(block)) {
            handleFoodGone(eVillager, targetEatable, uuid);
            return;
        }

        if (eVillager.getLocation().distance(loc) <= 2) {
            handleEating(eVillager, villager, block, targetEatable, uuid);
        }
    }

    private boolean isFoodGone(Block block) {
        return !(block.getBlockData() instanceof Ageable age) || age.getAge() != age.getMaximumAge();
    }

    private void handleFoodGone(Villager eVillager, EatableModel targetEatable, UUID uuid) {
        EatableRepository.remove(targetEatable);
        broadcastNoFoodMessage(eVillager);
        cancelTask(uuid);
    }

    private void handleEating(Villager eVillager, VillagerModel villager, Block block, EatableModel targetEatable, UUID uuid) {
        Ageable age = (Ageable) block.getBlockData();
        age.setAge(1);
        block.setBlockData(age);

        villager.setFood(villager.getFood() + 1);
        System.out.println("Le villageois " + eVillager.getCustomName() + " a mangé");
        VillagerRepository.update(villager);

        VillageModel village = VillageRepository.get(villager.getVillageName());
        if (villager.getFood() >= MAX_FOOD) {
            village.setProsperityPoints(village.getProsperityPoints() + 1);
        }

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