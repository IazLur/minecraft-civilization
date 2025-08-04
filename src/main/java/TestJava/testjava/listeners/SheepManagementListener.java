package TestJava.testjava.listeners;

import TestJava.testjava.services.SheepService;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;

public class SheepManagementListener implements Listener {

    /**
     * Empêche le spawn naturel des moutons
     * Autorise seulement les spawns via plugin ou commandes
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.SHEEP) {
            return;
        }

        // Autoriser les spawns via plugin, commandes, ou spawn eggs
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM ||
            event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG ||
            event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.DISPENSE_EGG) {
            return;
        }

        // Annuler tous les autres spawns naturels
        event.setCancelled(true);
        
        // Log pour debug
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL ||
            event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CHUNK_GEN) {
            // Silencieux pour les spawns naturels/chunk generation pour éviter le spam
        } else {
            System.out.println("[SheepManagement] ❌ Spawn de mouton annulé: " + event.getSpawnReason() + 
                             " à " + event.getLocation());
        }
    }

    /**
     * Empêche la reproduction des moutons avec customName (moutons de bergerie)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof Sheep)) {
            return;
        }

        // Vérifier si l'un des parents a un customName (mouton de bergerie)
        if ((event.getMother().getCustomName() != null && !event.getMother().getCustomName().trim().isEmpty()) ||
            (event.getFather().getCustomName() != null && !event.getFather().getCustomName().trim().isEmpty())) {
            
            event.setCancelled(true);
            System.out.println("[SheepManagement] ❌ Reproduction de moutons de bergerie empêchée");
            
            // Message aux joueurs proches si nécessaire
            event.getEntity().getWorld().getNearbyEntities(event.getEntity().getLocation(), 10, 10, 10)
                .stream()
                .filter(entity -> entity instanceof org.bukkit.entity.Player)
                .forEach(entity -> {
                    org.bukkit.entity.Player player = (org.bukkit.entity.Player) entity;
                    player.sendMessage(org.bukkit.ChatColor.RED + "Les moutons de bergerie ne peuvent pas se reproduire.");
                });
        }
    }
}