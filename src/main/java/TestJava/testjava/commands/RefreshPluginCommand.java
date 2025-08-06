package TestJava.testjava.commands;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.CustomJobArmorService;
import TestJava.testjava.threads.CustomJobMaintenanceThread;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.Collection;

/**
 * Commande pour rafraîchir et corriger l'état du plugin
 * - Équipe automatiquement les armures de cuir aux employés custom
 * - Synchronise les données des villageois
 */
public class RefreshPluginCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande ne peut être exécutée que par un joueur.");
            return true;
        }

        Player player = (Player) sender;
        
        // Vérifier les permissions (optionnel - vous pouvez ajouter une vérification de permission ici)
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "🔄 Rafraîchissement du plugin en cours...");
        
        // Exécuter le rafraîchissement sur le thread principal pour éviter AsyncCatcher
        Bukkit.getScheduler().runTask(TestJava.plugin, () -> {
            refreshCustomJobArmors(player);
        });

        return true;
    }

    /**
     * Équipe automatiquement les armures de cuir pour tous les employés custom
     */
    private void refreshCustomJobArmors(Player player) {
        try {
            Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
            int armorEquipped = 0;
            int customJobVillagers = 0;

            player.sendMessage(ChatColor.AQUA + "📋 Analyse des villageois en cours...");

            for (VillagerModel villager : allVillagers) {
                if (!villager.hasCustomJob()) {
                    continue; // Passer les villageois sans métier custom
                }

                customJobVillagers++;

                try {
                    // Récupérer l'entité villageois du monde
                    Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villager.getId());
                    if (entity == null) {
                        Bukkit.getLogger().warning("[RefreshPlugin] Villageois fantôme détecté: " + villager.getId());
                        continue;
                    }

                    // Vérifier et équiper l'armure si nécessaire
                    if (!CustomJobArmorService.isWearingLeatherArmor(entity)) {
                        CustomJobArmorService.equipLeatherArmor(entity);
                        
                        villager.setHasLeatherArmor(true);
                        VillagerRepository.update(villager);
                        armorEquipped++;

                        Bukkit.getLogger().info("[RefreshPlugin] Armure équipée pour " + villager.getId() + 
                                               " (métier: " + villager.getCurrentJobName() + ")");
                    }

                } catch (Exception e) {
                    Bukkit.getLogger().warning("[RefreshPlugin] Erreur traitement villageois " + villager.getId() + ": " + e.getMessage());
                }
            }

            // Rapport final
            player.sendMessage(ChatColor.GREEN + "✅ Rafraîchissement terminé !");
            player.sendMessage(ChatColor.WHITE + "📊 Employés custom trouvés: " + ChatColor.YELLOW + customJobVillagers);
            player.sendMessage(ChatColor.WHITE + "🛡️ Armures équipées: " + ChatColor.YELLOW + armorEquipped);
            
            // Déclencher une vérification de cohérence
            CustomJobMaintenanceThread maintenanceThread = new CustomJobMaintenanceThread();
            maintenanceThread.run();
            player.sendMessage(ChatColor.AQUA + "🔍 Vérification de cohérence effectuée (voir logs serveur)");
            
            if (armorEquipped > 0) {
                Bukkit.getServer().broadcastMessage(
                    ChatColor.AQUA + player.getName() + ChatColor.WHITE + 
                    " a rafraîchi le plugin - " + ChatColor.YELLOW + armorEquipped + 
                    ChatColor.WHITE + " armures d'employés custom équipées."
                );
            } else {
                player.sendMessage(ChatColor.GREEN + "Tous les employés custom portaient déjà leur armure !");
            }

        } catch (Exception e) {
            Bukkit.getLogger().severe("[RefreshPlugin] Erreur critique lors du rafraîchissement: " + e.getMessage());
            e.printStackTrace();
            
            player.sendMessage(ChatColor.RED + "❌ Erreur lors du rafraîchissement. Consultez les logs du serveur.");
        }
    }
}
