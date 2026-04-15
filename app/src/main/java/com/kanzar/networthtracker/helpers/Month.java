package com.kanzar.networthtracker.helpers;

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

    public String getValueChangeString() {
        double change = getValueChange();
        String sign = (change > 0) ? "+" : "";
        return sign + Tools.formatAmount(change);
    }

    public double getPercent() {
        return Tools.getPercent(getPreviousMonth().getValue(), value);
    }

    public Month getFirst() {
        Asset firstAsset = Realm.getDefaultInstance()
                .where(Asset.class)
                .sort(new String[]{AssetFields.YEAR, AssetFields.MONTH}, new Sort[]{Sort.ASCENDING, Sort.ASCENDING})
                .findFirst();

        if (firstAsset != null) {
            return new Month(firstAsset.getMonth(), firstAsset.getYear());
        }
        return new Month();
    }

    public Month getPreviousMonth() {
        Month prev = new Month(this.month, this.year);
        prev.previous();
        return prev;
    }

    public boolean isCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return (calendar.get(Calendar.MONTH) + 1 == this.month) && (calendar.get(Calendar.YEAR) == this.year);
    }

    public final void calculateValues() {
        Number sum = Realm.getDefaultInstance()
                .where(Asset.class)
                .equalTo(AssetFields.YEAR, year)
                .equalTo(AssetFields.MONTH, month)
                .sum(AssetFields.VALUE);
        this.value = sum != null ? sum.doubleValue() : 0.0;
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

    @Override
    public String toString() {
        return new DateFormatSymbols().getMonths()[month - 1] + " " + year;
    }

    public String monthToString() {
        return new DateFormatSymbols().getMonths()[month - 1];
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
                Asset helper = Realm.getDefaultInstance().copyFromRealm(asset);
                helper.setHelper(true);
                helpers.add(helper);
            }
        }
        return helpers;
    }

    private List<Asset> query(int month, int year) {
        return Realm.getDefaultInstance()
                .where(Asset.class)
                .equalTo(AssetFields.MONTH, month)
                .equalTo(AssetFields.YEAR, year)
                .sort(AssetFields.VALUE, Sort.DESCENDING)
                .findAll();
    }
}
