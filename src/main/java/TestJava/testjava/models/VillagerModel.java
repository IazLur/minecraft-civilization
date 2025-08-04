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
}
