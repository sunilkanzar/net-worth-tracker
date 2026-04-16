package com.kanzar.networthtracker.api.repositories;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    public Task<String> createFile(String fileName, String content) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList("appDataFolder"))
                    .setMimeType("application/json")
                    .setName(fileName);

            ByteArrayContent contentStream = ByteArrayContent.fromString("application/json", content);

            File googleFile = mDriveService.files().create(metadata, contentStream).execute();
            if (googleFile == null) {
                throw new IOException("Null result when creating file.");
            }

            return googleFile.getId();
        });
    }

    public Task<Void> updateFile(String fileId, String content) {
        return Tasks.call(mExecutor, () -> {
            ByteArrayContent contentStream = ByteArrayContent.fromString("application/json", content);
            mDriveService.files().update(fileId, null, contentStream).execute();
            return null;
        });
    }

    public Task<String> readFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            try (InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                return stringBuilder.toString();
            }
        });
    }

    public Task<Void> deleteFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            mDriveService.files().delete(fileId).execute();
            return null;
        });
    }

    public Task<String> searchFile(String fileName) {
        return Tasks.call(mExecutor, () -> {
            FileList result = mDriveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '" + fileName + "'")
                    .setFields("files(id, name)")
                    .execute();

            if (!result.getFiles().isEmpty()) {
                return result.getFiles().get(0).getId();
            }
            return null;
        });
    }
}
