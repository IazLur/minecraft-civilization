package TestJava.testjava.helpers;

import TestJava.testjava.models.ResourceModel;
import TestJava.testjava.repositories.ResourceRepository;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.stream.Collectors;

public class ResourceHelper {
    
    /**
     * Résultat de recherche de ressource avec suggestions
     */
    public static class ResourceSearchResult {
        private final ResourceModel resource;
        private final List<String> suggestions;
        private final boolean found;
        
        public ResourceSearchResult(ResourceModel resource) {
            this.resource = resource;
            this.suggestions = Collections.emptyList();
            this.found = true;
        }
        
        public ResourceSearchResult(List<String> suggestions) {
            this.resource = null;
            this.suggestions = suggestions;
            this.found = false;
        }
        
        public boolean isFound() { return found; }
        public ResourceModel getResource() { return resource; }
        public List<String> getSuggestions() { return suggestions; }
    }
    
    /**
     * Recherche STRICTE de ressource avec suggestions intelligentes
     * SÉCURITÉ : Seules les correspondances EXACTES permettent l'exécution
     */
    public static ResourceSearchResult findResourceWithSuggestions(String searchTerm) {
        Collection<ResourceModel> resources = ResourceRepository.getAll();
        String search = searchTerm.toLowerCase();
        
        // 1. Recherche EXACTE UNIQUEMENT (insensible à la casse)
        // SEULEMENT cette recherche permet l'exécution de la commande
        for (ResourceModel resource : resources) {
            if (resource.getName().toLowerCase().equals(search)) {
                return new ResourceSearchResult(resource);
            }
        }
        
        // 2. AUCUNE correspondance exacte trouvée
        // → Générer des suggestions SANS exécuter la commande
        List<String> suggestions = generateSmartSuggestions(search, resources);
        return new ResourceSearchResult(suggestions);
    }
    
    /**
     * Génère des suggestions intelligentes combinant contains() et Levenshtein
     */
    private static List<String> generateSmartSuggestions(String searchTerm, Collection<ResourceModel> resources) {
        List<String> containsMatches = new ArrayList<>();
        Map<String, Integer> distances = new HashMap<>();
        
        for (ResourceModel resource : resources) {
            String name = resource.getName().toLowerCase();
            
            // Priorité 1: Correspondances partielles (contains)
            if (name.contains(searchTerm)) {
                containsMatches.add(resource.getName());
            }
            
            // Priorité 2: Distance de Levenshtein pour tous
            int distance = levenshteinDistance(searchTerm, name);
            distances.put(resource.getName(), distance);
        }
        
        // Combiner : d'abord les contains, puis les plus proches par distance
        List<String> result = new ArrayList<>(containsMatches);
        
        // Ajouter les suggestions par distance (éviter les doublons)
        distances.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .map(Map.Entry::getKey)
                .filter(name -> !result.contains(name))
                .forEach(result::add);
        
        // Limiter à 5 suggestions max
        return result.stream().limit(5).collect(Collectors.toList());
    }
    
    /**
     * Calcule la distance de Levenshtein entre deux chaînes
     */
    private static int levenshteinDistance(String a, String b) {
        if (a.length() == 0) return b.length();
        if (b.length() == 0) return a.length();
        
        int[][] matrix = new int[a.length() + 1][b.length() + 1];
        
        for (int i = 0; i <= a.length(); i++) {
            matrix[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            matrix[0][j] = j;
        }
        
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                matrix[i][j] = Math.min(Math.min(
                        matrix[i - 1][j] + 1,      // suppression
                        matrix[i][j - 1] + 1),     // insertion
                        matrix[i - 1][j - 1] + cost); // substitution
            }
        }
        
        return matrix[a.length()][b.length()];
    }
    
    /**
     * Formate un message d'erreur avec suggestions
     */
    public static String formatResourceNotFoundMessage(String searchTerm, List<String> suggestions) {
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.RED).append("Impossible de trouver la ressource '")
               .append(searchTerm).append("'");
        
        if (!suggestions.isEmpty()) {
            message.append("\n").append(ChatColor.YELLOW).append("Ressources similaires : ")
                   .append(ChatColor.WHITE).append(String.join(", ", suggestions));
        }
        
        return message.toString();
    }
    

    
    /**
     * Affiche toutes les ressources disponibles
     */
    public static String formatAllResourcesList() {
        Collection<ResourceModel> resources = ResourceRepository.getAll();
        List<String> names = resources.stream()
                .map(ResourceModel::getName)
                .sorted()
                .collect(Collectors.toList());
        
        return ChatColor.YELLOW + "Ressources disponibles : " + 
               ChatColor.WHITE + String.join(", ", names);
    }
}