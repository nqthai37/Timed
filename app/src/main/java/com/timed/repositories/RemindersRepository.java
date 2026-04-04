package com.timed.repositories;

import androidx.annotation.NonNull;

import com.timed.models.Reminder;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class RemindersRepository {
    private final FirebaseFirestore db;
    private static final String REMINDERS_COLLECTION = "reminders";

    public RemindersRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new reminder
     */
    public Task<Void> createReminder(Reminder reminder) {
        return db.collection(REMINDERS_COLLECTION)
                .document(reminder.getId())
                .set(reminder);
    }

    /**
     * Get all reminders for a specific user
     */
    public Task<QuerySnapshot> getRemindersByUserId(String userId) {
        return db.collection(REMINDERS_COLLECTION)
                .whereEqualTo("user_id", userId)
                .orderBy("due_date")
                .get();
    }

    /**
     * Get pending reminders for a user
     */
    public Task<QuerySnapshot> getPendingRemindersByUserId(String userId) {
        return db.collection(REMINDERS_COLLECTION)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("status", "pending")
                .orderBy("due_date")
                .get();
    }

    /**
     * Get completed reminders for a user
     */
    public Task<QuerySnapshot> getCompletedRemindersByUserId(String userId) {
        return db.collection(REMINDERS_COLLECTION)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("status", "completed")
                .orderBy("updated_at")
                .get();
    }

    /**
     * Get reminders by category for a user
     */
    public Task<QuerySnapshot> getRemindersByCategory(String userId, String category) {
        return db.collection(REMINDERS_COLLECTION)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("category", category)
                .orderBy("due_date")
                .get();
    }

    /**
     * Get reminders by priority for a user
     */
    public Task<QuerySnapshot> getRemindersByPriority(String userId, String priority) {
        return db.collection(REMINDERS_COLLECTION)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("priority", priority)
                .orderBy("due_date")
                .get();
    }

    /**
     * Update an existing reminder
     */
    public Task<Void> updateReminder(String reminderId, Reminder reminder) {
        reminder.setUpdatedAt(com.google.firebase.Timestamp.now());
        return db.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .set(reminder);
    }

    /**
     * Mark a reminder as completed
     */
    public Task<Void> markAsCompleted(String reminderId) {
        return db.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .update(
                        "is_completed", true,
                        "status", "completed",
                        "completed_at", com.google.firebase.Timestamp.now(),
                        "updated_at", com.google.firebase.Timestamp.now()
                );
    }

    /**
     * Mark a reminder as pending
     */
    public Task<Void> markAsPending(String reminderId) {
        return db.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .update(
                        "is_completed", false,
                        "status", "pending",
                        "completed_at", null,
                        "updated_at", com.google.firebase.Timestamp.now()
                );
    }

    /**
     * Mark notification as sent
     */
    public Task<Void> markNotificationAsSent(String reminderId) {
        return db.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .update(
                        "notification_sent", true,
                        "updated_at", com.google.firebase.Timestamp.now()
                );
    }

    /**
     * Delete a reminder
     */
    public Task<Void> deleteReminder(String reminderId) {
        return db.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .delete();
    }

    /**
     * Get a reminder by ID
     */
    public Task<com.google.firebase.firestore.DocumentSnapshot> getReminderById(String reminderId) {
        return db.collection(REMINDERS_COLLECTION)
                .document(reminderId)
                .get();
    }

    /**
     * Get all reminders that need notification
     */
    public Task<QuerySnapshot> getRemindersNeedingNotification(String userId) {
        return db.collection(REMINDERS_COLLECTION)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("notification_sent", false)
                .whereEqualTo("is_completed", false)
                .get();
    }
}
