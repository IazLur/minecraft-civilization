package TestJava.testjava.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import TestJava.testjava.TestJava;
import TestJava.testjava.Config;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.services.CartographeService;
import TestJava.testjava.services.ArmorierService;
import TestJava.testjava.services.SocialClassService;
import TestJava.testjava.services.TaxService;
import TestJava.testjava.threads.VillagerGoEatThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final RefreshPluginCommand refreshCmd = new RefreshPluginCommand();
    private final DataCommand dataCmd = new DataCommand();
    private final EmptyVillageCommand emptyVillageCmd = new EmptyVillageCommand();
    private final ForceSpawnAtCommand forceSpawnCmd = new ForceSpawnAtCommand();
    private final TestAutoJobCommand testAutoJobCmd = new TestAutoJobCommand();
    private final TraderStatusCommand traderStatusCmd = new TraderStatusCommand();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Cette commande ne peut être exécutée que par un joueur.").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        
        // Vérifier les permissions
        if (!player.hasPermission("testjava.admin")) {
            player.sendMessage(Component.text("Vous n'avez pas la permission d'utiliser les commandes administratives.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "refresh":
                return refreshCmd.onCommand(sender, command, label, subArgs);
            case "data":
                return dataCmd.onCommand(sender, command, label, subArgs);
            case "emptyvillage":
                return emptyVillageCmd.onCommand(sender, command, label, subArgs);
            case "forcespawn":
                return forceSpawnCmd.onCommand(sender, command, label, subArgs);
            case "testautojob":
                return testAutoJobCmd.onCommand(sender, command, label, subArgs);
            case "traderstatus":
                return traderStatusCmd.onCommand(sender, command, label, subArgs);
            case "testsocialclass":
                return handleTestSocialClassCommand(player);
            case "collecttaxes":
                return handleCollectTaxesCommand(player);
            case "goeat":
                return handleGoEatCommand(player);
            case "cartographe":
                return handleCartographeCommand(player, subArgs);
            case "fletcher":
                return handleFletcherCommand(player, subArgs);
            case "armurier":
                return handleArmorierCommand(player, subArgs);
            default:
                showHelp(player);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("testjava.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "refresh", "data", "emptyvillage", "forcespawn",
                "testautojob", "traderstatus", "testsocialclass", "collecttaxes", "goeat", "cartographe", "fletcher", "armurier"
            );
            
            String input = args[0].toLowerCase();
            return subCommands.stream()
                    .filter(cmd -> cmd.startsWith(input))
                    .collect(Collectors.toList());
        }

        // Déléguer la complétion aux sous-commandes
        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "data":
                return dataCmd instanceof TabCompleter ? 
                    ((TabCompleter) dataCmd).onTabComplete(sender, command, alias, subArgs) : 
                    new ArrayList<>();
            case "emptyvillage":
                return emptyVillageCmd instanceof TabCompleter ? 
                    ((TabCompleter) emptyVillageCmd).onTabComplete(sender, command, alias, subArgs) : 
                    new ArrayList<>();
            case "forcespawn":
                return forceSpawnCmd instanceof TabCompleter ? 
                    ((TabCompleter) forceSpawnCmd).onTabComplete(sender, command, alias, subArgs) : 
                    new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Commande de test pour vérifier les classes sociales des villageois avec des métiers custom
     */
    private boolean handleTestSocialClassCommand(Player player) {
        player.sendMessage(Component.text("🔧 Test des classes sociales des villageois avec métiers custom...").color(NamedTextColor.YELLOW));
        
        // Importer les services nécessaires
        try {
            SocialClassService.forceReevaluateAllVillagers();
            player.sendMessage(Component.text("✅ Réévaluation forcée des classes sociales terminée.").color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("💡 Consultez les logs du serveur pour les détails.").color(NamedTextColor.GRAY));
        } catch (Exception e) {
            player.sendMessage(Component.text("❌ Erreur lors du test: " + e.getMessage()).color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    /**
     * Commande pour déclencher manuellement la collecte d'impôts
     */
    private boolean handleCollectTaxesCommand(Player player) {
        player.sendMessage(Component.text("💰 Déclenchement manuel de la collecte d'impôts...").color(NamedTextColor.YELLOW));
        
        try {
            // Déclencher la collecte d'impôts via TaxService
            TaxService.collectTaxes(); 
        } catch (Exception e) {
            player.sendMessage(Component.text("❌ Erreur lors de la collecte d'impôts: " + e.getMessage()).color(NamedTextColor.RED));
            e.printStackTrace();
        }
        
        return true;
    }
    
    /**
     * Commande pour déclencher manuellement l'événement de nourriture des villageois
     */
    private boolean handleGoEatCommand(Player player) {
        player.sendMessage(Component.text("🍵 Déclenchement manuel de l'événement de nourriture...").color(NamedTextColor.YELLOW));
        
        try {
            // Créer et exécuter une nouvelle instance du thread de nourriture
            VillagerGoEatThread goEatThread = new VillagerGoEatThread();
            goEatThread.run();
            
            player.sendMessage(Component.text("✅ Événement de nourriture déclenché avec succès.").color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("💡 Les villageois affamés vont chercher de la nourriture et les messages seront envoyés aux propriétaires.").color(NamedTextColor.GRAY));
            
        } catch (Exception e) {
            player.sendMessage(Component.text("❌ Erreur lors du déclenchement: " + e.getMessage()).color(NamedTextColor.RED));
            e.printStackTrace();
        }
        
        return true;
    }

    /**
     * Gère les commandes du cartographe
     */
    private boolean handleCartographeCommand(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("❌ Usage: /admin cartographe <status|test|debug> [village]").color(NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status":
                return handleCartographeStatus(player, args);
            case "test":
                return handleCartographeTest(player, args);
            case "debug":
                return handleCartographeDebug(player);
            default:
                player.sendMessage(Component.text("❌ Sous-commande inconnue. Utilisez: status, test, debug").color(NamedTextColor.RED));
                return true;
        }
    }

    /**
     * Affiche le statut des cartographes
     */
    private boolean handleCartographeStatus(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("❌ Usage: /admin cartographe status <village>").color(NamedTextColor.RED));
            return true;
        }

        String villageName = args[1];
        VillageModel village = VillageRepository.get(villageName);

        if (village == null) {
            player.sendMessage(Component.text("❌ Village '" + villageName + "' introuvable !").color(NamedTextColor.RED));
            return true;
        }

        boolean hasCartographer = CartographeService.hasActiveCartographer(village);
        if (hasCartographer) {
            player.sendMessage(Component.text("✅ Le village '" + villageName + "' possède un cartographe actif 🗺️").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("❌ Le village '" + villageName + "' n'a pas de cartographe actif").color(NamedTextColor.RED));
        }
        return true;
    }

    /**
     * Teste la détection d'intrusion
     */
    private boolean handleCartographeTest(Player player, String[] args) {
        Location playerLoc = player.getLocation();

        // Trouver le village le plus proche
        VillageModel nearestVillage = VillageRepository.getNearestOf(playerLoc);

        if (nearestVillage == null) {
            player.sendMessage(Component.text("❌ Aucun village trouvé à proximité !").color(NamedTextColor.RED));
            return true;
        }

        // Simuler une intrusion
        player.sendMessage(Component.text("🧪 Test d'intrusion sur le village '" + nearestVillage.getId() + "'...").color(NamedTextColor.YELLOW));
        CartographeService.handlePlayerMovement(player);
        player.sendMessage(Component.text("✅ Test terminé ! Vérifiez si des alertes ont été envoyées.").color(NamedTextColor.GREEN));
        return true;
    }

    /**
     * Affiche les informations de debug
     */
    private boolean handleCartographeDebug(Player player) {
        player.sendMessage(Component.text("🔍 === DEBUG CARTOGRAPHE ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Service actif: ✅").color(NamedTextColor.GRAY));
        
        // Statistiques des villages avec cartographes
        int villagesWithCartographer = 0;
        int totalVillages = 0;

        for (VillageModel village : VillageRepository.getAll()) {
            totalVillages++;
            if (CartographeService.hasActiveCartographer(village)) {
                villagesWithCartographer++;
            }
        }

        player.sendMessage(Component.text("Villages totaux: " + totalVillages).color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Villages avec cartographe: " + villagesWithCartographer).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Rayon de protection: " + Config.VILLAGE_PROTECTION_RADIUS + " blocs").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("=========================").color(NamedTextColor.GOLD));
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== Commandes Administratives ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/admin refresh").color(NamedTextColor.YELLOW).append(Component.text(" - Recharge le plugin").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin data").color(NamedTextColor.YELLOW).append(Component.text(" - Gestion des données").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin emptyvillage").color(NamedTextColor.YELLOW).append(Component.text(" - Vide un village").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin forcespawn").color(NamedTextColor.YELLOW).append(Component.text(" - Force le spawn d'un villageois").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin testautojob").color(NamedTextColor.YELLOW).append(Component.text(" - Test assignation automatique d'emplois").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin traderstatus").color(NamedTextColor.YELLOW).append(Component.text(" - Statut des marchands").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin testsocialclass").color(NamedTextColor.YELLOW).append(Component.text(" - Test classes sociales métiers custom").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin collecttaxes").color(NamedTextColor.YELLOW).append(Component.text(" - Déclencher manuellement la collecte d'impôts").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin goeat").color(NamedTextColor.YELLOW).append(Component.text(" - Déclencher manuellement l'événement de nourriture des villageois").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin cartographe").color(NamedTextColor.YELLOW).append(Component.text(" - Gestion du système cartographe (status|test|debug)").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin fletcher").color(NamedTextColor.YELLOW).append(Component.text(" - Gestion du système fletcher (status|test|debug)").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin armurier").color(NamedTextColor.YELLOW).append(Component.text(" - Gestion du système armurier (status|test|debug)").color(NamedTextColor.WHITE)));
    }

    /**
     * Gère les commandes du fletcher
     */
    private boolean handleFletcherCommand(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("❌ Usage: /admin fletcher <status|test|debug> [village]").color(NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status":
                return handleFletcherStatus(player, args);
            case "test":
                return handleFletcherTest(player, args);
            case "debug":
                return handleFletcherDebug(player);
            default:
                player.sendMessage(Component.text("❌ Sous-commande inconnue. Utilisez: status, test, debug").color(NamedTextColor.RED));
                return true;
        }
    }

    /**
     * Affiche le statut des fletchers
     */
    private boolean handleFletcherStatus(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("❌ Usage: /admin fletcher status <village>").color(NamedTextColor.RED));
            return true;
        }

        String villageName = args[1];
        VillageModel village = VillageRepository.get(villageName);

        if (village == null) {
            player.sendMessage(Component.text("❌ Village '" + villageName + "' introuvable !").color(NamedTextColor.RED));
            return true;
        }

        // Compter les fletchers dans le village
        player.sendMessage(Component.text("🏹 Statut Fletcher pour le village '" + villageName + "'").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Les fletchers équipent automatiquement les gardes squelettes avec de l'armure en or.").color(NamedTextColor.GRAY));
        return true;
    }

    /**
     * Teste le système fletcher
     */
    private boolean handleFletcherTest(Player player, String[] args) {
        Location playerLoc = player.getLocation();

        // Trouver le village le plus proche
        VillageModel nearestVillage = VillageRepository.getNearestOf(playerLoc);

        if (nearestVillage == null) {
            player.sendMessage(Component.text("❌ Aucun village trouvé à proximité !").color(NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("🧪 Test du système Fletcher sur le village '" + nearestVillage.getId() + "'...").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("ℹ️ Le système Fletcher s'active automatiquement après la collecte de taxes.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("ℹ️ Utilisez '/admin collecttaxes' pour déclencher une collecte de taxes.").color(NamedTextColor.GRAY));
        return true;
    }

    /**
     * Affiche les informations de debug du fletcher
     */
    private boolean handleFletcherDebug(Player player) {
        player.sendMessage(Component.text("🏹 === DEBUG FLETCHER ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Service actif: ✅").color(NamedTextColor.GRAY));
        
        // Compter les villageois fletchers
        int fletcherCount = 0;
        if (TestJava.world != null) {
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Villager villager && villager.getProfession() == Villager.Profession.FLETCHER) {
                    fletcherCount++;
                }
            }
        }

        player.sendMessage(Component.text("Fletchers actifs: " + fletcherCount).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Rayon de recherche: " + Config.VILLAGE_PROTECTION_RADIUS + " blocs").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Armure équipée: Plastron, Jambières, Bottes (pas de casque)").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("=========================").color(NamedTextColor.GOLD));
        return true;
    }

    /**
     * Gère les commandes de l'armurier
     */
    private boolean handleArmorierCommand(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("❌ Usage: /admin armurier <status|test|debug> [village]").color(NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "status":
                return handleArmorierStatus(player, subArgs);
            case "test":
                return handleArmorierTest(player, subArgs);
            case "debug":
                return handleArmorierDebug(player);
            default:
                player.sendMessage(Component.text("❌ Sous-commande inconnue. Usage: /admin armurier <status|test|debug>").color(NamedTextColor.RED));
                return true;
        }
    }

    /**
     * Affiche le statut des armuriers
     */
    private boolean handleArmorierStatus(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("❌ Usage: /admin armurier status <village>").color(NamedTextColor.RED));
            return true;
        }

        String villageName = args[0];
        VillageModel village = VillageRepository.get(villageName);
        if (village == null) {
            player.sendMessage(Component.text("❌ Village '" + villageName + "' introuvable").color(NamedTextColor.RED));
            return true;
        }

        // Compter les armuriers dans le village
        player.sendMessage(Component.text("🛡️ Statut Armurier pour le village '" + villageName + "'").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Les armuriers améliorent automatiquement l'armure du propriétaire du village.").color(NamedTextColor.GRAY));
        return true;
    }

    /**
     * Teste le système armurier
     */
    private boolean handleArmorierTest(Player player, String[] args) {
        VillageModel nearestVillage = VillageRepository.getNearestVillageOfPlayer(player.getName(), Config.VILLAGE_PROTECTION_RADIUS);
        if (nearestVillage == null) {
            player.sendMessage(Component.text("❌ Aucun village trouvé dans le rayon de " + Config.VILLAGE_PROTECTION_RADIUS + " blocs").color(NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("🧪 Test du système Armurier sur le village '" + nearestVillage.getId() + "'...").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("ℹ️ Le système Armurier s'active automatiquement après la collecte de taxes.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("ℹ️ L'armurier se déplace vers le joueur pour améliorer son armure.").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("ℹ️ Progression: Rien → Cuir → Maille → Fer → Fer+Solidité1-3").color(NamedTextColor.GRAY));
        return true;
    }

    /**
     * Affiche les informations de débogage de l'armurier
     */
    private boolean handleArmorierDebug(Player player) {
        player.sendMessage(Component.text("🛡️ === DEBUG ARMURIER ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Service actif: ✅").color(NamedTextColor.GRAY));
        
        // Compter les villageois armuriers
        int armorierCount = 0;
        if (TestJava.world != null) {
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Villager villager && villager.getProfession() == Villager.Profession.ARMORER) {
                    armorierCount++;
                }
            }
        }

        player.sendMessage(Component.text("Armuriers actifs: " + armorierCount).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Rayon de village: " + Config.VILLAGE_PROTECTION_RADIUS + " blocs").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Progression: Cuir → Maille → Fer → Fer+Solidité").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Enchantement max: Solidité III").color(NamedTextColor.GRAY));
        
        // Informations de debug depuis le service
        String debugInfo = ArmorierService.getDebugInfo();
        for (String line : debugInfo.split("\n")) {
            if (!line.trim().isEmpty()) {
                player.sendMessage(Component.text(line).color(NamedTextColor.GRAY));
            }
        }
        
        player.sendMessage(Component.text("==========================").color(NamedTextColor.GOLD));
        return true;
    }
}