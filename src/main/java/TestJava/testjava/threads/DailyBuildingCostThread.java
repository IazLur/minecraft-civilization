package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.services.SheepService;
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

            // Si le bâtiment est inactif, pas de coût
            if (!building.isActive()) {
                continue;
            }

            if (empire.getJuridictionCount() >= building.getCostPerDay()) {
                // Payer les coûts normalement
                empire.setJuridictionCount(empire.getJuridictionCount() - building.getCostPerDay());
                EmpireRepository.update(empire);
                Bukkit.getServer().broadcastMessage(Colorize.name(empire.getEmpireName()) + " a payé " + Colorize.name(building.getCostPerDay() + "µ")
                        + " pour " + Colorize.name(building.getBuildingType()));
            } else {
                // Pas assez d'argent : désactiver le bâtiment au lieu de le détruire
                building.setActive(false);
                BuildingRepository.update(building);

                // Si c'est une bergerie, tuer tous les moutons
                if ("bergerie".equals(building.getBuildingType())) {
                    SheepService.removeAllSheepForBuilding(building);
                    SheepService.updateSheepNamesForBuilding(building); // Mettre à jour les noms restants
                    Bukkit.getServer().broadcastMessage(Colorize.name(building.getBuildingType()) + " de " + Colorize.name(village.getId()) +
                            " s'est désactivée par manque de fonds. Tous les moutons ont été supprimés.");
                } else {
                    Bukkit.getServer().broadcastMessage(Colorize.name(building.getBuildingType()) + " de " + Colorize.name(village.getId()) +
                            " s'est désactivée par manque de fonds.");
                }

                // Mettre à jour l'ArmorStand pour montrer le statut inactif
                Location loc = new Location(TestJava.world, building.getX(), building.getY() + 1, building.getZ());
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0)) {
                    if (entity instanceof ArmorStand && entity.getLocation().distance(loc) < 1.0) {
                        ArmorStand armorStand = (ArmorStand) entity;
                        String newName = org.bukkit.ChatColor.RED + "{inactif} " + 
                                        org.bukkit.ChatColor.BLUE + "[" + village.getId() + "] " + 
                                        org.bukkit.ChatColor.WHITE + building.getBuildingType();
                        armorStand.setCustomName(newName);
                    }
                }
            }
        }
    }
}