package com.kanzar.networthtracker.backup;

import android.content.Context;
import com.kanzar.networthtracker.CoreApplication;
import com.kanzar.networthtracker.eventbus.BackupSavedEvent;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.statistics.Events;
import com.kanzar.networthtracker.statistics.events.ButtonClicked;
import io.realm.Realm;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LocalBackup {
    public static final String BACKUP_DATE_FORMAT = "yyyyMMdd";
    public static final String BACKUP_DATE_FORMAT_VISUAL = "dd.MM.yyyy";
    public static final String BACKUP_FILE = "NetWorthTracker.csv";
    public static final String BACKUP_FOLDER = "NetWorthTracker";

    private LocalBackup() {
    }

    public static void startExport() {
        startExport(false);
    }

    public static void startExport(boolean auto) {
        try {
            File folder = new File(getBackupFolder());
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(folder, getBackupName(auto));
            if (file.exists()) {
                file.delete();
            }

            String transactionsAsString = getTransactionsAsString();
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                fileOutputStream.write(transactionsAsString.getBytes(StandardCharsets.UTF_8));
            }

            if (!auto) {
                new Events().send(new ButtonClicked("export"));
                EventBus.getDefault().post(new BackupSavedEvent(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getBackupFolder() {
        Context ctx = CoreApplication.getContext();
        File externalFilesDir = ctx.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            return ctx.getFilesDir() + "/" + BACKUP_FOLDER;
        }
        return externalFilesDir + "/" + BACKUP_FOLDER;
    }

    private static String getBackupName(boolean auto) {
        String dateStr = dateToString(new Date(), BACKUP_DATE_FORMAT);
        if (auto) {
            dateStr += ".auto";
        }
        return dateStr + ".NetWorthTracker.csv";
    }

    private static String dateToString(Date date, String format) {
        return new SimpleDateFormat(format, Locale.ENGLISH).format(date);
    }

    private static String getTransactionsAsString() {
        StringBuilder sb = new StringBuilder();
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Asset> assets = realm.where(Asset.class).findAll();
            for (Asset asset : assets) {
                sb.append(asset.toString());
            }
        }
        return sb.toString();
    }
}
