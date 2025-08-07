package TestJava.testjava.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
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
    private final ReactivateCommand reactivateCmd = new ReactivateCommand();
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
            case "reactivate":
                return reactivateCmd.onCommand(sender, command, label, subArgs);
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
                "reactivate", "testautojob", "traderstatus", "testsocialclass", "collecttaxes", "goeat"
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
            TestJava.testjava.services.SocialClassService.forceReevaluateAllVillagers();
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

    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== Commandes Administratives ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/admin refresh").color(NamedTextColor.YELLOW).append(Component.text(" - Recharge le plugin").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin data").color(NamedTextColor.YELLOW).append(Component.text(" - Gestion des données").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin emptyvillage").color(NamedTextColor.YELLOW).append(Component.text(" - Vide un village").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin forcespawn").color(NamedTextColor.YELLOW).append(Component.text(" - Force le spawn d'un villageois").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin reactivate").color(NamedTextColor.YELLOW).append(Component.text(" - Réactive un bâtiment").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin testautojob").color(NamedTextColor.YELLOW).append(Component.text(" - Test assignation automatique d'emplois").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin traderstatus").color(NamedTextColor.YELLOW).append(Component.text(" - Statut des marchands").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin testsocialclass").color(NamedTextColor.YELLOW).append(Component.text(" - Test classes sociales métiers custom").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin collecttaxes").color(NamedTextColor.YELLOW).append(Component.text(" - Déclencher manuellement la collecte d'impôts").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/admin goeat").color(NamedTextColor.YELLOW).append(Component.text(" - Déclencher manuellement l'événement de nourriture des villageois").color(NamedTextColor.WHITE)));
    }
}