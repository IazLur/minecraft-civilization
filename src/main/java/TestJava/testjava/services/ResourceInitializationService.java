package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.ResourceModel;
import TestJava.testjava.repositories.ResourceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.Bukkit;

import java.io.*;
import java.util.Collection;

public class ResourceInitializationService {
    
    private static final String RESOURCES_FILE = "resources.json";
    
    /**
     * Initialise les ressources depuis resources.json si la collection est vide
     */
    public static void initializeResourcesIfEmpty() {
        try {
            // Vérifier si des ressources existent déjà
            Collection<ResourceModel> existingResources = ResourceRepository.getAll();
            if (!existingResources.isEmpty()) {
                Bukkit.getLogger().info("[TestJava] Ressources déjà chargées (" + existingResources.size() + " ressources)");
                return;
            }
            
            Bukkit.getLogger().info("[TestJava] Chargement des ressources depuis " + RESOURCES_FILE + "...");
            
            // Charger et parser le fichier resources.json
            InputStream resourceStream = TestJava.class.getResourceAsStream("/" + RESOURCES_FILE);
            if (resourceStream == null) {
                Bukkit.getLogger().severe("[TestJava] Fichier " + RESOURCES_FILE + " introuvable dans les ressources!");
                return;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));
            ObjectMapper mapper = new ObjectMapper();
            
            String line;
            int loadedCount = 0;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Ignorer la première ligne qui contient la version du schéma
                if (isFirstLine && line.contains("schemaVersion")) {
                    isFirstLine = false;
                    continue;
                }
                
                try {
                    // Parser la ligne JSON en ResourceModel
                    JsonNode jsonNode = mapper.readTree(line);
                    
                    ResourceModel resource = new ResourceModel();
                    resource.setId(jsonNode.get("id").asText());
                    resource.setName(jsonNode.get("name").asText());
                    resource.setQuantity(jsonNode.get("quantity").asInt());
                    
                    // Sauvegarder dans la base de données
                    ResourceRepository.update(resource);
                    loadedCount++;
                    
                    Bukkit.getLogger().info("[TestJava] Ressource chargée: " + resource.getName() + " (quantité: " + resource.getQuantity() + ")");
                    
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[TestJava] Erreur lors du parsing de la ligne: " + line);
                    e.printStackTrace();
                }
            }
            
            reader.close();
            
            Bukkit.getLogger().info("[TestJava] ✅ " + loadedCount + " ressources chargées avec succès!");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[TestJava] Erreur critique lors du chargement des ressources:");
            e.printStackTrace();
        }
    }
    
    /**
     * Force le rechargement des ressources (pour debug)
     */
    public static void forceReloadResources() {
        Bukkit.getLogger().info("[TestJava] Force reload des ressources...");
        
        // Supprimer toutes les ressources existantes
        Collection<ResourceModel> existingResources = ResourceRepository.getAll();
        for (ResourceModel resource : existingResources) {
            ResourceRepository.remove(resource);
        }
        
        // Recharger
        initializeResourcesIfEmpty();
    }
    
    /**
     * Affiche le statut des ressources (pour debug)
     */
    public static void debugResourcesStatus() {
        Collection<ResourceModel> resources = ResourceRepository.getAll();
        Bukkit.getLogger().info("[TestJava] DEBUG - Nombre de ressources en base: " + resources.size());
        
        for (ResourceModel resource : resources) {
            Bukkit.getLogger().info("[TestJava] DEBUG - Ressource: " + resource.getName() + " (ID: " + resource.getId() + ", Quantité: " + resource.getQuantity() + ")");
        }
    }
}