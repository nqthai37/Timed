package com.timed.models;

public class UpdateEventRequest {
    public String eventId;
    public String userRefreshToken;
    public EventUpdates updates;

    public UpdateEventRequest(String eventId, String userRefreshToken, EventUpdates updates) {
        this.eventId = eventId;
        this.userRefreshToken = userRefreshToken;
        this.updates = updates;
    }
}
