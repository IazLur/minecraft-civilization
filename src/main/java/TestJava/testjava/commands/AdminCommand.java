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
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.ArmorierService;
import TestJava.testjava.services.CartographeService;
import TestJava.testjava.services.ForestGuardService;
import TestJava.testjava.services.MasonService;
import TestJava.testjava.services.SocialClassService;
import TestJava.testjava.services.TaxService;
import TestJava.testjava.threads.VillagerGoEatThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
            case "tailleur":
                return handleTailleurCommand(player, subArgs);
            case "forestguard":
                return handleForestGuardCommand(player, subArgs);
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
                "testautojob", "traderstatus", "testsocialclass", "collecttaxes", "goeat", "cartographe", "fletcher", "armurier", "tailleur", "forestguard"
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
        player.sendMessage(Component.text("/admin tailleur").color(NamedTextColor.YELLOW).append(Component.text(" - Gestion du système tailleur de pierre (status|test|debug)").color(NamedTextColor.WHITE)));
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

    /**
     * Commande de test pour le Tailleur de Pierre
     */
    private boolean handleTailleurCommand(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("❌ Usage: /admin tailleur <status|test|debug> [village]").color(NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "status":
                return handleTailleurStatus(player, subArgs);
            case "test":
                return handleTailleurTest(player, subArgs);
            case "debug":
                return handleTailleurDebug(player);
            default:
                player.sendMessage(Component.text("❌ Sous-commande inconnue. Usage: /admin tailleur <status|test|debug>").color(NamedTextColor.RED));
                return true;
        }
    }

    /**
     * Affiche le statut des tailleurs de pierre
     */
    private boolean handleTailleurStatus(Player player, String[] args) {
        player.sendMessage(Component.text("🪨 === STATUT TAILLEUR DE PIERRE ===").color(NamedTextColor.GOLD));
        
        // Compter les tailleurs de pierre
        int masonCount = 0;
        if (TestJava.world != null) {
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Villager villager && villager.getProfession() == Villager.Profession.MASON) {
                    masonCount++;
                }
            }
        }

        player.sendMessage(Component.text("Tailleurs de pierre actifs: " + masonCount).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Fonction: Transforme cobblestone → pierre taillée, quartz → quartz taillé").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Déclenchement: Après paiement du salaire toutes les 5 minutes").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Rayon de recherche: " + Config.VILLAGE_PROTECTION_RADIUS + " blocs").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("=======================================").color(NamedTextColor.GOLD));
        return true;
    }

    /**
     * Test manuel du système Tailleur de Pierre
     */
    private boolean handleTailleurTest(Player player, String[] args) {
        // Trouver le village le plus proche
        VillageModel nearestVillage = null;
        double nearestDistance = Double.MAX_VALUE;
        
        Location playerLoc = player.getLocation();
        for (VillageModel village : VillageRepository.getAll()) {
            Location villageCenter = VillageRepository.getBellLocation(village);
            if (villageCenter != null && villageCenter.getWorld().equals(playerLoc.getWorld())) {
                double distance = playerLoc.distance(villageCenter);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestVillage = village;
                }
            }
        }

        if (nearestVillage == null) {
            player.sendMessage(Component.text("❌ Aucun village trouvé près de votre position.").color(NamedTextColor.RED));
            return true;
        }

        // Trouver un tailleur de pierre dans le village
        Villager mason = null;
        if (TestJava.world != null) {
            Location villageCenter = VillageRepository.getBellLocation(nearestVillage);
            if (villageCenter != null) {
                for (Entity entity : TestJava.world.getNearbyEntities(villageCenter, Config.VILLAGE_PROTECTION_RADIUS, Config.VILLAGE_PROTECTION_RADIUS, Config.VILLAGE_PROTECTION_RADIUS)) {
                    if (entity instanceof Villager villager && villager.getProfession() == Villager.Profession.MASON) {
                        mason = villager;
                        break;
                    }
                }
            }
        }

        if (mason == null) {
            player.sendMessage(Component.text("❌ Aucun tailleur de pierre trouvé dans le village '" + nearestVillage.getId() + "'.").color(NamedTextColor.RED));
            return true;
        }

        // Déclencher manuellement le service
        try {
            VillagerModel villagerModel = VillagerRepository.find(mason.getUniqueId());
            if (villagerModel != null) {
                MasonService.triggerBlockTransformationAfterSalary(villagerModel, mason);
                player.sendMessage(Component.text("✅ Test du Tailleur de Pierre déclenché pour le village '" + nearestVillage.getId() + "'.").color(NamedTextColor.GREEN));
                player.sendMessage(Component.text("ℹ️ Le tailleur va chercher des blocs de cobblestone ou de quartz à transformer.").color(NamedTextColor.GRAY));
            } else {
                player.sendMessage(Component.text("❌ Villageois non trouvé dans la base de données.").color(NamedTextColor.RED));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("❌ Erreur lors du test: " + e.getMessage()).color(NamedTextColor.RED));
            TestJava.plugin.getLogger().warning("Erreur test tailleur: " + e.getMessage());
        }

        return true;
    }

    /**
     * Affiche les informations de débogage du tailleur de pierre
     */
    private boolean handleTailleurDebug(Player player) {
        player.sendMessage(Component.text("🪨 === DEBUG TAILLEUR DE PIERRE ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Service actif: ✅").color(NamedTextColor.GRAY));
        
        // Compter les tailleurs de pierre
        int masonCount = 0;
        if (TestJava.world != null) {
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Villager villager && villager.getProfession() == Villager.Profession.MASON) {
                    masonCount++;
                }
            }
        }

        player.sendMessage(Component.text("Tailleurs de pierre actifs: " + masonCount).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Rayon de village: " + Config.VILLAGE_PROTECTION_RADIUS + " blocs").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Transformations: COBBLESTONE → STONE_BRICKS").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Alternative: QUARTZ_BLOCK → CHISELED_QUARTZ_BLOCK").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Distance de travail: 2.5 blocs").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Timeout: 20 secondes par bloc").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("=======================================").color(NamedTextColor.GOLD));
        return true;
    }

    private boolean handleForestGuardCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /admin forestguard <test|status>").color(NamedTextColor.RED));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "test":
                return handleForestGuardTestCommand(player);
            case "status":
                return handleForestGuardStatusCommand(player);
            default:
                player.sendMessage(Component.text("Sous-commandes disponibles: test, status").color(NamedTextColor.RED));
                return true;
        }
    }
    
    private boolean handleForestGuardTestCommand(Player player) {
        player.sendMessage(Component.text("================ TEST GARDE FORESTIER ================").color(NamedTextColor.GOLD));
        
        // Trouver tous les gardes forestiers dans le rayon
        int guardCount = 0;
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        
        for (VillagerModel villagerModel : allVillagers) {
            if (villagerModel.hasCustomJob() && "garde_forestier".equals(villagerModel.getCurrentJobName())) {
                guardCount++;
                
                // Trouver l'entité correspondante
                Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villagerModel.getId());
                if (entity != null) {
                    player.sendMessage(Component.text("🌲 Garde forestier trouvé: " + villagerModel.getId()).color(NamedTextColor.GREEN));
                    player.sendMessage(Component.text("   Village: " + villagerModel.getVillageName()).color(NamedTextColor.GRAY));
                    player.sendMessage(Component.text("   Position: " + entity.getLocation().getBlockX() + "," + entity.getLocation().getBlockY() + "," + entity.getLocation().getBlockZ()).color(NamedTextColor.GRAY));
                    player.sendMessage(Component.text("   Classe sociale: " + villagerModel.getSocialClassEnum().getColoredTag()).color(NamedTextColor.GRAY));
                    
                    // Test du déclenchement manuel
                    player.sendMessage(Component.text("   🧪 Test de plantation en cours...").color(NamedTextColor.YELLOW));
                    try {
                        ForestGuardService.triggerTreePlantingAfterSalary(villagerModel, entity);
                        player.sendMessage(Component.text("   ✅ Déclenchement réussi - Vérifiez les logs du serveur").color(NamedTextColor.GREEN));
                    } catch (Exception e) {
                        player.sendMessage(Component.text("   ❌ Erreur: " + e.getMessage()).color(NamedTextColor.RED));
                    }
                    
                } else {
                    player.sendMessage(Component.text("🌲 Garde forestier fantôme: " + villagerModel.getId()).color(NamedTextColor.RED));
                }
            }
        }
        
        if (guardCount == 0) {
            player.sendMessage(Component.text("❌ Aucun garde forestier trouvé sur le serveur").color(NamedTextColor.RED));
            player.sendMessage(Component.text("💡 Construisez un bâtiment garde_forestier et assignez des villageois").color(NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("📊 Total: " + guardCount + " garde(s) forestier(s) trouvé(s)").color(NamedTextColor.GREEN));
        }
        
        player.sendMessage(Component.text("=======================================").color(NamedTextColor.GOLD));
        return true;
    }
    
    private boolean handleForestGuardStatusCommand(Player player) {
        player.sendMessage(Component.text("=============== STATUS GARDE FORESTIER ===============").color(NamedTextColor.GOLD));
        
        // Statistiques des bâtiments garde forestier
        Collection<BuildingModel> allBuildings = BuildingRepository.getAll();
        int forestGuardBuildings = 0;
        int activeBuildings = 0;
        
        for (BuildingModel building : allBuildings) {
            if ("garde_forestier".equals(building.getBuildingType())) {
                forestGuardBuildings++;
                if (building.isActive()) {
                    activeBuildings++;
                    player.sendMessage(Component.text("🏢 Bâtiment actif: " + building.getVillageName()).color(NamedTextColor.GREEN));
                    player.sendMessage(Component.text("   Position: " + building.getX() + "," + building.getY() + "," + building.getZ()).color(NamedTextColor.GRAY));
                    player.sendMessage(Component.text("   Niveau: " + building.getLevel()).color(NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text("🏢 Bâtiment inactif: " + building.getVillageName()).color(NamedTextColor.RED));
                }
            }
        }
        
        // Statistiques des employés
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        int totalGuards = 0;
        int activeGuards = 0;
        
        for (VillagerModel villager : allVillagers) {
            if (villager.hasCustomJob() && "garde_forestier".equals(villager.getCurrentJobName())) {
                totalGuards++;
                Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villager.getId());
                if (entity != null && !entity.isDead() && entity.isValid()) {
                    activeGuards++;
                }
            }
        }
        
        // Résumé
        player.sendMessage(Component.text("📈 Bâtiments garde forestier: " + forestGuardBuildings + " (actifs: " + activeBuildings + ")").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("👥 Employés garde forestier: " + totalGuards + " (en vie: " + activeGuards + ")").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("⚙️ Fréquence de plantation: toutes les 5 minutes (avec les taxes)").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("🌱 Types d'arbres: Chêne (30%), Bouleau (25%), Épicéa (20%)...").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("📏 Rayon de recherche: 50 blocs autour du bâtiment").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("=======================================").color(NamedTextColor.GOLD));
        
        return true;
    }
}