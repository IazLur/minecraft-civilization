package TestJava.testjava.repositories;

import TestJava.testjava.models.VillagerHistoryModel;
import TestJava.testjava.models.VillageHistoryModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repository pour gérer les fichiers d'historique JSON
 */
public class HistoryRepository {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String HISTORY_BASE_PATH = "plugins/TestJava/history/";
    private static final String VILLAGERS_PATH = HISTORY_BASE_PATH + "villagers/";
    private static final String VILLAGES_PATH = HISTORY_BASE_PATH + "villages/";
    private static final String DEAD_PATH = HISTORY_BASE_PATH + "villagers/dead/";
    
    static {
        // Créer les dossiers nécessaires
        createDirectories();
    }
    
    /**
     * Crée les dossiers nécessaires pour l'historique
     */
    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(VILLAGERS_PATH));
            Files.createDirectories(Paths.get(VILLAGES_PATH));
            Files.createDirectories(Paths.get(DEAD_PATH));
        } catch (IOException e) {
            Bukkit.getLogger().severe("[History] Erreur création dossiers: " + e.getMessage());
        }
    }
    
    /**
     * Ajoute une entrée à l'historique d'un villageois
     */
    public static void addVillagerHistoryEntry(UUID villagerId, String entry) {
        try {
            VillagerHistoryModel history = getVillagerHistory(villagerId);
            if (history == null) {
                // Créer un nouveau fichier d'historique
                history = new VillagerHistoryModel();
                history.setVillagerId(villagerId);
                
                // Essayer de remplir les informations si le villageois existe
                try {
                    VillagerModel villager = VillagerRepository.find(villagerId);
                    if (villager != null) {
                        history.setVillageName(villager.getVillageName());
                        
                        // Extraire le nom depuis l'entité si possible
                        Villager entity = (Villager) Bukkit.getServer().getEntity(villagerId);
                        if (entity != null && entity.getCustomName() != null) {
                            String customName = entity.getCustomName();
                            String cleanName = customName.replaceAll("§[0-9a-fk-or]", "");
                            int bracketEnd = cleanName.indexOf(']');
                            if (bracketEnd != -1 && bracketEnd + 2 < cleanName.length()) {
                                String fullName = cleanName.substring(bracketEnd + 2);
                                history.setVillagerName(fullName.trim());
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore silencieusement, les champs resteront null
                }
            }
            
            history.addHistoryEntry(entry);
            saveVillagerHistory(history);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[History] Erreur ajout entrée villageois " + villagerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Ajoute une entrée à l'historique d'un village
     */
    public static void addVillageHistoryEntry(String villageName, String entry) {
        try {
            VillageHistoryModel history = getVillageHistory(villageName);
            if (history == null) {
                // Créer un nouveau fichier d'historique
                history = new VillageHistoryModel();
                history.setVillageName(villageName);
            }
            
            history.addHistoryEntry(entry);
            saveVillageHistory(history);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[History] Erreur ajout entrée village " + villageName + ": " + e.getMessage());
        }
    }
    
    /**
     * Récupère l'historique d'un villageois
     */
    public static VillagerHistoryModel getVillagerHistory(UUID villagerId) {
        try {
            File file = new File(VILLAGERS_PATH + villagerId.toString() + ".json");
            if (!file.exists()) {
                return null;
            }
            
            return objectMapper.readValue(file, VillagerHistoryModel.class);
            
        } catch (IOException e) {
            Bukkit.getLogger().warning("[History] Erreur lecture historique villageois " + villagerId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Récupère l'historique d'un village
     */
    public static VillageHistoryModel getVillageHistory(String villageName) {
        try {
            File file = new File(VILLAGES_PATH + sanitizeFileName(villageName) + ".json");
            if (!file.exists()) {
                return null;
            }
            
            return objectMapper.readValue(file, VillageHistoryModel.class);
            
        } catch (IOException e) {
            Bukkit.getLogger().warning("[History] Erreur lecture historique village " + villageName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Sauvegarde l'historique d'un villageois
     */
    public static void saveVillagerHistory(VillagerHistoryModel history) {
        try {
            File file = new File(VILLAGERS_PATH + history.getVillagerId().toString() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, history);
            
        } catch (IOException e) {
            Bukkit.getLogger().severe("[History] Erreur sauvegarde historique villageois " + history.getVillagerId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Sauvegarde l'historique d'un village
     */
    public static void saveVillageHistory(VillageHistoryModel history) {
        try {
            File file = new File(VILLAGES_PATH + sanitizeFileName(history.getVillageName()) + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, history);
            
        } catch (IOException e) {
            Bukkit.getLogger().severe("[History] Erreur sauvegarde historique village " + history.getVillageName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Archive le fichier d'historique d'un villageois mort
     */
    public static void archiveDeadVillager(UUID villagerId) {
        try {
            File sourceFile = new File(VILLAGERS_PATH + villagerId.toString() + ".json");
            if (!sourceFile.exists()) {
                return; // Pas d'historique à archiver
            }
            
            // Charger l'historique existant
            VillagerHistoryModel history = objectMapper.readValue(sourceFile, VillagerHistoryModel.class);
            history.setDead(true);
            
            // Sauvegarder dans le dossier dead
            File deadFile = new File(DEAD_PATH + villagerId.toString() + "_dead.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(deadFile, history);
            
            // Supprimer le fichier original
            if (sourceFile.delete()) {
                Bukkit.getLogger().info("[History] Historique villageois " + villagerId + " archivé dans dead/");
            } else {
                Bukkit.getLogger().warning("[History] Impossible de supprimer le fichier original pour " + villagerId);
            }
            
        } catch (IOException e) {
            Bukkit.getLogger().severe("[History] Erreur archivage villageois mort " + villagerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Gère le renommage d'un village (met à jour le fichier d'historique)
     */
    public static void handleVillageRename(String oldName, String newName) {
        try {
            File oldFile = new File(VILLAGES_PATH + sanitizeFileName(oldName) + ".json");
            if (!oldFile.exists()) {
                return; // Pas d'historique existant
            }
            
            // Charger l'historique existant
            VillageHistoryModel history = objectMapper.readValue(oldFile, VillageHistoryModel.class);
            history.setVillageName(newName);
            
            // Ajouter une entrée pour le renommage
            history.addHistoryEntry("Le village a été renommé de " + oldName + " à " + newName + ".");
            
            // Sauvegarder avec le nouveau nom
            File newFile = new File(VILLAGES_PATH + sanitizeFileName(newName) + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(newFile, history);
            
            // Supprimer l'ancien fichier
            if (oldFile.delete()) {
                Bukkit.getLogger().info("[History] Historique village renommé: " + oldName + " → " + newName);
            } else {
                Bukkit.getLogger().warning("[History] Impossible de supprimer l'ancien fichier pour " + oldName);
            }
            
        } catch (IOException e) {
            Bukkit.getLogger().severe("[History] Erreur renommage village " + oldName + " → " + newName + ": " + e.getMessage());
        }
    }
    
    /**
     * Récupère la liste complète de l'historique d'un villageois (plus récent en premier)
     */
    public static List<String> getVillagerHistoryList(UUID villagerId) {
        VillagerHistoryModel history = getVillagerHistory(villagerId);
        if (history == null || history.getHistoryList() == null) {
            return new ArrayList<>();
        }
        
        List<String> historyList = new ArrayList<>(history.getHistoryList());
        // Inverser pour avoir le plus récent en premier
        List<String> reversedList = new ArrayList<>();
        for (int i = historyList.size() - 1; i >= 0; i--) {
            reversedList.add(historyList.get(i));
        }
        
        return reversedList;
    }
    
    /**
     * Récupère la liste complète de l'historique d'un village (plus récent en premier)
     */
    public static List<String> getVillageHistoryList(String villageName) {
        VillageHistoryModel history = getVillageHistory(villageName);
        if (history == null || history.getHistoryList() == null) {
            return new ArrayList<>();
        }
        
        List<String> historyList = new ArrayList<>(history.getHistoryList());
        // Inverser pour avoir le plus récent en premier
        List<String> reversedList = new ArrayList<>();
        for (int i = historyList.size() - 1; i >= 0; i--) {
            reversedList.add(historyList.get(i));
        }
        
        return reversedList;
    }
    
    /**
     * Nettoie le nom de fichier pour éviter les caractères invalides
     */
    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}