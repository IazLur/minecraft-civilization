package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;

/**
 * Service pour gérer l'équipement d'armure de cuir des employés de métiers custom
 */
public class CustomJobArmorService {
    
    /**
     * Équipe un villageois avec une armure de cuir complète
     */
    public static void equipLeatherArmor(Villager villager) {
        try {
            EntityEquipment equipment = villager.getEquipment();
            if (equipment == null) {
                Bukkit.getLogger().warning("[CustomJobArmor] Impossible d'accéder à l'équipement du villageois " + villager.getUniqueId());
                return;
            }
            
            // Équiper armure de cuir
            equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
            equipment.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
            equipment.setBoots(new ItemStack(Material.LEATHER_BOOTS));
            
            // Empêcher le villageois de lâcher son équipement et le rendre visible
            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
            
            // SOLUTION ALTERNATIVE: Créer un ArmorStand invisible pour porter l'armure visible
            // Note: Les villageois peuvent porter de l'équipement mais il n'est pas toujours rendu visible
            // Nous marquons simplement l'état dans la base de données pour l'instant
            // Une amélioration future pourrait utiliser un ArmorStand invisible attaché au villageois
            
            Bukkit.getLogger().info("[CustomJobArmor] ✅ Armure de cuir équipée pour " + villager.getUniqueId());
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobArmor] Erreur lors de l'équipement: " + e.getMessage());
        }
    }
    
    /**
     * Retire l'armure de cuir d'un villageois
     */
    public static void removeLeatherArmor(Villager villager) {
        try {
            EntityEquipment equipment = villager.getEquipment();
            if (equipment == null) {
                Bukkit.getLogger().warning("[CustomJobArmor] Impossible d'accéder à l'équipement du villageois " + villager.getUniqueId());
                return;
            }
            
            // Retirer l'armure de cuir seulement si elle est présente
            if (isWearingLeatherArmor(equipment)) {
                equipment.setHelmet(null);
                equipment.setChestplate(null);
                equipment.setLeggings(null);
                equipment.setBoots(null);
                
                Bukkit.getLogger().info("[CustomJobArmor] ✅ Armure de cuir retirée pour " + villager.getUniqueId());
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobArmor] Erreur lors du retrait: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie si un villageois porte une armure de cuir
     */
    public static boolean isWearingLeatherArmor(Villager villager) {
        EntityEquipment equipment = villager.getEquipment();
        if (equipment == null) {
            return false;
        }
        
        return isWearingLeatherArmor(equipment);
    }
    
    /**
     * Vérifie si l'équipement contient une armure de cuir
     */
    private static boolean isWearingLeatherArmor(EntityEquipment equipment) {
        ItemStack helmet = equipment.getHelmet();
        ItemStack chestplate = equipment.getChestplate();
        ItemStack leggings = equipment.getLeggings();
        ItemStack boots = equipment.getBoots();
        
        return (helmet != null && helmet.getType() == Material.LEATHER_HELMET) ||
               (chestplate != null && chestplate.getType() == Material.LEATHER_CHESTPLATE) ||
               (leggings != null && leggings.getType() == Material.LEATHER_LEGGINGS) ||
               (boots != null && boots.getType() == Material.LEATHER_BOOTS);
    }
    
    /**
     * S'assure qu'un villageois avec un métier custom porte bien son armure
     */
    public static void ensureCustomJobArmorEquipped(Villager villager) {
        if (!isWearingLeatherArmor(villager)) {
            equipLeatherArmor(villager);
        }
    }
    
    /**
     * Nettoie l'armure d'un villageois qui perd son métier custom
     */
    public static void cleanupArmorOnJobLoss(Villager villager) {
        if (isWearingLeatherArmor(villager)) {
            removeLeatherArmor(villager);
        }
    }
    
    /**
     * Solution alternative: Crée un ArmorStand invisible pour afficher l'armure
     * Utilisé si l'armure du villageois n'est pas visible
     */
    public static ArmorStand createVisibleArmorStand(Villager villager) {
        try {
            Location location = villager.getLocation().add(0, 0.1, 0); // Légèrement au-dessus
            ArmorStand armorStand = villager.getWorld().spawn(location, ArmorStand.class);
            
            // Configuration de l'ArmorStand
            armorStand.setVisible(false); // ArmorStand invisible
            armorStand.setGravity(false); // Pas de gravité
            armorStand.setCanPickupItems(false);
            armorStand.setBasePlate(false);
            armorStand.setArms(true);
            armorStand.setSmall(true); // Plus petit pour mieux correspondre au villageois
            armorStand.setRemoveWhenFarAway(false);
            armorStand.setPersistent(true);
            
            // Équiper l'armure sur l'ArmorStand
            EntityEquipment equipment = armorStand.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
                equipment.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                equipment.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                equipment.setBoots(new ItemStack(Material.LEATHER_BOOTS));
                
                // Empêcher le drop
                equipment.setHelmetDropChance(0.0f);
                equipment.setChestplateDropChance(0.0f);
                equipment.setLeggingsDropChance(0.0f);
                equipment.setBootsDropChance(0.0f);
            }
            
            // Tag personnalisé pour identifier cet ArmorStand
            armorStand.customName(Component.text("CUSTOM_JOB_ARMOR:" + villager.getUniqueId()));
            armorStand.setCustomNameVisible(false);
            
            Bukkit.getLogger().info("[CustomJobArmor] 🛡️ ArmorStand créé pour afficher l'armure de " + villager.getUniqueId());
            
            return armorStand;
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobArmor] Erreur création ArmorStand: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Supprime l'ArmorStand d'armure associé à un villageois
     */
    public static void removeArmorStand(Villager villager) {
        try {
            String targetName = "CUSTOM_JOB_ARMOR:" + villager.getUniqueId();
            
            // Chercher l'ArmorStand dans un rayon de 5 blocs
            villager.getWorld().getNearbyEntities(villager.getLocation(), 5, 5, 5).stream()
                .filter(entity -> entity instanceof ArmorStand)
                .map(entity -> (ArmorStand) entity)
                .filter(armorStand -> targetName.equals(armorStand.getCustomName()))
                .forEach(armorStand -> {
                    armorStand.remove();
                    Bukkit.getLogger().info("[CustomJobArmor] 🗑️ ArmorStand d'armure supprimé pour " + villager.getUniqueId());
                });
                
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobArmor] Erreur suppression ArmorStand: " + e.getMessage());
        }
    }

    /**
     * Équipe tous les villageois ayant un métier personnalisé avec leur armure
     */
    public static void equipAllCustomJobVillagers() {
        if (TestJava.world == null) {
            Bukkit.getLogger().warning("[CustomJobArmor] Impossible d'équiper les villageois - monde non disponible");
            return;
        }

        try {
            TestJava.world.getEntities().stream()
                .filter(entity -> entity instanceof Villager)
                .map(entity -> (Villager) entity)
                .filter(villager -> villager.getProfession() != Villager.Profession.NONE)
                .forEach(villager -> {
                    ensureCustomJobArmorEquipped(villager);
                    Bukkit.getLogger().info("[CustomJobArmor] ✅ Armure vérifiée pour le villageois " + villager.getUniqueId());
                });
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CustomJobArmor] Erreur lors de l'équipement global : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
