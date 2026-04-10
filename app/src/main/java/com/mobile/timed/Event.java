package com.mobile.timed;

public class Event {
    private String time;
    private String title;
    private String details;

    public Event(String time, String title, String details) {
        this.time = time;
        this.title = title;
        this.details = details;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
