package TestJava.testjava.repositories;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.SheepModel;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;

public class SheepRepository {

    public static Collection<SheepModel> getAll() {
        return TestJava.database.findAll(SheepModel.class);
    }

    public static void update(@Nonnull SheepModel sheep) {
        TestJava.database.upsert(sheep);
    }

    public static void remove(@Nonnull SheepModel sheep) {
        SheepModel sheepModel = TestJava.database.findById(sheep.getId(), SheepModel.class);
        if (sheepModel != null) {
            TestJava.database.remove(sheepModel, SheepModel.class);
        }
    }

    public static SheepModel find(UUID id) {
        return TestJava.database.findById(id, SheepModel.class);
    }

    public static Collection<SheepModel> getSheepForBuilding(UUID buildingId) {
        String jxQuery = String.format("/.[buildingId='%s']", buildingId.toString());
        return TestJava.database.find(jxQuery, SheepModel.class);
    }

    public static Collection<SheepModel> getSheepForVillage(String villageName) {
        String jxQuery = String.format("/.[villageName='%s']", villageName);
        return TestJava.database.find(jxQuery, SheepModel.class);
    }

    public static int getSheepCountForBuilding(UUID buildingId) {
        return getSheepForBuilding(buildingId).size();
    }

    public static int getNextSheepNumberForVillage(String villageName) {
        Collection<SheepModel> villageShees = getSheepForVillage(villageName);
        return villageShees.stream()
                .mapToInt(SheepModel::getSheepNumber)
                .max()
                .orElse(0) + 1;
    }

    public static void removeAllSheepForBuilding(UUID buildingId) {
        Collection<SheepModel> sheep = getSheepForBuilding(buildingId);
        for (SheepModel sheepModel : sheep) {
            TestJava.database.remove(sheepModel, SheepModel.class);
        }
    }
}