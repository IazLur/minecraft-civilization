package TestJava.testjava.repositories;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.ResourceModel;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import java.util.Collection;

public class ResourceRepository {
    public static void update(@Nonnull ResourceModel resource) {
        TestJava.database.upsert(resource);
    }

    public static ResourceModel get(@Nonnull String id) {
        return TestJava.database.findById(id, ResourceModel.class);
    }

    public static ResourceModel remove(@Nonnull ResourceModel resource) {
        return TestJava.database.remove(resource, ResourceModel.class);
    }

    public static Collection<ResourceModel> getAll() {
        return TestJava.database.findAll(ResourceModel.class);
    }
}
