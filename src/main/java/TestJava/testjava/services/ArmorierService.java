package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.Config;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.services.VillagerMovementManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArmorierService {
    private static final TestJava plugin = TestJava.getInstance();
    
    // Cache des mouvements en cours
    private static final Map<UUID, BukkitRunnable> activeMovements = new ConcurrentHashMap<>();
    
    // Matériaux d'armure par ordre de progression
    private static final Material[] HELMET_PROGRESSION = {Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET};
    private static final Material[] CHESTPLATE_PROGRESSION = {Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE};
    private static final Material[] LEGGINGS_PROGRESSION = {Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS};
    private static final Material[] BOOTS_PROGRESSION = {Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS};
    
    /**
     * Déclenche l'amélioration d'armure après le paiement du salaire
     */
    public static void triggerArmorUpgradeAfterSalary(VillagerModel villager, VillageModel village) {
        if (villager == null || village == null) {
            plugin.getLogger().warning("ArmorierService: Villager ou Village null");
            return;
        }
        
        try {
            Player owner = Bukkit.getPlayer(village.getPlayerName());
            if (owner == null || !owner.isOnline()) {
                plugin.getLogger().info("ArmorierService: Propriétaire du village non connecté");
                return;
            }
            
            // Vérifier si le joueur est dans le village
            if (!isPlayerInVillage(owner, village)) {
                sendMessage(owner, "§cL'armurier ne peut pas vous rejoindre car vous êtes en dehors du village!");
                return;
            }
            
            plugin.getLogger().info("ArmorierService: Déclenchement de l'amélioration d'armure pour " + owner.getName());
            moveToPlayerAndUpgradeArmor(villager, owner, village);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors du déclenchement de l'amélioration d'armure: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Vérifie si le joueur est dans le village
     */
    private static boolean isPlayerInVillage(Player player, VillageModel village) {
        Location playerLoc = player.getLocation();
        Location villageLoc = new Location(
            player.getWorld(), // Utiliser le monde du joueur
            village.getX(),
            village.getY(),
            village.getZ()
        );
        
        double distance = playerLoc.distance(villageLoc);
        return distance <= Config.VILLAGE_PROTECTION_RADIUS;
    }
    
    /**
     * Déplace le villageois vers le joueur et améliore son armure
     * NOUVEAU: Utilise VillagerMovementManager pour centraliser la logique de déplacement
     */
    private static void moveToPlayerAndUpgradeArmor(VillagerModel villager, Player target, VillageModel village) {
        UUID villagerId = villager.getId();
        
        // Annuler tout mouvement précédent pour ce villageois
        BukkitRunnable existingTask = activeMovements.get(villagerId);
        if (existingTask != null) {
            existingTask.cancel();
            activeMovements.remove(villagerId);
        }
        
        // Trouver l'entité Bukkit villageois
        org.bukkit.entity.Villager bukkitVillager = findBukkitVillager(villager, village);
        if (bukkitVillager == null) {
            plugin.getLogger().warning("ArmorierService: Villageois Bukkit introuvable pour " + villagerId);
            return;
        }
        
        // Vérifications de sécurité
        if (!target.isOnline()) {
            plugin.getLogger().info("ArmorierService: Joueur déconnecté avant le début du mouvement");
            return;
        }
        
        if (!isPlayerInVillage(target, village)) {
            sendMessage(target, "§cVous avez quitté le village! L'armurier ne peut pas vous rejoindre.");
            return;
        }
        
        // Utiliser le gestionnaire centralisé pour le déplacement
        VillagerMovementManager.moveVillager(bukkitVillager, target.getLocation())
            .withSuccessDistance(3.0) // Distance pour l'amélioration d'armure
            .withMoveSpeed(1.0)
            .withTimeout(60) // 60 secondes maximum
            .withName("ArmorierUpgrade_" + target.getName())
            .onSuccess(() -> {
                // Améliorer l'armure à l'arrivée
                if (target.isOnline() && isPlayerInVillage(target, village)) {
                    plugin.getLogger().info("ArmorierService: Villageois arrivé, amélioration de l'armure pour " + target.getName());
                    performArmorUpgrade(target, village);
                } else {
                    plugin.getLogger().info("ArmorierService: Joueur non disponible à l'arrivée du villageois");
                }
            })
            .onFailure(() -> {
                // Échec du déplacement
                plugin.getLogger().info("ArmorierService: Échec du déplacement vers " + target.getName());
                if (target.isOnline()) {
                    sendMessage(target, "§cL'armurier n'a pas pu vous rejoindre. Réessayez plus tard.");
                }
            })
            .onPositionUpdate((distance, attempts) -> {
                // Vérifications périodiques pendant le déplacement
                if (!target.isOnline()) {
                    plugin.getLogger().info("ArmorierService: Joueur déconnecté pendant le mouvement");
                    return; // Le mouvement va s'arrêter automatiquement
                }
                
                if (!isPlayerInVillage(target, village)) {
                    plugin.getLogger().info("ArmorierService: Joueur a quitté le village pendant le mouvement");
                    sendMessage(target, "§cVous avez quitté le village! L'armurier ne peut plus vous rejoindre.");
                    return; // Le mouvement va s'arrêter automatiquement
                }
                
                // Log de progression occasionnel
                if (attempts % 10 == 0) {
                    plugin.getLogger().fine("ArmorierService: Distance vers " + target.getName() + ": " + String.format("%.2f", distance));
                }
            })
            .start();
            
        plugin.getLogger().info("ArmorierService: Démarrage du mouvement centralisé vers " + target.getName());
    }
    
    /**
     * Trouve l'entité Bukkit villageois correspondant au modèle
     */
    private static org.bukkit.entity.Villager findBukkitVillager(VillagerModel villager, VillageModel village) {
        // Chercher un villageois armurier dans le village
        Location center = new Location(
            Bukkit.getWorlds().get(0), // Monde principal par défaut
            village.getX(),
            village.getY(),
            village.getZ()
        );
        
        return center.getWorld().getNearbyEntities(center, Config.VILLAGE_PROTECTION_RADIUS, 50, Config.VILLAGE_PROTECTION_RADIUS)
            .stream()
            .filter(entity -> entity instanceof org.bukkit.entity.Villager)
            .map(entity -> (org.bukkit.entity.Villager) entity)
            .filter(v -> v.getProfession() == org.bukkit.entity.Villager.Profession.ARMORER)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Effectue l'amélioration de l'armure du joueur
     */
    private static void performArmorUpgrade(Player player, VillageModel village) {
        PlayerInventory inventory = player.getInventory();
        List<ArmorSlot> upgradeableSlots = new ArrayList<>();
        
        // Vérifier toutes les pièces d'armure
        checkArmorSlot(inventory.getHelmet(), ArmorSlot.HELMET, upgradeableSlots);
        checkArmorSlot(inventory.getChestplate(), ArmorSlot.CHESTPLATE, upgradeableSlots);
        checkArmorSlot(inventory.getLeggings(), ArmorSlot.LEGGINGS, upgradeableSlots);
        checkArmorSlot(inventory.getBoots(), ArmorSlot.BOOTS, upgradeableSlots);
        
        if (upgradeableSlots.isEmpty()) {
            sendMessage(player, "§eL'armurier vous examine mais ne trouve rien à améliorer sur votre armure.");
            return;
        }
        
        // Choisir une pièce aléatoirement
        ArmorSlot selectedSlot = upgradeableSlots.get(new Random().nextInt(upgradeableSlots.size()));
        ItemStack currentItem = getArmorFromSlot(inventory, selectedSlot);
        
        String upgradeMessage = upgradeArmorPiece(inventory, selectedSlot, currentItem);
        
        // Envoyer le message au propriétaire du village
        Player owner = Bukkit.getPlayer(village.getPlayerName());
        if (owner != null && owner.isOnline()) {
            sendMessage(owner, "§6[Village] §eL'armurier a " + upgradeMessage + " pour " + player.getName() + "!");
        }
        
        // Message au joueur
        sendMessage(player, "§aL'armurier a " + upgradeMessage + "!");
    }
    
    /**
     * Vérifie si une pièce d'armure peut être améliorée
     */
    private static void checkArmorSlot(ItemStack item, ArmorSlot slot, List<ArmorSlot> upgradeableSlots) {
        if (item == null || item.getType() == Material.AIR) {
            // Pas d'armure - peut équiper du cuir
            upgradeableSlots.add(slot);
            return;
        }
        
        Material[] progression = getProgressionForSlot(slot);
        Material currentMaterial = item.getType();
        
        // Vérifier si on peut passer au niveau suivant dans la progression
        for (int i = 0; i < progression.length - 1; i++) {
            if (progression[i] == currentMaterial) {
                upgradeableSlots.add(slot);
                return;
            }
        }
        
        // Si c'est du fer ou du diamant, vérifier l'enchantement Solidité
        if (currentMaterial == progression[progression.length - 1] || 
            currentMaterial == getDiamondEquivalent(slot)) {
            
            int currentDurability = item.getEnchantmentLevel(Enchantment.UNBREAKING);
            if (currentDurability < 3) {
                upgradeableSlots.add(slot);
            }
        }
    }
    
    /**
     * Améliore une pièce d'armure spécifique
     */
    private static String upgradeArmorPiece(PlayerInventory inventory, ArmorSlot slot, ItemStack currentItem) {
        Material[] progression = getProgressionForSlot(slot);
        String slotName = getSlotDisplayName(slot);
        
        if (currentItem == null || currentItem.getType() == Material.AIR) {
            // Équiper du cuir
            ItemStack newArmor = new ItemStack(progression[0]);
            setArmorToSlot(inventory, slot, newArmor);
            return "équipé un " + slotName + " en cuir";
        }
        
        Material currentMaterial = currentItem.getType();
        
        // Progression cuir -> maille -> fer
        for (int i = 0; i < progression.length - 1; i++) {
            if (progression[i] == currentMaterial) {
                ItemStack newArmor = new ItemStack(progression[i + 1]);
                setArmorToSlot(inventory, slot, newArmor);
                return "amélioré votre " + slotName + " en " + getMaterialDisplayName(progression[i + 1]);
            }
        }
        
        // Enchantement Solidité pour fer/diamant
        if (currentMaterial == progression[progression.length - 1] || 
            currentMaterial == getDiamondEquivalent(slot)) {
            
            int currentDurability = currentItem.getEnchantmentLevel(Enchantment.UNBREAKING);
            if (currentDurability < 3) {
                ItemStack newArmor = currentItem.clone();
                ItemMeta meta = newArmor.getItemMeta();
                meta.addEnchant(Enchantment.UNBREAKING, currentDurability + 1, true);
                newArmor.setItemMeta(meta);
                setArmorToSlot(inventory, slot, newArmor);
                return "enchanté votre " + slotName + " avec Solidité " + (currentDurability + 1);
            }
        }
        
        return "examiné votre " + slotName + " sans pouvoir l'améliorer davantage";
    }
    
    /**
     * Obtient la progression de matériaux pour un slot donné
     */
    private static Material[] getProgressionForSlot(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> HELMET_PROGRESSION;
            case CHESTPLATE -> CHESTPLATE_PROGRESSION;
            case LEGGINGS -> LEGGINGS_PROGRESSION;
            case BOOTS -> BOOTS_PROGRESSION;
        };
    }
    
    /**
     * Obtient l'équivalent diamant pour un slot
     */
    private static Material getDiamondEquivalent(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> Material.DIAMOND_HELMET;
            case CHESTPLATE -> Material.DIAMOND_CHESTPLATE;
            case LEGGINGS -> Material.DIAMOND_LEGGINGS;
            case BOOTS -> Material.DIAMOND_BOOTS;
        };
    }
    
    /**
     * Obtient l'item d'armure pour un slot donné
     */
    private static ItemStack getArmorFromSlot(PlayerInventory inventory, ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> inventory.getHelmet();
            case CHESTPLATE -> inventory.getChestplate();
            case LEGGINGS -> inventory.getLeggings();
            case BOOTS -> inventory.getBoots();
        };
    }
    
    /**
     * Définit l'item d'armure pour un slot donné
     */
    private static void setArmorToSlot(PlayerInventory inventory, ArmorSlot slot, ItemStack item) {
        switch (slot) {
            case HELMET -> inventory.setHelmet(item);
            case CHESTPLATE -> inventory.setChestplate(item);
            case LEGGINGS -> inventory.setLeggings(item);
            case BOOTS -> inventory.setBoots(item);
        }
    }
    
    /**
     * Obtient le nom d'affichage pour un slot
     */
    private static String getSlotDisplayName(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> "casque";
            case CHESTPLATE -> "plastron";
            case LEGGINGS -> "jambières";
            case BOOTS -> "bottes";
        };
    }
    
    /**
     * Obtient le nom d'affichage pour un matériau
     */
    private static String getMaterialDisplayName(Material material) {
        return switch (material) {
            case LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS -> "cuir";
            case CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_LEGGINGS, CHAINMAIL_BOOTS -> "maille";
            case IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS -> "fer";
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> "diamant";
            default -> material.name().toLowerCase();
        };
    }
    
    /**
     * Envoie un message avec Adventure API
     */
    private static void sendMessage(Player player, String message) {
        if (player != null && player.isOnline()) {
            Component component = Component.text(message)
                .color(NamedTextColor.YELLOW);
            player.sendMessage(component);
        }
    }
    
    /**
     * Énumération des slots d'armure
     */
    private enum ArmorSlot {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS
    }
    
    /**
     * Obtient des informations de débogage
     */
    public static String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== ArmorierService Debug ===\n");
        info.append("Mouvements actifs: ").append(activeMovements.size()).append("\n");
        
        for (Map.Entry<UUID, BukkitRunnable> entry : activeMovements.entrySet()) {
            info.append("- Villageois: ").append(entry.getKey()).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Force l'arrêt de tous les mouvements
     */
    public static void stopAllMovements() {
        for (BukkitRunnable task : activeMovements.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        activeMovements.clear();
        plugin.getLogger().info("ArmorierService: Tous les mouvements ont été arrêtés");
    }
}
