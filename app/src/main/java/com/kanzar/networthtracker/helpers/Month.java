package com.kanzar.networthtracker.helpers;

import androidx.annotation.NonNull;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import io.realm.Sort;

public class Month {

    private int month;
    private int year;
    private double value;

    public Month() {
        this(false);
    }

    public Month(boolean calculate) {
        Calendar calendar = Calendar.getInstance();
        this.month = calendar.get(Calendar.MONTH) + 1;
        this.year = calendar.get(Calendar.YEAR);
        if (calculate) calculateValues();
    }

    public Month(int month, int year) {
        this(month, year, false);
    }

    public Month(int month, int year, boolean calculate) {
        this.month = month;
        this.year = year;
        if (calculate) calculateValues();
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
        calculateValues();
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
        calculateValues();
    }

    public double getValue() {
        return value;
    }

    public double getValueChange(Realm realm) {
        return value - getPreviousMonth(false).getValue(realm);
    }


    public double getPercent(Realm realm) {
        return Tools.getPercent(getPreviousMonth(false).getValue(realm), value);
    }

    public Month getFirst() {
        try (Realm realm = Realm.getDefaultInstance()) {
            Asset firstAsset = realm
                    .where(Asset.class)
                    .sort(new String[]{AssetFields.YEAR, AssetFields.MONTH}, new Sort[]{Sort.ASCENDING, Sort.ASCENDING})
                    .findFirst();

            if (firstAsset != null) {
                Month m = new Month(firstAsset.getMonth(), firstAsset.getYear());
                m.calculateValues(realm);
                return m;
            }
        }
        return new Month(true);
    }

    public Month getLast() {
        try (Realm realm = Realm.getDefaultInstance()) {
            Asset lastAsset = realm
                    .where(Asset.class)
                    .sort(new String[]{AssetFields.YEAR, AssetFields.MONTH}, new Sort[]{Sort.DESCENDING, Sort.DESCENDING})
                    .findFirst();

            if (lastAsset != null) {
                Month m = new Month(lastAsset.getMonth(), lastAsset.getYear());
                m.calculateValues(realm);
                return m;
            }
        }
        return new Month(true);
    }

    public double getValue(Realm realm) {
        calculateValues(realm);
        return value;
    }

    public Month getPreviousMonth() {
        return getPreviousMonth(false);
    }

    public Month getPreviousMonth(boolean calculate) {
        int pm = month == 1 ? 12 : month - 1;
        int py = month == 1 ? year - 1 : year;
        return new Month(pm, py, calculate);
    }

    public Month getPreviousMonth(Realm realm) {
        int pm = month == 1 ? 12 : month - 1;
        int py = month == 1 ? year - 1 : year;
        Month m = new Month(pm, py, false);
        m.calculateValues(realm);
        return m;
    }


    public final void calculateValues() {
        try (Realm realm = Realm.getDefaultInstance()) {
            calculateValues(realm);
        }
    }

    public final void calculateValues(Realm realm) {
        Number sum = realm
                .where(Asset.class)
                .equalTo(AssetFields.YEAR, year)
                .equalTo(AssetFields.MONTH, month)
                .sum(AssetFields.VALUE);
        this.value = sum != null ? sum.doubleValue() : 0.0;
    }

    public void previous() {
        previous(null);
    }

    public void previous(Realm realm) {
        if (month == 1) {
            year--;
            month = 12;
        } else {
            month--;
        }
        if (realm != null) calculateValues(realm);
        else calculateValues();
    }

    public void next() {
        next(null);
    }

    public void next(Realm realm) {
        if (month == 12) {
            month = 1;
            year++;
        } else {
            month++;
        }
        if (realm != null) calculateValues(realm);
        else calculateValues();
    }

    @NonNull
    @Override
    public String toString() {
        return new DateFormatSymbols().getMonths()[month - 1] + " " + year;
    }


    public String toStringMMMYY() {
        String fullMonth = new DateFormatSymbols().getMonths()[month - 1];
        String shortMonth = fullMonth.length() > 3 ? fullMonth.substring(0, 3) : fullMonth;
        String yearStr = String.valueOf(year);
        String yearShort = yearStr.length() >= 4 ? yearStr.substring(2, 4) : yearStr;
        return shortMonth + " " + yearShort;
    }

    public String toStringMMYY() {
        String yearStr = String.valueOf(year);
        String yearShort = yearStr.length() >= 4 ? yearStr.substring(2, 4) : yearStr;
        return month + "." + yearShort;
    }

    public List<Asset> getAssets(Realm realm) {
        List<Asset> result = new ArrayList<>(query(realm, this.month, this.year));
        
        // Only show placeholders from the immediate previous month
        Month pm = getPreviousMonth();
        if (pm.hasAssets(realm)) {
            result.addAll(getHelpers(realm, result, pm.getMonth(), pm.getYear()));
        }

        return result;
    }

    public List<Asset> getAssets() {
        try (Realm realm = Realm.getDefaultInstance()) {
            return getAssets(realm);
        }
    }

    public boolean hasAssets(Realm realm) {
        return realm.where(Asset.class)
                .equalTo(AssetFields.MONTH, this.month)
                .equalTo(AssetFields.YEAR, this.year)
                .count() > 0;
    }

    public boolean hasAssets() {
        try (Realm realm = Realm.getDefaultInstance()) {
            return hasAssets(realm);
        }
    }

    private List<Asset> getHelpers(Realm realm, List<Asset> existingAssets, int month, int year) {
        java.util.Set<String> existingNames = new java.util.HashSet<>();
        for (Asset asset : existingAssets) {
            existingNames.add(asset.getName());
        }

        List<Asset> otherAssets = query(realm, month, year);
        List<Asset> helpers = new ArrayList<>();
        for (Asset asset : otherAssets) {
            if (!existingNames.contains(asset.getName())) {
                asset.setHelper(true);
                helpers.add(asset);
            }
        }
        return helpers;
    }

    private List<Asset> query(Realm realm, int month, int year) {
        return realm.copyFromRealm(realm
                .where(Asset.class)
                .equalTo(AssetFields.MONTH, month)
                .equalTo(AssetFields.YEAR, year)
                .sort(AssetFields.VALUE, Sort.DESCENDING)
                .findAll());
    }
}
