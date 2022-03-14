package TestJava.testjava.models;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;

import java.util.UUID;

@Document(collection = "war_blocks", schemaVersion = "1.0")
public class WarBlockModel {
    @Id
    private UUID id;
    private String village;
    private double x;
    private double y;
    private double z;
    private String type;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
