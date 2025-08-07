package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Villager;

import java.util.Collection;
import java.util.Random;


public class VillagerSpawnThread implements Runnable {
    @Override
    public void run() {
        try {
            Random rand = new Random();
            Collection<VillageModel> all = VillageRepository.getAll();
            int totalSpawned = 0;
            int villagesChecked = 0;
            int villagesSkipped = 0;
            
            for (VillageModel village : all) {
                villagesChecked++;
                
                // Vérification de sécurité: s'assurer que le village a des lits
                if (village.getBedsCount() == null || village.getBedsCount() <= 0) {
                    villagesSkipped++;
                    continue; // Skip les villages sans lits
                }
                
                // Vérification de la limite de population
                if (village.getPopulation() >= village.getBedsCount()) {
                    villagesSkipped++;
                    continue; // Skip si la population est au maximum
                }
                
                EmpireModel empire = EmpireRepository.get(village.getPlayerName());
                Location bell = VillageRepository.getBellLocation(village);
                
                // Spawn villageois avec vérification de succès
                if (rand.nextInt(20) == 1) {
                    try {
                        bell.setY(bell.getY() + 1);
                        Villager spawnedVillager = TestJava.world.spawn(bell, Villager.class);
                        
                        if (spawnedVillager != null) {
                            // Mise à jour de la population seulement si le spawn a réussi
                            village.setPopulation(village.getPopulation() + 1);
                            VillageRepository.update(village);
                            totalSpawned++;
                        }
                        
                    } catch (Exception e) {
                        Bukkit.getLogger().severe("[VillagerSpawnThread] Erreur lors du spawn dans " + 
                            village.getId() + ": " + e.getMessage());
                    }
                }
            }
            
            // Un seul log de résumé
            if (totalSpawned > 0) {
                Bukkit.getLogger().info("[VillagerSpawnThread] ✅ Résumé: " + totalSpawned + " villageois spawnés " +
                                       "(vérifié " + villagesChecked + " villages, " + villagesSkipped + " ignorés)");
            } else {
                Bukkit.getLogger().info("[VillagerSpawnThread] ℹ️ Aucun villageois spawné (vérifié " + villagesChecked + " villages)");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[VillagerSpawnThread] ❌ Erreur générale: " + e.getMessage());
        }
    }
}
