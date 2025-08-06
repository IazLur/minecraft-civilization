package TestJava.testjava.services;

import TestJava.testjava.models.VillagerHistoryModel;
import TestJava.testjava.models.VillageHistoryModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.enums.SocialClass;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.repositories.HistoryRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Villager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service pour gérer l'historique des villageois et des villages
 * Utilise un système de compression par templates pour éviter la répétition
 */
public class HistoryService {
    
    // Templates d'historique avec compression par ID
    private static final Map<String, String> VILLAGER_TEMPLATES = new HashMap<>();
    private static final Map<String, String> VILLAGE_TEMPLATES = new HashMap<>();
    
    static {
        // Templates pour villageois
        VILLAGER_TEMPLATES.put("BIRTH", "{0} {1} est né à {2}.");
        VILLAGER_TEMPLATES.put("FOOD", "{0} {1} a mangé {2}.");
        VILLAGER_TEMPLATES.put("SOCIAL_CLASS", "{0} {1} a changé de classe sociale pour la classe {2}.");
        VILLAGER_TEMPLATES.put("JOB", "{0} {1} a changé de métier pour {2}.");
        VILLAGER_TEMPLATES.put("PURCHASE", "{0} {1} a acheté {2} pour {3}µ à {4} {5}.");
        VILLAGER_TEMPLATES.put("FAMINE", "{0} {1} a subi de la famine.");
        VILLAGER_TEMPLATES.put("MOVEMENT_FAILED", "{0} {1} n'a pas réussi à se rendre au lieu indiqué.");
        
        // Templates pour villages
        VILLAGE_TEMPLATES.put("BIRTH", "{0} {1} est né ici.");
        VILLAGE_TEMPLATES.put("POPULATION", "Il y a actuellement {0}.");
        VILLAGE_TEMPLATES.put("TAX_COLLECTION", "La ville a récolté {0}µ d'impôts et sa richesse est désormais de {1}µ.");
        VILLAGE_TEMPLATES.put("DEATH", "{0} {1} est mort.");
    }
    
    /**
     * Enregistre la naissance d'un villageois
     */
    public static void recordVillagerBirth(VillagerModel villager, String villageName) {
        try {
            String[] names = extractNames(villager);
            String entry = formatEntry("BIRTH", names[0], names[1], villageName);
            
            Bukkit.getLogger().info("[History] Enregistrement naissance: " + entry);
            
            addVillagerHistoryEntry(villager.getId(), entry);
            addVillageHistoryEntry(villageName, formatVillageEntry("BIRTH", names[0], names[1]));
            
            Bukkit.getLogger().info("[History] ✅ Naissance enregistrée pour " + villager.getId());
        } catch (Exception e) {
            Bukkit.getLogger().severe("[History] ❌ Erreur enregistrement naissance: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Enregistre la consommation de nourriture
     */
    public static void recordFoodConsumption(VillagerModel villager, String foodName) {
        String[] names = extractNames(villager);
        String entry = formatEntry("FOOD", names[0], names[1], foodName);
        addVillagerHistoryEntry(villager.getId(), entry);
    }
    
    /**
     * Enregistre un changement de classe sociale
     */
    public static void recordSocialClassChange(VillagerModel villager, SocialClass newClass) {
        String[] names = extractNames(villager);
        String entry = formatEntry("SOCIAL_CLASS", names[0], names[1], newClass.getName());
        addVillagerHistoryEntry(villager.getId(), entry);
    }
    
    /**
     * Enregistre un changement de métier
     */
    public static void recordJobChange(VillagerModel villager, String jobName) {
        String[] names = extractNames(villager);
        String entry = formatEntry("JOB", names[0], names[1], jobName);
        addVillagerHistoryEntry(villager.getId(), entry);
    }
    
    /**
     * Enregistre un achat entre villageois
     */
    public static void recordPurchase(VillagerModel buyer, VillagerModel seller, String itemName, float cost) {
        String[] buyerNames = extractNames(buyer);
        String[] sellerNames = extractNames(seller);
        
        String entry = formatEntry("PURCHASE", buyerNames[0], buyerNames[1], itemName, 
                                 String.valueOf(cost), sellerNames[0], sellerNames[1]);
        addVillagerHistoryEntry(buyer.getId(), entry);
    }
    
    /**
     * Enregistre une famine
     */
    public static void recordFamine(VillagerModel villager) {
        String[] names = extractNames(villager);
        String entry = formatEntry("FAMINE", names[0], names[1]);
        addVillagerHistoryEntry(villager.getId(), entry);
    }
    
    /**
     * Enregistre un échec de déplacement
     */
    public static void recordMovementFailure(VillagerModel villager) {
        String[] names = extractNames(villager);
        String entry = formatEntry("MOVEMENT_FAILED", names[0], names[1]);
        addVillagerHistoryEntry(villager.getId(), entry);
    }
    
    /**
     * Enregistre la collecte d'impôts pour un village
     */
    public static void recordTaxCollection(String villageName, float taxAmount, float newWealth) {
        String entry = formatVillageEntry("TAX_COLLECTION", String.valueOf(taxAmount), String.valueOf(newWealth));
        addVillageHistoryEntry(villageName, entry);
    }
    
    /**
     * Enregistre la mort d'un villageois
     */
    public static void recordVillagerDeath(VillagerModel villager) {
        String[] names = extractNames(villager);
        String deathEntry = formatEntry("FAMINE", names[0], names[1]); // La mort est souvent due à la famine
        addVillagerHistoryEntry(villager.getId(), deathEntry);
        
        // Enregistre aussi dans l'historique du village
        String villageEntry = formatVillageEntry("DEATH", names[0], names[1]);
        addVillageHistoryEntry(villager.getVillageName(), villageEntry);
        
        // Archive le fichier du villageois mort
        HistoryRepository.archiveDeadVillager(villager.getId());
    }
    
    /**
     * Enregistre les statistiques de population d'un village
     */
    public static void recordPopulationStats(String villageName, Map<SocialClass, Integer> classCount) {
        StringBuilder stats = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<SocialClass, Integer> entry : classCount.entrySet()) {
            if (entry.getValue() > 0) {
                if (!first) stats.append(", ");
                stats.append(entry.getValue()).append(" classe ").append(entry.getKey().getName());
                first = false;
            }
        }
        
        if (stats.length() > 0) {
            String entry = formatVillageEntry("POPULATION", stats.toString());
            addVillageHistoryEntry(villageName, entry);
        }
    }
    
    /**
     * Ajoute une entrée à l'historique d'un villageois
     */
    private static void addVillagerHistoryEntry(UUID villagerId, String entry) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String timestampedEntry = "[" + timestamp + "] " + entry;
            HistoryRepository.addVillagerHistoryEntry(villagerId, timestampedEntry);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[History] Erreur lors de l'ajout d'entrée villageois: " + e.getMessage());
        }
    }
    
