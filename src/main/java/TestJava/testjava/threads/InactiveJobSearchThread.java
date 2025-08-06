package TestJava.testjava.threads;

import TestJava.testjava.services.InactiveJobSearchService;
import org.bukkit.Bukkit;

/**
 * Thread qui s'exécute périodiquement pour faire chercher des métiers aux villageois inactifs
 */
public class InactiveJobSearchThread implements Runnable {

    @Override
    public void run() {
        try {
            Bukkit.getLogger().info("[InactiveJobSearchThread] Démarrage recherche métiers...");
            
            InactiveJobSearchService.searchJobsForInactiveVillagers();
            
            Bukkit.getLogger().info("[InactiveJobSearchThread] Recherche terminée.");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[InactiveJobSearchThread] Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}