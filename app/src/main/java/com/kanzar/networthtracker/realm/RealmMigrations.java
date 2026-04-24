package com.kanzar.networthtracker.realm;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;

public final class RealmMigrations implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        // No migrations needed as app is not yet released.
        // Use deleteRealmIfMigrationNeeded() in CoreApplication for development.
    }
}
