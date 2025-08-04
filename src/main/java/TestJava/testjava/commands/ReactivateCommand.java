package TestJava.testjava.commands;

import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.services.SheepService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;

public class ReactivateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seulement un joueur peut utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;
        Location playerLoc = player.getLocation();

        // Chercher un bâtiment inactif dans un rayon de 5 blocs
        BuildingModel nearestBuilding = null;
        double nearestDistance = Double.MAX_VALUE;

        Collection<BuildingModel> allBuildings = BuildingRepository.getAll();
        for (BuildingModel building : allBuildings) {
            if (building.isActive()) {
                continue; // Bâtiment déjà actif
            }

            Location buildingLoc = new Location(player.getWorld(), building.getX(), building.getY(), building.getZ());
            double distance = playerLoc.distance(buildingLoc);

            if (distance <= 5.0 && distance < nearestDistance) {
                nearestDistance = distance;
                nearestBuilding = building;
            }
        }

        if (nearestBuilding == null) {
            player.sendMessage(ChatColor.RED + "Aucun bâtiment inactif trouvé dans un rayon de 5 blocs.");
            return true;
        }

        // Vérifier que le joueur possède le village
        VillageModel village = VillageRepository.get(nearestBuilding.getVillageName());
        if (village == null) {
            player.sendMessage(ChatColor.RED + "Village introuvable.");
            return true;
        }

        if (!village.getPlayerName().equals(player.getName())) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes pas le propriétaire de ce village.");
            return true;
        }

        // Vérifier que le joueur a assez d'argent
        EmpireModel empire = EmpireRepository.getForPlayer(player.getName());
        if (empire == null) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'empire.");
            return true;
        }

        if (empire.getJuridictionCount() < nearestBuilding.getCostPerDay()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas assez d'argent pour réactiver ce bâtiment. Coût: " + 
                             nearestBuilding.getCostPerDay() + "µ");
            return true;
        }

        // Déduire les coûts
        empire.setJuridictionCount(empire.getJuridictionCount() - nearestBuilding.getCostPerDay());
        EmpireRepository.update(empire);

        // Réactiver le bâtiment
        nearestBuilding.setActive(true);
        BuildingRepository.update(nearestBuilding);

        // Mettre à jour l'ArmorStand
        Location buildingLoc = new Location(player.getWorld(), nearestBuilding.getX(), nearestBuilding.getY() + 1, nearestBuilding.getZ());
        for (Entity entity : buildingLoc.getWorld().getNearbyEntities(buildingLoc, 1.0, 1.0, 1.0)) {
            if (entity instanceof ArmorStand && entity.getLocation().distance(buildingLoc) < 1.0) {
                ArmorStand armorStand = (ArmorStand) entity;
                String newName = ChatColor.GREEN + "{actif} " + 
                                ChatColor.BLUE + "[" + village.getId() + "] " + 
                                ChatColor.WHITE + nearestBuilding.getBuildingType();
                armorStand.setCustomName(newName);
            }
        }

        // Spawn un mouton si c'est une bergerie
        if ("bergerie".equals(nearestBuilding.getBuildingType())) {
            boolean spawned = SheepService.spawnSheepForBuilding(nearestBuilding);
            if (spawned) {
                player.sendMessage(ChatColor.GREEN + "Bergerie réactivée ! Un mouton a été spawné.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Bergerie réactivée ! (Limite de moutons atteinte)");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + nearestBuilding.getBuildingType() + " réactivé avec succès !");
        }

        // Message de broadcast
        player.getServer().broadcastMessage(Colorize.name(player.getName()) + " a réactivé " + 
                                          Colorize.name(nearestBuilding.getBuildingType()) + " à " + 
                                          Colorize.name(village.getId()) + " pour " + 
                                          Colorize.name(nearestBuilding.getCostPerDay() + "µ"));

        return true;
    }
}