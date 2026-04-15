package com.kanzar.networthtracker.statistics.events;

import com.google.gson.Gson;
import org.json.JSONObject;

public class BaseEvent {
    public final JSONObject asJSONObject() {
        try {
            return new JSONObject(new Gson().toJson(this));
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
