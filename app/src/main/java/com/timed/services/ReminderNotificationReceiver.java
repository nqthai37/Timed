package com.timed.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.timed.managers.ReminderNotificationManager;
import com.timed.repositories.RemindersRepository;

public class ReminderNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String reminderId = intent.getStringExtra("reminder_id");
        String title = intent.getStringExtra("reminder_title");
        String description = intent.getStringExtra("reminder_description");
        String action = intent.getAction();

        Log.d(TAG, "Received action: " + action + ", ReminderId: " + reminderId);

        ReminderNotificationManager notificationManager = new ReminderNotificationManager(context);

        if (action.equals("com.timed.REMINDER_NOTIFICATION")) {
            // Show the notification
            if (title != null && description != null) {
                notificationManager.showNotification(reminderId, title, description);
            }

            // Mark notification as sent in Firebase
            RemindersRepository repository = new RemindersRepository();
            repository.markNotificationAsSent(reminderId)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Notification marked as sent for reminder: " + reminderId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to mark notification as sent: " + e.getMessage());
                    });

        } else if (action.equals("com.timed.REMINDER_COMPLETE")) {
            // Mark reminder as completed
            RemindersRepository repository = new RemindersRepository();
            repository.markAsCompleted(reminderId)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Reminder marked as completed: " + reminderId);
                        notificationManager.cancelNotification(reminderId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to mark reminder as completed: " + e.getMessage());
                    });
        }
    }
}
