package com.kanzar.networthtracker.statistics.events;

import com.google.gson.annotations.Expose;

public final class PermissionNotGranted extends BaseEvent {

    @Expose
    private final int source;

    public PermissionNotGranted(int source) {
        this.source = source;
    }
}
