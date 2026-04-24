package com.kanzar.networthtracker.eventbus;

import com.kanzar.networthtracker.models.Asset;
import com.kanzar.networthtracker.models.Goal;
import com.kanzar.networthtracker.models.Note;
import java.util.ArrayList;

public final class ImportedEvent {
    private final ArrayList<Asset> assets;
    private final ArrayList<Note> notes;
    private final ArrayList<Goal> goals;

    public ImportedEvent(ArrayList<Asset> assets, ArrayList<Note> notes, ArrayList<Goal> goals) {
        this.assets = assets;
        this.notes = notes;
        this.goals = goals;
    }

    public ArrayList<Asset> getAssets() {
        return this.assets;
    }

    public ArrayList<Note> getNotes() {
        return this.notes;
    }

    public ArrayList<Goal> getGoals() {
        return this.goals;
    }
}
