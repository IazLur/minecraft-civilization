package TestJava.testjava.commands;

import TestJava.testjava.TestJava;
import TestJava.testjava.enums.SocialClass;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.services.GhostVillagerCleanupService;
import TestJava.testjava.services.SocialClassService;
import TestJava.testjava.services.VillagerSynchronizationService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.*;
import java.util.stream.Collectors;

public class SocialCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        // Vérification des arguments
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /social <village|villager|stats|refresh|migrate|cleanup|refreshnames|sync|testnames|migrateformat>");
            player.sendMessage(ChatColor.YELLOW + "  /social village <nom> - Statistiques des classes sociales d'un village");
            player.sendMessage(ChatColor.YELLOW + "  /social villager - Informations du villageois le plus proche");
            player.sendMessage(ChatColor.YELLOW + "  /social stats - Statistiques globales des classes sociales");
            player.sendMessage(ChatColor.YELLOW + "  /social refresh - Force la mise à jour de tous les villageois");
            player.sendMessage(ChatColor.YELLOW + "  /social migrate - Migration/réévaluation complète (villageois existants)");
            player.sendMessage(ChatColor.YELLOW + "  /social cleanup - Nettoie les villageois fantômes (admin)");
            player.sendMessage(ChatColor.YELLOW + "  /social refreshnames - Force la mise à jour des noms (debug)");
            player.sendMessage(ChatColor.YELLOW + "  /social sync - Synchronise villageois monde/base (admin)");
            player.sendMessage(ChatColor.YELLOW + "  /social testnames - Test extraction noms (admin debug)");
            player.sendMessage(ChatColor.YELLOW + "  /social migrateformat - Migration format tags [0] → {0} (admin)");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "village":
                return handleVillageCommand(player, args);
            case "villager":
                return handleVillagerCommand(player);
            case "stats":
                return handleStatsCommand(player);
            case "refresh":
                return handleRefreshCommand(player);
            case "migrate":
                return handleMigrateCommand(player);
            case "cleanup":
                return handleCleanupCommand(player);
            case "refreshnames":
                return handleRefreshNamesCommand(player);
            case "sync":
                return handleSyncCommand(player);
            case "testnames":
                return handleTestNamesCommand(player);
            case "migrateformat":
                return handleMigrateFormatCommand(player);
            default:
                player.sendMessage(ChatColor.RED + "Sous-commande inconnue: " + subCommand);
                return true;
        }
    }

    /**
     * Affiche les statistiques des classes sociales d'un village
     */
    private boolean handleVillageCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /social village <nom>");
            return true;
        }

        String villageName = args[1];
        VillageModel village = VillageRepository.get(villageName);
        
        if (village == null) {
            player.sendMessage(ChatColor.RED + "Village '" + villageName + "' introuvable");
            return true;
        }

        // Récupère tous les villageois du village
        Collection<VillagerModel> villagers = VillagerRepository.getAll()
                .stream()
                .filter(v -> villageName.equals(v.getVillageName()))
                .collect(Collectors.toList());

        if (villagers.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aucun villageois trouvé dans " + villageName);
            return true;
        }

        // Compte par classe sociale
        Map<SocialClass, Long> classCounts = villagers.stream()
                .collect(Collectors.groupingBy(
                    VillagerModel::getSocialClassEnum,
                    Collectors.counting()
                ));

        player.sendMessage(ChatColor.GOLD + "=== Classes Sociales de " + villageName + " ===");
        player.sendMessage(ChatColor.WHITE + "Population totale: " + ChatColor.YELLOW + villagers.size());
        
        for (SocialClass socialClass : SocialClass.values()) {
            long count = classCounts.getOrDefault(socialClass, 0L);
            if (count > 0 || socialClass.getLevel() <= 2) { // Affiche classes 0-2 même si 0
                double percentage = (double) count / villagers.size() * 100;
                player.sendMessage(socialClass.getColor() + socialClass.getName() + 
                                 ChatColor.WHITE + ": " + count + 
                                 ChatColor.GRAY + " (" + String.format("%.1f", percentage) + "%)");
            }
        }

        return true;
    }

    /**
     * Affiche les informations du villageois le plus proche
     */
    private boolean handleVillagerCommand(Player player) {
        Entity nearestVillager = null;
        double nearestDistance = Double.MAX_VALUE;

        // Trouve le villageois le plus proche
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Villager) {
                double distance = player.getLocation().distance(entity.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestVillager = entity;
                }
            }
        }

        if (nearestVillager == null) {
            player.sendMessage(ChatColor.RED + "Aucun villageois trouvé dans un rayon de 10 blocs");
            return true;
        }

        Villager villager = (Villager) nearestVillager;
        VillagerModel villagerModel = VillagerRepository.find(villager.getUniqueId());

        if (villagerModel == null) {
            player.sendMessage(ChatColor.RED + "Données du villageois introuvables");
            return true;
        }

        SocialClass socialClass = villagerModel.getSocialClassEnum();
        
        player.sendMessage(ChatColor.GOLD + "=== Informations Villageois ===");
        player.sendMessage(ChatColor.WHITE + "UUID: " + ChatColor.GRAY + villager.getUniqueId());
        player.sendMessage(ChatColor.WHITE + "Village: " + ChatColor.YELLOW + villagerModel.getVillageName());
        player.sendMessage(ChatColor.WHITE + "Nourriture: " + ChatColor.GREEN + villagerModel.getFood());
        player.sendMessage(ChatColor.WHITE + "Classe Sociale: " + socialClass.getColoredTag() + 
                         ChatColor.WHITE + " " + socialClass.getName());
        player.sendMessage(ChatColor.WHITE + "Profession: " + ChatColor.AQUA + villager.getProfession());
        player.sendMessage(ChatColor.WHITE + "Peut avoir métier: " + 
                         (socialClass.canHaveJob() ? ChatColor.GREEN + "Oui" : ChatColor.RED + "Non"));

        return true;
    }

    /**
     * Affiche les statistiques globales du système de classes sociales
     */
    private boolean handleStatsCommand(Player player) {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        
        if (allVillagers.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aucun villageois dans la base de données");
            return true;
        }

        // Statistiques globales
        Map<SocialClass, Long> globalCounts = allVillagers.stream()
                .collect(Collectors.groupingBy(
                    VillagerModel::getSocialClassEnum,
                    Collectors.counting()
                ));

        // Statistiques par village
        Map<String, Long> villageCounts = allVillagers.stream()
                .collect(Collectors.groupingBy(
                    VillagerModel::getVillageName,
                    Collectors.counting()
                ));

        player.sendMessage(ChatColor.GOLD + "=== Statistiques Globales Classes Sociales ===");
        player.sendMessage(ChatColor.WHITE + "Population totale: " + ChatColor.YELLOW + allVillagers.size());
        player.sendMessage(ChatColor.WHITE + "Nombre de villages: " + ChatColor.YELLOW + villageCounts.size());
        
        player.sendMessage(ChatColor.GOLD + "Répartition par classe:");
        for (SocialClass socialClass : SocialClass.values()) {
            long count = globalCounts.getOrDefault(socialClass, 0L);
            if (count > 0 || socialClass.getLevel() <= 2) { // Affiche classes 0-2 même si 0
                double percentage = (double) count / allVillagers.size() * 100;
                player.sendMessage("  " + socialClass.getColor() + socialClass.getName() + 
                                 ChatColor.WHITE + ": " + count + 
                                 ChatColor.GRAY + " (" + String.format("%.1f", percentage) + "%)");
            }
        }

        return true;
    }

    /**
     * Force la mise à jour de tous les villageois
     */
    private boolean handleRefreshCommand(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Démarrage de la mise à jour forcée...");
        
        int updated = 0;
        for (VillagerModel villager : VillagerRepository.getAll()) {
            try {
                SocialClassService.evaluateAndUpdateSocialClass(villager);
                SocialClassService.updateVillagerDisplayName(villager);
                updated++;
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Erreur villageois " + villager.getId() + ": " + e.getMessage());
            }
        }
        
        player.sendMessage(ChatColor.GREEN + "✅ " + updated + " villageois mis à jour");
        
        // Force l'enforcement des règles de métier
        try {
            Bukkit.getScheduler().runTask(TestJava.plugin, () -> {
                SocialClassService.initializeSocialClassForExistingVillagers();
                player.sendMessage(ChatColor.GREEN + "✅ Enforcement des règles de métier terminé");
            });
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Erreur lors de l'enforcement: " + e.getMessage());
        }
        
        return true;
    }

    /**
     * Migration et réévaluation complète des villageois existants
     */
    private boolean handleMigrateCommand(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Migration des Classes Sociales ===");
        player.sendMessage(ChatColor.YELLOW + "⚠️ Cette opération va réévaluer TOUS les villageois...");
        
        // Décompte des villageois avant migration
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        int totalBefore = allVillagers.size();
        
        Map<SocialClass, Long> beforeCounts = allVillagers.stream()
                .collect(Collectors.groupingBy(
                    VillagerModel::getSocialClassEnum,
                    Collectors.counting()
                ));
        
        player.sendMessage(ChatColor.WHITE + "Villageois avant migration: " + ChatColor.YELLOW + totalBefore);
        
        // Exécute la migration
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Migration des nouveaux (null → classe sociale)
            SocialClassService.initializeSocialClassForExistingVillagers();
            
            // 2. Réévaluation forcée de tous
            SocialClassService.forceReevaluateAllVillagers();
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            // Statistiques après migration
            Collection<VillagerModel> allVillagersAfter = VillagerRepository.getAll();
            Map<SocialClass, Long> afterCounts = allVillagersAfter.stream()
                    .collect(Collectors.groupingBy(
                        VillagerModel::getSocialClassEnum,
                        Collectors.counting()
                    ));
            
            player.sendMessage(ChatColor.GREEN + "✅ Migration terminée en " + 
                             String.format("%.2f", duration) + " secondes");
            
            // Affiche les changements
            player.sendMessage(ChatColor.GOLD + "Changements détectés:");
            for (SocialClass socialClass : SocialClass.values()) {
                if (socialClass.getLevel() <= 2) { // Affiche seulement les classes implémentées
                    long before = beforeCounts.getOrDefault(socialClass, 0L);
                    long after = afterCounts.getOrDefault(socialClass, 0L);
                    long diff = after - before;
                    
                    String diffStr = "";
                    if (diff > 0) {
                        diffStr = ChatColor.GREEN + " (+" + diff + ")";
                    } else if (diff < 0) {
                        diffStr = ChatColor.RED + " (" + diff + ")";
                    } else {
                        diffStr = ChatColor.GRAY + " (=)";
                    }
                    
                    player.sendMessage("  " + socialClass.getColor() + socialClass.getName() + 
                                     ChatColor.WHITE + ": " + before + " → " + after + diffStr);
                }
            }
            
            player.sendMessage(ChatColor.DARK_GREEN + "🏆 Tous les villageois ont maintenant des classes sociales appropriées !");
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "❌ Erreur lors de la migration: " + e.getMessage());
            Bukkit.getLogger().severe("[SocialClass] Erreur migration: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }

    /**
     * Nettoyage des villageois fantômes (admin seulement)
     */
    private boolean handleCleanupCommand(Player player) {
        // Vérification des permissions admin
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ Cette commande est réservée aux administrateurs");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Nettoyage des Villageois Fantômes ===");
        player.sendMessage(ChatColor.YELLOW + "🔍 Analyse en cours...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Lance le nettoyage
            GhostVillagerCleanupService.CleanupResult result = GhostVillagerCleanupService.cleanupGhostVillagers();
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            // Affiche les résultats
            player.sendMessage(ChatColor.WHITE + "Durée: " + ChatColor.YELLOW + String.format("%.2f", duration) + " secondes");
            player.sendMessage(ChatColor.WHITE + "Villageois en base: " + ChatColor.YELLOW + result.totalInDB);
            player.sendMessage(ChatColor.WHITE + "Villageois dans le monde: " + ChatColor.YELLOW + result.totalInWorld);
            
            if (result.hasGhosts()) {
                player.sendMessage(ChatColor.RED + "👻 Fantômes détectés: " + result.ghostsDetected);
                player.sendMessage(ChatColor.GREEN + "🧹 Fantômes supprimés: " + result.ghostsRemoved);
                player.sendMessage(ChatColor.AQUA + "🏘️ Villages mis à jour: " + result.villagesUpdated);
                
                if (result.errors > 0) {
                    player.sendMessage(ChatColor.RED + "❌ Erreurs: " + result.errors);
                }
                
                if (result.wasSuccessful()) {
                    player.sendMessage(ChatColor.GREEN + "✅ Nettoyage terminé avec succès !");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "⚠️ Nettoyage terminé avec quelques problèmes");
                }
                
                // Broadcast global
                Bukkit.getServer().broadcastMessage(
                    ChatColor.GRAY + "🧹 " + ChatColor.YELLOW + player.getName() + 
                    ChatColor.GRAY + " a nettoyé " + ChatColor.RED + result.ghostsRemoved + 
                    ChatColor.GRAY + " villageois fantômes"
                );
                
            } else {
                player.sendMessage(ChatColor.GREEN + "✅ Aucun villageois fantôme détecté !");
                player.sendMessage(ChatColor.GRAY + "Toutes les données sont cohérentes.");
            }
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "❌ Erreur lors du nettoyage: " + e.getMessage());
            Bukkit.getLogger().severe("[SocialCommand] Erreur cleanup: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }

    /**
     * Force la mise à jour des noms de tous les villageois (debug)
     */
    private boolean handleRefreshNamesCommand(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Actualisation des Noms des Villageois ===");
        player.sendMessage(ChatColor.YELLOW + "🔄 Mise à jour des noms en cours...");
        
        long startTime = System.currentTimeMillis();
        int updated = 0;
        int errors = 0;
        
        try {
            Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
            
            for (VillagerModel villager : allVillagers) {
                try {
                    SocialClassService.updateVillagerDisplayName(villager);
                    updated++;
                } catch (Exception e) {
                    errors++;
                    Bukkit.getLogger().warning("[SocialCommand] Erreur mise à jour nom villageois " + 
                                             villager.getId() + ": " + e.getMessage());
                }
            }
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            player.sendMessage(ChatColor.WHITE + "Durée: " + ChatColor.YELLOW + String.format("%.2f", duration) + " secondes");
            player.sendMessage(ChatColor.GREEN + "✅ Noms mis à jour: " + updated);
            
            if (errors > 0) {
                player.sendMessage(ChatColor.RED + "❌ Erreurs: " + errors);
            }
            
            player.sendMessage(ChatColor.GRAY + "💡 Vérifiez les logs serveur pour les détails de mise à jour");
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "❌ Erreur lors de l'actualisation: " + e.getMessage());
            Bukkit.getLogger().severe("[SocialCommand] Erreur refresh names: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }

    /**
     * Synchronisation des villageois du monde avec la base de données (admin seulement)
     */
    private boolean handleSyncCommand(Player player) {
        // Vérification des permissions admin
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ Cette commande est réservée aux administrateurs");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Synchronisation Villageois Monde/Base ===");
        player.sendMessage(ChatColor.YELLOW + "🔄 Analyse et synchronisation en cours...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Lance la synchronisation
            VillagerSynchronizationService.SynchronizationResult result = 
                VillagerSynchronizationService.synchronizeWorldVillagersWithDatabase();
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            // Affiche les résultats
            player.sendMessage(ChatColor.WHITE + "Durée: " + ChatColor.YELLOW + String.format("%.2f", duration) + " secondes");
            player.sendMessage(ChatColor.WHITE + "Villageois en base: " + ChatColor.YELLOW + result.existingInDB);
            player.sendMessage(ChatColor.WHITE + "Villageois dans le monde: " + ChatColor.YELLOW + result.worldVillagersWithName);
            
            if (result.foundUnsynchronized()) {
                player.sendMessage(ChatColor.AQUA + "🔄 Nouveaux synchronisés: " + result.syncedCount);
                player.sendMessage(ChatColor.GREEN + "🏘️ Villages mis à jour: " + result.villagesUpdated);
                
                if (result.errors > 0) {
                    player.sendMessage(ChatColor.RED + "❌ Erreurs: " + result.errors);
                }
                
                if (result.wasSuccessful()) {
                    player.sendMessage(ChatColor.GREEN + "✅ Synchronisation terminée avec succès !");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "⚠️ Synchronisation terminée avec quelques problèmes");
                }
                
                // Broadcast global
                Bukkit.getServer().broadcastMessage(
                    ChatColor.AQUA + "🔄 " + ChatColor.YELLOW + player.getName() + 
                    ChatColor.AQUA + " a synchronisé " + ChatColor.GREEN + result.syncedCount + 
                    ChatColor.AQUA + " villageois avec la base de données"
                );
                
            } else {
                player.sendMessage(ChatColor.GREEN + "✅ Tous les villageois sont déjà synchronisés !");
                player.sendMessage(ChatColor.GRAY + "Aucune action nécessaire.");
            }
            
            player.sendMessage(ChatColor.GRAY + "💡 Consultez les logs serveur pour plus de détails");
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "❌ Erreur lors de la synchronisation: " + e.getMessage());
            Bukkit.getLogger().severe("[SocialCommand] Erreur sync: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }

    /**
     * Test d'extraction des noms de village (admin debug)
     */
    private boolean handleTestNamesCommand(Player player) {
        // Vérification des permissions admin
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ Cette commande est réservée aux administrateurs");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Test Extraction Noms de Village ===");
        
        // Formats de test
        String[] testNames = {
            "[Truc] Jean Dupont",                    // Format standard (garde, golem, etc.)
            "{0} [Truc] Jean Dupont",                // Nouveau format classe sociale
            "§e{0}§r [Truc] Jean Dupont",           // Nouveau format avec couleurs
            "§e{1}§r§b[Truc]§r Marie Martin",       // Format complexe
            "{2} [Village] Paul Durand",             // Classe 2
            "§c{3}§r [TestVillage] Anna Smith",     // Classe 3 avec couleurs
            "[0][Truc] Jean Dupont",                 // Ancien format à migrer
            "§e[0]§r[Truc] Jean Dupont",            // Ancien format avec couleurs
            "[BadFormat Jean",                       // Format invalide
            "Pas de crochets"                        // Format invalide
        };
        
        for (String testName : testNames) {
            try {
                String village = CustomName.extractVillageName(testName);
                player.sendMessage(ChatColor.GREEN + "✅ '" + testName + "' → '" + village + "'");
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "❌ '" + testName + "' → ERREUR: " + e.getMessage());
            }
        }
        
        // Test avec de vrais villageois dans le monde
        player.sendMessage(ChatColor.GOLD + "\n=== Test Villageois Réels ===");
        int tested = 0;
        int success = 0;
        int errors = 0;
        
        if (TestJava.world != null) {
            for (org.bukkit.entity.Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof org.bukkit.entity.Villager villager && villager.getCustomName() != null) {
                    tested++;
                    String customName = villager.getCustomName();
                    try {
                        String village = CustomName.extractVillageName(customName);
                        player.sendMessage(ChatColor.AQUA + "🧑 " + customName + " → " + ChatColor.YELLOW + village);
                        success++;
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "💥 " + customName + " → ERREUR: " + e.getMessage());
                        errors++;
                    }
                    
                    // Limite l'affichage pour éviter le spam
                    if (tested >= 10) {
                        player.sendMessage(ChatColor.GRAY + "... (limite de 10 villageois atteinte)");
                        break;
                    }
                }
            }
        }
        
        player.sendMessage(ChatColor.WHITE + "\n📊 Résultats: " + ChatColor.GREEN + success + " succès" + 
                          ChatColor.WHITE + ", " + ChatColor.RED + errors + " erreurs" + 
                          ChatColor.WHITE + " sur " + ChatColor.YELLOW + tested + " villageois testés");
        
        return true;
    }

    /**
     * Migration format tags classe sociale (admin debug)
     */
    private boolean handleMigrateFormatCommand(Player player) {
        // Vérification des permissions admin
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ Cette commande est réservée aux administrateurs");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Migration Format Tags Classes Sociales ===");
        player.sendMessage(ChatColor.YELLOW + "Conversion [0][Village] → {0} [Village]");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Exécuter la migration
            SocialClassService.migrateSocialClassTagsToNewFormat();
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            player.sendMessage(ChatColor.GREEN + "✅ Migration terminée en " + 
                              String.format("%.2f", duration) + " secondes");
            player.sendMessage(ChatColor.AQUA + "📊 Consultez les logs serveur pour les détails");
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "❌ Erreur lors de la migration: " + e.getMessage());
            Bukkit.getLogger().severe("[SocialCommand] Erreur migration format: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}