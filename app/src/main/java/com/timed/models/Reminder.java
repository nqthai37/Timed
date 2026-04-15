package com.timed.models;

import com.google.gson.annotations.SerializedName;

public class Reminder {
    @SerializedName("type")
    private String type;

    @SerializedName("value")
    private int value;

    @SerializedName("enabled")
    private boolean enabled;

    public Reminder() {
    }

    public Reminder(String type, int value, boolean enabled) {
        this.type = type;
        this.value = value;
        this.enabled = enabled;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "type='" + type + '\'' +
                ", value=" + value +
                ", enabled=" + enabled +
                '}';
    }
}
