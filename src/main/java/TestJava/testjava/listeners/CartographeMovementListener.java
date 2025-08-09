package TestJava.testjava.listeners;

import TestJava.testjava.services.CartographeService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listener pour détecter les mouvements des joueurs
 * et déclencher les alertes des cartographes
 */
public class CartographeMovementListener implements Listener {

    /**
     * Détecte les mouvements des joueurs
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Vérifier que le joueur a réellement bougé (pas juste tourné la tête)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // Pas de mouvement réel
        }

        Player player = event.getPlayer();
        CartographeService.handlePlayerMovement(player);
    }
}
