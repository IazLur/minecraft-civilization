package TestJava.testjava.repositories;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.BuildingModel;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Location;

import java.util.Comparator;

public class BuildingRepository {

    public static Collection<BuildingModel> getAll() {
        return TestJava.database.findAll(BuildingModel.class);
    }

    public static void update(@Nonnull BuildingModel building) {
        TestJava.database.upsert(building);
    }

    public static void remove(@Nonnull BuildingModel building) {
        BuildingModel buildingModel = TestJava.database.findById(building.getId(), BuildingModel.class);
        if (buildingModel != null) {
            TestJava.database.remove(buildingModel, BuildingModel.class);
        }
    }

    public static BuildingModel getBuildingById(UUID id) {
        return TestJava.database.findById(id, BuildingModel.class);
    }

    public static Collection<BuildingModel> getBuildingsForVillage(String villageName) {
        String jxQuery = String.format("/.[villageName='%s']", villageName);
        return TestJava.database.find(jxQuery, BuildingModel.class);
    }

    public static BuildingModel getNearestBuilding(Location location) {
        Collection<BuildingModel> allBuildings = TestJava.database.findAll(BuildingModel.class);
        return allBuildings.stream()
                .min(Comparator.comparing(building -> location.distanceSquared(new Location(location.getWorld(), building.getX(), building.getY(), building.getZ()))))
                .orElse(null);
    }

    public static boolean isVillageHavingBuildingOfType(String villageName, String buildingType) {
        String jxQuery = String.format("/.[villageName='%s' and buildingType='%s']", villageName, buildingType);
        Collection<BuildingModel> buildings = TestJava.database.find(jxQuery, BuildingModel.class);
        return !buildings.isEmpty();
    }
}