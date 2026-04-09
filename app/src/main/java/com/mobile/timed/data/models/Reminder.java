package com.mobile.timed.data.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Represents a reminder for an event
 */
public class Reminder implements Serializable {
    @SerializedName("type")
    private String type; // e.g., "minutes", "hours", "days"

    @SerializedName("value")
    private int value; // e.g., 15 minutes before

    @SerializedName("notification_enabled")
    private boolean notificationEnabled;

    public Reminder() {}

    public Reminder(String type, int value, boolean notificationEnabled) {
        this.type = type;
        this.value = value;
        this.notificationEnabled = notificationEnabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }
}

