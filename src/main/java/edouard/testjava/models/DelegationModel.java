package edouard.testjava.models;

import io.jsondb.annotation.Document;
import io.jsondb.annotation.Id;

import java.util.UUID;

@Document(collection = "delegations", schemaVersion = "1.0")
public class DelegationModel {
    @Id
    private String id;
    private UUID uniq;
    private String materialToSend;
    private String materialToReceive;
    private Integer sendNumber;
    private Integer receiveNumber;
    private String targetVillage;
    private Integer status; // 0 en cours, 1 status quo, 2 accepté, 3 refusé

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaterialToSend() {
        return materialToSend;
    }

    public void setMaterialToSend(String materialToSend) {
        this.materialToSend = materialToSend;
    }

    public String getMaterialToReceive() {
        return materialToReceive;
    }

    public void setMaterialToReceive(String materialToReceive) {
        this.materialToReceive = materialToReceive;
    }

    public Integer getSendNumber() {
        return sendNumber;
    }

    public void setSendNumber(Integer sendNumber) {
        this.sendNumber = sendNumber;
    }

    public Integer getReceiveNumber() {
        return receiveNumber;
    }

    public void setReceiveNumber(Integer receiveNumber) {
        this.receiveNumber = receiveNumber;
    }

    public String getTargetVillage() {
        return targetVillage;
    }

    public void setTargetVillage(String to) {
        this.targetVillage = to;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public UUID getUniq() {
        return uniq;
    }

    public void setUniq(UUID uniq) {
        this.uniq = uniq;
    }
}