package TestJava.testjava.threads;

import TestJava.testjava.services.VillagerInventoryService;
import org.bukkit.Bukkit;

/**
 * Thread qui s'exécute périodiquement pour approvisionner les fermiers en nourriture
 * Permet aux fermiers d'avoir des stocks à vendre aux autres villageois
 */
public class FarmerSupplyThread implements Runnable {

    @Override
    public void run() {
        try {
            // Donner de la nourriture aux fermiers pour qu'ils puissent la vendre
            VillagerInventoryService.giveFoodToFarmers();
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[FarmerSupplyThread] Erreur lors de l'approvisionnement des fermiers: " + e.getMessage());
            e.printStackTrace();
        }
    }
}