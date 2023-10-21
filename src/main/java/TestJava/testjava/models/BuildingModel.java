package TestJava.testjava.models;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;

import java.util.UUID;

@Document(collection = "buildings", schemaVersion = "1.0")
public class BuildingModel {

    @Id
    private UUID id;

    private String villageName;

    private String buildingType;

    private int level;
    private int costToBuild;
    private int costPerDay;
    private int costPerUpgrade;
    private float costUpgradeMultiplier;
    private int x;
    private int y;
    private int z;

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

    public String getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(String buildingType) {
        this.buildingType = buildingType;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
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

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }
}
