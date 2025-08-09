package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service métier pour le Forgeron d'Outils (métier natif TOOLSMITH).
 * Après paiement du salaire, le forgeron répare les golems de fer du village.
 */
public class ToolsmithService {

    // Coût: 0.1 juridictions pour 10 coeurs (20 HP) => 0.005 par HP restauré
    private static final float COST_PER_HP = 0.005f;
    private static final double MOVE_SPEED = 1.0D;
    private static final double REPAIR_DISTANCE = 2.5D; // distance pour déclencher la réparation
    private static final int CHECK_PERIOD_TICKS = 10;    // 0.5s
    private static final int TIMEOUT_TICKS = 20 * 20;    // 20s

    /**
     * Déclenche la boucle de réparation des golems après paiement du salaire.
     */
    public static void triggerRepairsAfterSalary(VillagerModel model, Villager toolsmith) {
        if (model == null || toolsmith == null) return;

        String villageName = model.getVillageName();
        VillageModel village = VillageRepository.get(villageName);
        if (village == null) return;

        World world = TestJava.world;
        if (world == null) return;

        Location center = java.util.Objects.requireNonNullElse(VillageRepository.getBellLocation(village), toolsmith.getLocation());
        int radius = Config.VILLAGE_PROTECTION_RADIUS;

        // Compteur de réparations pour message final
    AtomicInteger repairedCount = new AtomicInteger(0);
    java.util.concurrent.atomic.AtomicReference<Float> totalCost = new java.util.concurrent.atomic.AtomicReference<>(0f);

        // Récupérer tous les golems de fer dans le rayon du village
        Collection<org.bukkit.entity.Entity> nearby = world.getNearbyEntities(center, radius, radius, radius,
                e -> e instanceof IronGolem);

        // Filtrer les golems endommagés
        java.util.List<IronGolem> damagedGolems = new java.util.ArrayList<>();
        for (org.bukkit.entity.Entity e : nearby) {
            if (!(e instanceof IronGolem golem)) continue;
            if (!golem.isValid() || golem.isDead()) continue;
            double maxHp = getMaxHealth(golem);
            double currentHp = golem.getHealth();
            double hpMissing = Math.max(0.0, maxHp - currentHp);
            if (hpMissing < 0.5) continue; // pas besoin de réparer
            damagedGolems.add(golem);
        }
        if (damagedGolems.isEmpty()) return;

        // Reset navigation to avoid stuck paths
        try {
            VillagerHomeService.resetVillagerNavigation(toolsmith);
        } catch (Exception ignored) {}

        // Process golems one by one, waiting for each repair before next
    processGolemSequentially(toolsmith, model, damagedGolems, 0, repairedCount, totalCost, village);
    }

    // Helper: process golems sequentially
    private static void processGolemSequentially(Villager toolsmith, VillagerModel model, java.util.List<IronGolem> golems, int idx, AtomicInteger repairedCount, java.util.concurrent.atomic.AtomicReference<Float> totalCost, VillageModel village) {
        if (idx >= golems.size()) {
            // All done, send message if needed
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                int count = repairedCount.get();
                float cost = totalCost.get();
                if (count <= 0) return;
                String owner = village.getPlayerName();
                if (owner != null && Bukkit.getPlayerExact(owner) != null) {
                    Bukkit.getPlayerExact(owner).sendMessage(Colorize.name("Le forgeron a réparé ") +
                            Colorize.name(count + " golems pour un total de ") +
                            Colorize.name(String.format("%.2fµ.", cost)));
                }
            }, 20L); // court délai après la dernière réparation
            return;
        }
        IronGolem golem = golems.get(idx);
        moveAndRepair(toolsmith, model, golem, repairedCount, totalCost, () -> {
            // Après réparation, passer au suivant
            processGolemSequentially(toolsmith, model, golems, idx + 1, repairedCount, totalCost, village);
        });
    }

    private static void moveAndRepair(Villager toolsmith, VillagerModel model, IronGolem golem, AtomicInteger repairedCount, java.util.concurrent.atomic.AtomicReference<Float> totalCost, Runnable onDone) {
        // Démarrer le déplacement
        toolsmith.getPathfinder().moveTo(golem.getLocation(), MOVE_SPEED);

        new org.bukkit.scheduler.BukkitRunnable() {
            int elapsed = 0;
            int moveTick = 0;
            @Override
            public void run() {
                elapsed += CHECK_PERIOD_TICKS;
                moveTick += CHECK_PERIOD_TICKS;

                if (!toolsmith.isValid() || toolsmith.isDead() || !golem.isValid() || golem.isDead()) {
                    this.cancel();
                    if (onDone != null) Bukkit.getScheduler().runTask(TestJava.plugin, onDone);
                    return;
                }

                // Toutes les 20 ticks (1 seconde), relancer le pathfinding
                if (moveTick >= 20) {
                    toolsmith.getPathfinder().moveTo(golem.getLocation(), MOVE_SPEED);
                    moveTick = 0;
                }

                // Si assez proche, réparer
                if (toolsmith.getLocation().distanceSquared(golem.getLocation()) <= REPAIR_DISTANCE * REPAIR_DISTANCE) {
                    performRepair(model, golem, repairedCount, totalCost);
                    this.cancel();
                    if (onDone != null) Bukkit.getScheduler().runTask(TestJava.plugin, onDone);
                    return;
                }

                // Timeout
                if (elapsed >= TIMEOUT_TICKS) {
                    this.cancel();
                    if (onDone != null) Bukkit.getScheduler().runTask(TestJava.plugin, onDone);
                }
            }
        }.runTaskTimer(TestJava.plugin, 0L, CHECK_PERIOD_TICKS);
    }

    private static void performRepair(VillagerModel model, IronGolem golem, AtomicInteger repairedCount, java.util.concurrent.atomic.AtomicReference<Float> totalCost) {
        VillageModel village = VillageRepository.get(model.getVillageName());
        if (village == null) return;

        EmpireModel empire = EmpireRepository.getForPlayer(village.getPlayerName());
        if (empire == null) return;

        double maxHp = getMaxHealth(golem);
        double currentHp = golem.getHealth();
        double hpMissing = Math.max(0.0, maxHp - currentHp);
        if (hpMissing < 0.5) return;

        float funds = empire.getJuridictionCount();
        if (funds <= 0f) return;

        // HP réparables selon les fonds
        double hpAffordable = Math.min(hpMissing, funds / COST_PER_HP);
        if (hpAffordable <= 0.0) return;

        double newHp = Math.min(maxHp, currentHp + hpAffordable);
        double hpRestored = newHp - currentHp;
        float cost = (float) (hpRestored * COST_PER_HP);

        // Appliquer la réparation et le coût
        try {
            golem.setHealth(newHp);
        } catch (IllegalArgumentException ignored) {
            // au cas où l’API rejette une valeur hors borne
            golem.setHealth(Math.min(maxHp, Math.max(1.0, newHp)));
        }

    empire.setJuridictionCount(funds - cost);
    EmpireRepository.update(empire);

    repairedCount.incrementAndGet();
    totalCost.set(totalCost.get() + cost);
    }

    private static double getMaxHealth(LivingEntity entity) {
        try {
            org.bukkit.attribute.AttributeInstance attr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) return attr.getBaseValue();
        } catch (Throwable ignored) {}
        // Fallback raisonnable si l'attribut n'est pas dispo
        return Math.max(20.0, entity.getHealth());
    }
}
