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
            int removedCount = 0;
            int totalSheep = allSheep.size();

            for (SheepModel sheepModel : allSheep) {
                BuildingModel building = BuildingRepository.getBuildingById(sheepModel.getBuildingId());
                
                if (building == null) {
                    // Bergerie n'existe plus, supprimer le mouton
                    SheepRepository.remove(sheepModel);
                    removedCount++;
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

            // Un seul log de résumé
            if (movedCount > 0 || removedCount > 0) {
                Bukkit.getLogger().info("[SheepMovement] 📍 Résumé: " + movedCount + " moutons déplacés, " + 
                                       removedCount + " supprimés (total: " + totalSheep + ")");
            } else {
                Bukkit.getLogger().info("[SheepMovement] ℹ️ Aucun mouton traité (total: " + totalSheep + ")");
            }

        } catch (Exception e) {
            Bukkit.getLogger().severe("[SheepMovement] ❌ Erreur dans SheepMovementThread: " + e.getMessage());
            e.printStackTrace();
        }
    }
}