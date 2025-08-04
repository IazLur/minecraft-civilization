package TestJava.testjava.repositories;

import TestJava.testjava.Config;
import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillageModel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class VillageRepository {

    @Nonnull
    public static Collection<VillageModel> getAll() {
        return TestJava.database.findAll(VillageModel.class);
    }

    @Nonnull
    public static Location getBellLocation(@Nonnull VillageModel village) {
        return new Location(TestJava.world, village.getX(), village.getY(), village.getZ());
    }

    @Nullable
    public static VillageModel getNearestOf(@Nonnull LivingEntity livingEntity) {
        double distance = 999D;
        Location location = livingEntity.getLocation();
        return getVillageModelDistance(location, distance);
    }

    @Nullable
    public static VillageModel getCurrentVillageConstruction(@Nonnull LivingEntity livingEntity) {
        VillageModel nearest = VillageRepository.getNearestOf(livingEntity);
        if (nearest == null) return null;
        Location bell = getBellLocation(nearest);
        Location loc = livingEntity.getLocation();
        if (bell.getBlockX() - Config.VILLAGE_CONSTRUCTION_RADIUS / 2 < loc.getBlockX()
                && bell.getBlockX() + Config.VILLAGE_CONSTRUCTION_RADIUS / 2 > loc.getBlockX()
                && bell.getBlockZ() - Config.VILLAGE_CONSTRUCTION_RADIUS / 2 < loc.getBlockZ()
                && bell.getBlockZ() + Config.VILLAGE_CONSTRUCTION_RADIUS / 2 > loc.getBlockZ()) {
            return nearest;
        }
        return null;
    }

    @Nullable
    public static VillageModel getCurrentVillageTerritory(@Nonnull LivingEntity livingEntity) {
        VillageModel nearest = VillageRepository.getNearestOf(livingEntity);
        if (nearest == null) return null;
        Location bell = getBellLocation(nearest);
        Location loc = livingEntity.getLocation();
        if (bell.getBlockX() - Config.VILLAGE_PROTECTION_RADIUS / 2 < loc.getBlockX()
                && bell.getBlockX() + Config.VILLAGE_PROTECTION_RADIUS / 2 > loc.getBlockX()
                && bell.getBlockZ() - Config.VILLAGE_PROTECTION_RADIUS / 2 < loc.getBlockZ()
                && bell.getBlockZ() + Config.VILLAGE_PROTECTION_RADIUS / 2 > loc.getBlockZ()) {
            return nearest;
        }
        return null;
    }

    @Nullable
    public static VillageModel getCurrentVillageConstructibleIfOwn(@Nonnull Player player) {
        VillageModel village = VillageRepository.getCurrentVillageConstruction(player);
        if (village == null) return null;
        return village.getPlayerName().equals(player.getName()) ? village : null;
    }

    public static Collection<VillageModel> getForPlayer(String p) {
        Collection<VillageModel> alls = VillageRepository.getAll();
        Collection<VillageModel> result = new ArrayList<>();
        for (VillageModel o : alls) {
            if (o.getPlayerName().equals(p)) {
                result.add(o);
            }
        }
        return result;
    }

    @Nullable
    public static VillageModel getNearestVillageOfPlayer(String p, int maxDistance) {
        Player player = Bukkit.getPlayerExact(p);
        Collection<VillageModel> villages = getForPlayer(p);
        double minDistance = maxDistance;
        VillageModel nearestVillage = null;
        for (VillageModel village : villages) {
            double currDistance = Objects.requireNonNull(player).getLocation().distance(getBellLocation(village));
            if (currDistance < minDistance && currDistance <= maxDistance) {
                minDistance = currDistance;
                nearestVillage = village;
            }
        }

        return nearestVillage;
    }

    public static void update(@Nonnull VillageModel village) {
        TestJava.database.upsert(village);
    }

    public static void remove(VillageModel village) {
        TestJava.database.remove(village, VillageModel.class);
    }

    public static VillageModel get(@Nonnull String id) {
        return TestJava.database.findById(id, VillageModel.class);
    }

    public static VillageModel getNearestOf(Location location) {
        double distance = 999D;
        return getVillageModelDistance(location, distance);
    }

    private static VillageModel getVillageModelDistance(Location location, double distance) {
        VillageModel currentVillage = null;
        for (VillageModel village : VillageRepository.getAll()) {
            Location bellLocation = VillageRepository.getBellLocation(village);
            double testDistance = bellLocation.distance(location);
            if (testDistance < distance) {
                currentVillage = village;
                distance = testDistance;
            }
        }
        return currentVillage;
    }

    public static VillageModel getNearestPopulatedOfWhereNot(Location location, VillageModel ignore) {
        double distance = 999D;
        VillageModel currentVillage = null;
        for (VillageModel village : VillageRepository.getAll()) {
            Location bellLocation = VillageRepository.getBellLocation(village);
            double testDistance = bellLocation.distance(location);
            if (testDistance < distance && !village.getId().equals(ignore.getId())
                    && village.getPopulation() > 0) {
                currentVillage = village;
                distance = testDistance;
            }
        }
        return currentVillage;
    }
}
