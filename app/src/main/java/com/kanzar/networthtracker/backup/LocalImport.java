package com.kanzar.networthtracker.backup;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.eventbus.ImportedEvent;
import com.kanzar.networthtracker.eventbus.MessageEvent;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.Goal;
import com.kanzar.networthtracker.models.Note;
import com.kanzar.networthtracker.statistics.Events;
import com.kanzar.networthtracker.statistics.events.ButtonClicked;
import com.kanzar.networthtracker.helpers.Tools;
import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class LocalImport {

    private LocalImport() {
    }

    public static void importFromUri(android.net.Uri uri) throws Exception {
        importFromFile(com.kanzar.networthtracker.CoreApplication.getContext().getContentResolver().openInputStream(uri));
    }

    public static void startImport(Activity activity) {
        new Events().send(new ButtonClicked("import"));
        final List<File> files = new ArrayList<>();
        List<String> fileDisplayNames = new ArrayList<>();
        
        File backupDir = new File(LocalBackup.getBackupFolder());
        File[] listFiles = backupDir.listFiles();

        if (listFiles == null || listFiles.length == 0) {
            EventBus.getDefault().post(new MessageEvent(activity.getString(R.string.import_no_backups)));
            return;
        }

        for (File file : listFiles) {
            String name = file.getName();
            String[] parts = name.split("\\.");
            
            // Format: yyyyMMdd.NetWorthTracker.csv or yyyyMMdd.auto.NetWorthTracker.csv
            boolean isNormalBackup = parts.length == 3 && parts[1].equalsIgnoreCase(LocalBackup.BACKUP_FOLDER);
            boolean isAutoBackup = parts.length == 4 && parts[1].equalsIgnoreCase("auto") && parts[2].equalsIgnoreCase(LocalBackup.BACKUP_FOLDER);

            if (isNormalBackup || isAutoBackup) {
                Date date = stringToDate(parts[0], LocalBackup.BACKUP_DATE_FORMAT);
                if (date != null) {
                    String displayName = dateToString(date, LocalBackup.BACKUP_DATE_FORMAT_VISUAL);
                    if (isAutoBackup) {
                        displayName += " " + activity.getString(R.string.backup_autosave);
                    }
                    fileDisplayNames.add(displayName);
                    files.add(file);
                }
            }
        }

        if (fileDisplayNames.isEmpty()) {
            EventBus.getDefault().post(new MessageEvent(activity.getString(R.string.import_no_backups)));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.import_choose_file);
        builder.setItems(fileDisplayNames.toArray(new CharSequence[0]), (dialog, which) -> {
            try {
                importFromFile(new FileInputStream(files.get(which)));
            } catch (Exception e) {
                Log.e("LocalImport", "Import failed", e);
            }
        });
        AlertDialog dialog = builder.create();
        Tools.styleDialog(dialog);
        dialog.show();
    }

    private static String dateToString(Date date, String format) {
        return new SimpleDateFormat(format, Locale.ENGLISH).format(date);
    }

    private static Date stringToDate(String date, String format) {
        try {
            return new SimpleDateFormat(format, Locale.ENGLISH).parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    public static void importFromFile(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<Asset> assets = new ArrayList<>();
            List<Note> notes = new ArrayList<>();
            List<Goal> goals = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("NOTE,")) {
                    Note note = Note.fromString(line);
                    if (note != null) notes.add(note);
                } else if (line.startsWith("GOAL,")) {
                    Goal goal = Goal.fromString(line);
                    if (goal != null) goals.add(goal);
                } else {
                    Asset asset = Asset.fromString(line);
                    if (asset != null) assets.add(asset);
                }
            }
            EventBus.getDefault().post(new ImportedEvent(new ArrayList<>(assets), new ArrayList<>(notes), new ArrayList<>(goals)));
        } catch (IOException e) {
            Log.e("LocalImport", "Read failed", e);
        }
    }
}
