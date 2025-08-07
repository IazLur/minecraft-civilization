package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import org.bukkit.Bukkit;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.WanderingTrader;

/**
 * Thread de maintenance pour surveiller et nettoyer les marchands spéciaux
 * S'exécute toutes les 2 minutes pour détecter les marchands orphelins
 */
public class TraderMaintenanceThread implements Runnable {

    @Override
    public void run() {
        if (TestJava.world == null) {
            return;
        }

        try {
            int orphanTraders = 0;
            int orphanLlamas = 0;

            // Nettoyer d'abord via le système de tracking du TraderThread
            int trackedCount = TraderThread.getActiveTraderCount();
            
            // Compter et supprimer les marchands orphelins (sans tracking)
            for (WanderingTrader trader : TestJava.world.getEntitiesByClass(WanderingTrader.class)) {
                // Si le marchand n'est pas tracké par le système, c'est probablement un orphelin
                if (!isTrackedTrader(trader)) {
                    trader.remove();
                    orphanTraders++;
                }
            }

            // Compter et supprimer les lamas orphelins (sans maître ou maître mort)
            for (TraderLlama llama : TestJava.world.getEntitiesByClass(TraderLlama.class)) {
                if (llama.getLeashHolder() == null || llama.getLeashHolder().isDead()) {
                    llama.remove();
                    orphanLlamas++;
                }
            }

            // Log seulement si quelque chose a été nettoyé
            if (orphanTraders > 0 || orphanLlamas > 0) {
                Bukkit.getLogger().info("[TraderMaintenance] Nettoyage : " + orphanTraders + 
                    " marchands orphelins et " + orphanLlamas + " lamas orphelins supprimés");
            }

            // Log des statistiques de temps en temps (seulement si il y a des marchands actifs)
            if (trackedCount > 0) {
                Bukkit.getLogger().info("[TraderMaintenance] " + trackedCount + " marchands actuellement trackés");
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[TraderMaintenance] Erreur pendant la maintenance : " + e.getMessage());
        }
    }

    /**
     * Vérifie si un marchand est actuellement tracké par le système
     * Note: Cette méthode est simplifiée - on pourrait l'améliorer avec un accès direct au tracking
     */
    private boolean isTrackedTrader(WanderingTrader trader) {
        // Pour l'instant, on considère qu'un marchand avec un despawnDelay > 0 est probablement tracké
        // Cette heuristique n'est pas parfaite mais aide à identifier les orphelins
        return trader.getDespawnDelay() > 0;
    }
}
