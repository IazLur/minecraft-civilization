package TestJava.testjava.classes;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.entity.LivingEntity;

public class CustomEntity {
    private LivingEntity entity;

    private CustomEntity() {
    }

    public CustomEntity(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public VillageModel getVillage() {
        return VillageRepository.get(CustomName.extractVillageName(this.entity.getCustomName()));
    }

    public void setVillage(VillageModel village) {
        String currentName = this.entity.getCustomName();
        if (currentName == null) {
            return;
        }
        
        try {
            // Détecte le format et remplace intelligemment
            String newName = replaceVillageNameInCustomName(currentName, village.getId());
            this.entity.setCustomName(newName);
        } catch (Exception e) {
            TestJava.plugin.getLogger().warning("Erreur lors du changement de village pour " + 
                currentName + ": " + e.getMessage());
        }
    }
    
    /**
     * Remplace intelligemment le nom du village dans un customName
     * en préservant les tags de classe sociale
     */
    private String replaceVillageNameInCustomName(String customName, String newVillageName) {
        // Supprime les codes de couleur pour l'analyse
        String cleanName = org.bukkit.ChatColor.stripColor(customName);
        
        // Trouve tous les éléments entre crochets avec leurs positions
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(.*?)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(cleanName);
        java.util.List<String> brackets = new java.util.ArrayList<>();
        java.util.List<Integer> startPositions = new java.util.ArrayList<>();
        java.util.List<Integer> endPositions = new java.util.ArrayList<>();
        
        while (matcher.find()) {
            brackets.add(matcher.group(1));
            startPositions.add(matcher.start());
            endPositions.add(matcher.end());
        }
        
        if (brackets.isEmpty()) {
            throw new IllegalArgumentException("Aucun élément entre crochets dans: " + customName);
        }
        
        int villageIndex = 0; // Par défaut, le premier élément
        
        // Si le premier élément est un tag de classe sociale, le village est au second
        if (brackets.size() > 1 && brackets.get(0).matches("^[0-4]$")) {
            villageIndex = 1;
        }
        
        if (villageIndex >= brackets.size()) {
            throw new IllegalArgumentException("Index village invalide dans: " + customName);
        }
        
        // Remplace le village à la position correcte dans le nom original (avec couleurs)
        int startPos = startPositions.get(villageIndex) + 1; // +1 pour passer le '['
        int endPos = endPositions.get(villageIndex) - 1;     // -1 pour exclure le ']'
        
        // Ajuste les positions pour le nom original avec couleurs
        String result = customName.substring(0, findRealPosition(customName, startPos)) + 
                       newVillageName + 
                       customName.substring(findRealPosition(customName, endPos));
        
        return result;
    }
    
    /**
     * Trouve la position réelle dans une chaîne avec codes couleur
     * basée sur une position dans la chaîne nettoyée
     */
    private int findRealPosition(String originalString, int cleanPosition) {
        String stripped = org.bukkit.ChatColor.stripColor(originalString);
        if (cleanPosition >= stripped.length()) {
            return originalString.length();
        }
        
        int realPos = 0;
        int strippedPos = 0;
        
        while (strippedPos < cleanPosition && realPos < originalString.length()) {
            if (originalString.charAt(realPos) == '§' && realPos + 1 < originalString.length()) {
                // Saute le code couleur (§ + caractère)
                realPos += 2;
            } else {
                // Caractère normal
                realPos++;
                strippedPos++;
            }
        }
        
        return realPos;
    }
}
