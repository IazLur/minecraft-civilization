package TestJava.testjava.helpers;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.EatableModel;
import org.bukkit.Location;
import org.bukkit.block.data.Ageable;

import java.util.Collection;

public class EatableHelper {
    public static Collection<EatableModel> filter(Collection<EatableModel> eatables) {
        return eatables.stream().filter((eatableModel) -> {
            Location location = new Location(TestJava.world, eatableModel.getX(), eatableModel.getY(), eatableModel.getZ());
            if (!(location.getBlock().getBlockData() instanceof Ageable age)) {
                return false;
            }
            return age.getMaximumAge() == age.getAge();
        }).toList();
    }
}
