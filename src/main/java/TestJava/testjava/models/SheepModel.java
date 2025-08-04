package TestJava.testjava.models;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;

import java.util.UUID;

@Document(collection = "sheep", schemaVersion = "1.0")
public class SheepModel {

    @Id
    private UUID id;

    private UUID buildingId;
    private String villageName;
    private int sheepNumber;
    private int x;
    private int y;
    private int z;
    private String worldName;

    public SheepModel() {}

    public SheepModel(UUID entityId, UUID buildingId, String villageName, int sheepNumber, int x, int y, int z, String worldName) {
        this.id = entityId;
        this.buildingId = buildingId;
        this.villageName = villageName;
        this.sheepNumber = sheepNumber;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(UUID buildingId) {
        this.buildingId = buildingId;
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }

    public int getSheepNumber() {
        return sheepNumber;
    }

    public void setSheepNumber(int sheepNumber) {
        this.sheepNumber = sheepNumber;
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

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
}