package TestJava.testjava.models;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;

@Document(collection = "empires", schemaVersion = "1.0")
public class EmpireModel {
    @Id
    private String id;
    private String empireName;
    private Boolean isInWar;
    private String enemyName;
    private Integer totalAttackerCount;
    private Integer totalDefenderCount;

    private Float juridictionCount = 0.0f;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmpireName() {
        return empireName;
    }

    public void setEmpireName(String empireName) {
        this.empireName = empireName;
    }

    public Boolean getIsInWar() {
        return isInWar;
    }

    public void setIsInWar(Boolean isInWar) {
        this.isInWar = isInWar;
    }

    public String getEnemyName() {
        return enemyName;
    }

    public void setEnemyName(String enemyName) {
        this.enemyName = enemyName;
    }

    public Integer getTotalAttackerCount() {
        return totalAttackerCount;
    }

    public void setTotalAttackerCount(Integer totalAttackerCount) {
        this.totalAttackerCount = totalAttackerCount;
    }

    public Integer getTotalDefenderCount() {
        return totalDefenderCount;
    }

    public void setTotalDefenderCount(Integer totalDefenderCount) {
        this.totalDefenderCount = totalDefenderCount;
    }

    public float getJuridictionCount() {
        return Math.round(juridictionCount * 100.0f) / 100.0f;
    }

    public void setJuridictionCount(float juridictionCount) {
        this.juridictionCount = Math.round(juridictionCount * 100.0f) / 100.0f;
    }

}
