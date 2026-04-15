package com.timed.Features.FocusMode;

import com.google.firebase.firestore.Exclude;

public class FocusPreset {
    private String id;
    private String title;
    private String description;
    private int minutes;
    private int seconds;
    private String iconName;

    public FocusPreset() {}

    public FocusPreset(String id, String title, String description, int minutes, int seconds, String iconName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.minutes = minutes;
        this.seconds = seconds;
        this.iconName = iconName;
    }

    @Exclude
    public String getId() { return id; }

    @Exclude
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getMinutes() { return minutes; }
    public int getSeconds() { return seconds; }
    public String getIconName() { return iconName; }
}