package com.timed.models;

public class ErrorResponse {
    public boolean success;
    public String error;
    public String message;

    public ErrorResponse() {
        this.success = false;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
