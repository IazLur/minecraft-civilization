package TestJava.testjava.listeners;

import TestJava.testjava.services.VillagerMovementManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;

/**
 * Listener pour gérer les conflits de déplacement des villageois
 * 
 * Ce listener intercepte les tentatives de déplacement naturel des villageois
 * quand ils sont déjà contrôlés par le VillagerMovementManager, évitant
 * les conflits entre les mouvements programmés et les comportements IA naturels.
 * 
 * Fonctionnalités :
 * - Bloque le pathfinding naturel pendant les déplacements contrôlés
 * - Empêche les changements de cible pendant les mouvements
 * - Préserve les interactions importantes (commerce, etc.)
 * - Log les conflits pour debug si nécessaire
 */
public class VillagerMovementConflictListener implements Listener {
    
    private static final boolean DEBUG_MODE = false; // Activer pour debug
    
    /**
     * Intercepte les événements de pathfinding pour bloquer les mouvements parasites
     * Cet événement se déclenche quand un villageois tente de calculer un nouveau chemin
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityPathfind(EntityPathfindEvent event) {
        Entity entity = event.getEntity();
        
        // Vérifier si c'est un villageois
        if (!(entity instanceof Villager)) {
            return;
        }
        
        Villager villager = (Villager) entity;
        
        // NEUF: Si whiteliste, ne pas annuler ce pathfinding (c'est un appel géré par nous)
        if (VillagerMovementManager.isPathfindingWhitelisted(villager)) {
            return; // laisser passer
        }
        
        // Utiliser la nouvelle méthode de résolution de conflit
        if (VillagerMovementManager.resolvePathfindingConflict(villager, "EntityPathfindEvent")) {
            // Bloquer le pathfinding naturel
            event.setCancelled(true);
            
            if (DEBUG_MODE) {
                logMovementConflict(villager, "Pathfinding naturel bloqué - villageois sous contrôle du plugin");
            }
        }
    }
    
    /**
     * Intercepte les changements de cible pour éviter les conflits
     * Empêche les villageois de changer de cible pendant un déplacement contrôlé
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        
        // Vérifier si c'est un villageois
        if (!(entity instanceof Villager)) {
            return;
        }
        
        Villager villager = (Villager) entity;
        
        // Vérifier si ce villageois est actuellement géré par notre système
        if (VillagerMovementManager.isMoving(villager)) {
            // Ne pas bloquer complètement car certaines cibles sont importantes
            // Bloquer seulement les changements de cible qui pourraient interférer
            
            // Permettre le ciblage de nourriture ou d'objets importants
            if (event.getReason() == EntityTargetEvent.TargetReason.CLOSEST_PLAYER ||
                event.getReason() == EntityTargetEvent.TargetReason.RANDOM_TARGET) {
                
                event.setCancelled(true);
                
                if (DEBUG_MODE) {
                    logMovementConflict(villager, "Changement de cible bloqué: " + event.getReason());
                }
            }
        }
    }
    
    /**
     * Préserve les interactions importantes comme le commerce
     * Ne bloque pas les interactions critiques même pendant les déplacements
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        
        // Vérifier si c'est un villageois
        if (!(entity instanceof Villager)) {
            return;
        }
        
        Villager villager = (Villager) entity;
        
        // Si le villageois est en mouvement, libérer le villageois pour l'interaction
        if (VillagerMovementManager.isMoving(villager)) {
            // Utiliser la nouvelle méthode de libération forcée
            VillagerMovementManager.forceReleaseVillager(villager, 
                "Interaction joueur " + event.getPlayer().getName());
            
            if (DEBUG_MODE) {
                logMovementConflict(villager, "Mouvement annulé pour interaction avec joueur " + 
                                   event.getPlayer().getName());
            }
            
            // L'interaction peut maintenant procéder normalement
        }
    }
    
    /**
     * Méthode utilitaire pour logger les conflits de mouvement
     * Utile pour debug et monitoring des performances
     */
    private void logMovementConflict(Villager villager, String reason) {
        String villagerInfo = String.format("[%s] %s", 
                                           villager.getUniqueId().toString().substring(0, 8),
                                           villager.getProfession());
        
        System.out.println("[VillagerMovementConflict] " + villagerInfo + " - " + reason);
    }
    
    /**
     * Active ou désactive le mode debug
     * @param enabled true pour activer les logs de debug
     */
    public static void setDebugMode(boolean enabled) {
        // Cette méthode pourrait être utilisée par une commande admin
        // ou un fichier de configuration pour contrôler le niveau de logging
    }
}
