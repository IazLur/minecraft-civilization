package TestJava.testjava.models;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;

@Document(collection = "villages", schemaVersion = "1.0")
public class VillageModel {

    @Id
    private String id;
    private String playerName;
    private Integer bedsCount;
    private Integer population = 0;
    private Integer garrison = 0;
    private Integer groundArmy = 0;
    private double x;
    private double y;
    private double z;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String name) {
        this.playerName = name;
    }

    public Integer getBedsCount() {
        return bedsCount;
    }

    public void setBedsCount(Integer bedsCount) {
        this.bedsCount = bedsCount;
    }

    public Integer getPopulation() {
        return population;
    }

    public void setPopulation(Integer population) {
        this.population = population;
    }

    public Integer getGarrison() {
        return garrison;
    }

    public void setGarrison(Integer garrison) {
        this.garrison = garrison;
    }

    public Integer getGroundArmy() {
        return groundArmy;
    }

    public void setGroundArmy(Integer groundArmy) {
        this.groundArmy = groundArmy;
    }
}
