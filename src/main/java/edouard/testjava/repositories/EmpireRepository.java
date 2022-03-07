package edouard.testjava.repositories;

import edouard.testjava.TestJava;
import edouard.testjava.models.EmpireModel;

import javax.annotation.Nonnull;

public class EmpireRepository {
    public static void update(@Nonnull EmpireModel empire) {
        TestJava.database.upsert(empire);
    }

    public static EmpireModel get(@Nonnull String id) {
        return TestJava.database.findById(id, EmpireModel.class);
    }

    public static EmpireModel remove(@Nonnull EmpireModel empire) {
        return TestJava.database.remove(empire, EmpireModel.class);
    }
}
