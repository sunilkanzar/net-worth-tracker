package com.kanzar.networthtracker.realm;

import com.kanzar.networthtracker.models.AssetFields;
import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

public final class RealmMigrations implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();
        if (oldVersion == 1) {
            RealmObjectSchema assetSchema = schema.get("Asset");
            if (assetSchema != null) {
                assetSchema.addField(AssetFields.UPDATED_AT, long.class, FieldAttribute.REQUIRED);
            }
        }
    }
}
