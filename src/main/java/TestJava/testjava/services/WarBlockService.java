package TestJava.testjava.services;

import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.WarBlockModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.WarBlockRepository;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.UUID;

public class WarBlockService {
    public boolean testIfCanPlaceTNT(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.TNT) {
            return false;
        }
        VillageModel enemy = VillageRepository.getNearestOf(e.getPlayer());
        EmpireModel monEmpire = EmpireRepository.get(e.getPlayer().getDisplayName());
        EmpireModel sonEmpire = EmpireRepository.get(enemy.getPlayerName());

        if (!monEmpire.getIsInWar() || !monEmpire.getEnemyName().equals(enemy.getId())) {
            e.getPlayer().sendMessage(ChatColor.RED + """
                        Vous devez être en guerre contre ce village 
                        pour y poser des TNT.
                    """);
            return false;
        }
        return true;
    }

    public void testIfTNTExplode(EntityExplodeEvent e) {
        System.out.println("Detected TNT explosion");
        VillageModel village = VillageRepository.getNearestOf(e.getEntity().getLocation());
        for (Block block : e.blockList()) {
            System.out.println("Adding type " + block.getType().name());
            WarBlockModel save = new WarBlockModel();
            save.setId(UUID.randomUUID());
            save.setType(block.getType().name());
            save.setX(block.getX());
            save.setY(block.getY());
            save.setZ(block.getZ());
            save.setVillage(village.getId());
            WarBlockRepository.update(save);
        }
    }
}
