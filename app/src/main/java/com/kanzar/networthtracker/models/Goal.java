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
    private int targetMonth;
    private long createdAt;
    private long updatedAt;

    public Goal() {
        this.id = UUID.randomUUID().toString();
        this.targetMonth = 12;
        this.createdAt = Tools.getUnixTime();
        this.updatedAt = this.createdAt;
    }

    public Goal(int targetYear, int targetMonth, double targetValue) {
        this();
        this.targetYear = targetYear;
        this.targetMonth = targetMonth;
        this.targetValue = targetValue;
    }

    public Goal(int targetYear, double targetValue) {
        this(targetYear, 12, targetValue);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getTargetValue() { return targetValue; }
    public void setTargetValue(double targetValue) { this.targetValue = targetValue; }

    public int getTargetYear() { return targetYear; }
    public void setTargetYear(int targetYear) { this.targetYear = targetYear; }

    public int getTargetMonth() { return targetMonth; }
    public void setTargetMonth(int targetMonth) { this.targetMonth = targetMonth; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "GOAL," + targetYear + "," + targetMonth + "," + targetValue + "\n";
    }

    public static Goal fromString(String row) {
        try {
            if (!row.startsWith("GOAL,")) return null;
            String[] parts = row.split(",");
            
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[1].trim());
                double value = Double.parseDouble(parts[2].trim());
                return new Goal(year, 12, value);
            } else if (parts.length == 4) {
                int year = Integer.parseInt(parts[1].trim());
                int month = Integer.parseInt(parts[2].trim());
                double value = Double.parseDouble(parts[3].trim());
                return new Goal(year, month, value);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void updateTimestamp() {
        this.updatedAt = Tools.getUnixTime();
    }
}
