package TestJava.testjava.threads;

import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.services.SheepService;
import org.bukkit.Bukkit;

import java.util.Collection;

/**
 * Thread qui s'exécute quotidiennement pour faire spawner des moutons dans les bergeries actives
 */
public class SheepSpawnThread implements Runnable {

    @Override
    public void run() {
        try {
            Collection<BuildingModel> allBuildings = BuildingRepository.getAll();
            int totalSpawned = 0;

            for (BuildingModel building : allBuildings) {
                if (!"bergerie".equals(building.getBuildingType())) {
                    continue;
                }

                if (!building.isActive()) {
                    continue;
                }

                // Tenter de spawner un mouton
                boolean spawned = SheepService.spawnSheepForBuilding(building);
                if (spawned) {
                    totalSpawned++;
                    Bukkit.getLogger().info("[SheepSpawn] ✅ Mouton spawné pour bergerie de " + building.getVillageName() + 
                                          " (niveau " + building.getLevel() + ")");
                }
            }

            if (totalSpawned > 0) {
                Bukkit.getLogger().info("[SheepSpawn] 📊 Total de moutons spawnés aujourd'hui: " + totalSpawned);
            }

            // Nettoyer les moutons naturels qui auraient pu apparaître
            SheepService.removeNaturalSheep();

        } catch (Exception e) {
            Bukkit.getLogger().severe("[SheepSpawn] ❌ Erreur dans SheepSpawnThread: " + e.getMessage());
            e.printStackTrace();
        }
    }
}