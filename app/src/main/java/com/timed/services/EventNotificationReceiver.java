package com.timed.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.timed.managers.EventsNotificationManager;
import com.timed.repositories.EventsRepository;

public class EventNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "EventNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String eventId = intent.getStringExtra("event_id");
        String action = intent.getAction();

        Log.d(TAG, "Received action: " + action + ", EventId: " + eventId);

        switch (action) {
            case "com.timed.EVENT_REMINDER":
                handleEventReminder(context, intent);
                break;
            case "com.timed.EVENT_ACCEPT":
                handleEventAccept(context, eventId);
                break;
            case "com.timed.EVENT_DECLINE":
                handleEventDecline(context, eventId);
                break;
        }
    }

    /**
     * Handle event reminder - show notification
     */
    private void handleEventReminder(Context context, Intent intent) {
        String eventId = intent.getStringExtra("event_id");
        String title = intent.getStringExtra("event_title");
        String description = intent.getStringExtra("event_description");
        String location = intent.getStringExtra("event_location");
        String reminderType = intent.getStringExtra("reminder_type");
        Long minutesBefore = intent.getLongExtra("reminder_minutes_before", 0);

        Log.d(TAG, "Event reminder triggered for: " + title + 
                " (reminder_type: " + reminderType + ", minutes_before: " + minutesBefore + ")");

        EventsNotificationManager notificationManager = new EventsNotificationManager(context);
        
        if (title != null) {
            notificationManager.showEventReminder(eventId, title, description, location, reminderType);
        }
    }

    /**
     * Handle event acceptance
     */
    private void handleEventAccept(Context context, String eventId) {
        Log.d(TAG, "Event accepted: " + eventId);

        EventsRepository repository = new EventsRepository();
        repository.getEventById(eventId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.timed.models.Event event = documentSnapshot.toObject(com.timed.models.Event.class);
                        if (event != null) {
                            // Update participant status to "accepted"
                            String userId = getUserId(context);
                            if (userId != null && event.getParticipantStatus() != null) {
                                event.getParticipantStatus().put(userId, "accepted");
                                repository.updateEvent(eventId, event)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Event accepted status updated");
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get event for acceptance: " + e.getMessage());
                });

        EventsNotificationManager notificationManager = new EventsNotificationManager(context);
        notificationManager.cancelNotification(eventId);
    }

    /**
     * Handle event decline
     */
    private void handleEventDecline(Context context, String eventId) {
        Log.d(TAG, "Event declined: " + eventId);

        EventsRepository repository = new EventsRepository();
        repository.getEventById(eventId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.timed.models.Event event = documentSnapshot.toObject(com.timed.models.Event.class);
                        if (event != null) {
                            // Update participant status to "declined"
                            String userId = getUserId(context);
                            if (userId != null && event.getParticipantStatus() != null) {
                                event.getParticipantStatus().put(userId, "declined");
                                repository.updateEvent(eventId, event)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Event declined status updated");
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get event for decline: " + e.getMessage());
                });

        EventsNotificationManager notificationManager = new EventsNotificationManager(context);
        notificationManager.cancelNotification(eventId);
    }

    /**
     * Get current user ID from Firebase Auth or SharedPreferences
     */
    private String getUserId(Context context) {
        try {
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                return currentUser.getUid();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting user ID: " + e.getMessage());
        }
        return null;
    }
}
