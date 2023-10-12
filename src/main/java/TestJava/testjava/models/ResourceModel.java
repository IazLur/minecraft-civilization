package TestJava.testjava.models;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;

@Document(collection = "resources", schemaVersion = "1.0")
public class ResourceModel {
    @Id
    private String id;

    private String name;

    private int quantity;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
