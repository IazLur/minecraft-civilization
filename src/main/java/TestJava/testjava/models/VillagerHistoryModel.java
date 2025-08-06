package TestJava.testjava.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modèle pour l'historique d'un villageois
 */
public class VillagerHistoryModel {
    
    private UUID villagerId;
    private String villagerName;
    private String villageName;
    private List<String> historyList;
    private boolean isDead;
    
    public VillagerHistoryModel() {
        this.historyList = new ArrayList<>();
        this.isDead = false;
    }
    
    public VillagerHistoryModel(UUID villagerId, String villagerName, String villageName) {
        this();
        this.villagerId = villagerId;
        this.villagerName = villagerName;
        this.villageName = villageName;
    }
    
    // Getters et Setters
    public UUID getVillagerId() {
        return villagerId;
    }
    
    public void setVillagerId(UUID villagerId) {
        this.villagerId = villagerId;
    }
    
    public String getVillagerName() {
        return villagerName;
    }
    
    public void setVillagerName(String villagerName) {
        this.villagerName = villagerName;
    }
    
    public String getVillageName() {
        return villageName;
    }
    
    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }
    
    public List<String> getHistoryList() {
        return historyList;
    }
    
    public void setHistoryList(List<String> historyList) {
        this.historyList = historyList;
    }
    
    public boolean isDead() {
        return isDead;
    }
    
    public void setDead(boolean dead) {
        isDead = dead;
    }
    
    /**
     * Ajoute une entrée à l'historique
     */
    public void addHistoryEntry(String entry) {
        if (entry != null && !entry.trim().isEmpty()) {
            this.historyList.add(entry);
        }
    }
}