package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.classes.CustomEntity;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Villager;

import java.util.Collection;

public class VillagerEatThread implements Runnable {
    @Override
    public void run() {
        Collection<VillagerModel> villagers = VillagerRepository.getAll();
        villagers.forEach(villagerModel -> {
            villagerModel.setFood(villagerModel.getFood() - 1);
            Villager v = ((Villager) TestJava.plugin.getServer().getEntity(villagerModel.getId()));
            VillageModel village = VillageRepository.get(villagerModel.getVillageName());
            if (villagerModel.getFood() <= 0) {
                if (v == null) {
                    return;
                }
                v.setHealth(0D);
                TestJava.plugin.getServer().broadcastMessage(
                        ChatColor.GRAY + "La famine sévit à " + Colorize.name(villagerModel.getVillageName())
                );
            } else if (villagerModel.getFood() < 5) {
                Collection<VillageModel> villages = VillageRepository.getAll();
                VillageModel prosp = null;
                for (VillageModel nVillage : villages) {
                    if (prosp == null || (nVillage.getPopulation() > prosp.getPopulation() &&
                            !nVillage.getId().equals(prosp.getId()))) {
                        prosp = nVillage;
                    }
                }
                CustomEntity ce = new CustomEntity(v);
                villagerModel.setVillageName(prosp.getId());
                Bukkit.getServer().broadcastMessage(Colorize.name(v.getCustomName()) + " s'est barré à "
                        + Colorize.name(prosp.getId()) + " car il n'avait pas assez à manger");
                ce.setVillage(prosp);
                v.teleport(VillageRepository.getBellLocation(prosp));
            }
            VillagerRepository.update(villagerModel);
        });
    }
}
