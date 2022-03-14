package TestJava.testjava.repositories;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.EatableModel;

import javax.annotation.Nonnull;
import java.util.Collection;

public class EatableRepository {
    public static Collection<EatableModel> getAll() {
        return TestJava.database.findAll(EatableModel.class);
    }

    public static void update(@Nonnull EatableModel eatable) {
        TestJava.database.upsert(eatable);
    }

    public static void remove(@Nonnull EatableModel eatable) {
        TestJava.database.remove(eatable, EatableModel.class);
    }
}
