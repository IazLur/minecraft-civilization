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
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service métier pour le Clerc (métier natif CLERIC).
 * Après paiement du salaire, le clerc sélectionne un personnage aléatoire et lui applique
 * un effet de buff aléatoire (Régénération I, Force I, ou Vitesse I).
 * 
 * Personnages possibles : Propriétaire du village, Gardes squelettes, Armée de terre, Garde nationale
 * Durée des effets : 15 minutes pour le joueur, 60 minutes pour les autres entités
 */
public class ClericService {

    private static final Random RANDOM = new Random();
    
    // Durées des effets en secondes (convertis en ticks : 1 seconde = 20 ticks)
    private static final int PLAYER_BUFF_DURATION_TICKS = 15 * 60 * 20; // 15 minutes
    private static final int ENTITY_BUFF_DURATION_TICKS = 60 * 60 * 20; // 60 minutes
    
    // Types d'effets disponibles
    private static final PotionEffectType[] BUFF_EFFECTS = {
        PotionEffectType.REGENERATION, // Régénération I
        PotionEffectType.STRENGTH,     // Force I
        PotionEffectType.SPEED         // Vitesse I
    };

    /**
     * Déclenche l'application de buff aléatoire après paiement du salaire.
     */
    public static void triggerRandomBuffAfterSalary(VillagerModel model, Villager cleric) {
        if (model == null || cleric == null) {
            Bukkit.getLogger().warning("[ClericService] Clerc ou modèle villageois null");
            return;
        }

        String villageName = model.getVillageName();
        VillageModel village = VillageRepository.get(villageName);
        if (village == null) {
            Bukkit.getLogger().warning("[ClericService] Village introuvable: " + villageName);
            return;
        }

        World world = TestJava.world;
        if (world == null) {
            Bukkit.getLogger().warning("[ClericService] Monde introuvable");
            return;
        }

        // Collecter tous les personnages éligibles pour le buff
        List<BuffTarget> targets = collectBuffTargets(village, world);
        
        if (targets.isEmpty()) {
            Bukkit.getLogger().info("[ClericService] Aucun personnage éligible trouvé dans le village " + villageName);
            return;
        }

        // Sélectionner une cible aléatoire
        BuffTarget selectedTarget = targets.get(RANDOM.nextInt(targets.size()));
        
        // Sélectionner un effet aléatoire
        PotionEffectType selectedEffect = BUFF_EFFECTS[RANDOM.nextInt(BUFF_EFFECTS.length)];
        
        // Appliquer l'effet
        applyBuffToTarget(selectedTarget, selectedEffect, village);

        Bukkit.getLogger().info("[ClericService] Clerc " + model.getId() + 
                               " a appliqué " + getEffectDisplayName(selectedEffect) + 
                               " à " + selectedTarget.getName() + 
                               " dans le village " + villageName);
    }

    /**
     * Collecte tous les personnages éligibles pour recevoir un buff dans le village
     */
    private static List<BuffTarget> collectBuffTargets(VillageModel village, World world) {
        List<BuffTarget> targets = new ArrayList<>();
        
        Location center = VillageRepository.getBellLocation(village);
        int radius = Config.VILLAGE_PROTECTION_RADIUS;
        
        // 1. Propriétaire du village (s'il est connecté et dans le village)
        String ownerName = village.getPlayerName();
        if (ownerName != null) {
            Player owner = Bukkit.getPlayerExact(ownerName);
            if (owner != null && owner.isOnline()) {
                double distance = owner.getLocation().distance(center);
                if (distance <= radius) {
                    targets.add(new BuffTarget(owner, "Propriétaire " + ownerName, BuffTargetType.PLAYER));
                }
            }
        }
        
        // 2. Gardes squelettes du village
        List<Skeleton> villageGuards = findVillageSkeletons(world, center, radius, village.getId());
        for (Skeleton guard : villageGuards) {
            String guardName = getEntityDisplayName(guard, "Garde");
            targets.add(new BuffTarget(guard, guardName, BuffTargetType.SKELETON_GUARD));
        }
        
        // 3. Armée de terre (autres squelettes avec noms personnalisés)
        List<Skeleton> armySkeletons = findArmySkeletons(world, center, radius, village.getId());
        for (Skeleton soldier : armySkeletons) {
            String soldierName = getEntityDisplayName(soldier, "Soldat");
            targets.add(new BuffTarget(soldier, soldierName, BuffTargetType.ARMY));
        }
        
        // 4. Garde nationale (tous les autres squelettes proches sans distinction spécifique)
        // Note: Pour l'instant, on considère que tous les squelettes trouvés font partie soit des gardes soit de l'armée
        // Si vous voulez une distinction plus fine, il faudrait ajouter des critères spécifiques
        
        return targets;
    }

