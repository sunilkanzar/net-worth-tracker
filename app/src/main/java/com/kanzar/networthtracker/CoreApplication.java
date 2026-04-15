package com.kanzar.networthtracker;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import com.amplitude.api.Amplitude;
import com.kanzar.networthtracker.realm.RealmMigrations;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class CoreApplication extends MultiDexApplication {
    private static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate();
        context = getApplicationContext();

        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .schemaVersion(2L)
                .migration(new RealmMigrations())
                .allowWritesOnUiThread(true)
                .build();
        Realm.setDefaultConfiguration(config);

        Amplitude.getInstance()
                .initialize(this, "145c628a04e250cca32a8835c84f12c3")
                .enableForegroundTracking(this);
    }
}
