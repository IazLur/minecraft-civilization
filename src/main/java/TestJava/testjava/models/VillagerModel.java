package TestJava.testjava.models;

import TestJava.testjava.enums.SocialClass;
import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;

import java.util.UUID;

@Document(collection = "villagers", schemaVersion = "1.0")
public class VillagerModel {

    @Id
    private UUID id;
    private String villageName;
    private Integer food;
    private boolean isEating = false;
    private Integer socialClass = 0; // 0 = Misérable par défaut
    private Float richesse = 0.0f; // Richesse personnelle en juridictions
    private Integer education = 0; // Niveau d'éducation (0-8)
    
    // Gestion des métiers
    private String currentJobType; // "native" ou "custom" ou null
    private String currentJobName; // Nom du métier (pour custom) ou null pour natif
    private UUID currentBuildingId; // ID du bâtiment custom où il travaille (null pour métier natif)
    private boolean hasLeatherArmor = false; // Indicateur si le villageois porte une armure de cuir (métier custom)

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }

    public Integer getFood() {
        return food;
    }

    public void setFood(Integer food) {
        this.food = food;
    }

    public boolean isEating() {
        return isEating;
    }

    public void setEating(boolean eating) {
        isEating = eating;
    }

    public Integer getSocialClass() {
        return socialClass;
    }

    public void setSocialClass(Integer socialClass) {
        this.socialClass = socialClass;
    }

    /**
     * Retourne l'enum SocialClass correspondant au niveau actuel
     */
    public SocialClass getSocialClassEnum() {
        return SocialClass.fromLevel(this.socialClass != null ? this.socialClass : 0);
    }

    /**
     * Définit la classe sociale via l'enum
     */
    public void setSocialClassEnum(SocialClass socialClass) {
        this.socialClass = socialClass.getLevel();
    }

    public Float getRichesse() {
        return richesse != null ? Math.round(richesse * 100.0f) / 100.0f : 0.0f;
    }

    public void setRichesse(Float richesse) {
        this.richesse = richesse != null ? Math.round(richesse * 100.0f) / 100.0f : 0.0f;
    }

    public Integer getEducation() {
        return education != null ? education : 0;
    }

    public void setEducation(Integer education) {
        if (education == null) education = 0;
        this.education = Math.max(0, Math.min(8, education));
    }

    // ========== Getters/Setters pour les métiers ==========
    
    public String getCurrentJobType() {
        return currentJobType;
    }

    public void setCurrentJobType(String currentJobType) {
        this.currentJobType = currentJobType;
    }

    public String getCurrentJobName() {
        return currentJobName;
    }

    public void setCurrentJobName(String currentJobName) {
        this.currentJobName = currentJobName;
    }

    public UUID getCurrentBuildingId() {
        return currentBuildingId;
    }

    public void setCurrentBuildingId(UUID currentBuildingId) {
        this.currentBuildingId = currentBuildingId;
    }

    public boolean hasLeatherArmor() {
        return hasLeatherArmor;
    }

    public void setHasLeatherArmor(boolean hasLeatherArmor) {
        this.hasLeatherArmor = hasLeatherArmor;
    }

    // ========== Méthodes utilitaires pour les métiers ==========
    
    /**
     * Vérifie si le villageois a un métier (natif ou custom)
     */
    public boolean hasJob() {
        return currentJobType != null && !currentJobType.isEmpty();
    }

    /**
     * Vérifie si le villageois a un métier natif
     */
    public boolean hasNativeJob() {
        return "native".equals(currentJobType);
    }

    /**
     * Vérifie si le villageois a un métier custom
     */
    public boolean hasCustomJob() {
        return "custom".equals(currentJobType);
    }

    /**
     * Remet à zéro toutes les informations de métier
     */
    public void clearJob() {
        this.currentJobType = null;
        this.currentJobName = null;
        this.currentBuildingId = null;
        this.hasLeatherArmor = false;
    }

    /**
     * Assigne un métier natif au villageois
     */
    public void assignNativeJob() {
        this.currentJobType = "native";
        this.currentJobName = null; // Pour les métiers natifs, le nom est déterminé par la profession Minecraft
        this.currentBuildingId = null;
        this.hasLeatherArmor = false;
    }

    /**
     * Assigne un métier custom au villageois
     */
    public void assignCustomJob(String jobName, UUID buildingId) {
        this.currentJobType = "custom";
        this.currentJobName = jobName;
        this.currentBuildingId = buildingId;
        this.hasLeatherArmor = true; // Les métiers custom portent une armure de cuir
    }
}
