package TestJava.testjava.commands;

import TestJava.testjava.commands.ReactivateCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildingCommand implements CommandExecutor {
    private final ReactivateCommand reactivateCmd = new ReactivateCommand();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage("Usage: /building reactivate");
            return true;
        }
        String subCommand = args[0].toLowerCase();
        String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
        switch (subCommand) {
            case "reactivate":
                return reactivateCmd.onCommand(sender, command, label, subArgs);
            default:
                player.sendMessage("Usage: /building reactivate");
                return true;
        }
    }
}
