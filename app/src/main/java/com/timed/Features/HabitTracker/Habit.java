package com.timed.Features.HabitTracker; // Nhớ đổi đúng package của bạn

public class Habit {
    private String title;
    private String subtitle;
    private int iconDrawableId;
    private boolean isCompleted;

    public Habit(String title, String subtitle, int iconDrawableId, boolean isCompleted) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconDrawableId = iconDrawableId;
        this.isCompleted = isCompleted;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getIconDrawableId() { return iconDrawableId; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}