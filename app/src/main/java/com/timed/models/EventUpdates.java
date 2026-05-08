package com.timed.models;

public class EventUpdates {
    public String summary;
    public String description;
    public String location;

    public EventUpdates() {
    }

    public EventUpdates(String summary, String description, String location) {
        this.summary = summary;
        this.description = description;
        this.location = location;
    }
}
