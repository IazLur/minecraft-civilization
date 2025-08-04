package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.BuildingDistanceConfig;
import TestJava.testjava.models.JobDistanceConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service pour charger et gérer les configurations de distance des métiers et bâtiments
 */
public class DistanceConfigService {

    private static Map<Material, JobDistanceConfig> jobConfigs = new HashMap<>();
    private static Map<String, BuildingDistanceConfig> buildingConfigs = new HashMap<>();
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Charge toutes les configurations depuis les fichiers JSON
     */
    public static void loadAllConfigurations() {
        loadJobConfigurations();
        loadBuildingConfigurations();
    }

    /**
     * Charge les configurations des métiers depuis metiers.json
     */
    public static void loadJobConfigurations() {
        try (InputStream inputStream = TestJava.class.getResourceAsStream("/metiers.json")) {
            if (inputStream == null) {
                Bukkit.getLogger().warning("[DistanceConfig] Fichier metiers.json introuvable dans les resources");
                return;
            }

            String json;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                json = reader.lines().collect(Collectors.joining("\n"));
            }

            List<JobDistanceConfig> configs = objectMapper.readValue(json, new TypeReference<List<JobDistanceConfig>>() {});
            
            jobConfigs.clear();
            int loaded = 0;
            int errors = 0;

            for (JobDistanceConfig config : configs) {
                try {
                    Material material = Material.valueOf(config.getMaterial().toUpperCase());
                    jobConfigs.put(material, config);
                    loaded++;
                    Bukkit.getLogger().info("[DistanceConfig] Métier chargé: " + config.getJobName() + 
                                          " (" + material + ") - Distance: " + config.getDistanceMin() + 
                                          "-" + config.getDistanceMax() + " blocs");
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[DistanceConfig] ⚠️ Matériau invalide: " + config.getMaterial() + 
                                             " pour le métier " + config.getJobName());
                    errors++;
                }
            }

            Bukkit.getLogger().info("[DistanceConfig] ✅ " + loaded + " configurations de métiers chargées" + 
                                  (errors > 0 ? " (" + errors + " erreurs)" : ""));

        } catch (IOException e) {
            Bukkit.getLogger().severe("[DistanceConfig] ❌ Erreur lors du chargement de metiers.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Charge les configurations des bâtiments depuis metiers_custom.json
     */
    public static void loadBuildingConfigurations() {
        try (InputStream inputStream = TestJava.class.getResourceAsStream("/metiers_custom.json")) {
            if (inputStream == null) {
                Bukkit.getLogger().warning("[DistanceConfig] Fichier metiers_custom.json introuvable dans les resources");
                return;
            }

            String json;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                json = reader.lines().collect(Collectors.joining("\n"));
            }

            List<BuildingDistanceConfig> configs = objectMapper.readValue(json, new TypeReference<List<BuildingDistanceConfig>>() {});
            
            buildingConfigs.clear();
            
            for (BuildingDistanceConfig config : configs) {
                buildingConfigs.put(config.getBuildingType().toLowerCase(), config);
                Bukkit.getLogger().info("[DistanceConfig] Bâtiment chargé: " + config.getBuildingType() + 
                                      " - Distance: " + config.getDistanceMin() + "-" + config.getDistanceMax() + 
                                      " blocs, Coût: " + config.getCostToBuild() + "µ");
            }

            Bukkit.getLogger().info("[DistanceConfig] ✅ " + configs.size() + " configurations de bâtiments chargées");

        } catch (IOException e) {
            Bukkit.getLogger().severe("[DistanceConfig] ❌ Erreur lors du chargement de metiers_custom.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtient la configuration d'un métier par matériau
     */
    public static JobDistanceConfig getJobConfig(Material material) {
        return jobConfigs.get(material);
    }

    /**
     * Obtient la configuration d'un bâtiment par type
     */
    public static BuildingDistanceConfig getBuildingConfig(String buildingType) {
        return buildingConfigs.get(buildingType.toLowerCase());
    }

    /**
     * Vérifie si un matériau est un bloc de métier configuré
     */
    public static boolean isJobBlock(Material material) {
        return jobConfigs.containsKey(material);
    }

    /**
     * Vérifie si un type de bâtiment est configuré
     */
    public static boolean isBuildingConfigured(String buildingType) {
        return buildingConfigs.containsKey(buildingType.toLowerCase());
    }

    /**
     * Retourne tous les métiers configurés
     */
    public static Map<Material, JobDistanceConfig> getAllJobConfigs() {
        return new HashMap<>(jobConfigs);
    }

    /**
     * Retourne tous les bâtiments configurés
     */
    public static Map<String, BuildingDistanceConfig> getAllBuildingConfigs() {
        return new HashMap<>(buildingConfigs);
    }

    /**
     * Obtient la liste des matériaux de métiers configurés
     */
    public static List<Material> getConfiguredJobMaterials() {
        return new ArrayList<>(jobConfigs.keySet());
    }

    /**
     * Obtient la liste des types de bâtiments configurés
     */
    public static List<String> getConfiguredBuildingTypes() {
        return new ArrayList<>(buildingConfigs.keySet());
    }

    /**
     * Recharge toutes les configurations
     */
    public static void reloadConfigurations() {
        Bukkit.getLogger().info("[DistanceConfig] 🔄 Rechargement des configurations...");
        loadAllConfigurations();
        Bukkit.getLogger().info("[DistanceConfig] ✅ Rechargement terminé");
    }
}