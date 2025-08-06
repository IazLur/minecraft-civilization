package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.enums.SocialClass;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service pour faire chercher automatiquement des métiers aux villageois inactifs
 */
public class InactiveJobSearchService {
    
    private static final int SEARCH_RADIUS = 50; // Rayon de recherche de blocs de métier
    
    // Blocs de métier officiels Minecraft
    private static final Set<Material> JOB_BLOCKS = Set.of(
        Material.COMPOSTER,
        Material.BLAST_FURNACE,
        Material.SMOKER,
        Material.CARTOGRAPHY_TABLE,
        Material.BREWING_STAND,
        Material.SMITHING_TABLE,
        Material.FLETCHING_TABLE,
        Material.LOOM,
        Material.STONECUTTER,
        Material.CAULDRON,
        Material.LECTERN,
        Material.GRINDSTONE,
        Material.BARREL
    );
    
    /**
     * Fait chercher un métier à tous les villageois inactifs
     */
    public static void searchJobsForInactiveVillagers() {
        if (TestJava.world == null) {
            return;
        }
        
        try {
            List<VillagerModel> inactiveVillagers = findInactiveVillagers();
            
            Bukkit.getLogger().info("[InactiveJobSearch] Recherche métiers pour " + 
                                   inactiveVillagers.size() + " villageois inactifs");
            
            for (VillagerModel villagerModel : inactiveVillagers) {
                assignJobToInactiveVillager(villagerModel);
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[InactiveJobSearch] Erreur: " + e.getMessage());
        }
    }
    
    /**
     * Trouve tous les villageois inactifs sans métier
     */
    private static List<VillagerModel> findInactiveVillagers() {
        List<VillagerModel> inactiveVillagers = new ArrayList<>();
        
        for (VillagerModel villagerModel : VillagerRepository.getAll()) {
            // Vérifier si le villageois est inactif
            if (villagerModel.getSocialClassEnum() == SocialClass.INACTIVE) {
                // Vérifier si l'entité existe et n'a pas de métier
                Entity entity = Bukkit.getServer().getEntity(villagerModel.getId());
                if (entity instanceof Villager villager && 
                    villager.getProfession() == Villager.Profession.NONE) {
                    
                    inactiveVillagers.add(villagerModel);
                }
            }
        }
        
        return inactiveVillagers;
    }
    
    /**
     * Assigne un métier à un villageois inactif
     */
    private static void assignJobToInactiveVillager(VillagerModel villagerModel) {
        try {
            Villager villager = (Villager) Bukkit.getServer().getEntity(villagerModel.getId());
            if (villager == null) {
                return;
            }
            
            // Chercher un bloc de métier libre dans le rayon
            Block jobBlock = findAvailableJobBlock(villager.getLocation());
            if (jobBlock == null) {
                return;
            }
            
            // Réveiller le villageois s'il dort (correction bug nocturne)
            if (villager.isSleeping()) {
                villager.wakeup();
                
                // Empêcher de redormir immédiatement
                preventImmediateSleep(villager);
            }
            
            // Diriger le villageois vers le bloc de métier
            Location targetLocation = jobBlock.getLocation().add(0.5, 1, 0.5);
            villager.getPathfinder().moveTo(targetLocation, 1.0);
            
            Bukkit.getLogger().info("[InactiveJobSearch] " + villager.getUniqueId() + 
                                   " dirigé vers " + jobBlock.getType() + 
                                   " à " + locationToString(jobBlock.getLocation()));
            
            // Programmer une vérification pour s'assurer que le métier est pris
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                verifyJobAssignment(villager, jobBlock);
            }, 100L); // 5 secondes de délai
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[InactiveJobSearch] Erreur assignment: " + e.getMessage());
        }
    }
    
    /**
     * Cherche un bloc de métier libre dans un rayon donné
     */
    private static Block findAvailableJobBlock(Location center) {
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    Block block = center.getWorld().getBlockAt(
                        center.getBlockX() + x,
                        center.getBlockY() + y,
                        center.getBlockZ() + z
                    );
                    
                    // Vérifier si c'est un bloc de métier
                    if (JOB_BLOCKS.contains(block.getType())) {
                        // Vérifier s'il est libre (pas déjà utilisé par un autre villageois)
                        if (isJobBlockAvailable(block)) {
                            return block;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Vérifie si un bloc de métier est disponible
     */
    private static boolean isJobBlockAvailable(Block jobBlock) {
        Location blockLocation = jobBlock.getLocation();
        
        // Chercher des villageois dans un rayon de 5 blocs autour du bloc
        for (Entity entity : TestJava.world.getNearbyEntities(blockLocation, 5, 5, 5)) {
            if (entity instanceof Villager villager) {
                // Si un villageois a une profession et est proche du bloc, il l'utilise probablement
                if (villager.getProfession() != Villager.Profession.NONE) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Empêche un villageois de redormir immédiatement
     */
    private static void preventImmediateSleep(Villager villager) {
        // Programmer des réveils répétés pendant 30 secondes
        for (int i = 1; i <= 6; i++) {
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                if (villager.isSleeping()) {
                    villager.wakeup();
                    Bukkit.getLogger().info("[InactiveJobSearch] Réveil forcé: " + villager.getUniqueId());
                }
            }, i * 100L); // Toutes les 5 secondes pendant 30 secondes
        }
    }
    
    /**
     * Vérifie que le villageois a bien pris le métier
     */
    private static void verifyJobAssignment(Villager villager, Block jobBlock) {
        try {
            if (villager.getProfession() != Villager.Profession.NONE) {
                Bukkit.getLogger().info("[InactiveJobSearch] ✅ Métier attribué avec succès: " + 
                                       villager.getProfession() + " pour " + villager.getUniqueId());
            } else {
                Bukkit.getLogger().warning("[InactiveJobSearch] ❌ Échec attribution métier pour " + 
                                          villager.getUniqueId());
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[InactiveJobSearch] Erreur vérification: " + e.getMessage());
        }
    }
    
    /**
     * Convertit une location en chaîne lisible
     */
    private static String locationToString(Location location) {
        return String.format("(%d, %d, %d)", 
                           location.getBlockX(), 
                           location.getBlockY(), 
                           location.getBlockZ());
    }
}