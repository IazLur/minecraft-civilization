package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VillagerSynchronizationService {
    private static final int CHUNK_LOAD_RADIUS = 10; // Rayon de chunks à charger autour des villages
    private static final long SYNC_DELAY_TICKS = 200L; // 10 secondes après le démarrage
    private static boolean isSynchronizing = false;

    /**
     * Démarre la synchronisation avec délai pour permettre le chargement des chunks
     */
    public static void startDelayedSync() {
        if (isSynchronizing) {
            Bukkit.getLogger().warning("[VillagerSync] Une synchronisation est déjà en cours");
            return;
        }

        Bukkit.getLogger().info("[VillagerSync] Planification de la synchronisation dans " + 
            (SYNC_DELAY_TICKS / 20) + " secondes...");

        new BukkitRunnable() {
            @Override
            public void run() {
                synchronizeWorldVillagersWithDatabase();
            }
        }.runTaskLater(TestJava.getInstance(), SYNC_DELAY_TICKS);
    }

    /**
     * Synchronise tous les villageois du monde avec la base de données
     */
    public static SynchronizationResult synchronizeWorldVillagersWithDatabase() {
        if (isSynchronizing) {
            Bukkit.getLogger().warning("[VillagerSync] Une synchronisation est déjà en cours");
            return new SynchronizationResult();
        }

        isSynchronizing = true;
        SynchronizationResult result = new SynchronizationResult();
        
        try {
            if (TestJava.world == null) {
                throw new IllegalStateException("Monde non disponible");
            }
            
            // Création d'un backup avant la synchronisation
            Path backupPath = DatabaseBackupService.createBackup();
            if (backupPath == null) {
                throw new IllegalStateException("Échec de la création du backup");
            }

            // Chargement des chunks autour des villages
            loadVillageChunks().thenRun(() -> {
                try {
                    performSynchronization(result);
                } catch (Exception e) {
                    handleSyncError(e, backupPath, result);
                } finally {
                    isSynchronizing = false;
                }
            });

            return result;

        } catch (Exception e) {
            Bukkit.getLogger().severe("[VillagerSync] Erreur critique: " + e.getMessage());
            e.printStackTrace();
            isSynchronizing = false;
            return result;
        }
    }

    private static CompletableFuture<Void> loadVillageChunks() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Récupération de tous les villages
        Collection<VillageModel> villages = VillageRepository.getAll();
        Set<ChunkCoord> chunksToLoad = new HashSet<>();
        
        // Calcul des chunks à charger
        for (VillageModel village : villages) {
            int baseX = (int) village.getX() >> 4;
            int baseZ = (int) village.getZ() >> 4;
            
            for (int x = -CHUNK_LOAD_RADIUS; x <= CHUNK_LOAD_RADIUS; x++) {
                for (int z = -CHUNK_LOAD_RADIUS; z <= CHUNK_LOAD_RADIUS; z++) {
                    chunksToLoad.add(new ChunkCoord(baseX + x, baseZ + z));
                }
            }
        }

        // Compteur de chunks chargés
        AtomicInteger loadedChunks = new AtomicInteger(0);
        int totalChunks = chunksToLoad.size();

        // Chargement asynchrone des chunks
        for (ChunkCoord coord : chunksToLoad) {
            if (!TestJava.world.isChunkLoaded(coord.x, coord.z)) {
                TestJava.world.getChunkAtAsync(coord.x, coord.z).thenAccept(chunk -> {
                    if (loadedChunks.incrementAndGet() == totalChunks) {
                        Bukkit.getLogger().info("[VillagerSync] Tous les chunks sont chargés");
                        future.complete(null);
                    }
                });
            } else if (loadedChunks.incrementAndGet() == totalChunks) {
                future.complete(null);
            }
        }

        return future;
    }

    private static void performSynchronization(SynchronizationResult result) {
        // Récupération des villageois en base
        Collection<VillagerModel> dbVillagers = VillagerRepository.getAll();
        Map<UUID, VillagerModel> dbVillagerMap = dbVillagers.stream()
            .collect(Collectors.toMap(VillagerModel::getId, v -> v));
        
        result.existingInDB = dbVillagers.size();
        
        // Récupération des villageois du monde
        Map<String, Integer> villagePopulationUpdates = new HashMap<>();
        Set<UUID> worldVillagerIds = new HashSet<>();
        
        for (Entity entity : TestJava.world.getEntities()) {
            if (entity instanceof Villager villager && villager.customName() != null) {
                result.worldVillagersWithName++;
                worldVillagerIds.add(villager.getUniqueId());
                
                // Vérification si le villageois existe en base
                if (!dbVillagerMap.containsKey(villager.getUniqueId())) {
                    try {
                        VillagerModel newVillagerModel = VillagerService.createVillagerModelFromEntity(villager);
                        if (newVillagerModel != null) {
                            VillagerRepository.update(newVillagerModel);
                            result.syncedCount++;
                            
                            String villageName = newVillagerModel.getVillageName();
                            villagePopulationUpdates.merge(villageName, 1, Integer::sum);
                        }
                    } catch (Exception e) {
                        result.errors++;
                        Bukkit.getLogger().severe("[VillagerSync] Erreur synchronisation " + 
                            villager.getUniqueId() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // Vérification des villageois disparus
        for (VillagerModel dbVillager : dbVillagers) {
            if (!worldVillagerIds.contains(dbVillager.getId())) {
                try {
                    VillagerRepository.remove(dbVillager.getId());
                    result.removedCount++;
                    
                    String villageName = dbVillager.getVillageName();
                    villagePopulationUpdates.merge(villageName, -1, Integer::sum);
                } catch (Exception e) {
                    result.errors++;
                    Bukkit.getLogger().severe("[VillagerSync] Erreur suppression " + 
                        dbVillager.getId() + ": " + e.getMessage());
                }
            }
        }
        
        // Mise à jour des populations
        updateVillagePopulations(villagePopulationUpdates, result);
        
        // Logs de résumé
        logSyncResults(result);
    }

    private static void handleSyncError(Exception e, Path backupPath, SynchronizationResult result) {
        Bukkit.getLogger().severe("[VillagerSync] Erreur pendant la synchronisation: " + e.getMessage());
        e.printStackTrace();
        
        // Tentative de restauration du backup
        if (DatabaseBackupService.restoreBackup(backupPath)) {
            Bukkit.getLogger().info("[VillagerSync] Backup restauré avec succès");
            result.backupRestored = true;
        } else {
            Bukkit.getLogger().severe("[VillagerSync] Échec de la restauration du backup");
        }
        
        result.errors++;
    }

    private static void updateVillagePopulations(Map<String, Integer> updates, SynchronizationResult result) {
        for (Map.Entry<String, Integer> entry : updates.entrySet()) {
            try {
                String villageName = entry.getKey();
                
                VillageModel village = VillageRepository.get(villageName);
                if (village != null) {
                    // Recompte complet de la population
                    int actualPopulation = countVillagePopulation(villageName);
                    
                    village.setPopulation(actualPopulation);
                    VillageRepository.update(village);
                    
                    result.villagesUpdated++;
                    
                    Bukkit.getLogger().info("[VillagerSync] Population " + villageName + 
                        " mise à jour: " + actualPopulation);
                }
            } catch (Exception e) {
                result.errors++;
                Bukkit.getLogger().severe("[VillagerSync] Erreur mise à jour population " + 
                    entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private static int countVillagePopulation(String villageName) {
        return (int) VillagerRepository.getAll().stream()
            .filter(v -> villageName.equals(v.getVillageName()))
            .count();
    }

    private static void logSyncResults(SynchronizationResult result) {
        Bukkit.getLogger().info("[VillagerSync] ===============================================");
        Bukkit.getLogger().info("[VillagerSync] Synchronisation terminée");
        Bukkit.getLogger().info("[VillagerSync] Villageois en base: " + result.existingInDB);
        Bukkit.getLogger().info("[VillagerSync] Villageois dans le monde: " + result.worldVillagersWithName);
        Bukkit.getLogger().info("[VillagerSync] Nouveaux synchronisés: " + result.syncedCount);
        Bukkit.getLogger().info("[VillagerSync] Villageois supprimés: " + result.removedCount);
        Bukkit.getLogger().info("[VillagerSync] Villages mis à jour: " + result.villagesUpdated);
        Bukkit.getLogger().info("[VillagerSync] Erreurs: " + result.errors);
        
        if (result.backupRestored) {
            Bukkit.getLogger().warning("[VillagerSync] ⚠ Un backup a dû être restauré suite à une erreur");
        }
        
        if (result.syncedCount > 0 || result.removedCount > 0) {
            Bukkit.getServer().sendMessage(Component.text("🔄 Synchronisation: " + 
                result.syncedCount + " nouveaux villageois, " +
                result.removedCount + " supprimés"));
        }
    }

    private static class ChunkCoord {
        final int x;
        final int z;
        
        ChunkCoord(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkCoord that = (ChunkCoord) o;
            return x == that.x && z == that.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }

    public static class SynchronizationResult {
        public int existingInDB = 0;
        public int worldVillagersWithName = 0;
        public int syncedCount = 0;
        public int removedCount = 0;
        public int villagesUpdated = 0;
        public int errors = 0;
        public boolean backupRestored = false;
        
        public boolean wasSuccessful() {
            return errors == 0;
        }
        
        public boolean hadChanges() {
            return syncedCount > 0 || removedCount > 0;
        }
    }
}