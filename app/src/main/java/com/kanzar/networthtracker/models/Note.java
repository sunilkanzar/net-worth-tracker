package com.kanzar.networthtracker.models;

import com.kanzar.networthtracker.helpers.Tools;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class Note extends RealmObject {

    @PrimaryKey
    private String id;
    @Index
    private int month;
    @Index
    private int year;
    private String content;
    private long updatedAt;

    public Note() {
    }

    public Note(int month, int year, String content) {
        this.month = month;
        this.year = year;
        this.content = content;
        this.id = generateId(month, year);
        this.updatedAt = Tools.getUnixTime();
    }

    public static String generateId(int month, int year) {
        return "note_" + year + "_" + month;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "NOTE," + year + "," + month + "," + content.replace("\n", "\\n") + "\n";
    }

    public static Note fromString(String row) {
        try {
            if (!row.startsWith("NOTE,")) return null;
            String[] parts = row.split(",", 4);
            if (parts.length != 4) return null;

            int year = Integer.parseInt(parts[1].trim());
            int month = Integer.parseInt(parts[2].trim());
            String content = parts[3].trim().replace("\\n", "\n");

            return new Note(month, year, content);
        } catch (Exception e) {
            return null;
        }
    }
}
