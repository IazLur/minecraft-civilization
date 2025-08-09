package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service métier pour le Fletcher (métier natif FLETCHER).
 * Après paiement du salaire, le fletcher équipe un garde squelette aléatoire
 * avec une pièce d'armure en or manquante (sauf le casque).
 */
public class FletcherService {

    private static final double MOVE_SPEED = 1.0D;
    private static final double EQUIP_DISTANCE = 2.5D; // distance pour déclencher l'équipement
    private static final int CHECK_PERIOD_TICKS = 10;    // 0.5s
    private static final int TIMEOUT_TICKS = 20 * 20;    // 20s
    private static final Random RANDOM = new Random();

    /**
     * Déclenche l'équipement d'armure en or sur un garde squelette après paiement du salaire.
     */
    public static void triggerArmorEquippingAfterSalary(VillagerModel model, Villager fletcher) {
        if (model == null || fletcher == null) return;

        String villageName = model.getVillageName();
        VillageModel village = VillageRepository.get(villageName);
        if (village == null) return;

        World world = TestJava.world;
        if (world == null) return;

        Location center = VillageRepository.getBellLocation(village);
        int radius = Config.VILLAGE_PROTECTION_RADIUS;

        // Trouver tous les gardes squelettes du village
        List<Skeleton> villageGuards = findVillageGuards(world, center, radius, villageName);
        
        if (villageGuards.isEmpty()) {
            Bukkit.getLogger().info("[FletcherService] Aucun garde squelette trouvé dans le village " + villageName);
            return;
        }

        // Filtrer les gardes qui ont besoin d'équipement (armure or manquante, sauf casque)
        List<Skeleton> guardsNeedingArmor = new ArrayList<>();
        for (Skeleton guard : villageGuards) {
            if (needsGoldenArmor(guard)) {
                guardsNeedingArmor.add(guard);
            }
        }

        if (guardsNeedingArmor.isEmpty()) {
            Bukkit.getLogger().info("[FletcherService] Tous les gardes du village " + villageName + " sont déjà équipés");
            return;
        }

        // Sélectionner un garde aléatoire
        Skeleton selectedGuard = guardsNeedingArmor.get(RANDOM.nextInt(guardsNeedingArmor.size()));

        // Démarrer le processus d'équipement
        AtomicInteger equippedCount = new AtomicInteger(0);
        moveAndEquip(fletcher, model, selectedGuard, equippedCount, village);
    }

