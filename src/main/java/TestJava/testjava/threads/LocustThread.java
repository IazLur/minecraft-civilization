package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.helpers.EatableHelper;
import TestJava.testjava.models.EatableModel;
import TestJava.testjava.repositories.EatableRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Bat;

import java.util.Collection;

public class LocustThread implements Runnable {
    @Override
    public void run() {
        TestJava.locustTargets.forEach(((uuid, villageModel) -> {
            Bat locust = (Bat) TestJava.world.getEntity(uuid);
            if (locust == null) {
                TestJava.locustTargets.remove(uuid);
                return;
            }

            Collection<EatableModel> eatables = EatableHelper.filter(EatableRepository.getWhere(villageModel.getId()));
            if (eatables.size() == 0) {
                return;
            }
            EatableModel eatable = (EatableModel) eatables.toArray()[0];
            Location location = new Location(TestJava.world, eatable.getX(), eatable.getY(), eatable.getZ());
            if (locust.getLocation().distance(location) <= 4) {
                location.getBlock().setType(Material.AIR);
                EatableRepository.remove(eatable);
                TestJava.locustTargets.remove(uuid);
                locust.remove();
                Bukkit.getServer().broadcastMessage(ChatColor.GRAY + "Un criquet a ravagé du blé à "
                        + Colorize.name(eatable.getVillage()));
                return;
            }

            locust.getPathfinder().moveTo(location, 1D);
        }));
    }
}
