package TestJava.testjava.threads;

import TestJava.testjava.services.VillageStatsService;
import org.bukkit.Bukkit;

/**
 * Thread qui synchronise périodiquement les stats des villages avec la réalité
 */
public class VillageStatsThread implements Runnable {

    @Override
    public void run() {
        try {
            VillageStatsService.synchronizeAllVillageStats();
        } catch (Exception e) {
            Bukkit.getLogger().severe("[VillageStatsThread] Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}