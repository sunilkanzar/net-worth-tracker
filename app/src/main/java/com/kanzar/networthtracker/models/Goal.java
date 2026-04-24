package com.kanzar.networthtracker.models;

import java.util.UUID;
import com.kanzar.networthtracker.helpers.Tools;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Goal extends RealmObject {

    @PrimaryKey
    private String id;
    private double targetValue;
    private int targetYear;
    private long createdAt;
    private long updatedAt;

    public Goal() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Tools.getUnixTime();
        this.updatedAt = this.createdAt;
    }

    public Goal(int targetYear, double targetValue) {
        this();
        this.targetYear = targetYear;
        this.targetValue = targetValue;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getTargetValue() { return targetValue; }
    public void setTargetValue(double targetValue) { this.targetValue = targetValue; }

    public int getTargetYear() { return targetYear; }
    public void setTargetYear(int targetYear) { this.targetYear = targetYear; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "GOAL," + targetYear + "," + targetValue + "\n";
    }

    public static Goal fromString(String row) {
        try {
            if (!row.startsWith("GOAL,")) return null;
            String[] parts = row.split(",");
            if (parts.length != 3) return null;

            int year = Integer.parseInt(parts[1].trim());
            double value = Double.parseDouble(parts[2].trim());

            return new Goal(year, value);
        } catch (Exception e) {
            return null;
        }
    }

    public void updateTimestamp() {
        this.updatedAt = Tools.getUnixTime();
    }
}
