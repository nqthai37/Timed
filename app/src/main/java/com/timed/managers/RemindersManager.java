package com.timed.managers;

import android.content.Context;
import android.util.Log;

import com.timed.models.Reminder;
import com.timed.repositories.RemindersRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class RemindersManager {
    private static RemindersManager instance;
    private final RemindersRepository remindersRepository;
    private final ReminderNotificationManager notificationManager;
    private final Context context;
    private static final String TAG = "RemindersManager";

    private RemindersManager(Context context) {
        this.context = context;
        this.remindersRepository = new RemindersRepository();
        this.notificationManager = new ReminderNotificationManager(context);
    }

    public static synchronized RemindersManager getInstance(Context context) {
        if (instance == null) {
            instance = new RemindersManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Create a new reminder and schedule notification
     */
    public Task<DocumentReference> createReminder(String userId, String title, String description, 
                                                   Date dueDate, String priority, String category) {
        Reminder reminder = new Reminder();
        reminder.setId(UUID.randomUUID().toString());
        reminder.setUserId(userId);
        reminder.setTitle(title);
        reminder.setDescription(description);
        reminder.setDueDate(new Timestamp(dueDate));
        reminder.setPriority(priority != null ? priority : "medium");
        reminder.setCategory(category != null ? category : "personal");
        reminder.setIsCompleted(false);
        reminder.setNotificationSent(false);
        reminder.setNotificationTimeBefore(15L);
        reminder.setStatus("pending");
        reminder.setCreatedAt(Timestamp.now());
        reminder.setUpdatedAt(Timestamp.now());

        final String reminderId = reminder.getId();
        return remindersRepository.createReminder(reminder)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reminder created successfully: " + reminderId);
                    // Schedule the notification
                    notificationManager.scheduleReminder(reminder);
                })
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        DocumentReference docRef = 
                                FirebaseFirestore.getInstance()
                                .collection("reminders")
                                .document(reminderId);
                        return Tasks.forResult(docRef);
                    }
                    return Tasks.forException(task.getException());
                });
    }

    /**
     * Get all reminders for the current user
     */
    public Task<List<Reminder>> getAllReminders(String userId) {
        return remindersRepository.getRemindersByUserId(userId)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Reminder> reminders = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Reminder reminder = doc.toObject(Reminder.class);
                        reminder.setId(doc.getId());
                        reminders.add(reminder);
                    }
                    return reminders;
                });
    }

    /**
     * Get pending reminders for the current user
     */
    public Task<List<Reminder>> getPendingReminders(String userId) {
        return remindersRepository.getPendingRemindersByUserId(userId)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Reminder> reminders = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Reminder reminder = doc.toObject(Reminder.class);
                        reminder.setId(doc.getId());
                        reminders.add(reminder);
                    }
                    return reminders;
                });
    }

    /**
     * Get completed reminders for the current user
     */
    public Task<List<Reminder>> getCompletedReminders(String userId) {
        return remindersRepository.getCompletedRemindersByUserId(userId)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Reminder> reminders = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Reminder reminder = doc.toObject(Reminder.class);
                        reminder.setId(doc.getId());
                        reminders.add(reminder);
                    }
                    return reminders;
                });
    }

    /**
     * Get reminders by category
     */
    public Task<List<Reminder>> getRemindersByCategory(String userId, String category) {
        return remindersRepository.getRemindersByCategory(userId, category)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Reminder> reminders = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Reminder reminder = doc.toObject(Reminder.class);
                        reminder.setId(doc.getId());
                        reminders.add(reminder);
                    }
                    return reminders;
                });
    }

    /**
     * Get reminders by priority
     */
    public Task<List<Reminder>> getRemindersByPriority(String userId, String priority) {
        return remindersRepository.getRemindersByPriority(userId, priority)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Reminder> reminders = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Reminder reminder = doc.toObject(Reminder.class);
                        reminder.setId(doc.getId());
                        reminders.add(reminder);
                    }
                    return reminders;
                });
    }

    /**
     * Update a reminder
     */
    public Task<Void> updateReminder(String reminderId, Reminder reminder) {
        reminder.setUpdatedAt(Timestamp.now());
        return remindersRepository.updateReminder(reminderId, reminder)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reminder updated successfully: " + reminderId);
                    // Reschedule the notification
                    notificationManager.cancelReminder(reminderId);
                    notificationManager.scheduleReminder(reminder);
                });
    }

    /**
     * Mark a reminder as completed
     */
    public Task<Void> markReminderAsCompleted(String reminderId) {
        return remindersRepository.markAsCompleted(reminderId)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reminder marked as completed: " + reminderId);
                    notificationManager.cancelReminder(reminderId);
                    notificationManager.cancelNotification(reminderId);
                });
    }

    /**
     * Mark a reminder as pending
     */
    public Task<Void> markReminderAsPending(String reminderId) {
        return remindersRepository.markAsPending(reminderId)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reminder marked as pending: " + reminderId);
                    // Fetch and reschedule
                    remindersRepository.getReminderById(reminderId)
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Reminder reminder = documentSnapshot.toObject(Reminder.class);
                                    if (reminder != null) {
                                        notificationManager.scheduleReminder(reminder);
                                    }
                                }
                            });
                });
    }

    /**
     * Delete a reminder
     */
    public Task<Void> deleteReminder(String reminderId) {
        notificationManager.cancelReminder(reminderId);
        notificationManager.cancelNotification(reminderId);
        return remindersRepository.deleteReminder(reminderId)
                .addOnSuccessListener(aVoid -> 
                        Log.d(TAG, "Reminder deleted successfully: " + reminderId));
    }

    /**
     * Get reminder by ID
     */
    public Task<Reminder> getReminderById(String reminderId) {
        return remindersRepository.getReminderById(reminderId)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    Reminder reminder = task.getResult().toObject(Reminder.class);
                    if (reminder != null) {
                        reminder.setId(task.getResult().getId());
                    }
                    return reminder;
                });
    }

    /**
     * Reschedule all pending reminders (call on app startup)
     */
    public void reschedulePendingReminders(String userId) {
        getPendingReminders(userId)
                .addOnSuccessListener(reminders -> {
                    Log.d(TAG, "Rescheduling " + reminders.size() + " pending reminders");
                    notificationManager.rescheduleAllReminders(reminders);
                })
                .addOnFailureListener(e -> 
                        Log.e(TAG, "Failed to reschedule reminders: " + e.getMessage()));
    }
}
