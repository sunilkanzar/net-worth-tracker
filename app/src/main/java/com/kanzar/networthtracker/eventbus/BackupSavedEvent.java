package com.kanzar.networthtracker.eventbus;

import java.io.File;

public final class BackupSavedEvent {
    private final File file;

    public BackupSavedEvent(File file) {
        this.file = file;
    }

    public File getFile() {
        return this.file;
    }
}
