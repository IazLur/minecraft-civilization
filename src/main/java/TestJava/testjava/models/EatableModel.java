package TestJava.testjava.models;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;
import org.bukkit.Location;

@Document(collection = "eatables", schemaVersion = "1.0")
public class EatableModel {
    @Id
    private Location id;
    private String village;

    public Location getId() {
        return id;
    }

    public void setId(Location id) {
        this.id = id;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }
}
