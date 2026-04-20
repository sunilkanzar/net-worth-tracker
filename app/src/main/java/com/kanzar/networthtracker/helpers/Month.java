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
        Calendar calendar = Calendar.getInstance();
        this.month = calendar.get(Calendar.MONTH) + 1;
        this.year = calendar.get(Calendar.YEAR);
        calculateValues();
    }

    public Month(int month, int year) {
        this.month = month;
        this.year = year;
        calculateValues();
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

    public double getValueChange() {
        return value - getPreviousMonth().getValue();
    }


    public double getPercent() {
        return Tools.getPercent(getPreviousMonth().getValue(), value);
    }

    public Month getFirst() {
        try (Realm realm = Realm.getDefaultInstance()) {
            Asset firstAsset = realm
                    .where(Asset.class)
                    .sort(new String[]{AssetFields.YEAR, AssetFields.MONTH}, new Sort[]{Sort.ASCENDING, Sort.ASCENDING})
                    .findFirst();

            if (firstAsset != null) {
                return new Month(firstAsset.getMonth(), firstAsset.getYear());
            }
        }
        return new Month();
    }

    public Month getLast() {
        try (Realm realm = Realm.getDefaultInstance()) {
            Asset lastAsset = realm
                    .where(Asset.class)
                    .sort(new String[]{AssetFields.YEAR, AssetFields.MONTH}, new Sort[]{Sort.DESCENDING, Sort.DESCENDING})
                    .findFirst();

            if (lastAsset != null) {
                return new Month(lastAsset.getMonth(), lastAsset.getYear());
            }
        }
        return new Month();
    }

    public Month getPreviousMonth() {
        int pm = month == 1 ? 12 : month - 1;
        int py = month == 1 ? year - 1 : year;
        return new Month(pm, py);
    }


    public final void calculateValues() {
        try (Realm realm = Realm.getDefaultInstance()) {
            Number sum = realm
                    .where(Asset.class)
                    .equalTo(AssetFields.YEAR, year)
                    .equalTo(AssetFields.MONTH, month)
                    .sum(AssetFields.VALUE);
            this.value = sum != null ? sum.doubleValue() : 0.0;
        }
    }

    public void previous() {
        if (month == 1) {
            year--;
            month = 12;
        } else {
            month--;
        }
        calculateValues();
    }

    public void next() {
        if (month == 12) {
            month = 1;
            year++;
        } else {
            month++;
        }
        calculateValues();
    }

    @NonNull
    @Override
    public String toString() {
        return new DateFormatSymbols().getMonths()[month - 1] + " " + year;
    }


    public String toStringMMYY() {
        String yearStr = String.valueOf(year);
        String yearShort = yearStr.length() >= 4 ? yearStr.substring(2, 4) : yearStr;
        return month + "." + yearShort;
    }

    public List<Asset> getAssets() {
        Month prevMonth = getPreviousMonth();
        Month nextMonth = new Month(month, year);
        nextMonth.next();

        List<Asset> result = new ArrayList<>(query(this.month, this.year));
        result.addAll(getHelpers(result, prevMonth.month, prevMonth.year));
        result.addAll(getHelpers(result, nextMonth.month, nextMonth.year));

        return result;
    }

    public boolean hasAssets() {
        return !query(this.month, this.year).isEmpty();
    }

    private List<Asset> getHelpers(List<Asset> existingAssets, int month, int year) {
        List<String> existingNames = new ArrayList<>();
        for (Asset asset : existingAssets) {
            existingNames.add(asset.getName());
        }

        List<Asset> otherAssets = query(month, year);
        List<Asset> helpers = new ArrayList<>();
        for (Asset asset : otherAssets) {
            if (!existingNames.contains(asset.getName())) {
                asset.setHelper(true);
                helpers.add(asset);
            }
        }
        return helpers;
    }

    private List<Asset> query(int month, int year) {
        try (Realm realm = Realm.getDefaultInstance()) {
            return realm.copyFromRealm(realm
                    .where(Asset.class)
                    .equalTo(AssetFields.MONTH, month)
                    .equalTo(AssetFields.YEAR, year)
                    .sort(AssetFields.VALUE, Sort.DESCENDING)
                    .findAll());
        }
    }
}
