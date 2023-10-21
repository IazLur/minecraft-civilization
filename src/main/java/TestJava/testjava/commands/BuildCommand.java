package TestJava.testjava.commands;

import TestJava.testjava.Config;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.BuildingModel;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.BuildingRepository;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class BuildCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seulement un joueur peut utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("Usage: /build <buildingType>");
            return true;
        }

        String buildingType = args[0];

        Collection<String> buildingTypes = new ArrayList<>();
        buildingTypes.add("bergerie");

        VillageModel village = VillageRepository
                .getNearestVillageOfPlayer(player.getName(), Config.VILLAGE_CONSTRUCTION_RADIUS);

        if (village == null) {
            player.sendMessage("Vous devez être dans votre village pour construire un bâtiment.");
            return true;
        }

        EmpireModel empire = EmpireRepository.getForPlayer(player.getName());
        if (empire == null) {
            player.sendMessage("Vous n'avez pas d'empire.");
            return true;
        }

        BuildingModel building = null;
        Location loc = player.getLocation();
        loc.setY(loc.getBlockY() - 1);

        switch (buildingType) {
            case "bergerie":
                building = new BuildingModel();
                building.setId(UUID.randomUUID());
                building.setBuildingType("bergerie");
                building.setVillageName(village.getId());
                building.setX(loc.getBlockX());
                building.setY(loc.getBlockY());
                building.setZ(loc.getBlockZ());
                building.setLevel(0);
                building.setCostToBuild(2500);
                building.setCostPerDay(50);
                building.setCostPerUpgrade(1500);
                building.setCostUpgradeMultiplier(1.2f);
                break;
        }

        if (building == null) {
            player.sendMessage("Ce bâtiment n'existe pas.");
            return true;
        }

        player.sendMessage(Colorize.name(building.getBuildingType()) + " va vous coûter " + Colorize.name(building.getCostToBuild() + "µ"));

        if (empire.getJuridictionCount() < building.getCostToBuild()) {
            player.sendMessage("Vous n'avez pas assez d'argent pour construire ce bâtiment.");
            return true;
        }

        empire.setJuridictionCount(empire.getJuridictionCount() - building.getCostToBuild());
        EmpireRepository.update(empire);

        BuildingRepository.update(building);
        loc.getBlock().setType(Material.BEDROCK);
        loc.setY(loc.getY() + 1);
        ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

        armorStand.setCustomName(ChatColor.BLUE + "[" + village.getId() + "] " + ChatColor.WHITE
                + building.getBuildingType());
        armorStand.setVisible(true);
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setVisible(false);
        armorStand.setCustomNameVisible(true);

        Bukkit.getServer().broadcastMessage(Colorize.name(player.getName()) + " a construit le bâtiment " + Colorize.name(building.getBuildingType()));
        player.sendMessage("Bâtiment construit avec succès!");

        return true;
    }
}