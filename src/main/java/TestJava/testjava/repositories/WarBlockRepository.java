package TestJava.testjava.repositories;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.WarBlockModel;

import java.util.Collection;
import java.util.UUID;

public class WarBlockRepository {
    public static void remove(UUID uniqueId) {
        WarBlockModel warBlock = TestJava.database.findById(uniqueId, WarBlockModel.class);
        if (warBlock != null) {
            TestJava.database.remove(warBlock, WarBlockModel.class);
        }
    }

    public static void update(WarBlockModel warBlock) {
        TestJava.database.upsert(warBlock);
    }

    public static Collection<WarBlockModel> getAll() {
        return TestJava.database.findAll(WarBlockModel.class);
    }
}
