package TestJava.testjava.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration de distance pour un bloc de métier
 */
public class JobDistanceConfig {

    @JsonProperty("material")
    private String material;

    @JsonProperty("jobName")
    private String jobName;

    @JsonProperty("distanceMin")
    private int distanceMin;

    @JsonProperty("distanceMax")
    private int distanceMax;

    @JsonProperty("description")
    private String description;

    public JobDistanceConfig() {}

    public JobDistanceConfig(String material, String jobName, int distanceMin, int distanceMax, String description) {
        this.material = material;
        this.jobName = jobName;
        this.distanceMin = distanceMin;
        this.distanceMax = distanceMax;
        this.description = description;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
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

    @Override
    public String toString() {
        return "JobDistanceConfig{" +
                "material='" + material + '\'' +
                ", jobName='" + jobName + '\'' +
                ", distanceMin=" + distanceMin +
                ", distanceMax=" + distanceMax +
                ", description='" + description + '\'' +
                '}';
    }
}