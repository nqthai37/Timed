package com.timed.Features.HabitTracker.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import java.io.Serializable;

/**
 * Represents a single day's completion record for a habit.
 * Tracks when a habit was completed on a specific date.
 */
@Entity(
    tableName = "habit_completions",
    foreignKeys = @ForeignKey(
        entity = Habit.class,
        parentColumns = "id",
        childColumns = "habitId",
        onDelete = ForeignKey.CASCADE
    )
)
public class HabitCompletion implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int habitId; // Foreign key to habits table
    private long completedDate; // Date (milliseconds since epoch, at start of day)
    private long completedAt; // Exact timestamp of completion
    private boolean markedAsComplete; // User explicitly marked as complete
    
    public HabitCompletion() {
    }
    
    public HabitCompletion(int habitId, long completedDate, long completedAt) {
        this.habitId = habitId;
        this.completedDate = completedDate;
        this.completedAt = completedAt;
        this.markedAsComplete = true;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getHabitId() {
        return habitId;
    }
    
    public void setHabitId(int habitId) {
        this.habitId = habitId;
    }
    
    public long getCompletedDate() {
        return completedDate;
    }
    
    public void setCompletedDate(long completedDate) {
        this.completedDate = completedDate;
    }
    
    public long getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }
    
    public boolean isMarkedAsComplete() {
        return markedAsComplete;
    }
    
    public void setMarkedAsComplete(boolean markedAsComplete) {
        this.markedAsComplete = markedAsComplete;
    }
    
    @Override
    public String toString() {
        return "HabitCompletion{" +
                "id=" + id +
                ", habitId=" + habitId +
                ", completedDate=" + completedDate +
                ", markedAsComplete=" + markedAsComplete +
                '}';
    }
}
