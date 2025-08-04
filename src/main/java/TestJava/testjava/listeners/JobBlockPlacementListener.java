package TestJava.testjava.listeners;

import TestJava.testjava.services.DistanceConfigService;
import TestJava.testjava.services.DistanceValidationService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Listener pour intercepter et valider la pose de blocs de métier
 */
public class JobBlockPlacementListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Vérifier si le bloc est un bloc de métier configuré
        if (!DistanceConfigService.isJobBlock(blockType)) {
            return; // Pas un bloc de métier, laisser passer
        }

        // Valider la distance pour ce bloc de métier
        DistanceValidationService.ValidationResult result = 
            DistanceValidationService.validateJobBlockPlacement(player, event.getBlock().getLocation(), blockType);

        if (!result.isValid()) {
            // Distance incorrecte - annuler la pose et informer le joueur
            event.setCancelled(true);
            player.sendMessage(result.getMessage());
            
            // Log pour debug
            System.out.println("[JobBlockPlacement] ❌ " + player.getName() + " tentative de pose " + 
                             blockType + " à distance " + String.format("%.1f", result.getCurrentDistance()) + 
                             " (requis: " + result.getMinDistance() + "-" + result.getMaxDistance() + ")");
        } else {
            // Distance correcte - autoriser la pose et confirmer
            player.sendMessage(result.getMessage());
            
            // Log pour confirmation
            System.out.println("[JobBlockPlacement] ✅ " + player.getName() + " a placé " + 
                             blockType + " à distance " + String.format("%.1f", result.getCurrentDistance()) + 
                             " (zone autorisée: " + result.getMinDistance() + "-" + result.getMaxDistance() + ")");
        }
    }
}