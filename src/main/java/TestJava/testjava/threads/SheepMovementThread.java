package TestJava.testjava.threads;

import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.SheepModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.SheepRepository;
import TestJava.testjava.services.SheepService;
import org.bukkit.Bukkit;

import java.util.Collection;

/**
 * Thread qui s'exécute toutes les 5 minutes pour déplacer les moutons vers leur bergerie
 */
public class SheepMovementThread implements Runnable {

    @Override
    public void run() {
        try {
            Collection<SheepModel> allSheep = SheepRepository.getAll();
            int movedCount = 0;

            for (SheepModel sheepModel : allSheep) {
                BuildingModel building = BuildingRepository.getBuildingById(sheepModel.getBuildingId());
                
                if (building == null) {
                    // Bergerie n'existe plus, supprimer le mouton
                    Bukkit.getLogger().warning("[SheepMovement] ⚠️ Bergerie introuvable pour mouton " + 
                                             sheepModel.getVillageName() + " N°" + sheepModel.getSheepNumber() + 
                                             ", suppression du mouton");
                    SheepRepository.remove(sheepModel);
                    continue;
                }

                if (!building.isActive()) {
                    // Bergerie inactive, ne pas déplacer
                    continue;
                }

                // Déplacer le mouton vers sa bergerie
                SheepService.moveSheepToBuilding(sheepModel, building);
                movedCount++;
            }

            if (movedCount > 0) {
                Bukkit.getLogger().info("[SheepMovement] 📍 " + movedCount + " moutons déplacés vers leur bergerie");
            }

        } catch (Exception e) {
            Bukkit.getLogger().severe("[SheepMovement] ❌ Erreur dans SheepMovementThread: " + e.getMessage());
            e.printStackTrace();
        }
    }
}