package com.kanzar.networthtracker.models;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.kanzar.networthtracker.helpers.Tools;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Asset extends RealmObject {

    @PrimaryKey
    private String id;
    @Index
    private String name;
    private double value;
    @Index
    private int month;
    @Index
    private int year;
    private long updatedAt;

    @Ignore
    private transient boolean isHelper;

    @Ignore
    private transient double prevValue = Double.NaN;

    public Asset() {
        this.id = "";
        this.name = "";
    }

    public Asset(String name, double value, int month, int year) {
        this.name = name;
        this.value = value;
        this.month = month;
        this.year = year;
        this.updatedAt = Tools.getUnixTime();
        updateId();
    }

    public void updateId() {
        this.id = year + String.valueOf(month) + name;
        this.updatedAt = System.currentTimeMillis() / 1000L;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isHelper() { return isHelper; }
    public void setHelper(boolean helper) { isHelper = helper; }

    public double getPrevValue() {
        return prevValue;
    }

    public void setPrevValue(double prevValue) {
        this.prevValue = prevValue;
    }

    public boolean hasPrevValue() {
        return !Double.isNaN(prevValue);
    }

    public Asset getPrevious() {
        try (Realm realm = Realm.getDefaultInstance()) {
            return getPrevious(realm);
        }
    }

    public Asset getPrevious(Realm realm) {
        int pm = month == 1 ? 12 : month - 1;
        int py = month == 1 ? year - 1 : year;
        Asset result = realm.where(Asset.class)
                .equalTo("name", name)
                .equalTo("month", pm)
                .equalTo("year", py)
                .findFirst();
        return result != null ? realm.copyFromRealm(result) : null;
    }

    public Asset getNext() {
        try (Realm realm = Realm.getDefaultInstance()) {
            return getNext(realm);
        }
    }

    public Asset getNext(Realm realm) {
        int nm = month == 12 ? 1 : month + 1;
        int ny = month == 12 ? year + 1 : year;
        Asset result = realm.where(Asset.class)
                .equalTo("name", name)
                .equalTo("month", nm)
                .equalTo("year", ny)
                .findFirst();
        return result != null ? realm.copyFromRealm(result) : null;
    }

    public static Asset fromString(String row) {
        try {
            String[] parts = row.split(",");
            if (parts.length != 4) return null;

            Asset asset = new Asset();
            asset.year = Integer.parseInt(parts[0].trim());
            asset.month = Integer.parseInt(parts[1].trim());
            asset.name = parts[2].trim();
            asset.value = Double.parseDouble(parts[3].trim());
            asset.updateId();
            return asset;
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TextUtils.join(",", new String[]{
                String.valueOf(year),
                String.valueOf(month),
                name,
                String.valueOf(value)
        }) + "\n";
    }
}
