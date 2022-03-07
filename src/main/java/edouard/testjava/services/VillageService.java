package edouard.testjava.services;

import edouard.testjava.Config;
import edouard.testjava.TestJava;
import edouard.testjava.helpers.Colorize;
import edouard.testjava.models.EmpireModel;
import edouard.testjava.models.VillageModel;
import edouard.testjava.repositories.EmpireRepository;
import edouard.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Collection;
import java.util.UUID;

public class VillageService {
    public void create(BlockPlaceEvent e) {
        Location location = e.getBlockPlaced().getLocation();
        VillageModel village = new VillageModel();
        village.setId(Config.DEFAULT_VILLAGE_ID + "-" + UUID.randomUUID());
        village.setX(location.getBlockX());
        village.setY(location.getBlockY());
        village.setZ(location.getBlockZ());
        village.setBedsCount(0);
        village.setPlayerName(e.getPlayer().getDisplayName());
        VillageRepository.update(village);
        Bukkit.getServer().broadcastMessage(Colorize.name(village.getPlayerName()) +
                " a créé " + Colorize.name(village.getId()));
        location.setY(location.getY() - 1);
        location.getBlock().setType(Material.BEDROCK);
    }

    public void testIfPlacingBed(BlockPlaceEvent e) {
        if (!e.getBlockPlaced().getType().name().contains(Config.BED_TYPE)) {
            return;
        }

        VillageModel village = VillageRepository.getCurrentVillageConstructibleIfOwn(e.getPlayer());
        village.setBedsCount(village.getBedsCount() + 1);
        VillageRepository.update(village);

        Bukkit.getServer().broadcastMessage(Colorize.name(e.getPlayer().getDisplayName()) +
                " a ajouté un lit à " + Colorize.name(village.getId()));
    }

    public void testIfBreakingBed(BlockBreakEvent e) {
        if (!e.getBlock().getType().name().contains(Config.BED_TYPE)) {
            return;
        }

        VillageModel village = VillageRepository.getCurrentVillageConstructibleIfOwn(e.getPlayer());
        village.setBedsCount(village.getBedsCount() - 1);
        VillageRepository.update(village);

        Bukkit.getServer().broadcastMessage(Colorize.name(e.getPlayer().getDisplayName()) +
                " a détruit un lit à " + Colorize.name(village.getId()));
    }

    public boolean canConquerVillage(BlockPlaceEvent e) {
        Collection<VillageModel> villages = VillageRepository.getAll();
        for (VillageModel village : villages) {
            Location bell = VillageRepository.getBellLocation(village);
            Location block = e.getBlock().getLocation();
            if (bell.distance(block) <= 1) {
                return true;
            }
        }
        return false;
    }

    public void conquer(BlockPlaceEvent e) {
        VillageModel village = VillageRepository.getNearestOf(e.getPlayer());
        Player loser = Bukkit.getPlayer(village.getPlayerName());
        village.setPlayerName(e.getPlayer().getDisplayName());
        VillageRepository.update(village);
        Bukkit.getServer().broadcastMessage(Colorize.name(e.getPlayer().getDisplayName()) + " a conquis " +
                Colorize.name(village.getId()));
        e.getBlock().setType(Material.AIR);
        EmpireModel empire = EmpireRepository.get(village.getPlayerName());
        if (VillageRepository.getForPlayer(village.getPlayerName()).size() == 0) {
            EmpireRepository.remove(empire);
            if (loser != null && loser.isOnline()) {
                TestJava.playerService.addEmpireIfNotOwnsOne(loser);
            }
        }
    }
}
