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

    public double getValue(Realm realm) {
        calculateValues(realm);
        return value;
    }

    public double getValue(Realm realm, String type) {
        if ("Assets".equals(type)) return getAssetsValue(realm);
        if ("Liabilities".equals(type)) return getLiabilitiesValue(realm);
        return getValue(realm);
    }

    public double getAssetsValue(Realm realm) {
        Number sum = realm
                .where(Asset.class)
                .equalTo(AssetFields.YEAR, year)
                .equalTo(AssetFields.MONTH, month)
                .greaterThanOrEqualTo(AssetFields.VALUE, 0)
                .sum(AssetFields.VALUE);
        return sum != null ? sum.doubleValue() : 0.0;
    }

    public double getLiabilitiesValue(Realm realm) {
        Number sum = realm
                .where(Asset.class)
                .equalTo(AssetFields.YEAR, year)
                .equalTo(AssetFields.MONTH, month)
                .lessThan(AssetFields.VALUE, 0)
                .sum(AssetFields.VALUE);
        return sum != null ? Math.abs(sum.doubleValue()) : 0.0;
    }

    public double getValueChange(Realm realm) {
        return value - getPreviousMonth(false).getValue(realm);
    }

    public double getPercent(Realm realm) {
        return Tools.getPercent(getPreviousMonth(false).getValue(realm), value);
    }

    public Month getFirst() {
        try (Realm realm = Realm.getDefaultInstance()) {
            return getFirst(realm);
        }
    }

    public Month getFirst(Realm realm) {
        Asset firstAsset = realm
                .where(Asset.class)
                .sort(new String[]{AssetFields.YEAR, AssetFields.MONTH}, new Sort[]{Sort.ASCENDING, Sort.ASCENDING})
                .findFirst();

        if (firstAsset != null) {
            Month m = new Month(firstAsset.getMonth(), firstAsset.getYear());
            m.calculateValues(realm);
            return m;
        }
        return new Month(true);
    }

    public Month getLast() {
        try (Realm realm = Realm.getDefaultInstance()) {
            return getLast(realm);
        }
    }

    public Month getLast(Realm realm) {
        Asset lastAsset = realm
                .where(Asset.class)
                .sort(new String[]{AssetFields.YEAR, AssetFields.MONTH}, new Sort[]{Sort.DESCENDING, Sort.DESCENDING})
                .findFirst();

        if (lastAsset != null) {
            Month m = new Month(lastAsset.getMonth(), lastAsset.getYear());
            m.calculateValues(realm);
            return m;
        }
        return new Month(true);
    }

    public Month getPreviousMonth() {
        return getPreviousMonth(false);
    }

    public Month getPreviousMonth(boolean calculate) {
        Calendar cal = getCalendar();
        cal.add(Calendar.MONTH, -1);
        return new Month(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR), calculate);
    }

    public Month getPreviousMonth(Realm realm) {
        Month m = getPreviousMonth(false);
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
        Calendar cal = getCalendar();
        cal.add(Calendar.MONTH, -1);
        this.month = cal.get(Calendar.MONTH) + 1;
        this.year = cal.get(Calendar.YEAR);
        
        if (realm != null) calculateValues(realm);
        else calculateValues();
    }

    public void next() {
        next(null);
    }

    public void next(Realm realm) {
        Calendar cal = getCalendar();
        cal.add(Calendar.MONTH, 1);
        this.month = cal.get(Calendar.MONTH) + 1;
        this.year = cal.get(Calendar.YEAR);
        
        if (realm != null) calculateValues(realm);
        else calculateValues();
    }

    private Calendar getCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal;
    }

    @NonNull
    @Override
    public String toString() {
        return new DateFormatSymbols().getMonths()[month - 1] + " " + year;
    }

    public String toStringMMMYY() {
        String fullMonth = new DateFormatSymbols().getMonths()[month - 1];
        String shortMonth = fullMonth.length() > 3 ? fullMonth.substring(0, 3) : fullMonth;
        String yearShort = String.valueOf(year % 100);
        if (yearShort.length() == 1) yearShort = "0" + yearShort;
        return shortMonth + " " + yearShort;
    }

    public String toStringMMYY() {
        String yearShort = String.valueOf(year % 100);
        if (yearShort.length() == 1) yearShort = "0" + yearShort;
        return month + "." + yearShort;
    }

    public List<Asset> getAssets(Realm realm) {
        List<Asset> result = new ArrayList<>(query(realm, this.month, this.year));
        
        // Find placeholders from the immediately previous month
        Month pm = getPreviousMonth(false);
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
