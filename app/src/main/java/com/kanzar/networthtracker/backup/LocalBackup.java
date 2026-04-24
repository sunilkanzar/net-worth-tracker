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
    public static final String BACKUP_DATE_FORMAT = "yyyy-MM-dd";
    public static final String BACKUP_TIME_FORMAT = "HH-mm-ss";
    public static final String BACKUP_DATE_FORMAT_VISUAL = "dd.MM.yyyy";
    public static final String BACKUP_FILE = "Backup.csv";
    public static final String BACKUP_FOLDER = "NetWorthTracker";

    public enum ExportType {
        MANUAL, AUTO, SHARE
    }

    private LocalBackup() {
    }

    public static void startExport() {
        startExport(ExportType.MANUAL);
    }

    @Deprecated
    public static void startExport(boolean auto) {
        startExport(auto ? ExportType.AUTO : ExportType.MANUAL);
    }

    public static void startExport(ExportType type) {
        try {
            File folder;
            String fileName;
            boolean shareImmediately = false;

            if (type == ExportType.SHARE) {
                folder = CoreApplication.getContext().getCacheDir();
                fileName = "Export.csv";
                shareImmediately = true;
            } else {
                folder = new File(getBackupFolder());
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                fileName = getBackupName(type == ExportType.AUTO);
            }

            File file = new File(folder, fileName);
            if (file.exists()) {
                file.delete();
            }

            String transactionsAsString = getTransactionsAsString();
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                fileOutputStream.write(transactionsAsString.getBytes(StandardCharsets.UTF_8));
            }

            if (type == ExportType.AUTO) {
                Prefs.save(Prefs.PREFS_LAST_AUTO_BACKUP_TIME, System.currentTimeMillis());
            } else {
                new Events().send(new ButtonClicked(type == ExportType.SHARE ? "export_share" : "export_backup"));
                EventBus.getDefault().post(new BackupSavedEvent(file, shareImmediately));
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
        String dateStr = dateToString(new Date(), BACKUP_DATE_FORMAT);
        if (!auto) {
            String timeStr = dateToString(new Date(), BACKUP_TIME_FORMAT);
            return "Backup_Manual_" + dateStr + "_" + timeStr + ".csv";
        }

        String frequency = Prefs.getString(Prefs.PREFS_AUTOSAVE_FREQUENCY, Prefs.DEFAULT_AUTOSAVE_FREQUENCY);
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        String period;

        switch (frequency) {
            case "weekly":
                period = "Weekly_" + year + "-W" + now.get(Calendar.WEEK_OF_YEAR);
                break;
            case "monthly":
                period = "Monthly_" + year + "-" + String.format(Locale.ENGLISH, "%02d", (now.get(Calendar.MONTH) + 1));
                break;
            case "quarterly":
                period = "Quarterly_" + year + "-Q" + ((now.get(Calendar.MONTH) / 3) + 1);
                break;
            case "half_yearly":
                period = "HalfYearly_" + year + "-H" + ((now.get(Calendar.MONTH) / 6) + 1);
                break;
            case "yearly":
                period = "Yearly_" + year;
                break;
            case "daily":
            default:
                period = "Daily_" + dateStr;
                break;
        }

        return "Backup_Auto_" + period + ".csv";
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
