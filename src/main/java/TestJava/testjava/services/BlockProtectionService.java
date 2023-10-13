package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.Objects;

public class BlockProtectionService {
    public void protectRestOfTheWorld(BlockBreakEvent e) {
        VillageModel village = VillageRepository.getCurrentVillageTerritory(e.getPlayer());
        if (village != null && !Objects.equals(village.getPlayerName(), e.getPlayer().getDisplayName())) {
            e.setCancelled(true);
            e.setDropItems(false);
            e.getPlayer().sendMessage(ChatColor.RED + "Ce terrain n'est pas dans votre territoire");
        }
    }

    public void protectRestOfTheWorld(BlockPlaceEvent e) {
        VillageModel village = VillageRepository.getCurrentVillageTerritory(e.getPlayer());
        if (village != null && !Objects.equals(village.getPlayerName(), e.getPlayer().getDisplayName())) {
            e.setCancelled(true);
            e.setBuild(false);
            e.getPlayer().sendMessage(ChatColor.RED + "Ce terrain n'est pas dans votre territoire");
        }
    }

    public boolean canPlayerBreakBlock(BlockBreakEvent e) {
        return VillageRepository.getCurrentVillageConstructibleIfOwn(e.getPlayer()) != null;
    }

    public boolean canPlayerCreateVillage(BlockPlaceEvent e) {
        boolean result = VillageRepository.getCurrentVillageTerritory(e.getPlayer()) == null;
        if (!result) {
            e.setCancelled(true);
            e.setBuild(false);
        }
        return result;
    }

    public boolean isVillageCenterTypeGettingDestroyed(BlockBreakEvent e) {
        if (e.getBlock().getType() == Config.VILLAGE_CENTER_TYPE) {
            e.setCancelled(true);
            e.setDropItems(false);
            return true;
        }

        return false;
    }

    public boolean canPlayerPlaceBlock(BlockPlaceEvent e) {
        return VillageRepository.getCurrentVillageConstructibleIfOwn(e.getPlayer()) != null;
    }

    public boolean preventCultivableDestroy(EntityChangeBlockEvent e) {
        Material toMaterial = e.getTo();
        Material fromMaterial = e.getBlock().getType();

        if (fromMaterial == Material.FARMLAND && (toMaterial == Material.DIRT || toMaterial == Material.GRASS_BLOCK)) {
            e.setCancelled(true);
            return true;
        }

        return false;
    }
}