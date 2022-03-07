package edouard.testjava.repositories;

import edouard.testjava.Config;
import edouard.testjava.TestJava;
import edouard.testjava.models.VillageModel;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

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
        VillageModel currentVillage = null;
        Location location = livingEntity.getLocation();
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

    @Nullable
    public static VillageModel getCurrentVillageConstructible(@Nonnull LivingEntity livingEntity) {
        VillageModel nearest = VillageRepository.getNearestOf(livingEntity);
        if (nearest == null) return null;
        return getBellLocation(nearest).distance(livingEntity.getLocation())
                <= Config.VILLAGE_CONSTRUCTIBLE_RADIUS ? nearest : null;
    }

    @Nullable
    public static VillageModel getCurrentVillageTerritory(@Nonnull LivingEntity livingEntity) {
        VillageModel nearest = VillageRepository.getNearestOf(livingEntity);
        if (nearest == null) return null;
        return getBellLocation(nearest).distance(livingEntity.getLocation())
                <= Config.VILLAGE_PROTECTION_RADIUS ? nearest : null;
    }

    @Nullable
    public static VillageModel getCurrentVillageConstructibleIfOwn(@Nonnull Player player) {
        VillageModel village = VillageRepository.getCurrentVillageConstructible(player);
        if (village == null) return null;
        return village.getPlayerName().equals(player.getDisplayName()) ? village : null;
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

    public static void update(@Nonnull VillageModel village) {
        TestJava.database.upsert(village);
    }

    public static void remove(VillageModel village) {
        TestJava.database.remove(village, VillageModel.class);
    }

    public static VillageModel get(@Nonnull String id) {
        return TestJava.database.findById(id, VillageModel.class);
    }
}
