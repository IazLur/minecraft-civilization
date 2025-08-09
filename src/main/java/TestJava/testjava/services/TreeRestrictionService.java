package TestJava.testjava.services;

import org.bukkit.Material;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Service pour gérer les restrictions d'arbres et de graines
 * Empêche la plantation d'arbres par les joueurs et le drop de graines de feuilles
 */
public class TreeRestrictionService {
    
    // Tous les types de saplings (graines d'arbres) disponibles dans Minecraft
    private static final Set<Material> SAPLING_MATERIALS = Set.of(
        Material.OAK_SAPLING,
        Material.BIRCH_SAPLING,
        Material.SPRUCE_SAPLING,
        Material.JUNGLE_SAPLING,
        Material.ACACIA_SAPLING,
        Material.DARK_OAK_SAPLING,
        Material.CHERRY_SAPLING,
        Material.MANGROVE_PROPAGULE,
        Material.PALE_OAK_SAPLING,
        Material.CRIMSON_FUNGUS,    // Nether "trees"
        Material.WARPED_FUNGUS,     // Nether "trees"
        Material.AZALEA,            // Azalea can grow into trees
        Material.FLOWERING_AZALEA   // Flowering azalea can grow into trees
    );
    
    // Tous les types de feuilles pour empêcher le drop de graines
    private static final Set<Material> LEAVES_MATERIALS = Set.of(
        Material.OAK_LEAVES,
        Material.BIRCH_LEAVES,
        Material.SPRUCE_LEAVES,
        Material.JUNGLE_LEAVES,
        Material.ACACIA_LEAVES,
        Material.DARK_OAK_LEAVES,
        Material.CHERRY_LEAVES,
        Material.MANGROVE_LEAVES,
        Material.PALE_OAK_LEAVES,
        Material.NETHER_WART_BLOCK,     // Crimson "leaves"
        Material.WARPED_WART_BLOCK,     // Warped "leaves"
        Material.AZALEA_LEAVES,
        Material.FLOWERING_AZALEA_LEAVES
    );
    
    /**
     * Empêche les joueurs de planter des saplings et autres graines d'arbres
     * @param event L'événement de placement de bloc
     * @return true si l'événement a été traité (annulé), false sinon
     */
    public static boolean preventSaplingPlacement(BlockPlaceEvent event) {
        Material placedMaterial = event.getBlockPlaced().getType();
        
        if (SAPLING_MATERIALS.contains(placedMaterial)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c❌ Vous ne pouvez pas planter d'arbres manuellement !");
            return true;
        }
        
        return false;
    }
    
    /**
     * Empêche le drop de graines d'arbres quand les feuilles sont cassées
     * @param event L'événement de spawn d'item
     * @return true si l'événement a été traité (annulé), false sinon
     */
    public static boolean preventSaplingDropFromLeaves(ItemSpawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        
        // Vérifier si l'item qui spawn est un sapling
        if (SAPLING_MATERIALS.contains(item.getType())) {
            // Vérifier si il y a des feuilles à proximité (indique que ça vient probablement des feuilles)
            if (isNearLeaves(event)) {
                event.setCancelled(true);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Vérifie s'il y a des feuilles à proximité de l'endroit où l'item spawn
     * @param event L'événement de spawn d'item
     * @return true s'il y a des feuilles à proximité
     */
    private static boolean isNearLeaves(ItemSpawnEvent event) {
        var location = event.getLocation();
        var world = location.getWorld();
        
        if (world == null) return false;
        
        // Vérifier dans un rayon de 3 blocs autour du spawn
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    var checkLocation = location.clone().add(x, y, z);
                    var material = world.getBlockAt(checkLocation).getType();
                    
                    if (LEAVES_MATERIALS.contains(material)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Vérifie si le matériau est un sapling
     * @param material Le matériau à vérifier
     * @return true si c'est un sapling
     */
    public static boolean isSapling(Material material) {
        return SAPLING_MATERIALS.contains(material);
    }
    
    /**
     * Vérifie si le matériau est des feuilles
     * @param material Le matériau à vérifier
     * @return true si ce sont des feuilles
     */
    public static boolean isLeaves(Material material) {
        return LEAVES_MATERIALS.contains(material);
    }
}
