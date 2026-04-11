package com.timed;

public class Task {
    private String title;
    private String time;
    private String category;
    private boolean isCompleted;
    private String date; // e.g., "Today", "Tomorrow", "26"

    public Task(String title, String time, String category, boolean isCompleted, String date) {
        this.title = title;
        this.time = time;
        this.category = category;
        this.isCompleted = isCompleted;
        this.date = date;
    }

    public String getTitle() { return title; }
    public String getTime() { return time; }
    public String getCategory() { return category; }
    public boolean isCompleted() { return isCompleted; }
    public String getDate() { return date; }

    public void setCompleted(boolean completed) { isCompleted = completed; }
}