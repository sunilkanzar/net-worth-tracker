package com.kanzar.networthtracker.statistics.events;

import com.google.gson.annotations.Expose;

public final class ButtonClicked extends BaseEvent {

    @Expose
    private final String button;

    public ButtonClicked(String button) {
        this.button = button;
    }
}
