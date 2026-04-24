package com.kanzar.networthtracker.backup;

import android.content.Context;
import com.kanzar.networthtracker.CoreApplication;
import com.kanzar.networthtracker.eventbus.BackupSavedEvent;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.Goal;
import com.kanzar.networthtracker.models.Note;
import com.kanzar.networthtracker.statistics.Events;
import com.kanzar.networthtracker.statistics.events.ButtonClicked;
import com.kanzar.networthtracker.helpers.Prefs;
import io.realm.Realm;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;

import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
            } else {
                Prefs.save(Prefs.PREFS_LAST_AUTO_BACKUP_TIME, System.currentTimeMillis());
            }
        } catch (Exception e) {
            Log.e("LocalBackup", "Export failed", e);
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
        if (!auto) {
            return dateToString(new Date(), BACKUP_DATE_FORMAT) + ".NetWorthTracker.csv";
        }

        String frequency = Prefs.getString(Prefs.PREFS_AUTOSAVE_FREQUENCY, Prefs.DEFAULT_AUTOSAVE_FREQUENCY);
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        String period;

        switch (frequency) {
            case "weekly":
                period = year + "w" + now.get(Calendar.WEEK_OF_YEAR);
                break;
            case "monthly":
                period = year + "m" + (now.get(Calendar.MONTH) + 1);
                break;
            case "quarterly":
                period = year + "Q" + ((now.get(Calendar.MONTH) / 3) + 1);
                break;
            case "half_yearly":
                period = year + "H" + ((now.get(Calendar.MONTH) / 6) + 1);
                break;
            case "yearly":
                period = String.valueOf(year);
                break;
            case "daily":
            default:
                period = dateToString(new Date(), BACKUP_DATE_FORMAT);
                break;
        }

        return period + ".auto.NetWorthTracker.csv";
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
            
            RealmResults<Note> notes = realm.where(Note.class).findAll();
            for (Note note : notes) {
                sb.append(note.toString());
            }

            RealmResults<Goal> goals = realm.where(Goal.class).findAll();
            for (Goal goal : goals) {
                sb.append(goal.toString());
            }
        }
        return sb.toString();
    }
}