    /**
     * Ajoute une entrée à l'historique d'un village
     */
    private static void addVillageHistoryEntry(String villageName, String entry) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String timestampedEntry = "[" + timestamp + "] " + entry;
            HistoryRepository.addVillageHistoryEntry(villageName, timestampedEntry);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[History] Erreur lors de l'ajout d'entrée village: " + e.getMessage());
        }
    }
    
    /**
     * Formate une entrée d'historique de villageois
     */
    private static String formatEntry(String templateKey, String... args) {
        String template = VILLAGER_TEMPLATES.get(templateKey);
        if (template == null) {
            return "Entrée inconnue: " + templateKey;
        }
        
        // Remplace les placeholders {0}, {1}, etc.
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", args[i]);
        }
        
        return template;
    }
    
    /**
     * Formate une entrée d'historique de village
     */
    private static String formatVillageEntry(String templateKey, String... args) {
        String template = VILLAGE_TEMPLATES.get(templateKey);
        if (template == null) {
            return "Entrée inconnue: " + templateKey;
        }
        
        // Remplace les placeholders {0}, {1}, etc.
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", args[i]);
        }
        
        return template;
    }
    
    /**
     * Extrait le prénom et nom d'un villageois depuis son nom personnalisé
     */
    private static String[] extractNames(VillagerModel villager) {
        try {
            // Récupère l'entité Minecraft pour obtenir le nom personnalisé
            Villager entity = (Villager) Bukkit.getServer().getEntity(villager.getId());
            if (entity != null && entity.getCustomName() != null) {
                String customName = entity.getCustomName();
                // Format: {X} [VillageName] Prénom Nom
                String cleanName = customName.replaceAll("§[0-9a-fk-or]", ""); // Enlève les codes couleur
                
                // Cherche après le "]"
                int bracketEnd = cleanName.indexOf(']');
                if (bracketEnd != -1 && bracketEnd + 2 < cleanName.length()) {
                    String fullName = cleanName.substring(bracketEnd + 2); // +2 pour "] "
                    String[] parts = fullName.split(" ");
                    if (parts.length >= 2) {
                        return new String[]{parts[0], parts[1]}; // Prénom, Nom
                    }
                }
            }
            
            // Fallback: génère un nom aléatoire
            String generatedName = CustomName.generate();
            String[] parts = generatedName.split(" ");
            if (parts.length >= 2) {
                return new String[]{parts[0], parts[1]};
            } else {
                return new String[]{"Villageois", "Nouveau"};
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[History] Erreur extraction nom: " + e.getMessage());
            return new String[]{"Villageois", "Inconnu"};
        }
    }
    
    /**
     * Convertit un Material en nom français lisible
     */
    public static String getFoodDisplayName(Material foodType) {
        switch (foodType) {
            case WHEAT: return "blé";
            case BREAD: return "pain";
            case HAY_BLOCK: return "bloc de foin";
            case CARROT: return "carotte";
            case POTATO: return "pomme de terre";
            case BEETROOT: return "betterave";
            default: return foodType.name().toLowerCase();
        }
    }
}