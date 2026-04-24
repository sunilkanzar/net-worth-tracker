package com.kanzar.networthtracker.backup;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.kanzar.networthtracker.R;
import com.kanzar.networthtracker.eventbus.ImportedEvent;
import com.kanzar.networthtracker.eventbus.MessageEvent;
import com.kanzar.networthtracker.helpers.Tools;
import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.Goal;
import com.kanzar.networthtracker.models.Note;
import com.kanzar.networthtracker.statistics.Events;
import com.kanzar.networthtracker.statistics.events.ButtonClicked;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        
        File backupDir = new File(LocalBackup.getBackupFolder());
        File[] listFiles = backupDir.listFiles();

        if (listFiles == null || listFiles.length == 0) {
            EventBus.getDefault().post(new MessageEvent(activity.getString(R.string.import_no_backups)));
            return;
        }

        List<File> filesList = new ArrayList<>(Arrays.asList(listFiles));
        Collections.sort(filesList, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_backups, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.backupsList);
        TextView clearAll = dialogView.findViewById(R.id.clearAllBackups);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.import_choose_file)
                .setView(dialogView)
                .setNegativeButton(R.string.action_close, null)
                .create();

        BackupAdapter adapter = new BackupAdapter(activity, filesList);
        adapter.setListener(new BackupAdapter.OnBackupClickListener() {
            @Override
            public void onBackupClick(File file) {
                try {
                    importFromFile(new FileInputStream(file));
                    dialog.dismiss();
                } catch (Exception e) {
                    Log.e("LocalImport", "Import failed", e);
                }
            }

            @Override
            public void onDeleteClick(File file, int position) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.import_delete_confirm)
                        .setMessage(file.getName())
                        .setPositiveButton(R.string.menu_delete, (d, which) -> {
                            if (file.delete()) {
                                filesList.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(activity, R.string.import_delete_success, Toast.LENGTH_SHORT).show();
                                if (filesList.isEmpty()) {
                                    dialog.dismiss();
                                    EventBus.getDefault().post(new MessageEvent(activity.getString(R.string.import_no_backups)));
                                }
                            }
                        })
                        .setNegativeButton(R.string.backup_share_no, null)
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);

        clearAll.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle(R.string.import_clear_all)
                .setMessage(R.string.import_clear_all_confirm)
                .setPositiveButton(R.string.menu_delete, (d, which) -> {
                    for (File file : filesList) {
                        file.delete();
                    }
                    filesList.clear();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(activity, R.string.import_clear_success, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    EventBus.getDefault().post(new MessageEvent(activity.getString(R.string.import_no_backups)));
                })
                .setNegativeButton(R.string.backup_share_no, null)
                .show());

        Tools.styleDialog(dialog);
        dialog.show();
    }

    private static class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.ViewHolder> {
        private final Context context;
        private final List<File> files;
        private OnBackupClickListener listener;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

        interface OnBackupClickListener {
            void onBackupClick(File file);
            void onDeleteClick(File file, int position);
        }

        BackupAdapter(Context context, List<File> files) {
            this.context = context;
            this.files = files;
        }

        void setListener(OnBackupClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_backup, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = files.get(position);
            holder.name.setText(file.getName());
            
            String size = Tools.formatBytes(file.length());
            String date = dateFormat.format(new Date(file.lastModified()));
            holder.details.setText(String.format("%s  •  %s", date, size));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBackupClick(file);
            });
            holder.delete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(file, position);
            });
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, details;
            ImageView delete;

            ViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.backupName);
                details = view.findViewById(R.id.backupDetails);
                delete = view.findViewById(R.id.deleteBackup);
            }
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
