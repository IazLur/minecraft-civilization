package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.Collection;
public class DailyBuildingCostThread implements Runnable {

    @Override
    public void run() {
        Collection<BuildingModel> allBuildings = BuildingRepository.getAll();

        for (BuildingModel building : allBuildings) {
            VillageModel village = VillageRepository.get(building.getVillageName());

            if(village == null) {
                continue; // passez à la prochaine itération
            }

            EmpireModel empire = EmpireRepository.getForPlayer(village.getPlayerName());

            if(empire == null) {
                continue; // passez à la prochaine itération
            }

            if (empire.getJuridictionCount() >= building.getCostPerDay()) {
                empire.setJuridictionCount(empire.getJuridictionCount() - building.getCostPerDay());
                EmpireRepository.update(empire);
                Bukkit.getServer().broadcastMessage(Colorize.name(empire.getEmpireName()) + " a payé " + Colorize.name(building.getCostPerDay() + "µ")
                        + " pour " + Colorize.name(building.getBuildingType()));
            } else {
                Location loc = new Location(TestJava.world,
                        building.getX(), building.getY(), building.getZ());
                BuildingRepository.remove(building);
                loc.getBlock().setType(Material.AIR);
                loc.setY(loc.getY() + 1);

                // Réduire la recherche d'entités à une petite zone autour de la Location du bâtiment
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (entity instanceof ArmorStand && entity.getLocation().distance(loc) < 1.0) {
                        entity.remove();
                    }
                }

                Bukkit.getServer().broadcastMessage(Colorize.name(building.getBuildingType()) + " s'est effrondré à " + Colorize.name(village.getId()) +
                        " par négligence");
            }
        }
    }
}