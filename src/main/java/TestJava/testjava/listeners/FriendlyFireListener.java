package TestJava.testjava.listeners;

import TestJava.testjava.helpers.CustomName;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Listener pour empêcher les dégâts entre entités du même village
 * (friendly fire entre armée, garnison, gardes et villageois)
 */
public class FriendlyFireListener implements Listener {

    /**
     * Empêche les dégâts entre entités du même village
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        LivingEntity attacker = getActualAttacker(event);
        LivingEntity victim = getActualVictim(event);
        
        if (attacker == null || victim == null) {
            return;
        }
        
        // Vérifier si les deux entités ont des noms personnalisés (= entités gérées)
        if (attacker.getCustomName() == null || victim.getCustomName() == null) {
            return;
        }
        
        // Extraire les noms de village des deux entités
        String attackerVillage = CustomName.extractVillageName(attacker.getCustomName());
        String victimVillage = CustomName.extractVillageName(victim.getCustomName());
        
        // Si les deux entités appartiennent au même village, annuler les dégâts
        if (attackerVillage != null && victimVillage != null && attackerVillage.equals(victimVillage)) {
            event.setCancelled(true);
            event.setDamage(0.0);
            
            org.bukkit.Bukkit.getLogger().info("[FriendlyFire] Dégâts annulés entre " + 
                                             attacker.getType() + " et " + victim.getType() + 
                                             " du village " + attackerVillage);
        }
    }
    
    /**
     * Obtient l'attaquant réel (gère les projectiles)
     */
    private LivingEntity getActualAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity damager = (LivingEntity) event.getDamager();
            
            // Vérifier que c'est une entité de village (Skeleton, Pillager, Villager)
            if (damager instanceof Skeleton || damager instanceof Pillager || damager instanceof Villager) {
                return damager;
            }
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            ProjectileSource source = projectile.getShooter();
            
            if (source instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) source;
                
                // Vérifier que c'est une entité de village
                if (shooter instanceof Skeleton || shooter instanceof Pillager || shooter instanceof Villager) {
                    return shooter;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Obtient la victime réelle
     */
    private LivingEntity getActualVictim(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity victim = (LivingEntity) event.getEntity();
            
            // Vérifier que c'est une entité de village (Skeleton, Pillager, Villager)
            if (victim instanceof Skeleton || victim instanceof Pillager || victim instanceof Villager) {
                return victim;
            }
        }
        
        return null;
    }
}