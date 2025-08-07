package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillageModel;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.logging.Logger;

public class ChunkManagerService {
    private static final int VILLAGE_RADIUS = 100; // Rayon maximum d'un village en blocs
    private static final Logger LOGGER = TestJava.plugin.getLogger();
    private final Set<String> loadedChunkKeys = new HashSet<>();
    private final World world;

    public ChunkManagerService(World world) {
        this.world = world;
    }

    /**
     * Force le chargement de tous les chunks nécessaires pour tous les villages
     * @param villages Liste des villages à traiter
     */
    public void forceLoadAllVillageChunks(Collection<VillageModel> villages) {
        LOGGER.info("[ChunkManager] Début du chargement forcé des chunks pour " + villages.size() + " villages...");
        
        for (VillageModel village : villages) {
            Set<ChunkCoordinate> chunks = calculateVillageChunks(village);
            for (ChunkCoordinate coord : chunks) {
                forceLoadChunk(coord);
            }
        }
        
        LOGGER.info("[ChunkManager] ✅ Chargement terminé. " + loadedChunkKeys.size() + " chunks maintenus actifs.");
    }

    /**
     * Calcule les coordonnées de tous les chunks dans le rayon du village
     */
    private Set<ChunkCoordinate> calculateVillageChunks(VillageModel village) {
        Set<ChunkCoordinate> chunks = new HashSet<>();
        Location center = new Location(world, village.getX(), village.getY(), village.getZ());
        
        // Convertir le rayon en chunks (16 blocs par chunk)
        int chunkRadius = (VILLAGE_RADIUS / 16) + 1;
        
        // Chunk central
        int baseX = center.getBlockX() >> 4;
        int baseZ = center.getBlockZ() >> 4;
        
        // Ajouter tous les chunks dans le rayon
        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                // Vérifier si le chunk est dans le rayon circulaire
                if (x * x + z * z <= chunkRadius * chunkRadius) {
                    chunks.add(new ChunkCoordinate(baseX + x, baseZ + z));
                }
            }
        }
        
        return chunks;
    }

    /**
     * Force le chargement d'un chunk et le maintient chargé
     */
    private void forceLoadChunk(ChunkCoordinate coord) {
        String key = coord.toString();
        if (loadedChunkKeys.contains(key)) {
            return; // Déjà chargé
        }

        Chunk chunk = world.getChunkAt(coord.x, coord.z);
        if (!chunk.isLoaded()) {
            chunk.load(true);
        }
        chunk.setForceLoaded(true);
        loadedChunkKeys.add(key);
        
        LOGGER.fine("[ChunkManager] Chunk forcé: " + key);
    }

    /**
     * Décharge tous les chunks précédemment forcés
     */
    public void unloadAllChunks() {
        LOGGER.info("[ChunkManager] Déchargement des chunks forcés...");
        
        for (String key : loadedChunkKeys) {
            String[] coords = key.split(":");
            int x = Integer.parseInt(coords[0]);
            int z = Integer.parseInt(coords[1]);
            
            Chunk chunk = world.getChunkAt(x, z);
            chunk.setForceLoaded(false);
        }
        
        loadedChunkKeys.clear();
        LOGGER.info("[ChunkManager] ✅ Tous les chunks ont été déchargés.");
    }

    /**
     * Classe utilitaire pour stocker les coordonnées des chunks
     */
    private static class ChunkCoordinate {
        final int x;
        final int z;

        ChunkCoordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkCoordinate that = (ChunkCoordinate) o;
            return x == that.x && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }

        @Override
        public String toString() {
            return x + ":" + z;
        }
    }
}
