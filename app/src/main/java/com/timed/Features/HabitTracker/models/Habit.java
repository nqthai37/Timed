package com.timed.Features.HabitTracker.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;
import java.util.Objects;

/**
 * Habit entity for Room Database.
 * Represents a habit that a user can track daily.
 */
@Entity(tableName = "habits")
public class Habit implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String description;
    private int iconDrawableId;
    private String color; // Hex color code for the habit
    
    // Recurrence pattern: "daily", "weekdays", "custom" or comma-separated days (0-6)
    private String recurrencePattern;
    
    // Notification hour (0-23), -1 for no notification
    private int notificationHour;
    
    private long createdAt;
    private long lastCompletedAt; // Timestamp of last completion
    private int currentStreak; // Current consecutive days completed
    private int longestStreak; // Longest streak achieved
    private int totalCompletions; // Total number of times completed
    
    // Flag for grace day feature (1 day allowed to break without reset)
    private boolean allowGraceDay;
    private long graceDaySpecifiedAt; // When grace day was used, 0 if not used
    
    private boolean isActive; // Soft delete
    
    public Habit() {
    }
    
    public Habit(String title, String description, int iconDrawableId, String color) {
        this.title = title;
        this.description = description;
        this.iconDrawableId = iconDrawableId;
        this.color = color;
        this.recurrencePattern = "daily";
        this.notificationHour = -1;
        this.createdAt = System.currentTimeMillis();
        this.lastCompletedAt = 0;
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.totalCompletions = 0;
        this.allowGraceDay = false;
        this.graceDaySpecifiedAt = 0;
        this.isActive = true;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getIconDrawableId() {
        return iconDrawableId;
    }
    
    public void setIconDrawableId(int iconDrawableId) {
        this.iconDrawableId = iconDrawableId;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getRecurrencePattern() {
        return recurrencePattern;
    }
    
    public void setRecurrencePattern(String recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
    }
    
    public int getNotificationHour() {
        return notificationHour;
    }
    
    public void setNotificationHour(int notificationHour) {
        this.notificationHour = notificationHour;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getLastCompletedAt() {
        return lastCompletedAt;
    }
    
    public void setLastCompletedAt(long lastCompletedAt) {
        this.lastCompletedAt = lastCompletedAt;
    }
    
    public int getCurrentStreak() {
        return currentStreak;
    }
    
    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }
    
    public int getLongestStreak() {
        return longestStreak;
    }
    
    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }
    
    public int getTotalCompletions() {
        return totalCompletions;
    }
    
    public void setTotalCompletions(int totalCompletions) {
        this.totalCompletions = totalCompletions;
    }
    
    public boolean isAllowGraceDay() {
        return allowGraceDay;
    }
    
    public void setAllowGraceDay(boolean allowGraceDay) {
        this.allowGraceDay = allowGraceDay;
    }
    
    public long getGraceDaySpecifiedAt() {
        return graceDaySpecifiedAt;
    }
    
    public void setGraceDaySpecifiedAt(long graceDaySpecifiedAt) {
        this.graceDaySpecifiedAt = graceDaySpecifiedAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Habit habit = (Habit) o;
        return id == habit.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Habit{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", currentStreak=" + currentStreak +
                ", totalCompletions=" + totalCompletions +
                '}';
    }
}
