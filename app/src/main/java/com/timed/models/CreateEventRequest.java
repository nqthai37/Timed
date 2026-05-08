package com.timed.models;

import java.util.List;

public class CreateEventRequest {
    public String summary;
    public String startTime;      // ISO 8601 format
    public String endTime;        // ISO 8601 format
    public String userRefreshToken;
    public String description;
    public String location;
    public List<String> attendees;

    public CreateEventRequest(String summary, String startTime, String endTime, String userRefreshToken) {
        this.summary = summary;
        this.startTime = startTime;
        this.endTime = endTime;
        this.userRefreshToken = userRefreshToken;
        this.description = "";
        this.location = "";
        this.attendees = new java.util.ArrayList<>();
    }

    public CreateEventRequest(String summary, String startTime, String endTime, String userRefreshToken,
                            String description, String location, List<String> attendees) {
        this.summary = summary;
        this.startTime = startTime;
        this.endTime = endTime;
        this.userRefreshToken = userRefreshToken;
        this.description = description != null ? description : "";
        this.location = location != null ? location : "";
        this.attendees = attendees != null ? attendees : new java.util.ArrayList<>();
    }
}
