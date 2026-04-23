package com.kanzar.networthtracker.helpers;

import android.util.LruCache;
import com.github.mikephil.charting.data.Entry;
import io.realm.Realm;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.AssetFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SparklineHelper {
    private static final LruCache<String, List<Entry>> cache = new LruCache<>(500);

    private static String makeKey(String name, int month, int year) {
        return name + "_" + month + "_" + year;
    }

    public static List<Entry> getCachedData(String assetName, int month, int year) {
        if (assetName == null) return null;
        return cache.get(makeKey(assetName, month, year));
    }

    public static List<Entry> getSparklineData(String assetName, int month, int year) {
        if (assetName == null) return new ArrayList<>();
        String key = makeKey(assetName, month, year);
        List<Entry> cached = cache.get(key);
        if (cached != null) return cached;

        List<Entry> entries = new ArrayList<>();
        
        try (Realm realm = Realm.getDefaultInstance()) {
            int mo = month;
            int yr = year;
            
            for (int i = 0; i < 6; i++) {
                Asset a = realm.where(Asset.class)
                        .equalTo(AssetFields.NAME, assetName)
                        .equalTo(AssetFields.MONTH, mo)
                        .equalTo(AssetFields.YEAR, yr)
                        .findFirst();
                
                float val = a != null ? (float) a.getValue() : 0f;
                // Index 5 is current month, 0 is 5 months ago
                entries.add(new Entry(5 - i, val));

                if (mo == 1) {
                    mo = 12;
                    yr--;
                } else {
                    mo--;
                }
            }
        }
        
        // Sort by X value so the line is drawn correctly (left to right)
        Collections.sort(entries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));
        cache.put(key, entries);
        return entries;
    }

    public static void clearCache() {
        cache.evictAll();
    }
}
