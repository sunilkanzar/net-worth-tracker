package com.kanzar.networthtracker.statistics;

import com.amplitude.api.Amplitude;
import com.amplitude.api.Identify;
import com.kanzar.networthtracker.statistics.events.BaseEvent;

public final class Events {

    public enum Property {
        numberOfAssets
    }

    public void send(BaseEvent event) {
        if (event == null) return;
        Amplitude.getInstance().logEvent(event.getClass().getSimpleName(), event.asJSONObject());
    }

    public void setProperty(String key, String value) {
        if (key == null || value == null) return;
        Amplitude.getInstance().identify(new Identify().unset(key));
        Amplitude.getInstance().identify(new Identify().append(key, value));
    }
}
