package io.realm;


import android.annotation.TargetApi;
import android.os.Build;
import android.util.JsonReader;
import android.util.JsonToken;
import io.realm.ImportFlag;
import io.realm.ProxyUtils;
import io.realm.exceptions.RealmMigrationNeededException;
import io.realm.internal.ColumnInfo;
import io.realm.internal.NativeContext;
import io.realm.internal.OsList;
import io.realm.internal.OsMap;
import io.realm.internal.OsObject;
import io.realm.internal.OsObjectSchemaInfo;
import io.realm.internal.OsSchemaInfo;
import io.realm.internal.OsSet;
import io.realm.internal.Property;
import io.realm.internal.RealmObjectProxy;
import io.realm.internal.Row;
import io.realm.internal.Table;
import io.realm.internal.android.JsonUtils;
import io.realm.internal.core.NativeRealmAny;
import io.realm.internal.objectstore.OsObjectBuilder;
import io.realm.log.RealmLog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("all")
public class com_kanzar_networthtracker_models_AssetRealmProxy extends com.kanzar.networthtracker.models.Asset
    implements RealmObjectProxy, com_kanzar_networthtracker_models_AssetRealmProxyInterface {

    static final class AssetColumnInfo extends ColumnInfo {
        long idColKey;
        long nameColKey;
        long valueColKey;
        long monthColKey;
        long yearColKey;
        long updatedAtColKey;

        AssetColumnInfo(OsSchemaInfo schemaInfo) {
            super(6);
            OsObjectSchemaInfo objectSchemaInfo = schemaInfo.getObjectSchemaInfo("Asset");
            this.idColKey = addColumnDetails("id", "id", objectSchemaInfo);
            this.nameColKey = addColumnDetails("name", "name", objectSchemaInfo);
            this.valueColKey = addColumnDetails("value", "value", objectSchemaInfo);
            this.monthColKey = addColumnDetails("month", "month", objectSchemaInfo);
            this.yearColKey = addColumnDetails("year", "year", objectSchemaInfo);
            this.updatedAtColKey = addColumnDetails("updatedAt", "updatedAt", objectSchemaInfo);
        }

        AssetColumnInfo(ColumnInfo src, boolean mutable) {
            super(src, mutable);
            copy(src, this);
        }

        @Override
        protected final ColumnInfo copy(boolean mutable) {
            return new AssetColumnInfo(this, mutable);
        }

        @Override
        protected final void copy(ColumnInfo rawSrc, ColumnInfo rawDst) {
            final AssetColumnInfo src = (AssetColumnInfo) rawSrc;
            final AssetColumnInfo dst = (AssetColumnInfo) rawDst;
            dst.idColKey = src.idColKey;
            dst.nameColKey = src.nameColKey;
            dst.valueColKey = src.valueColKey;
            dst.monthColKey = src.monthColKey;
            dst.yearColKey = src.yearColKey;
            dst.updatedAtColKey = src.updatedAtColKey;
        }
    }

    private static final String NO_ALIAS = "";
    private static final OsObjectSchemaInfo expectedObjectSchemaInfo = createExpectedObjectSchemaInfo();

    private AssetColumnInfo columnInfo;
    private ProxyState<com.kanzar.networthtracker.models.Asset> proxyState;

    com_kanzar_networthtracker_models_AssetRealmProxy() {
        proxyState.setConstructionFinished();
    }

    @Override
    public void realm$injectObjectContext() {
        if (this.proxyState != null) {
            return;
        }
        final BaseRealm.RealmObjectContext context = BaseRealm.objectContext.get();
        this.columnInfo = (AssetColumnInfo) context.getColumnInfo();
        this.proxyState = new ProxyState<com.kanzar.networthtracker.models.Asset>(this);
        proxyState.setRealm$realm(context.getRealm());
        proxyState.setRow$realm(context.getRow());
        proxyState.setAcceptDefaultValue$realm(context.getAcceptDefaultValue());
        proxyState.setExcludeFields$realm(context.getExcludeFields());
    }

    @Override
    @SuppressWarnings("cast")
    public String realmGet$id() {
        proxyState.getRealm$realm().checkIfValid();
        return (java.lang.String) proxyState.getRow$realm().getString(columnInfo.idColKey);
    }

    @Override
    public void realmSet$id(String value) {
        if (proxyState.isUnderConstruction()) {
            // default value of the primary key is always ignored.
            return;
        }

        proxyState.getRealm$realm().checkIfValid();
        throw new io.realm.exceptions.RealmException("Primary key field 'id' cannot be changed after object was created.");
    }

    @Override
    @SuppressWarnings("cast")
    public String realmGet$name() {
        proxyState.getRealm$realm().checkIfValid();
        return (java.lang.String) proxyState.getRow$realm().getString(columnInfo.nameColKey);
    }

    @Override
    public void realmSet$name(String value) {
        if (proxyState.isUnderConstruction()) {
            if (!proxyState.getAcceptDefaultValue$realm()) {
                return;
            }
            final Row row = proxyState.getRow$realm();
            if (value == null) {
                row.getTable().setNull(columnInfo.nameColKey, row.getObjectKey(), true);
                return;
            }
            row.getTable().setString(columnInfo.nameColKey, row.getObjectKey(), value, true);
            return;
        }

        proxyState.getRealm$realm().checkIfValid();
        if (value == null) {
            proxyState.getRow$realm().setNull(columnInfo.nameColKey);
            return;
        }
        proxyState.getRow$realm().setString(columnInfo.nameColKey, value);
    }

    @Override
    @SuppressWarnings("cast")
    public double realmGet$value() {
        proxyState.getRealm$realm().checkIfValid();
        return (double) proxyState.getRow$realm().getDouble(columnInfo.valueColKey);
    }

    @Override
    public void realmSet$value(double value) {
        if (proxyState.isUnderConstruction()) {
            if (!proxyState.getAcceptDefaultValue$realm()) {
                return;
            }
            final Row row = proxyState.getRow$realm();
            row.getTable().setDouble(columnInfo.valueColKey, row.getObjectKey(), value, true);
            return;
        }

        proxyState.getRealm$realm().checkIfValid();
        proxyState.getRow$realm().setDouble(columnInfo.valueColKey, value);
    }

    @Override
    @SuppressWarnings("cast")
    public int realmGet$month() {
        proxyState.getRealm$realm().checkIfValid();
        return (int) proxyState.getRow$realm().getLong(columnInfo.monthColKey);
    }

    @Override
    public void realmSet$month(int value) {
        if (proxyState.isUnderConstruction()) {
            if (!proxyState.getAcceptDefaultValue$realm()) {
                return;
            }
            final Row row = proxyState.getRow$realm();
            row.getTable().setLong(columnInfo.monthColKey, row.getObjectKey(), value, true);
            return;
        }

        proxyState.getRealm$realm().checkIfValid();
        proxyState.getRow$realm().setLong(columnInfo.monthColKey, value);
    }

    @Override
    @SuppressWarnings("cast")
    public int realmGet$year() {
        proxyState.getRealm$realm().checkIfValid();
        return (int) proxyState.getRow$realm().getLong(columnInfo.yearColKey);
    }

    @Override
    public void realmSet$year(int value) {
        if (proxyState.isUnderConstruction()) {
            if (!proxyState.getAcceptDefaultValue$realm()) {
                return;
            }
            final Row row = proxyState.getRow$realm();
            row.getTable().setLong(columnInfo.yearColKey, row.getObjectKey(), value, true);
            return;
        }

        proxyState.getRealm$realm().checkIfValid();
        proxyState.getRow$realm().setLong(columnInfo.yearColKey, value);
    }

    @Override
    @SuppressWarnings("cast")
    public long realmGet$updatedAt() {
        proxyState.getRealm$realm().checkIfValid();
        return (long) proxyState.getRow$realm().getLong(columnInfo.updatedAtColKey);
    }

    @Override
    public void realmSet$updatedAt(long value) {
        if (proxyState.isUnderConstruction()) {
            if (!proxyState.getAcceptDefaultValue$realm()) {
                return;
            }
            final Row row = proxyState.getRow$realm();
            row.getTable().setLong(columnInfo.updatedAtColKey, row.getObjectKey(), value, true);
            return;
        }

        proxyState.getRealm$realm().checkIfValid();
        proxyState.getRow$realm().setLong(columnInfo.updatedAtColKey, value);
    }

    private static OsObjectSchemaInfo createExpectedObjectSchemaInfo() {
        OsObjectSchemaInfo.Builder builder = new OsObjectSchemaInfo.Builder(NO_ALIAS, "Asset", false, 6, 0);
        builder.addPersistedProperty(NO_ALIAS, "id", RealmFieldType.STRING, Property.PRIMARY_KEY, !Property.INDEXED, !Property.REQUIRED);
        builder.addPersistedProperty(NO_ALIAS, "name", RealmFieldType.STRING, !Property.PRIMARY_KEY, !Property.INDEXED, !Property.REQUIRED);
        builder.addPersistedProperty(NO_ALIAS, "value", RealmFieldType.DOUBLE, !Property.PRIMARY_KEY, !Property.INDEXED, Property.REQUIRED);
        builder.addPersistedProperty(NO_ALIAS, "month", RealmFieldType.INTEGER, !Property.PRIMARY_KEY, !Property.INDEXED, Property.REQUIRED);
        builder.addPersistedProperty(NO_ALIAS, "year", RealmFieldType.INTEGER, !Property.PRIMARY_KEY, !Property.INDEXED, Property.REQUIRED);
        builder.addPersistedProperty(NO_ALIAS, "updatedAt", RealmFieldType.INTEGER, !Property.PRIMARY_KEY, !Property.INDEXED, Property.REQUIRED);
        return builder.build();
    }

    public static OsObjectSchemaInfo getExpectedObjectSchemaInfo() {
        return expectedObjectSchemaInfo;
    }

    public static AssetColumnInfo createColumnInfo(OsSchemaInfo schemaInfo) {
        return new AssetColumnInfo(schemaInfo);
    }

    public static String getSimpleClassName() {
        return "Asset";
    }

    public static final class ClassNameHelper {
        public static final String INTERNAL_CLASS_NAME = "Asset";
    }

    @SuppressWarnings("cast")
    public static com.kanzar.networthtracker.models.Asset createOrUpdateUsingJsonObject(Realm realm, JSONObject json, boolean update)
        throws JSONException {
        final List<String> excludeFields = Collections.<String> emptyList();
        com.kanzar.networthtracker.models.Asset obj = null;
        if (update) {
            Table table = realm.getTable(com.kanzar.networthtracker.models.Asset.class);
            AssetColumnInfo columnInfo = (AssetColumnInfo) realm.getSchema().getColumnInfo(com.kanzar.networthtracker.models.Asset.class);
            long pkColumnKey = columnInfo.idColKey;
            long objKey = Table.NO_MATCH;
            if (json.isNull("id")) {
                objKey = table.findFirstNull(pkColumnKey);
            } else {
                objKey = table.findFirstString(pkColumnKey, json.getString("id"));
            }
            if (objKey != Table.NO_MATCH) {
                final BaseRealm.RealmObjectContext objectContext = BaseRealm.objectContext.get();
                try {
                    objectContext.set(realm, table.getUncheckedRow(objKey), realm.getSchema().getColumnInfo(com.kanzar.networthtracker.models.Asset.class), false, Collections.<String> emptyList());
                    obj = new io.realm.com_kanzar_networthtracker_models_AssetRealmProxy();
                } finally {
                    objectContext.clear();
                }
            }
        }
        if (obj == null) {
            if (json.has("id")) {
                if (json.isNull("id")) {
                    obj = (io.realm.com_kanzar_networthtracker_models_AssetRealmProxy) realm.createObjectInternal(com.kanzar.networthtracker.models.Asset.class, null, true, excludeFields);
                } else {
                    obj = (io.realm.com_kanzar_networthtracker_models_AssetRealmProxy) realm.createObjectInternal(com.kanzar.networthtracker.models.Asset.class, json.getString("id"), true, excludeFields);
                }
            } else {
                throw new IllegalArgumentException("JSON object doesn't have the primary key field 'id'.");
            }
        }

        final com_kanzar_networthtracker_models_AssetRealmProxyInterface objProxy = (com_kanzar_networthtracker_models_AssetRealmProxyInterface) obj;
        if (json.has("name")) {
            if (json.isNull("name")) {
                objProxy.realmSet$name(null);
            } else {
                objProxy.realmSet$name((String) json.getString("name"));
            }
        }
        if (json.has("value")) {
            if (json.isNull("value")) {
                throw new IllegalArgumentException("Trying to set non-nullable field 'value' to null.");
            } else {
                objProxy.realmSet$value((double) json.getDouble("value"));
            }
        }
        if (json.has("month")) {
            if (json.isNull("month")) {
                throw new IllegalArgumentException("Trying to set non-nullable field 'month' to null.");
            } else {
                objProxy.realmSet$month((int) json.getInt("month"));
            }
        }
        if (json.has("year")) {
            if (json.isNull("year")) {
                throw new IllegalArgumentException("Trying to set non-nullable field 'year' to null.");
            } else {
                objProxy.realmSet$year((int) json.getInt("year"));
            }
        }
        if (json.has("updatedAt")) {
            if (json.isNull("updatedAt")) {
                throw new IllegalArgumentException("Trying to set non-nullable field 'updatedAt' to null.");
            } else {
                objProxy.realmSet$updatedAt((long) json.getLong("updatedAt"));
            }
        }
        return obj;
    }

    @SuppressWarnings("cast")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static com.kanzar.networthtracker.models.Asset createUsingJsonStream(Realm realm, JsonReader reader)
        throws IOException {
        boolean jsonHasPrimaryKey = false;
        final com.kanzar.networthtracker.models.Asset obj = new com.kanzar.networthtracker.models.Asset();
        final com_kanzar_networthtracker_models_AssetRealmProxyInterface objProxy = (com_kanzar_networthtracker_models_AssetRealmProxyInterface) obj;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (false) {
            } else if (name.equals("id")) {
                if (reader.peek() != JsonToken.NULL) {
                    objProxy.realmSet$id((String) reader.nextString());
                } else {
                    reader.skipValue();
                    objProxy.realmSet$id(null);
                }
                jsonHasPrimaryKey = true;
            } else if (name.equals("name")) {
                if (reader.peek() != JsonToken.NULL) {
                    objProxy.realmSet$name((String) reader.nextString());
                } else {
                    reader.skipValue();
                    objProxy.realmSet$name(null);
                }
            } else if (name.equals("value")) {
                if (reader.peek() != JsonToken.NULL) {
                    objProxy.realmSet$value((double) reader.nextDouble());
                } else {
                    reader.skipValue();
                    throw new IllegalArgumentException("Trying to set non-nullable field 'value' to null.");
                }
            } else if (name.equals("month")) {
                if (reader.peek() != JsonToken.NULL) {
                    objProxy.realmSet$month((int) reader.nextInt());
                } else {
                    reader.skipValue();
                    throw new IllegalArgumentException("Trying to set non-nullable field 'month' to null.");
                }
            } else if (name.equals("year")) {
                if (reader.peek() != JsonToken.NULL) {
                    objProxy.realmSet$year((int) reader.nextInt());
                } else {
                    reader.skipValue();
                    throw new IllegalArgumentException("Trying to set non-nullable field 'year' to null.");
                }
            } else if (name.equals("updatedAt")) {
                if (reader.peek() != JsonToken.NULL) {
                    objProxy.realmSet$updatedAt((long) reader.nextLong());
                } else {
                    reader.skipValue();
                    throw new IllegalArgumentException("Trying to set non-nullable field 'updatedAt' to null.");
                }
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        if (!jsonHasPrimaryKey) {
            throw new IllegalArgumentException("JSON object doesn't have the primary key field 'id'.");
        }
        return realm.copyToRealmOrUpdate(obj);
    }

    static com_kanzar_networthtracker_models_AssetRealmProxy newProxyInstance(BaseRealm realm, Row row) {
        // Ignore default values to avoid creating unexpected objects from RealmModel/RealmList fields
        final BaseRealm.RealmObjectContext objectContext = BaseRealm.objectContext.get();
        objectContext.set(realm, row, realm.getSchema().getColumnInfo(com.kanzar.networthtracker.models.Asset.class), false, Collections.<String>emptyList());
        io.realm.com_kanzar_networthtracker_models_AssetRealmProxy obj = new io.realm.com_kanzar_networthtracker_models_AssetRealmProxy();
        objectContext.clear();
        return obj;
    }

    public static com.kanzar.networthtracker.models.Asset copyOrUpdate(Realm realm, AssetColumnInfo columnInfo, com.kanzar.networthtracker.models.Asset object, boolean update, Map<RealmModel,RealmObjectProxy> cache, Set<ImportFlag> flags) {
        if (object instanceof RealmObjectProxy && !RealmObject.isFrozen(object) && ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm() != null) {
            final BaseRealm otherRealm = ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm();
            if (otherRealm.threadId != realm.threadId) {
                throw new IllegalArgumentException("Objects which belong to Realm instances in other threads cannot be copied into this Realm instance.");
            }
            if (otherRealm.getPath().equals(realm.getPath())) {
                return object;
            }
        }
        final BaseRealm.RealmObjectContext objectContext = BaseRealm.objectContext.get();
        RealmObjectProxy cachedRealmObject = cache.get(object);
        if (cachedRealmObject != null) {
            return (com.kanzar.networthtracker.models.Asset) cachedRealmObject;
        }

        com.kanzar.networthtracker.models.Asset realmObject = null;
        boolean canUpdate = update;
        if (canUpdate) {
            Table table = realm.getTable(com.kanzar.networthtracker.models.Asset.class);
            long pkColumnKey = columnInfo.idColKey;
            String value = ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$id();
            long objKey = Table.NO_MATCH;
            if (value == null) {
                objKey = table.findFirstNull(pkColumnKey);
            } else {
                objKey = table.findFirstString(pkColumnKey, value);
            }
            if (objKey == Table.NO_MATCH) {
                canUpdate = false;
            } else {
                try {
                    objectContext.set(realm, table.getUncheckedRow(objKey), columnInfo, false, Collections.<String> emptyList());
                    realmObject = new io.realm.com_kanzar_networthtracker_models_AssetRealmProxy();
                    cache.put(object, (RealmObjectProxy) realmObject);
                } finally {
                    objectContext.clear();
                }
            }
        }

        return (canUpdate) ? update(realm, columnInfo, realmObject, object, cache, flags) : copy(realm, columnInfo, object, update, cache, flags);
    }

    public static com.kanzar.networthtracker.models.Asset copy(Realm realm, AssetColumnInfo columnInfo, com.kanzar.networthtracker.models.Asset newObject, boolean update, Map<RealmModel,RealmObjectProxy> cache, Set<ImportFlag> flags) {
        RealmObjectProxy cachedRealmObject = cache.get(newObject);
        if (cachedRealmObject != null) {
            return (com.kanzar.networthtracker.models.Asset) cachedRealmObject;
        }

        com_kanzar_networthtracker_models_AssetRealmProxyInterface unmanagedSource = (com_kanzar_networthtracker_models_AssetRealmProxyInterface) newObject;

        Table table = realm.getTable(com.kanzar.networthtracker.models.Asset.class);
        OsObjectBuilder builder = new OsObjectBuilder(table, flags);

        // Add all non-"object reference" fields
        builder.addString(columnInfo.idColKey, unmanagedSource.realmGet$id());
        builder.addString(columnInfo.nameColKey, unmanagedSource.realmGet$name());
        builder.addDouble(columnInfo.valueColKey, unmanagedSource.realmGet$value());
        builder.addInteger(columnInfo.monthColKey, unmanagedSource.realmGet$month());
        builder.addInteger(columnInfo.yearColKey, unmanagedSource.realmGet$year());
        builder.addInteger(columnInfo.updatedAtColKey, unmanagedSource.realmGet$updatedAt());

        // Create the underlying object and cache it before setting any object/objectlist references
        // This will allow us to break any circular dependencies by using the object cache.
        Row row = builder.createNewObject();
        io.realm.com_kanzar_networthtracker_models_AssetRealmProxy managedCopy = newProxyInstance(realm, row);
        cache.put(newObject, managedCopy);

        return managedCopy;
    }

    public static long insert(Realm realm, com.kanzar.networthtracker.models.Asset object, Map<RealmModel,Long> cache) {
        if (object instanceof RealmObjectProxy && !RealmObject.isFrozen(object) && ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm() != null && ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm().getPath().equals(realm.getPath())) {
            return ((RealmObjectProxy) object).realmGet$proxyState().getRow$realm().getObjectKey();
        }
        Table table = realm.getTable(com.kanzar.networthtracker.models.Asset.class);
        long tableNativePtr = table.getNativePtr();
        AssetColumnInfo columnInfo = (AssetColumnInfo) realm.getSchema().getColumnInfo(com.kanzar.networthtracker.models.Asset.class);
        long pkColumnKey = columnInfo.idColKey;
        String primaryKeyValue = ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$id();
        long objKey = Table.NO_MATCH;
        if (primaryKeyValue == null) {
            objKey = Table.nativeFindFirstNull(tableNativePtr, pkColumnKey);
        } else {
            objKey = Table.nativeFindFirstString(tableNativePtr, pkColumnKey, primaryKeyValue);
        }
        if (objKey == Table.NO_MATCH) {
            objKey = OsObject.createRowWithPrimaryKey(table, pkColumnKey, primaryKeyValue);
        } else {
            Table.throwDuplicatePrimaryKeyException(primaryKeyValue);
        }
        cache.put(object, objKey);
        String realmGet$name = ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$name();
        if (realmGet$name != null) {
            Table.nativeSetString(tableNativePtr, columnInfo.nameColKey, objKey, realmGet$name, false);
        }
        Table.nativeSetDouble(tableNativePtr, columnInfo.valueColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$value(), false);
        Table.nativeSetLong(tableNativePtr, columnInfo.monthColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$month(), false);
        Table.nativeSetLong(tableNativePtr, columnInfo.yearColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$year(), false);
        Table.nativeSetLong(tableNativePtr, columnInfo.updatedAtColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$updatedAt(), false);
        return objKey;
    }

    public static void insert(Realm realm, Iterator<? extends RealmModel> objects, Map<RealmModel,Long> cache) {
        Table table = realm.getTable(com.kanzar.networthtracker.models.Asset.class);
        long tableNativePtr = table.getNativePtr();
        AssetColumnInfo columnInfo = (AssetColumnInfo) realm.getSchema().getColumnInfo(com.kanzar.networthtracker.models.Asset.class);
        long pkColumnKey = columnInfo.idColKey;
        com.kanzar.networthtracker.models.Asset object = null;
        while (objects.hasNext()) {
            object = (com.kanzar.networthtracker.models.Asset) objects.next();
            if (cache.containsKey(object)) {
                continue;
            }
            if (object instanceof RealmObjectProxy && !RealmObject.isFrozen(object) && ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm() != null && ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm().getPath().equals(realm.getPath())) {
                cache.put(object, ((RealmObjectProxy) object).realmGet$proxyState().getRow$realm().getObjectKey());
                continue;
            }
            String primaryKeyValue = ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$id();
            long objKey = Table.NO_MATCH;
            if (primaryKeyValue == null) {
                objKey = Table.nativeFindFirstNull(tableNativePtr, pkColumnKey);
            } else {
                objKey = Table.nativeFindFirstString(tableNativePtr, pkColumnKey, primaryKeyValue);
            }
            if (objKey == Table.NO_MATCH) {
                objKey = OsObject.createRowWithPrimaryKey(table, pkColumnKey, primaryKeyValue);
            } else {
                Table.throwDuplicatePrimaryKeyException(primaryKeyValue);
            }
            cache.put(object, objKey);
            String realmGet$name = ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$name();
            if (realmGet$name != null) {
                Table.nativeSetString(tableNativePtr, columnInfo.nameColKey, objKey, realmGet$name, false);
            }
            Table.nativeSetDouble(tableNativePtr, columnInfo.valueColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$value(), false);
            Table.nativeSetLong(tableNativePtr, columnInfo.monthColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$month(), false);
            Table.nativeSetLong(tableNativePtr, columnInfo.yearColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$year(), false);
            Table.nativeSetLong(tableNativePtr, columnInfo.updatedAtColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$updatedAt(), false);
        }
    }

    public static long insertOrUpdate(Realm realm, com.kanzar.networthtracker.models.Asset object, Map<RealmModel,Long> cache) {
        if (object instanceof RealmObjectProxy && !RealmObject.isFrozen(object) && ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm() != null && ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm().getPath().equals(realm.getPath())) {
            return ((RealmObjectProxy) object).realmGet$proxyState().getRow$realm().getObjectKey();
        }
        Table table = realm.getTable(com.kanzar.networthtracker.models.Asset.class);
        long tableNativePtr = table.getNativePtr();
        AssetColumnInfo columnInfo = (AssetColumnInfo) realm.getSchema().getColumnInfo(com.kanzar.networthtracker.models.Asset.class);
        long pkColumnKey = columnInfo.idColKey;
        String primaryKeyValue = ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$id();
        long objKey = Table.NO_MATCH;
        if (primaryKeyValue == null) {
            objKey = Table.nativeFindFirstNull(tableNativePtr, pkColumnKey);
        } else {
            objKey = Table.nativeFindFirstString(tableNativePtr, pkColumnKey, primaryKeyValue);
        }
        if (objKey == Table.NO_MATCH) {
            objKey = OsObject.createRowWithPrimaryKey(table, pkColumnKey, primaryKeyValue);
        }
        cache.put(object, objKey);
        String realmGet$name = ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$name();
        if (realmGet$name != null) {
            Table.nativeSetString(tableNativePtr, columnInfo.nameColKey, objKey, realmGet$name, false);
        } else {
            Table.nativeSetNull(tableNativePtr, columnInfo.nameColKey, objKey, false);
        }
        Table.nativeSetDouble(tableNativePtr, columnInfo.valueColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$value(), false);
        Table.nativeSetLong(tableNativePtr, columnInfo.monthColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$month(), false);
        Table.nativeSetLong(tableNativePtr, columnInfo.yearColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$year(), false);
        Table.nativeSetLong(tableNativePtr, columnInfo.updatedAtColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$updatedAt(), false);
        return objKey;
    }

    public static void insertOrUpdate(Realm realm, Iterator<? extends RealmModel> objects, Map<RealmModel,Long> cache) {
        Table table = realm.getTable(com.kanzar.networthtracker.models.Asset.class);
        long tableNativePtr = table.getNativePtr();
        AssetColumnInfo columnInfo = (AssetColumnInfo) realm.getSchema().getColumnInfo(com.kanzar.networthtracker.models.Asset.class);
        long pkColumnKey = columnInfo.idColKey;
        com.kanzar.networthtracker.models.Asset object = null;
        while (objects.hasNext()) {
            object = (com.kanzar.networthtracker.models.Asset) objects.next();
            if (cache.containsKey(object)) {
                continue;
            }
            if (object instanceof RealmObjectProxy && !RealmObject.isFrozen(object) && ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm() != null && ((RealmObjectProxy) object).realmGet$proxyState().getRealm$realm().getPath().equals(realm.getPath())) {
                cache.put(object, ((RealmObjectProxy) object).realmGet$proxyState().getRow$realm().getObjectKey());
                continue;
            }
            String primaryKeyValue = ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$id();
            long objKey = Table.NO_MATCH;
            if (primaryKeyValue == null) {
                objKey = Table.nativeFindFirstNull(tableNativePtr, pkColumnKey);
            } else {
                objKey = Table.nativeFindFirstString(tableNativePtr, pkColumnKey, primaryKeyValue);
            }
            if (objKey == Table.NO_MATCH) {
                objKey = OsObject.createRowWithPrimaryKey(table, pkColumnKey, primaryKeyValue);
            }
            cache.put(object, objKey);
            String realmGet$name = ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$name();
            if (realmGet$name != null) {
                Table.nativeSetString(tableNativePtr, columnInfo.nameColKey, objKey, realmGet$name, false);
            } else {
                Table.nativeSetNull(tableNativePtr, columnInfo.nameColKey, objKey, false);
            }
            Table.nativeSetDouble(tableNativePtr, columnInfo.valueColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$value(), false);
            Table.nativeSetLong(tableNativePtr, columnInfo.monthColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$month(), false);
            Table.nativeSetLong(tableNativePtr, columnInfo.yearColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$year(), false);
            Table.nativeSetLong(tableNativePtr, columnInfo.updatedAtColKey, objKey, ((com_kanzar_networthtracker_models_AssetRealmProxyInterface) object).realmGet$updatedAt(), false);
        }
    }

    public static com.kanzar.networthtracker.models.Asset createDetachedCopy(com.kanzar.networthtracker.models.Asset realmObject, int currentDepth, int maxDepth, Map<RealmModel, CacheData<RealmModel>> cache) {
        if (currentDepth > maxDepth || realmObject == null) {
            return null;
        }
        CacheData<RealmModel> cachedObject = cache.get(realmObject);
        com.kanzar.networthtracker.models.Asset unmanagedObject;
        if (cachedObject == null) {
            unmanagedObject = new com.kanzar.networthtracker.models.Asset();
            cache.put(realmObject, new RealmObjectProxy.CacheData<RealmModel>(currentDepth, unmanagedObject));
        } else {
            // Reuse cached object or recreate it because it was encountered at a lower depth.
            if (currentDepth >= cachedObject.minDepth) {
                return (com.kanzar.networthtracker.models.Asset) cachedObject.object;
            }
            unmanagedObject = (com.kanzar.networthtracker.models.Asset) cachedObject.object;
            cachedObject.minDepth = currentDepth;
        }
        com_kanzar_networthtracker_models_AssetRealmProxyInterface unmanagedCopy = (com_kanzar_networthtracker_models_AssetRealmProxyInterface) unmanagedObject;
        com_kanzar_networthtracker_models_AssetRealmProxyInterface realmSource = (com_kanzar_networthtracker_models_AssetRealmProxyInterface) realmObject;
        Realm objectRealm = (Realm) ((RealmObjectProxy) realmObject).realmGet$proxyState().getRealm$realm();
        unmanagedCopy.realmSet$id(realmSource.realmGet$id());
        unmanagedCopy.realmSet$name(realmSource.realmGet$name());
        unmanagedCopy.realmSet$value(realmSource.realmGet$value());
        unmanagedCopy.realmSet$month(realmSource.realmGet$month());
        unmanagedCopy.realmSet$year(realmSource.realmGet$year());
        unmanagedCopy.realmSet$updatedAt(realmSource.realmGet$updatedAt());

        return unmanagedObject;
    }

    static com.kanzar.networthtracker.models.Asset update(Realm realm, AssetColumnInfo columnInfo, com.kanzar.networthtracker.models.Asset realmObject, com.kanzar.networthtracker.models.Asset newObject, Map<RealmModel, RealmObjectProxy> cache, Set<ImportFlag> flags) {
        com_kanzar_networthtracker_models_AssetRealmProxyInterface realmObjectTarget = (com_kanzar_networthtracker_models_AssetRealmProxyInterface) realmObject;
        com_kanzar_networthtracker_models_AssetRealmProxyInterface realmObjectSource = (com_kanzar_networthtracker_models_AssetRealmProxyInterface) newObject;
        Table table = realm.getTable(com.kanzar.networthtracker.models.Asset.class);
        OsObjectBuilder builder = new OsObjectBuilder(table, flags);
        builder.addString(columnInfo.idColKey, realmObjectSource.realmGet$id());
        builder.addString(columnInfo.nameColKey, realmObjectSource.realmGet$name());
        builder.addDouble(columnInfo.valueColKey, realmObjectSource.realmGet$value());
        builder.addInteger(columnInfo.monthColKey, realmObjectSource.realmGet$month());
        builder.addInteger(columnInfo.yearColKey, realmObjectSource.realmGet$year());
        builder.addInteger(columnInfo.updatedAtColKey, realmObjectSource.realmGet$updatedAt());

        builder.updateExistingTopLevelObject();
        return realmObject;
    }

    @Override
    public ProxyState<?> realmGet$proxyState() {
        return proxyState;
    }

    @Override
    public int hashCode() {
        String realmName = proxyState.getRealm$realm().getPath();
        String tableName = proxyState.getRow$realm().getTable().getName();
        long objKey = proxyState.getRow$realm().getObjectKey();

        int result = 17;
        result = 31 * result + ((realmName != null) ? realmName.hashCode() : 0);
        result = 31 * result + ((tableName != null) ? tableName.hashCode() : 0);
        result = 31 * result + (int) (objKey ^ (objKey >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        com_kanzar_networthtracker_models_AssetRealmProxy aAsset = (com_kanzar_networthtracker_models_AssetRealmProxy)o;

        BaseRealm realm = proxyState.getRealm$realm();
        BaseRealm otherRealm = aAsset.proxyState.getRealm$realm();
        String path = realm.getPath();
        String otherPath = otherRealm.getPath();
        if (path != null ? !path.equals(otherPath) : otherPath != null) return false;
        if (realm.isFrozen() != otherRealm.isFrozen()) return false;
        if (!realm.sharedRealm.getVersionID().equals(otherRealm.sharedRealm.getVersionID())) {
            return false;
        }

        String tableName = proxyState.getRow$realm().getTable().getName();
        String otherTableName = aAsset.proxyState.getRow$realm().getTable().getName();
        if (tableName != null ? !tableName.equals(otherTableName) : otherTableName != null) return false;

        if (proxyState.getRow$realm().getObjectKey() != aAsset.proxyState.getRow$realm().getObjectKey()) return false;

        return true;
    }
}
