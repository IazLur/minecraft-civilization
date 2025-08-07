package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.classes.CustomEntity;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.SocialClassService;
import TestJava.testjava.services.HistoryService;
import TestJava.testjava.services.VillagerHomeService;
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
                // Enregistrer la famine avant la mort
                HistoryService.recordFamine(villagerModel);
                
                // Enregistrer la mort dans l'historique
                HistoryService.recordVillagerDeath(villagerModel);
                
                TestJava.plugin.getServer().broadcastMessage(
                        ChatColor.GRAY + "La famine sévit à " + Colorize.name(villagerModel.getVillageName())
                );
                v.setHealth(0D);
            } else if (villagerModel.getFood() < 5) {
                if (v == null) {
                    return;
                }
                Collection<VillageModel> villages = VillageRepository.getAll();
                VillageModel prosp = null;
                for (VillageModel nVillage : villages) {
                    if(nVillage.getId().equals(village.getId()))
                        continue;
                    if (prosp == null || nVillage.getPopulation() > prosp.getPopulation()) {
                        prosp = nVillage;
                    }
                }
                
                // Vérifier qu'un village prospère a été trouvé
                if (prosp == null) {
                    Bukkit.getLogger().warning("[VillagerMigration] Aucun village prospère trouvé pour la migration de " + villagerModel.getId());
                    return; // Impossible de migrer
                }
                
                // Enregistrer la famine avant la migration
                HistoryService.recordFamine(villagerModel);
                
                CustomEntity ce = new CustomEntity(v);
                villagerModel.setVillageName(prosp.getId());
                Bukkit.getServer().broadcastMessage(Colorize.name(v.getCustomName()) + " est parti à "
                        + Colorize.name(prosp.getId()) + " par famine");
                ce.setVillage(prosp);
                village.setPopulation(village.getPopulation() - 1);
                prosp.setPopulation(prosp.getPopulation() + 1);
                
                // CORRECTION BUG: Réinitialiser les données de navigation du villageois
                VillagerHomeService.resetVillagerNavigation(v);
                v.teleport(VillageRepository.getBellLocation(prosp));
                
                VillageRepository.update(village);
                VillageRepository.update(prosp);
            }
            
            // CORRECTION BUG: Évaluation de la classe sociale après changement de nourriture
            // evaluateAndUpdateSocialClass sauvegarde déjà les changements, pas besoin de VillagerRepository.update
            SocialClassService.evaluateAndUpdateSocialClass(villagerModel);
            
            // Sauvegarde de la nourriture mise à jour
            VillagerRepository.update(villagerModel);
        });
    }
    

}
