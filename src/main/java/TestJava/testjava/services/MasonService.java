package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Villager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service métier pour le Tailleur de Pierre (métier natif MASON).
 * Après paiement du salaire, le tailleur cherche des blocs de cobblestone ou de quartz 
 * dans le rayon du village et les transforme en pierre taillée/quartz taillé.
 */
public class MasonService {

    private static final double MOVE_SPEED = 1.0D;
    private static final double WORK_DISTANCE = 2.5D; // distance pour déclencher la transformation
    private static final int CHECK_PERIOD_TICKS = 20;    // 1s
    private static final int TIMEOUT_TICKS = 20 * 20;    // 20s

    /**
     * Déclenche la transformation de blocs après paiement du salaire.
     */
    public static void triggerBlockTransformationAfterSalary(VillagerModel model, Villager mason) {
        if (model == null || mason == null) return;

        String villageName = model.getVillageName();
        VillageModel village = VillageRepository.get(villageName);
        if (village == null) return;

        World world = TestJava.world;
        if (world == null) return;

        Location center = mason.getLocation();
        int radius = Config.VILLAGE_PROTECTION_RADIUS / 4;

        // Compteur de transformations pour message final
        AtomicInteger transformedCount = new AtomicInteger(0);

        // Chercher d'abord les blocs de cobblestone dans le rayon du village
        List<Block> cobblestoneBlocks = findBlocksInRadius(world, center, radius, Material.COBBLESTONE);
        
        // Si pas de cobblestone, chercher les blocs de quartz
        List<Block> targetBlocks = cobblestoneBlocks.isEmpty() ? 
            findBlocksInRadius(world, center, radius, Material.QUARTZ_BLOCK) : cobblestoneBlocks;

        if (targetBlocks.isEmpty()) {
            Bukkit.getLogger().info("[MasonService] Aucun bloc à transformer trouvé pour " + villageName);
            return;
        }

        // Reset navigation to avoid stuck paths
        try {
            VillagerHomeService.resetVillagerNavigation(mason);
        } catch (Exception ignored) {}

        // Sélectionner un bloc aléatoire dans la liste
        Block targetBlock = targetBlocks.get((int) (Math.random() * targetBlocks.size()));
        
        // Démarrer le processus de transformation
        processBlockTransformation(mason, model, targetBlock, transformedCount, village);
    }

    /**
     * Trouve tous les blocs d'un type donné dans un rayon autour d'un centre
     */
    private static List<Block> findBlocksInRadius(World world, Location center, int radius, Material material) {
        List<Block> blocks = new java.util.ArrayList<>();
        int minY = Math.max(center.getBlockY() - 15, world.getMinHeight());
        int maxY = Math.min(center.getBlockY() + 30, world.getMaxHeight() - 1);

        for (int x = -radius; x <= radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location blockLoc = new Location(world, center.getX() + x, y, center.getZ() + z);
                    Block block = world.getBlockAt(blockLoc);
                    if (block.getType() == material) {
                        blocks.add(block);
                    }
                }
            }
        }
        return blocks;
    }

    /**
     * Traite la transformation d'un bloc spécifique
     */
    private static void processBlockTransformation(Villager mason, VillagerModel model, Block targetBlock, 
                                                   AtomicInteger transformedCount, VillageModel village) {
        // Démarrer le déplacement vers le bloc
        mason.getPathfinder().moveTo(targetBlock.getLocation().add(0.5, 0, 0.5), MOVE_SPEED);

        new org.bukkit.scheduler.BukkitRunnable() {
            int elapsed = 0;
            int moveTick = 0;
            
            @Override
            public void run() {
                elapsed += CHECK_PERIOD_TICKS;
                moveTick += CHECK_PERIOD_TICKS;

                if (!mason.isValid() || mason.isDead()) {
                    this.cancel();
                    return;
                }

                // Vérifier si le bloc existe encore
                if (targetBlock.getType() != Material.COBBLESTONE && targetBlock.getType() != Material.QUARTZ_BLOCK) {
                    this.cancel();
                    return;
                }

                // Toutes les 20 ticks (1 seconde), relancer le pathfinding
                if (moveTick >= 20) {
                    mason.getPathfinder().moveTo(targetBlock.getLocation().add(0.5, 0, 0.5), MOVE_SPEED);
                    moveTick = 0;
                }

                // Si assez proche, transformer le bloc
                if (mason.getLocation().distanceSquared(targetBlock.getLocation().add(0.5, 0, 0.5)) <= WORK_DISTANCE * WORK_DISTANCE) {
                    performBlockTransformation(targetBlock, transformedCount, village);
                    this.cancel();
                    return;
                }

                // Timeout
                if (elapsed >= TIMEOUT_TICKS) {
                    this.cancel();
                }
            }
        }.runTaskTimer(TestJava.plugin, 0L, CHECK_PERIOD_TICKS);
    }

    /**
     * Effectue la transformation physique du bloc
     */
    private static void performBlockTransformation(Block targetBlock, AtomicInteger transformedCount, VillageModel village) {
        Material originalMaterial = targetBlock.getType();
        Material newMaterial;
        String transformationName;

        // Déterminer le type de transformation
        if (originalMaterial == Material.COBBLESTONE) {
            newMaterial = Material.STONE_BRICKS;
            transformationName = "cobblestone en pierre taillée";
        } else if (originalMaterial == Material.QUARTZ_BLOCK) {
            newMaterial = Material.CHISELED_QUARTZ_BLOCK;
            transformationName = "quartz en quartz taillé";
        } else {
            return; // Bloc non supporté
        }

        // Effectuer la transformation
        targetBlock.setType(newMaterial);
        transformedCount.incrementAndGet();

        // Envoyer un message au propriétaire du village
        String ownerName = village.getPlayerName();
        if (ownerName != null && Bukkit.getPlayerExact(ownerName) != null) {
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                Bukkit.getPlayerExact(ownerName).sendMessage(
                    Colorize.name("Le tailleur de pierre a transformé ") +
                    Colorize.name("1 bloc de " + transformationName) +
                    Colorize.name(" à ") + Colorize.name(village.getId())
                );
            }, 2L);
        }

        // Log pour le serveur
        Bukkit.getLogger().info("[MasonService] Transformation: " + transformationName + 
                               " dans le village " + village.getId());
    }
}
