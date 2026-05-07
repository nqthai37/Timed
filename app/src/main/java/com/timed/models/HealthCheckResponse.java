package com.timed.models;

public class HealthCheckResponse {
    public String status;
    public String timestamp;
    public String message;

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}
