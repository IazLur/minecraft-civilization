package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.services.CartographeService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Pillager;

/**
 * Thread pour surveiller les mouvements des mercenaires
 * et déclencher les alertes des cartographes
 */
public class CartographeMercenaryTrackingThread implements Runnable {

    @Override
    public void run() {
        try {
            if (TestJava.world == null) {
                return;
            }

            // Surveiller tous les mercenaires dans le monde
            for (Entity entity : TestJava.world.getEntities()) {
                if (isMercenary(entity)) {
                    CartographeService.handleMercenaryMovement(entity);
                }
            }

            // Nettoyer les données obsolètes périodiquement
            CartographeService.cleanupOldMovementData();

        } catch (Exception e) {
            Bukkit.getLogger().warning("[CartographeMercenaryTracking] Erreur dans le thread: " + e.getMessage());
        }
    }

    /**
     * Vérifie si une entité est un mercenaire
     */
    private boolean isMercenary(Entity entity) {
        if (!entity.isCustomNameVisible()) {
            return false;
        }

        // Vérifier si c'est un type de mercenaire
        if (!(entity instanceof Zombie || entity instanceof Skeleton || entity instanceof Pillager)) {
            return false;
        }

        // Utiliser customName() pour éviter l'avertissement de dépréciation
        if (entity.customName() != null) {
            String customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(entity.customName());
            return (entity instanceof Zombie && customName.contains("Mercenaire")) ||
                   (customName.contains("[") && customName.contains("]"));
        }

        return false;
    }
}
