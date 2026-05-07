package com.timed.models;

public class DeleteEventResponse {
    public boolean success;
    public String message;
    public String eventId;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getEventId() {
        return eventId;
    }
}
