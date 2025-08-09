package TestJava.testjava.listeners;

import TestJava.testjava.enums.SocialClass;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.SocialClassService;
import TestJava.testjava.services.HistoryService;
import TestJava.testjava.services.NativeJobLevelService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;



@SuppressWarnings("deprecation")
public class SocialClassJobListener implements Listener {



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
            Bukkit.getLogger().info("[SocialClass] Villageois misérable tente d'obtenir un métier natif - annulation et blocage du pathfinding");
            
            // Annule le changement de profession
            event.setCancelled(true);
            
            // Force la profession à NONE et bloque le pathfinding
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("TestJava"), () -> {
                villager.setProfession(Villager.Profession.NONE);
                villager.getPathfinder().stopPathfinding();
                
                // Empêcher le villageois de continuer à chercher des blocs de métier
                preventMiserableFromSeekingJobs(villager);
                // MAJ nom (peut affecter l'étiquette de métier)
                SocialClassService.updateVillagerDisplayName(villagerModel);
            }, 1L);
            
            return;
        }
        
        // EMPÊCHER LES CONFLITS : Si le villageois a déjà un métier custom, empêcher l'obtention d'un métier natif
        if (villagerModel.hasCustomJob() && eventProfession != Villager.Profession.NONE) {
            Bukkit.getLogger().info("[SocialClass] Villageois avec métier custom (" + villagerModel.getCurrentJobName() + 
                                   ") tente d'obtenir un métier natif (" + eventProfession + ") - CONFLIT EMPÊCHÉ");
            
            // Annule le changement de profession
            event.setCancelled(true);
            
            // Force la profession à NONE et arrête le pathfinding
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("TestJava"), () -> {
                villager.setProfession(Villager.Profession.NONE);
                villager.getPathfinder().stopPathfinding();
                // MAJ nom (peut affecter l'étiquette de métier)
                SocialClassService.updateVillagerDisplayName(villagerModel);
            }, 1L);
            
            return;
        }

        // CORRECTION: event.getProfession() donne la NOUVELLE profession
        Villager.Profession newProfession = eventProfession;
        
        // Délai pour que le changement soit effectif
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("TestJava"), () -> {
            // Récupère la nouvelle profession après le changement
            Villager.Profession finalProfession = villager.getProfession();
            
            // Si le villageois obtient un métier natif (passe de NONE à autre chose)
            if (newProfession != Villager.Profession.NONE && 
                villagerModel.getSocialClassEnum() == SocialClass.INACTIVE &&
                !villagerModel.hasCustomJob()) { // Seulement si pas de métier custom
                
                Bukkit.getLogger().info("[SocialClass] Villageois obtient un métier natif: " + finalProfession + 
                                       " - Promotion Inactive → Ouvrière");
                
                // Marquer comme métier natif
                villagerModel.assignNativeJob();
                VillagerRepository.update(villagerModel);
                
                // NOUVEAU : Appliquer le niveau d'éducation au métier natif
                NativeJobLevelService.applyEducationToNativeJobLevel(villagerModel);
                
                // Enregistrer le changement de métier dans l'historique
                String jobName = getJobNameFromProfession(finalProfession);
                HistoryService.recordJobChange(villagerModel, jobName);
                
                SocialClassService.promoteToWorkerOnJobAssignment(villagerModel);
                // MAJ nom (étiquette de métier)
                SocialClassService.updateVillagerDisplayName(villagerModel);
            }
            
            // Si le villageois perd son métier NATIF (passe à NONE) - MAIS PAS SI IL A UN MÉTIER CUSTOM
            else if (newProfession == Villager.Profession.NONE && 
                     villagerModel.getSocialClassEnum() == SocialClass.OUVRIERE &&
                     !villagerModel.hasCustomJob()) { // CORRECTION: Ne pas rétrograder si métier custom
                
                Bukkit.getLogger().info("[SocialClass] Villageois perd son métier NATIF - " +
                                       "Rétrogradation Ouvrière → Inactive");
                
                // Nettoyer les données de métier natif
                villagerModel.clearJob();
                VillagerRepository.update(villagerModel);
                
                // Enregistrer la perte de métier dans l'historique
                HistoryService.recordJobChange(villagerModel, "Sans emploi");
                
                SocialClassService.demoteToInactiveOnJobLoss(villagerModel);
                // MAJ nom (étiquette de métier)
                SocialClassService.updateVillagerDisplayName(villagerModel);
            }
            // AJOUT: Logger si villageois custom avec profession NONE (normal)
            else if (newProfession == Villager.Profession.NONE && 
                     villagerModel.hasCustomJob()) {
                Bukkit.getLogger().info("[SocialClass] Villageois avec métier custom (" + villagerModel.getCurrentJobName() + 
                                       ") a profession NONE - C'EST NORMAL, pas de rétrogradation");
                // MAJ nom par sécurité
                SocialClassService.updateVillagerDisplayName(villagerModel);
            }
            
            // Cas général: profession a changé sans changement de classe → rafraîchir le nom
            else {
                SocialClassService.updateVillagerDisplayName(villagerModel);
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
     * Empêche un villageois misérable de chercher activement des blocs de métier
     */
    private void preventMiserableFromSeekingJobs(Villager villager) {
        // Programmer des vérifications répétées pour s'assurer que le villageois
        // ne se dirige pas vers des blocs de métier
        final int[] checkCount = {0}; // Compteur pour limiter les vérifications
        final int maxChecks = 30; // Maximum 30 vérifications (60 secondes)
        
        Bukkit.getScheduler().runTaskTimer(Bukkit.getPluginManager().getPlugin("TestJava"), new Runnable() {
            @Override
            public void run() {
                try {
                    checkCount[0]++;
                    
                    // Arrêter après un certain nombre de vérifications
                    if (checkCount[0] > maxChecks) {
                        return;
                    }
                    
                    // Vérifier si le villageois existe encore
                    VillagerModel villagerModel = VillagerRepository.find(villager.getUniqueId());
                    if (villagerModel == null) {
                        return; // Villageois supprimé, arrêter la vérification
                    }
                    
                    // Si le villageois n'est plus misérable, arrêter la vérification
                    if (SocialClassService.canVillagerHaveJob(villagerModel)) {
                        return;
                    }
                    
                    // Si le villageois a une profession, la retirer
                    if (villager.getProfession() != Villager.Profession.NONE) {
                        Bukkit.getLogger().info("[SocialClass] Retrait forcé du métier pour villageois misérable: " + 
                                               villager.getUniqueId());
                        villager.setProfession(Villager.Profession.NONE);
                    }
                    
                    // Arrêter tout pathfinding vers des blocs de métier
                    villager.getPathfinder().stopPathfinding();
                    
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[SocialClass] Erreur lors de la prévention de recherche d'emploi: " + 
                                             e.getMessage());
                }
            }
        }, 20L, 40L); // Démarrer après 1 seconde, répéter toutes les 2 secondes
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
    
    /**
     * Convertit une profession Minecraft en nom de métier français
     */
    private String getJobNameFromProfession(Villager.Profession profession) {
        if (profession == Villager.Profession.CARTOGRAPHER) {
            return "Cartographe";
        } else if (profession == Villager.Profession.CLERIC) {
            return "Clerc";
        } else if (profession == Villager.Profession.TOOLSMITH) {
            return "Forgeron d'Outils";
        } else if (profession == Villager.Profession.FLETCHER) {
            return "Archer";
        } else if (profession == Villager.Profession.SHEPHERD) {
            return "Tisserand";
        } else if (profession == Villager.Profession.MASON) {
            return "Tailleur de Pierre";
        } else if (profession == Villager.Profession.FARMER) {
            return "Fermier";
        } else if (profession == Villager.Profession.FISHERMAN) {
            return "Pêcheur";
        } else if (profession == Villager.Profession.BUTCHER) {
            return "Boucher";
        } else if (profession == Villager.Profession.ARMORER) {
            return "Armurier";
        } else if (profession == Villager.Profession.LIBRARIAN) {
            return "Bibliothécaire";
        } else if (profession == Villager.Profession.WEAPONSMITH) {
            return "Réparateur d'Armes";
        } else if (profession == Villager.Profession.LEATHERWORKER) {
            return "Travailleur du Cuir";
        } else if (profession == Villager.Profession.NONE) {
            return "Sans emploi";
        } else if (profession == Villager.Profession.NITWIT) {
            return "Idiot du village";
        } else {
            return profession.toString();
        }
    }
}