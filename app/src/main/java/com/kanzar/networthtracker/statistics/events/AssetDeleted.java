package com.kanzar.networthtracker.statistics.events;

import com.google.gson.annotations.Expose;
import com.kanzar.networthtracker.models.Asset;

public final class AssetDeleted extends BaseEvent {

    @Expose
    private final int month;

    @Expose
    private final boolean negativeValue;

    @Expose
    private final boolean nextMonth;

    @Expose
    private final boolean previousMonth;

    @Expose
    private final int year;

    @Expose
    private final boolean zeroValue;

    public AssetDeleted(Asset asset) {
        this.negativeValue = asset.getValue() < 0;
        this.zeroValue = asset.getValue() == 0;
        this.month = asset.getMonth();
        this.year = asset.getYear();
        this.previousMonth = asset.getPrevious() != null;
        this.nextMonth = asset.getNext() != null;
    }
}
