package TestJava.testjava.repositories;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;

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

    public static EmpireModel getForPlayer(String p) {
        Collection<EmpireModel> alls = EmpireRepository.getAll();
        for (EmpireModel o : alls) {
            if (o.getEmpireName().toLowerCase().contains(p.toLowerCase())) {
                return o;
            }
        }

        return null;
    }

    public static Collection<EmpireModel> getAll() {
        return TestJava.database.findAll(EmpireModel.class);
    }
}
