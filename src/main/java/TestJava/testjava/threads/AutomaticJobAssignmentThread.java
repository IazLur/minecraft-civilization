package TestJava.testjava.threads;

import TestJava.testjava.services.AutomaticJobAssignmentService;
import org.bukkit.Bukkit;

/**
 * Thread pour l'assignation automatique d'emplois aux villageois inactifs
 * S'exécute toutes les minutes pour chercher des opportunités d'emploi
 */
public class AutomaticJobAssignmentThread implements Runnable {
    
    @Override
    public void run() {
        try {
            // Exécuter l'assignation automatique
            AutomaticJobAssignmentService.executeAutomaticJobAssignment();
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[AutoJobAssignmentThread] ❌ Erreur lors du cycle d'assignation automatique: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
