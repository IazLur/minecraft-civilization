package TestJava.testjava.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle pour l'historique d'un village
 */
public class VillageHistoryModel {
    
    private String villageName;
    private List<String> historyList;
    
    public VillageHistoryModel() {
        this.historyList = new ArrayList<>();
    }
    
    public VillageHistoryModel(String villageName) {
        this();
        this.villageName = villageName;
    }
    
    // Getters et Setters
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
    
    /**
     * Ajoute une entrée à l'historique
     */
    public void addHistoryEntry(String entry) {
        if (entry != null && !entry.trim().isEmpty()) {
            this.historyList.add(entry);
        }
    }
}