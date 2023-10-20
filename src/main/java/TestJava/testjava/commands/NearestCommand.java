package TestJava.testjava.commands;

import TestJava.testjava.Config;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NearestCommand implements CommandExecutor {

    /*
     * Returns the nearest village to the player executing this command.
     *
     * @param sender The CommandSender (player) executing this command.
     * @param command The Command instance being executed.
     * @param label The alias used to execute this command.
     * @param args Any additional arguments provided to the command.
     *
     * This uses the VillageRepository to look up the nearest village to the
     * player within 2 * VILLAGE_CONSTRUCTIBLE_RADIUS distance.
     * It gets the player's name from the sender to pass to the repository.
     *
     * If a village is found, it prints a message with the player name and
     * village id.
     * If no village is found, it prints a message saying the player is not
     * near a village.
     *
     * Returns true to indicate the command executed successfully.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        VillageModel nearestVillage = VillageRepository.getNearestVillageOfPlayer(sender.getName(),
                Config.VILLAGE_CONSTRUCTION_RADIUS * 2);
        if(nearestVillage != null){
            sender.sendMessage(Colorize.name(sender.getName()) + " is near " + nearestVillage.getId());
        } else {
            sender.sendMessage(Colorize.name(sender.getName()) + " is not near a village");
        }
        return true;
    }

}
