package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;

import java.util.Collection;

/**
 * Service pour synchroniser l'état des métiers custom au démarrage du serveur
 */
public class CustomJobSynchronizationService {

    /**
     * Synchronise tous les employés custom au démarrage du serveur
     * - Équipe automatiquement les armures de cuir
     * - Vérifie la cohérence des données
     */
    public static void synchronizeCustomJobsOnStartup() {
        Bukkit.getLogger().info("[CustomJobSync] Synchronisation des métiers custom au démarrage...");
        
        try {
            Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
            int customJobVillagers = 0;
            int armorEquipped = 0;
            int orphansRemoved = 0;

            for (VillagerModel villager : allVillagers) {
                if (!villager.hasCustomJob()) {
                    continue;
                }

                customJobVillagers++;

                try {
                    // Récupérer l'entité villageois du monde
                    Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villager.getId());
                    if (entity == null) {
                        // Villageois fantôme : nettoyer le métier custom
                        villager.clearJob();
                        VillagerRepository.update(villager);
                        orphansRemoved++;
                        Bukkit.getLogger().warning("[CustomJobSync] Villageois fantôme nettoyé: " + villager.getId());
                        continue;
                    }

                    // Équiper l'armure si nécessaire
                    if (!CustomJobArmorService.isWearingLeatherArmor(entity)) {
                        CustomJobArmorService.equipLeatherArmor(entity);
                        villager.setHasLeatherArmor(true);
                        VillagerRepository.update(villager);
                        armorEquipped++;
                        
                        Bukkit.getLogger().info("[CustomJobSync] Armure équipée pour employé " + villager.getCurrentJobName() + 
                                               " (villageois: " + villager.getId() + ")");
                    }

                } catch (Exception e) {
                    Bukkit.getLogger().warning("[CustomJobSync] Erreur synchronisation villageois " + villager.getId() + ": " + e.getMessage());
                }
            }

            // Rapport de synchronisation
            Bukkit.getLogger().info("[CustomJobSync] ✅ Synchronisation terminée:");
            Bukkit.getLogger().info("[CustomJobSync]   - Employés custom trouvés: " + customJobVillagers);
            Bukkit.getLogger().info("[CustomJobSync]   - Armures équipées: " + armorEquipped);
            Bukkit.getLogger().info("[CustomJobSync]   - Villageois fantômes nettoyés: " + orphansRemoved);

            if (customJobVillagers > 0) {
                Bukkit.getServer().broadcastMessage(
                    org.bukkit.ChatColor.AQUA + "🔄 Système de métiers custom synchronisé - " + 
                    org.bukkit.ChatColor.YELLOW + customJobVillagers + 
                    org.bukkit.ChatColor.WHITE + " employés trouvés, " + 
                    org.bukkit.ChatColor.YELLOW + armorEquipped + 
                    org.bukkit.ChatColor.WHITE + " armures équipées."
                );
            }

        } catch (Exception e) {
            Bukkit.getLogger().severe("[CustomJobSync] Erreur critique lors de la synchronisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Équipe l'armure d'un villageois spécifique quand il obtient un métier custom
     */
    public static void equipArmorOnJobAssignment(VillagerModel villager) {
        if (!villager.hasCustomJob()) {
            return;
        }

        try {
            Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villager.getId());
            if (entity == null) {
                Bukkit.getLogger().warning("[CustomJobSync] Impossible d'équiper l'armure - villageois introuvable: " + villager.getId());
                return;
            }

            // Programmer l'équipement de l'armure avec un délai pour s'assurer que l'entité est stable
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                CustomJobArmorService.equipLeatherArmor(entity);
                villager.setHasLeatherArmor(true);
                VillagerRepository.update(villager);
                
                Bukkit.getLogger().info("[CustomJobSync] Armure équipée immédiatement pour nouveau employé " + 
                                       villager.getCurrentJobName() + " (villageois: " + villager.getId() + ")");
            }, 20L); // 1 seconde de délai

        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobSync] Erreur équipement armure pour " + villager.getId() + ": " + e.getMessage());
        }
    }
}