    /**
     * Trouve tous les gardes squelettes d'un village dans un rayon donné
     */
    private static List<Skeleton> findVillageGuards(World world, Location center, int radius, String villageName) {
        List<Skeleton> guards = new ArrayList<>();
        
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Skeleton skeleton)) continue;
            if (!skeleton.isValid() || skeleton.isDead()) continue;
            
            // Utiliser customName() pour éviter l'avertissement de dépréciation
            String customName = null;
            if (skeleton.customName() != null) {
                customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(skeleton.customName());
            }
            if (customName == null) continue;

            try {
                String entityVillage = CustomName.extractVillageName(customName);
                if (villageName.equals(entityVillage)) {
                    guards.add(skeleton);
                }
            } catch (Exception e) {
                // Ignorer les erreurs d'extraction de nom
            }
        }
        
        return guards;
    }

    /**
     * Vérifie si un garde squelette a besoin d'armure en or (manque des pièces sauf casque)
     */
    private static boolean needsGoldenArmor(Skeleton guard) {
        EntityEquipment equipment = guard.getEquipment();
        if (equipment == null) return true;

        // Vérifier chaque pièce d'armure (sauf casque)
        ItemStack chestplate = equipment.getChestplate();
        ItemStack leggings = equipment.getLeggings();
        ItemStack boots = equipment.getBoots();

        boolean needsChestplate = (chestplate == null || chestplate.getType() != Material.GOLDEN_CHESTPLATE);
        boolean needsLeggings = (leggings == null || leggings.getType() != Material.GOLDEN_LEGGINGS);
        boolean needsBoots = (boots == null || boots.getType() != Material.GOLDEN_BOOTS);

        return needsChestplate || needsLeggings || needsBoots;
    }

    /**
     * Fait se déplacer le fletcher vers le garde et l'équipe
     */
    private static void moveAndEquip(Villager fletcher, VillagerModel model, Skeleton guard, AtomicInteger equippedCount, VillageModel village) {
        // Démarrer le déplacement
        fletcher.getPathfinder().moveTo(guard.getLocation(), MOVE_SPEED);

        new org.bukkit.scheduler.BukkitRunnable() {
            int elapsed = 0;
            int moveTick = 0;
            
            @Override
            public void run() {
                elapsed += CHECK_PERIOD_TICKS;
                moveTick += CHECK_PERIOD_TICKS;

                if (!fletcher.isValid() || fletcher.isDead() || !guard.isValid() || guard.isDead()) {
                    this.cancel();
                    return;
                }

                // Toutes les 20 ticks (1 seconde), relancer le pathfinding
                if (moveTick >= 20) {
                    fletcher.getPathfinder().moveTo(guard.getLocation(), MOVE_SPEED);
                    moveTick = 0;
                }

                // Si assez proche, équiper
                if (fletcher.getLocation().distanceSquared(guard.getLocation()) <= EQUIP_DISTANCE * EQUIP_DISTANCE) {
                    performEquipment(model, guard, equippedCount, village);
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
     * Équipe le garde squelette avec une pièce d'armure en or manquante
     */
    private static void performEquipment(VillagerModel model, Skeleton guard, AtomicInteger equippedCount, VillageModel village) {
        EntityEquipment equipment = guard.getEquipment();
        if (equipment == null) return;

        // Déterminer quelle pièce d'armure est manquante
        List<Material> missingArmor = new ArrayList<>();
        
        ItemStack chestplate = equipment.getChestplate();
        ItemStack leggings = equipment.getLeggings();
        ItemStack boots = equipment.getBoots();

        if (chestplate == null || chestplate.getType() != Material.GOLDEN_CHESTPLATE) {
            missingArmor.add(Material.GOLDEN_CHESTPLATE);
        }
        if (leggings == null || leggings.getType() != Material.GOLDEN_LEGGINGS) {
            missingArmor.add(Material.GOLDEN_LEGGINGS);
        }
        if (boots == null || boots.getType() != Material.GOLDEN_BOOTS) {
            missingArmor.add(Material.GOLDEN_BOOTS);
        }

        if (missingArmor.isEmpty()) return;

        // Sélectionner une pièce aléatoire parmi celles manquantes
        Material armorToEquip = missingArmor.get(RANDOM.nextInt(missingArmor.size()));
        ItemStack armorPiece = new ItemStack(armorToEquip);

        // Équiper la pièce d'armure
        switch (armorToEquip) {
            case GOLDEN_CHESTPLATE:
                equipment.setChestplate(armorPiece);
                break;
            case GOLDEN_LEGGINGS:
                equipment.setLeggings(armorPiece);
                break;
            case GOLDEN_BOOTS:
                equipment.setBoots(armorPiece);
                break;
            default:
                // Ne devrait pas arriver, mais au cas où
                return;
        }

        // Empêcher le drop de l'armure
        equipment.setChestplateDropChance(0.0f);
        equipment.setLeggingsDropChance(0.0f);
        equipment.setBootsDropChance(0.0f);

        equippedCount.incrementAndGet();

        // Envoyer un message coloré au propriétaire du village
        String ownerName = village.getPlayerName();
        if (ownerName != null && Bukkit.getPlayerExact(ownerName) != null) {
            String armorName = getArmorDisplayName(armorToEquip);
            
            // Utiliser customName() pour éviter l'avertissement de dépréciation
            String guardName = "un garde";
            if (guard.customName() != null) {
                guardName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(guard.customName());
            }
            
            String message = "🏹 " + Colorize.name("Fletcher") + " : " +
                           Colorize.name(guardName) + " a été équipé d'un(e) " +
                           Colorize.name(armorName) + " en or ! ✨";
            
            Bukkit.getPlayerExact(ownerName).sendMessage(message);
        }

        Bukkit.getLogger().info("[FletcherService] Fletcher " + model.getId() + 
                               " a équipé un garde avec " + armorToEquip.name() + 
                               " dans le village " + village.getId());
    }

    /**
     * Obtient le nom d'affichage d'une pièce d'armure
     */
    private static String getArmorDisplayName(Material armor) {
        switch (armor) {
            case GOLDEN_CHESTPLATE:
                return "plastron";
            case GOLDEN_LEGGINGS:
                return "jambières";
            case GOLDEN_BOOTS:
                return "bottes";
            default:
                return armor.name().toLowerCase();
        }
    }
}
