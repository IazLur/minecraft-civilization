package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.enums.SocialClass;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocialClassService {

    // Seuils de nourriture pour les transitions
    private static final int FOOD_THRESHOLD_0_TO_1 = 19; // Misérable → Inactive
    private static final int FOOD_THRESHOLD_1_TO_0 = 6;  // Inactive → Misérable  
    private static final int FOOD_THRESHOLD_2_TO_0 = 5;  // Ouvrière → Misérable

    // Pattern pour nettoyer les tags de classe existants (anciens crochets ET nouveaux accolades)
    private static final Pattern SOCIAL_CLASS_TAG_PATTERN = Pattern.compile("(\\[\\d\\]|\\{\\d\\})\\s*");

    /**
     * Évalue et met à jour la classe sociale d'un villageois en fonction de sa nourriture
     */
    public static void evaluateAndUpdateSocialClass(VillagerModel villager) {
        if (villager == null || villager.getFood() == null) {
            return;
        }

        SocialClass currentClass = villager.getSocialClassEnum();
        SocialClass newClass = currentClass;
        int food = villager.getFood();
        
        Bukkit.getLogger().info("[SocialClass] Évaluation pour villageois " + villager.getId() + 
                               " - Classe actuelle: " + currentClass.getName() + 
                               " - Nourriture: " + food);

        // Logique de transition basée sur la classe actuelle
        switch (currentClass) {
            case MISERABLE: // Classe 0
                if (food >= FOOD_THRESHOLD_0_TO_1) {
                    newClass = SocialClass.INACTIVE;
                    Bukkit.getLogger().info("[SocialClass] Promotion: Misérable → Inactive");
                }
                break;

            case INACTIVE: // Classe 1
                if (food < FOOD_THRESHOLD_1_TO_0) {
                    newClass = SocialClass.MISERABLE;
                    Bukkit.getLogger().info("[SocialClass] Rétrogradation: Inactive → Misérable");
                }
                // Note: Promotion vers Ouvrière se fait lors de l'obtention d'un métier
                break;

            case OUVRIERE: // Classe 2
                if (food <= FOOD_THRESHOLD_2_TO_0) {
                    newClass = SocialClass.MISERABLE;
                    removeJobFromVillager(villager);
                    Bukkit.getLogger().info("[SocialClass] Rétrogradation drastique: Ouvrière → Misérable (perte métier)");
                }
                // Si perd métier pour autre raison → retourne à Inactive (géré ailleurs)
                break;

            default:
                // Classes 3 et 4 pas encore implémentées
                break;
        }

        // Applique le changement si nécessaire
        if (newClass != currentClass) {
            updateVillagerSocialClass(villager, newClass);
        }
    }

    /**
     * Met à jour la classe sociale d'un villageois et sauvegarde
     */
    public static void updateVillagerSocialClass(VillagerModel villager, SocialClass newClass) {
        SocialClass oldClass = villager.getSocialClassEnum();
        villager.setSocialClassEnum(newClass);
        
        // Sauvegarde immédiate
        VillagerRepository.update(villager);
        
        // Met à jour l'affichage du nom
        updateVillagerDisplayName(villager);
        
        // Gère les restrictions de métier
        handleJobRestrictions(villager, oldClass, newClass);
        
        Bukkit.getLogger().info("[SocialClass] Changement de classe pour " + villager.getId() + 
                               ": " + oldClass.getName() + " → " + newClass.getName());
        
        // Broadcast si changement significatif
        broadcastSocialClassChange(villager, oldClass, newClass);
    }

    /**
     * Promotion automatique vers Ouvrière lors de l'obtention d'un métier
     */
    public static void promoteToWorkerOnJobAssignment(VillagerModel villager) {
        // Récupère la version fraîche depuis la base de données
        VillagerModel freshVillager = VillagerRepository.find(villager.getId());
        if (freshVillager == null) return;
        
        if (freshVillager.getSocialClassEnum() == SocialClass.INACTIVE) {
            updateVillagerSocialClass(freshVillager, SocialClass.OUVRIERE);
            Bukkit.getLogger().info("[SocialClass] ✅ Promotion automatique: Inactive → Ouvrière (obtention métier) pour " + freshVillager.getId());
        } else {
            Bukkit.getLogger().warning("[SocialClass] ❌ Tentative promotion villageois non-Inactive: " + 
                                     freshVillager.getSocialClassEnum().getName() + " pour " + freshVillager.getId());
        }
    }

    /**
     * Rétrogradation vers Inactive lors de la perte d'un métier (non-alimentaire)
     */
    public static void demoteToInactiveOnJobLoss(VillagerModel villager) {
        // Récupère la version fraîche depuis la base de données
        VillagerModel freshVillager = VillagerRepository.find(villager.getId());
        if (freshVillager == null) return;
        
        if (freshVillager.getSocialClassEnum() == SocialClass.OUVRIERE) {
            updateVillagerSocialClass(freshVillager, SocialClass.INACTIVE);
            Bukkit.getLogger().info("[SocialClass] ✅ Rétrogradation: Ouvrière → Inactive (perte métier) pour " + freshVillager.getId());
        } else {
            Bukkit.getLogger().warning("[SocialClass] ❌ Tentative rétrogradation villageois non-Ouvrière: " + 
                                     freshVillager.getSocialClassEnum().getName() + " pour " + freshVillager.getId());
        }
    }

    /**
     * Met à jour le nom d'affichage du villageois avec le tag de classe sociale
     */
    public static void updateVillagerDisplayName(VillagerModel villager) {
        if (TestJava.world == null) {
            Bukkit.getLogger().warning("[SocialClass] Monde null - impossible de mettre à jour le nom");
            return;
        }

        try {
            Bukkit.getLogger().info("[SocialClass] Mise à jour nom pour villageois " + villager.getId() + 
                                   " - Classe: " + villager.getSocialClassEnum().getName() + 
                                   " (niveau " + villager.getSocialClass() + ")");
            
            // Trouve l'entité villageois correspondante
            boolean entityFound = false;
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Villager bukkit_villager && 
                    entity.getUniqueId().equals(villager.getId())) {
                    
                    String currentName = bukkit_villager.getCustomName();
                    Bukkit.getLogger().info("[SocialClass] Nom actuel: '" + currentName + "'");
                    
                    if (currentName == null) {
                        currentName = "Villageois";
                        Bukkit.getLogger().info("[SocialClass] Nom null, utilisation par défaut: " + currentName);
                    }
                    
                    // Nettoie les anciens tags de classe sociale
                    String cleanName = SOCIAL_CLASS_TAG_PATTERN.matcher(currentName).replaceAll("").trim();
                    Bukkit.getLogger().info("[SocialClass] Nom nettoyé: '" + cleanName + "'");
                    
                    // Ajoute le nouveau tag
                    SocialClass socialClass = villager.getSocialClassEnum();
                    String coloredTag = socialClass.getColoredTag();
                    String newName = coloredTag + cleanName;
                    
                    Bukkit.getLogger().info("[SocialClass] Nouveau nom complet: '" + newName + "'");
                    Bukkit.getLogger().info("[SocialClass] Tag coloré: '" + coloredTag + "'");
                    
                    bukkit_villager.setCustomName(newName);
                    bukkit_villager.setCustomNameVisible(true);
                    
                    // Vérification immédiate
                    String verifyName = bukkit_villager.getCustomName();
                    Bukkit.getLogger().info("[SocialClass] ✅ Nom appliqué avec succès: '" + verifyName + "'");
                    
                    entityFound = true;
                    break;
                }
            }
            
            // Si l'entité n'est pas trouvée, c'est potentiellement un villageois fantôme
            if (!entityFound) {
                Bukkit.getLogger().warning("[SocialClass] ❌ Entité villageois introuvable pour " + 
                                         villager.getId() + " - possible villageois fantôme");
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[SocialClass] ❌ Erreur lors de la mise à jour du nom: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gère les restrictions de métier selon la classe sociale
     */
    private static void handleJobRestrictions(VillagerModel villager, SocialClass oldClass, SocialClass newClass) {
        // Si passe à classe 0, doit perdre son métier
        if (newClass.shouldLoseJob()) {
            removeJobFromVillager(villager);
        }
    }

    /**
     * Retire le métier d'un villageois
     */
    private static void removeJobFromVillager(VillagerModel villager) {
        if (TestJava.world == null) return;

        try {
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Villager bukkit_villager && 
                    entity.getUniqueId().equals(villager.getId())) {
                    
                    // Retire la profession
                    bukkit_villager.setProfession(Villager.Profession.NONE);
                    
                    // Annule les déplacements vers les blocs de métier
                    bukkit_villager.getPathfinder().stopPathfinding();
                    
                    Bukkit.getLogger().info("[SocialClass] Métier retiré pour villageois " + villager.getId());
                    break;
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SocialClass] Erreur lors du retrait du métier: " + e.getMessage());
        }
    }

    /**
     * Diffuse un message de changement de classe sociale significatif
     */
    private static void broadcastSocialClassChange(VillagerModel villager, SocialClass oldClass, SocialClass newClass) {
        // Ne diffuse que les changements majeurs
        if (Math.abs(newClass.getLevel() - oldClass.getLevel()) >= 1) {
            String message = ChatColor.YELLOW + "Un villageois de " + villager.getVillageName() + 
                           " est passé de " + oldClass.getColor() + oldClass.getName() + 
                           ChatColor.YELLOW + " à " + newClass.getColor() + newClass.getName();
            
            Bukkit.getServer().broadcastMessage(message);
        }
    }

    /**
     * Vérifie si un villageois peut avoir un métier selon sa classe sociale
     */
    public static boolean canVillagerHaveJob(VillagerModel villager) {
        return villager.getSocialClassEnum().canHaveJob();
    }

    /**
     * Vérifie si un villageois peut avoir un métier selon son UUID
     */
    public static boolean canVillagerHaveJob(UUID villagerId) {
        VillagerModel villager = VillagerRepository.find(villagerId);
        return villager != null && canVillagerHaveJob(villager);
    }

    /**
     * Initialise la classe sociale pour tous les villageois existants (migration)
     */
    public static void initializeSocialClassForExistingVillagers() {
        Bukkit.getLogger().info("[SocialClass] Initialisation des classes sociales existantes...");
        
        int initialized = 0;
        int reevaluated = 0;
        
        for (VillagerModel villager : VillagerRepository.getAll()) {
            boolean wasNull = villager.getSocialClass() == null;
            
            if (wasNull) {
                // Initialise à 0 si pas défini
                villager.setSocialClass(0);
                VillagerRepository.update(villager);
                initialized++;
                Bukkit.getLogger().info("[SocialClass] Villageois " + villager.getId() + 
                                       " initialisé avec classe 0 (nourriture: " + villager.getFood() + ")");
            }
            
            // Évalue la classe sociale basée sur la nourriture actuelle
            SocialClass oldClass = villager.getSocialClassEnum();
            evaluateAndUpdateSocialClass(villager);
            SocialClass newClass = villager.getSocialClassEnum();
            
            if (newClass != oldClass) {
                reevaluated++;
                Bukkit.getLogger().info("[SocialClass] Villageois " + villager.getId() + 
                                       " réévalué: " + oldClass.getName() + " → " + newClass.getName() + 
                                       " (nourriture: " + villager.getFood() + ")");
            }
            
            // S'assure que le nom d'affichage est correct
            updateVillagerDisplayName(villager);
        }
        
        Bukkit.getLogger().info("[SocialClass] ✅ Migration terminée: " + 
                               initialized + " initialisés, " + 
                               reevaluated + " réévalués");
    }

    /**
     * Force la réévaluation de TOUS les villageois (commande admin)
     */
    public static void forceReevaluateAllVillagers() {
        Bukkit.getLogger().info("[SocialClass] Réévaluation forcée de tous les villageois...");
        
        int total = 0;
        int changed = 0;
        
        for (VillagerModel villager : VillagerRepository.getAll()) {
            SocialClass oldClass = villager.getSocialClassEnum();
            evaluateAndUpdateSocialClass(villager);
            SocialClass newClass = villager.getSocialClassEnum();
            
            total++;
            if (newClass != oldClass) {
                changed++;
                Bukkit.getLogger().info("[SocialClass] Changement forcé: " + villager.getId() + 
                                       " " + oldClass.getName() + " → " + newClass.getName() + 
                                       " (nourriture: " + villager.getFood() + ")");
            }
            
            updateVillagerDisplayName(villager);
        }
        
        Bukkit.getLogger().info("[SocialClass] ✅ Réévaluation forcée terminée: " + 
                               changed + "/" + total + " villageois modifiés");
    }

    /**
     * Migre tous les villageois existants du format ancien [0][Village] vers nouveau {0} [Village]
     */
    public static void migrateSocialClassTagsToNewFormat() {
        Bukkit.getLogger().info("[SocialClass] ===============================================");
        Bukkit.getLogger().info("[SocialClass] Démarrage migration format tags classe sociale...");
        
        if (TestJava.world == null) {
            Bukkit.getLogger().severe("[SocialClass] Monde non chargé - migration impossible");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        int migratedCount = 0;
        int errorCount = 0;
        
        try {
            // 1. Migrer tous les villageois Bukkit dans le monde
            for (org.bukkit.entity.Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof org.bukkit.entity.Villager villager && villager.getCustomName() != null) {
                    totalProcessed++;
                    String currentName = villager.getCustomName();
                    
                    try {
                        String newName = convertOldTagFormatToNew(currentName);
                        if (!newName.equals(currentName)) {
                            villager.setCustomName(newName);
                            villager.setCustomNameVisible(true);
                            migratedCount++;
                            Bukkit.getLogger().info("[SocialClass] Migration: '" + currentName + "' → '" + newName + "'");
                        }
                    } catch (Exception e) {
                        errorCount++;
                        Bukkit.getLogger().warning("[SocialClass] Erreur migration villageois " + 
                            villager.getUniqueId() + ": " + e.getMessage());
                    }
                }
            }
            
            // 2. Forcer la mise à jour de tous les noms via le système de classes sociales
            for (VillagerModel villager : VillagerRepository.getAll()) {
                try {
                    updateVillagerDisplayName(villager);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[SocialClass] Erreur update nom villageois " + 
                        villager.getId() + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[SocialClass] Erreur critique migration: " + e.getMessage());
            e.printStackTrace();
        }
        
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        
        Bukkit.getLogger().info("[SocialClass] ===============================================");
        Bukkit.getLogger().info("[SocialClass] ✅ Migration terminée en " + String.format("%.2f", duration) + " secondes");
        Bukkit.getLogger().info("[SocialClass] Villageois traités: " + totalProcessed);
        Bukkit.getLogger().info("[SocialClass] Noms migrés: " + migratedCount);
        Bukkit.getLogger().info("[SocialClass] Erreurs: " + errorCount);
        Bukkit.getLogger().info("[SocialClass] ===============================================");
        
        if (migratedCount > 0) {
            Bukkit.getServer().broadcastMessage(
                ChatColor.AQUA + "🔄 Migration format: " + ChatColor.YELLOW + migratedCount + 
                ChatColor.AQUA + " villageois migrés vers le nouveau format {classe} [village]"
            );
        }
    }
    
    /**
     * Convertit un nom du format ancien [0][Village] vers nouveau {0} [Village]
     */
    private static String convertOldTagFormatToNew(String oldName) {
        if (oldName == null || oldName.trim().isEmpty()) {
            return oldName;
        }
        
        // Pattern pour détecter l'ancien format [chiffre][Village] Nom
        Pattern oldFormatPattern = Pattern.compile("^(§.)?\\[(\\d)\\](§.)?\\[([^\\]]+)\\](.*)$");
        Matcher matcher = oldFormatPattern.matcher(oldName);
        
        if (matcher.matches()) {
            String colorBefore = matcher.group(1) != null ? matcher.group(1) : "";
            String classNumber = matcher.group(2);
            String colorAfter = matcher.group(3) != null ? matcher.group(3) : "";
            String villageName = matcher.group(4);
            String rest = matcher.group(5) != null ? matcher.group(5) : "";
            
            // Obtenir la couleur appropriée pour la classe sociale
            SocialClass socialClass = SocialClass.fromLevel(Integer.parseInt(classNumber));
            String coloredTag = socialClass.getColoredTag();
            
            // Nouveau format: {classe} [village] nom
            return coloredTag + " [" + villageName + "]" + rest;
        }
        
        // Pas d'ancien format détecté, retourner tel quel
        return oldName;
    }
}