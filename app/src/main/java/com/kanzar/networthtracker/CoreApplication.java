package com.kanzar.networthtracker;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import com.amplitude.api.Amplitude;
import com.kanzar.networthtracker.helpers.Prefs;
import com.kanzar.networthtracker.reminders.ReminderManager;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class CoreApplication extends MultiDexApplication {
    private static CoreApplication instance;

    public static CoreApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Load theme preference
        int theme = Prefs.getInt(Prefs.PREFS_THEME, Prefs.DEFAULT_THEME);
        AppCompatDelegate.setDefaultNightMode(theme);

        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .schemaVersion(3L)
                .deleteRealmIfMigrationNeeded()
                .allowWritesOnUiThread(true)
                .build();
        Realm.setDefaultConfiguration(config);

        Amplitude.getInstance()
                .initialize(this, "145c628a04e250cca32a8835c84f12c3")
                .enableForegroundTracking(this);

        ReminderManager.updateReminder(this);
    }
}
