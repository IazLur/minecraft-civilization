package TestJava.testjava.listeners;

import org.bukkit.entity.Pillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * Listener pour désactiver la peur des villageois face aux pillagers avec customName
 * (armée du village)
 */
public class VillagerFearListener implements Listener {

    /**
     * Empêche les villageois de fuir les pillagers avec des noms personnalisés (armée du village)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerFearPillager(EntityTargetLivingEntityEvent event) {
        // Si un pillager cible un villageois
        if (event.getEntity() instanceof Pillager pillager && 
            event.getTarget() instanceof Villager villager) {
            
            // Si le pillager a un nom personnalisé (= armée du village)
            if (pillager.getCustomName() != null) {
                // Annuler le ciblage pour empêcher la peur
                event.setCancelled(true);
                event.setTarget(null);
                
                // Log pour debug (réduit le spam)
                // org.bukkit.Bukkit.getLogger().info("[VillagerFear] Peur annulée: " + 
                //                                  pillager.getCustomName() + " ne fait plus peur aux villageois");
            }
        }
    }
    
    /**
     * Empêche les villageois de cibler (fuir) les pillagers avec des noms personnalisés
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerTargetPillager(EntityTargetLivingEntityEvent event) {
        // Si un villageois cible (fuit) un pillager
        if (event.getEntity() instanceof Villager villager && 
            event.getTarget() instanceof Pillager pillager) {
            
            // Si le pillager a un nom personnalisé (= armée du village)
            if (pillager.getCustomName() != null) {
                // Annuler le ciblage pour empêcher la fuite
                event.setCancelled(true);
                event.setTarget(null);
                
                // Log pour debug (réduit le spam)
                // org.bukkit.Bukkit.getLogger().info("[VillagerFear] Fuite annulée: villageois ne fuit plus " + 
                //                                  pillager.getCustomName());
            }
        }
    }
}