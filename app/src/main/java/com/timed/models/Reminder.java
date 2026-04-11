package com.timed.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

public class Reminder {
    @DocumentId
    private String id;
    
    @PropertyName("user_id")
    private String userId;
    
    private String title;
    private String description;
    
    @PropertyName("due_date")
    private Timestamp dueDate;
    
    private String priority; // "high", "medium", "low"
    private String category; // "work", "personal", "health", "other"
    
    @PropertyName("is_completed")
    private Boolean isCompleted;
    
    @PropertyName("notification_sent")
    private Boolean notificationSent;
    
    @PropertyName("notification_time_before")
    private Long notificationTimeBefore; // in minutes before due date
    
    private String status; // "pending", "completed", "cancelled"
    
    @PropertyName("created_at")
    private Timestamp createdAt;
    
    @PropertyName("updated_at")
    private Timestamp updatedAt;
    
    @PropertyName("completed_at")
    private Timestamp completedAt;

    public Reminder() {}

    public Reminder(String title, String description, Timestamp dueDate) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = "medium";
        this.category = "personal";
        this.isCompleted = false;
        this.notificationSent = false;
        this.notificationTimeBefore = 15L; // 15 minutes before
        this.status = "pending";
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @PropertyName("due_date")
    public Timestamp getDueDate() { return dueDate; }
    @PropertyName("due_date")
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @PropertyName("is_completed")
    public Boolean getIsCompleted() { return isCompleted; }
    @PropertyName("is_completed")
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }

    @PropertyName("notification_sent")
    public Boolean getNotificationSent() { return notificationSent; }
    @PropertyName("notification_sent")
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }

    @PropertyName("notification_time_before")
    public Long getNotificationTimeBefore() { return notificationTimeBefore; }
    @PropertyName("notification_time_before")
    public void setNotificationTimeBefore(Long notificationTimeBefore) { this.notificationTimeBefore = notificationTimeBefore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @PropertyName("created_at")
    public Timestamp getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @PropertyName("updated_at")
    public Timestamp getUpdatedAt() { return updatedAt; }
    @PropertyName("updated_at")
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @PropertyName("completed_at")
    public Timestamp getCompletedAt() { return completedAt; }
    @PropertyName("completed_at")
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }

    @Override
    public String toString() {
        return "Reminder{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", dueDate=" + dueDate +
                ", priority='" + priority + '\'' +
                ", category='" + category + '\'' +
                ", isCompleted=" + isCompleted +
                ", status='" + status + '\'' +
                '}';
    }
}
