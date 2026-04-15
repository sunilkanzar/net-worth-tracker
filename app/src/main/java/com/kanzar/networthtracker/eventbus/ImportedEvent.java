package com.kanzar.networthtracker.eventbus;

import com.kanzar.networthtracker.models.Asset;
import java.util.ArrayList;

public final class ImportedEvent {
    private final ArrayList<Asset> assets;

    public ImportedEvent(ArrayList<Asset> assets) {
        this.assets = assets;
    }

    public ArrayList<Asset> getAssets() {
        return this.assets;
    }
}
