package com.timed.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

public class User {
    @DocumentId
    private String uid;
    private String avatar;
    private String email;
    private String name;
    private String auth_provider;
    private Boolean email_verified;
    private String timezone;
    private Settings settings;

    @PropertyName("created_at")
    private Timestamp createdAt;

    @PropertyName("updated_at")
    private Timestamp updatedAt;

    public User() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthProvider() {
        return auth_provider;
    }

    public void setAuthProvider(String provider) {
        this.auth_provider = provider;
    }

    public Boolean getEmailVerified() {
        return email_verified;
    }

    public void setEmailVerified(Boolean email_verified) {
        this.email_verified = email_verified;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @PropertyName("created_at")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("created_at")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("updated_at")
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updated_at")
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class Settings {
        private String theme;
        private Notifications notifications;

        public Settings() {
        }

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }

        public Notifications getNotifications() {
            return notifications;
        }

        public void setNotifications(Notifications notifications) {
            this.notifications = notifications;
        }
    }

    public static class Notifications {
        private boolean email;
        private boolean push;

        @PropertyName("snooze_default_minutes")
        private int snoozeDefaultMinutes;

        public Notifications() {
        }

        public boolean isEmail() {
            return email;
        }

        public void setEmail(boolean email) {
            this.email = email;
        }

        public boolean isPush() {
            return push;
        }

        public void setPush(boolean push) {
            this.push = push;
        }

        @PropertyName("snooze_default_minutes")
        public int getSnoozeDefaultMinutes() {
            return snoozeDefaultMinutes;
        }

        @PropertyName("snooze_default_minutes")
        public void setSnoozeDefaultMinutes(int snoozeDefaultMinutes) {
            this.snoozeDefaultMinutes = snoozeDefaultMinutes;
        }
    }
}
