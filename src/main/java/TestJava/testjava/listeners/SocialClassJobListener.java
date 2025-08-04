package TestJava.testjava.listeners;

import TestJava.testjava.enums.SocialClass;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.SocialClassService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Arrays;
import java.util.List;

public class SocialClassJobListener implements Listener {

    // Blocs de métier que les villageois ne peuvent pas utiliser s'ils sont classe 0
    private static final List<Material> JOB_BLOCKS = Arrays.asList(
        Material.COMPOSTER,
        Material.BARREL,
        Material.BLAST_FURNACE,
        Material.SMOKER,
        Material.CARTOGRAPHY_TABLE,
        Material.FLETCHING_TABLE,
        Material.GRINDSTONE,
        Material.LECTERN,
        Material.LOOM,
        Material.SMITHING_TABLE,
        Material.STONECUTTER,
        Material.CAULDRON,
        Material.BREWING_STAND,
        Material.ENCHANTING_TABLE
    );

    /**
     * Gère le changement de profession d'un villageois
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVillagerCareerChange(VillagerCareerChangeEvent event) {
        Villager villager = event.getEntity();
        VillagerModel villagerModel = VillagerRepository.find(villager.getUniqueId());
        
        if (villagerModel == null) {
            return;
        }

        Villager.Profession eventProfession = event.getProfession();
        Villager.Profession currentProfession = villager.getProfession();
        
        Bukkit.getLogger().info("[SocialClass] Changement de profession détecté: " + 
                               "event.getProfession()=" + eventProfession + 
                               ", villager.getProfession()=" + currentProfession + 
                               " pour villageois " + villager.getUniqueId() + 
                               " (classe actuelle: " + villagerModel.getSocialClassEnum().getName() + ")");

        // Si le villageois ne peut pas avoir de métier (classe 0)
        if (!SocialClassService.canVillagerHaveJob(villagerModel)) {
            Bukkit.getLogger().info("[SocialClass] Villageois classe 0 tente d'obtenir un métier - annulation");
            
            // Annule le changement de profession
            event.setCancelled(true);
            
            // Force la profession à NONE
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("TestJava"), () -> {
                villager.setProfession(Villager.Profession.NONE);
                villager.getPathfinder().stopPathfinding();
            }, 1L);
            
            return;
        }

        // CORRECTION: event.getProfession() donne la NOUVELLE profession
        Villager.Profession newProfession = eventProfession;
        Villager.Profession oldProfession = currentProfession; // Profession actuelle avant changement
        
        // Délai pour que le changement soit effectif
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("TestJava"), () -> {
            // Récupère la nouvelle profession après le changement
            Villager.Profession finalProfession = villager.getProfession();
            
            // Si le villageois obtient un métier (passe de NONE à autre chose)
            if (newProfession != Villager.Profession.NONE && 
                villagerModel.getSocialClassEnum() == SocialClass.INACTIVE) {
                Bukkit.getLogger().info("[SocialClass] Villageois obtient un métier: " + finalProfession + 
                                       " - Promotion Inactive → Ouvrière");
                SocialClassService.promoteToWorkerOnJobAssignment(villagerModel);
            }
            
            // Si le villageois perd son métier (passe à NONE)
            else if (newProfession == Villager.Profession.NONE && 
                     villagerModel.getSocialClassEnum() == SocialClass.OUVRIERE) {
                Bukkit.getLogger().info("[SocialClass] Villageois perd son métier - " +
                                       "Rétrogradation Ouvrière → Inactive");
                SocialClassService.demoteToInactiveOnJobLoss(villagerModel);
            }
        }, 2L); // Délai de 2 ticks pour s'assurer que le changement est effectif
    }

    /**
     * Gère l'acquisition de trades par les villageois
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof Villager)) {
            return;
        }
        Villager villager = (Villager) event.getEntity();
        VillagerModel villagerModel = VillagerRepository.find(villager.getUniqueId());
        
        if (villagerModel == null) {
            return;
        }

        // Si le villageois ne peut pas avoir de métier (classe 0)
        if (!SocialClassService.canVillagerHaveJob(villagerModel)) {
            Bukkit.getLogger().info("[SocialClass] Villageois classe 0 tente d'acquérir des trades - annulation");
            event.setCancelled(true);
            
            // Force la profession à NONE
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("TestJava"), () -> {
                villager.setProfession(Villager.Profession.NONE);
            }, 1L);
        }
    }

    /**
     * Empêche l'interaction avec les blocs de métier pour les villageois classe 0
     * Note: Cet événement capture les interactions des joueurs, 
     * mais nous devons aussi gérer les mouvements automatiques des villageois
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractWithJobBlock(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }

        VillagerModel villagerModel = VillagerRepository.find(villager.getUniqueId());
        if (villagerModel == null) {
            return;
        }

        // Si le villageois ne peut pas avoir de métier, informe le joueur
        if (!SocialClassService.canVillagerHaveJob(villagerModel)) {
            SocialClass socialClass = villagerModel.getSocialClassEnum();
            event.getPlayer().sendMessage(
                ChatColor.RED + "Ce villageois (" + socialClass.getColoredTag() + ChatColor.RED + 
                ") ne peut pas exercer de métier car il est de classe " + socialClass.getName()
            );
            
            // Optionnel: annuler l'interaction si nécessaire
            // event.setCancelled(true);
        }
    }

    /**
     * Vérifie si un bloc est un bloc de métier
     */
    private boolean isJobBlock(Block block) {
        return JOB_BLOCKS.contains(block.getType());
    }

    /**
     * Vérifie périodiquement et retire les métiers des villageois classe 0
     * (Cette méthode peut être appelée par un thread)
     */
    public static void enforceJobRestrictions() {
        for (VillagerModel villagerModel : VillagerRepository.getAll()) {
            if (!SocialClassService.canVillagerHaveJob(villagerModel)) {
                // Force la suppression des métiers pour les villageois classe 0
                for (org.bukkit.entity.Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
                    if (entity instanceof Villager && 
                        entity.getUniqueId().equals(villagerModel.getId())) {
                        
                        Villager villager = (Villager) entity;
                        
                        if (villager.getProfession() != Villager.Profession.NONE) {
                            Bukkit.getLogger().info("[SocialClass] Suppression forcée du métier pour villageois classe 0: " + 
                                                   villager.getUniqueId());
                            villager.setProfession(Villager.Profession.NONE);
                            villager.getPathfinder().stopPathfinding();
                        }
                        break;
                    }
                }
            }
        }
    }
}