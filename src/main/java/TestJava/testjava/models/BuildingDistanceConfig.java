package TestJava.testjava.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration de distance pour un bâtiment custom
 */
public class BuildingDistanceConfig {

    @JsonProperty("buildingType")
    private String buildingType;

    @JsonProperty("distanceMin")
    private int distanceMin;

    @JsonProperty("distanceMax")
    private int distanceMax;

    @JsonProperty("description")
    private String description;

    @JsonProperty("costToBuild")
    private int costToBuild;

    @JsonProperty("costPerDay")
    private int costPerDay;

    @JsonProperty("costPerUpgrade")
    private int costPerUpgrade;

    @JsonProperty("costUpgradeMultiplier")
    private float costUpgradeMultiplier;

    @JsonProperty("nombreEmployesMax")
    private int nombreEmployesMax;

    @JsonProperty("salaireEmploye")
    private int salaireEmploye;

    @JsonProperty("tauxTaxeEmploye")
    private float tauxTaxeEmploye;

    public BuildingDistanceConfig() {}

    public BuildingDistanceConfig(String buildingType, int distanceMin, int distanceMax, String description,
                                  int costToBuild, int costPerDay, int costPerUpgrade, float costUpgradeMultiplier,
                                  int nombreEmployesMax, int salaireEmploye, float tauxTaxeEmploye) {
        this.buildingType = buildingType;
        this.distanceMin = distanceMin;
        this.distanceMax = distanceMax;
        this.description = description;
        this.costToBuild = costToBuild;
        this.costPerDay = costPerDay;
        this.costPerUpgrade = costPerUpgrade;
        this.costUpgradeMultiplier = costUpgradeMultiplier;
        this.nombreEmployesMax = nombreEmployesMax;
        this.salaireEmploye = salaireEmploye;
        this.tauxTaxeEmploye = tauxTaxeEmploye;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(String buildingType) {
        this.buildingType = buildingType;
    }

    public int getDistanceMin() {
        return distanceMin;
    }

    public void setDistanceMin(int distanceMin) {
        this.distanceMin = distanceMin;
    }

    public int getDistanceMax() {
        return distanceMax;
    }

    public void setDistanceMax(int distanceMax) {
        this.distanceMax = distanceMax;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCostToBuild() {
        return costToBuild;
    }

    public void setCostToBuild(int costToBuild) {
        this.costToBuild = costToBuild;
    }

    public int getCostPerDay() {
        return costPerDay;
    }

    public void setCostPerDay(int costPerDay) {
        this.costPerDay = costPerDay;
    }

    public int getCostPerUpgrade() {
        return costPerUpgrade;
    }

    public void setCostPerUpgrade(int costPerUpgrade) {
        this.costPerUpgrade = costPerUpgrade;
    }

    public float getCostUpgradeMultiplier() {
        return costUpgradeMultiplier;
    }

    public void setCostUpgradeMultiplier(float costUpgradeMultiplier) {
        this.costUpgradeMultiplier = costUpgradeMultiplier;
    }

    public int getNombreEmployesMax() {
        return nombreEmployesMax;
    }

    public void setNombreEmployesMax(int nombreEmployesMax) {
        this.nombreEmployesMax = nombreEmployesMax;
    }

    public int getSalaireEmploye() {
        return salaireEmploye;
    }

    public void setSalaireEmploye(int salaireEmploye) {
        this.salaireEmploye = salaireEmploye;
    }

    public float getTauxTaxeEmploye() {
        return tauxTaxeEmploye;
    }

    public void setTauxTaxeEmploye(float tauxTaxeEmploye) {
        this.tauxTaxeEmploye = tauxTaxeEmploye;
    }

    @Override
    public String toString() {
        return "BuildingDistanceConfig{" +
                "buildingType='" + buildingType + '\'' +
                ", distanceMin=" + distanceMin +
                ", distanceMax=" + distanceMax +
                ", description='" + description + '\'' +
                ", costToBuild=" + costToBuild +
                ", costPerDay=" + costPerDay +
                ", costPerUpgrade=" + costPerUpgrade +
                ", costUpgradeMultiplier=" + costUpgradeMultiplier +
                ", nombreEmployesMax=" + nombreEmployesMax +
                ", salaireEmploye=" + salaireEmploye +
                ", tauxTaxeEmploye=" + tauxTaxeEmploye +
                '}';
    }
}