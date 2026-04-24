package com.kanzar.networthtracker.eventbus;

import java.io.File;

public final class BackupSavedEvent {
    private final File file;
    private final boolean shareImmediately;

    public BackupSavedEvent(File file) {
        this(file, false);
    }

    public BackupSavedEvent(File file, boolean shareImmediately) {
        this.file = file;
        this.shareImmediately = shareImmediately;
    }

    public File getFile() {
        return this.file;
    }

    public boolean isShareImmediately() {
        return shareImmediately;
    }
}
