package com.kanzar.networthtracker.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class NoteTag extends RealmObject {
    @PrimaryKey
    private String name;
    private int index;

    public NoteTag() {}

    public NoteTag(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
}
