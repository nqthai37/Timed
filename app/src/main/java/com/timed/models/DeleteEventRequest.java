package com.timed.models;

public class DeleteEventRequest {
    public String eventId;
    public String userRefreshToken;

    public DeleteEventRequest(String eventId, String userRefreshToken) {
        this.eventId = eventId;
        this.userRefreshToken = userRefreshToken;
    }
}
