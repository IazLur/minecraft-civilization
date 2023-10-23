package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Collection;
import java.util.UUID;

public class VillageService {

    private void drawCircle(Location center, int radius, Material material) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Vérification si le point (x,z) est sur la circonférence du cercle
                if ((x * x) + (z * z) <= radius * radius + 1 && (x * x) + (z * z) >= radius * radius - 1) {
                    for (int y = 0; y < world.getMaxHeight(); y++) {
                        Block block = world.getBlockAt(centerX + x, y, centerZ + z);
                        Material blockType = block.getType();
                        // Remplacer le bloc par de la bedrock sauf s'il s'agit d'air, d'eau ou de lave
                        if (blockType != Material.AIR && blockType != Material.WATER && blockType != Material.LAVA) {
                            block.setType(material);
                        }
                    }
                }
            }
        }
    }

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
        drawCircle(location, Config.VILLAGE_PROTECTION_RADIUS, Material.BEDROCK);
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
