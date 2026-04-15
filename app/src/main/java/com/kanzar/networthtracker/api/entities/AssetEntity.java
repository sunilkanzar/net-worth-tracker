package com.kanzar.networthtracker.api.entities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.kanzar.networthtracker.models.Asset;

public final class AssetEntity {

    @SerializedName("month")
    @Expose
    private int month;

    @SerializedName("name")
    @Expose
    private String name = "";

    @SerializedName("updatedAt")
    @Expose
    private long updatedAt;

    @SerializedName("value")
    @Expose
    private double value;

    @SerializedName("year")
    @Expose
    private int year;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Asset toAsset() {
        Asset asset = new Asset();
        asset.setYear(this.year);
        asset.setMonth(this.month);
        asset.setName(this.name);
        asset.setValue(this.value);
        asset.setUpdatedAt(this.updatedAt);
        asset.updateId();
        return asset;
    }
}