    /**
     * Trouve tous les squelettes gardes d'un village dans un rayon donné
     */
    private static List<Skeleton> findVillageSkeletons(World world, Location center, int radius, String villageName) {
        List<Skeleton> skeletons = new ArrayList<>();
        
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Skeleton skeleton)) continue;
            if (!skeleton.isValid() || skeleton.isDead()) continue;
            
            // Vérifier si le squelette appartient au village
            String customName = getEntityCustomName(skeleton);
            if (customName != null) {
                try {
                    String entityVillage = CustomName.extractVillageName(customName);
                    if (villageName.equals(entityVillage)) {
                        skeletons.add(skeleton);
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs d'extraction de nom
                }
            }
        }
        
        return skeletons;
    }

    /**
     * Trouve les squelettes de l'armée (critères à définir selon vos besoins)
     */
    private static List<Skeleton> findArmySkeletons(World world, Location center, int radius, String villageName) {
        // Pour l'instant, on utilise la même logique que les gardes
        // Vous pouvez modifier cette méthode pour distinguer l'armée des gardes
        // par exemple par des noms spécifiques, des équipements différents, etc.
        return new ArrayList<>(); // Retourne une liste vide pour l'instant
    }

    /**
     * Applique l'effet de buff à la cible sélectionnée
     */
    private static void applyBuffToTarget(BuffTarget target, PotionEffectType effectType, VillageModel village) {
        int duration = (target.getType() == BuffTargetType.PLAYER) ? 
                       PLAYER_BUFF_DURATION_TICKS : ENTITY_BUFF_DURATION_TICKS;
        
        PotionEffect effect = new PotionEffect(effectType, duration, 0); // Niveau I (amplifier = 0)
        
        if (target.getEntity() instanceof Player player) {
            player.addPotionEffect(effect);
            
            // Message au joueur
            String effectName = getEffectDisplayName(effectType);
            String durationText = (target.getType() == BuffTargetType.PLAYER) ? "15 minutes" : "60 minutes";
            player.sendMessage("✨ " + Colorize.name("Clerc") + " : Vous avez reçu " + 
                             Colorize.name(effectName + " I") + " pendant " + 
                             Colorize.name(durationText) + " !");
            
        } else if (target.getEntity() instanceof Skeleton skeleton) {
            skeleton.addPotionEffect(effect);
        }
        
        // Message au propriétaire du village
        String ownerName = village.getPlayerName();
        if (ownerName != null) {
            Player owner = Bukkit.getPlayerExact(ownerName);
            if (owner != null && owner.isOnline()) {
                String effectName = getEffectDisplayName(effectType);
                String durationText = (target.getType() == BuffTargetType.PLAYER) ? "15 minutes" : "60 minutes";
                
                String message = "⚗️ " + Colorize.name("Clerc") + " : " +
                               Colorize.name(target.getName()) + " a reçu " +
                               Colorize.name(effectName + " I") + " pendant " +
                               Colorize.name(durationText) + " !";
                               
                owner.sendMessage(message);
            }
        }
    }

    /**
     * Obtient le nom personnalisé d'une entité de manière sécurisée
     */
    private static String getEntityCustomName(Entity entity) {
        try {
            if (entity.customName() != null) {
                return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(entity.customName());
            }
        } catch (Exception e) {
            // Ignorer les erreurs d'accès au nom personnalisé
        }
        return null;
    }

    /**
     * Obtient le nom d'affichage d'une entité
     */
    private static String getEntityDisplayName(Entity entity, String defaultPrefix) {
        String customName = getEntityCustomName(entity);
        if (customName != null) {
            return customName;
        }
        return defaultPrefix + " #" + entity.getEntityId();
    }

    /**
     * Obtient le nom d'affichage d'un effet de potion
     */
    private static String getEffectDisplayName(PotionEffectType effectType) {
        if (effectType.equals(PotionEffectType.REGENERATION)) {
            return "Régénération";
        } else if (effectType.equals(PotionEffectType.STRENGTH)) {
            return "Force";
        } else if (effectType.equals(PotionEffectType.SPEED)) {
            return "Vitesse";
        }
        return effectType.getKey().getKey(); // Utilise la clé au lieu de getName()
    }

    /**
     * Classe interne pour représenter une cible de buff
     */
    private static class BuffTarget {
        private final Entity entity;
        private final String name;
        private final BuffTargetType type;

        public BuffTarget(Entity entity, String name, BuffTargetType type) {
            this.entity = entity;
            this.name = name;
            this.type = type;
        }

        public Entity getEntity() { return entity; }
        public String getName() { return name; }
        public BuffTargetType getType() { return type; }
    }

    /**
     * Énumération des types de cibles
     */
    private enum BuffTargetType {
        PLAYER,          // Propriétaire du village
        SKELETON_GUARD,  // Gardes squelettes
        ARMY,            // Armée de terre
        NATIONAL_GUARD   // Garde nationale
    }
}
