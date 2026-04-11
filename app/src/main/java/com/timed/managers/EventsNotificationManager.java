package com.timed.managers;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.timed.models.Event;
import com.timed.services.EventNotificationReceiver;

import java.util.Date;

public class EventsNotificationManager {
    private final Context context;
    private final NotificationManager notificationManager;
    private static final String CHANNEL_ID = "events_reminders_channel";
    private static final String CHANNEL_NAME = "Event Reminders";
    private static final int NOTIFICATION_ID_BASE = 2000;
    private static final String TAG = "EventsNotificationManager";

    public EventsNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    /**
     * Create notification channel for Android 8.0 and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for event reminders");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Schedule all reminders for an event
     */
    public void scheduleEventReminders(Event event) {
        if (event == null || event.getStartTime() == null) {
            Log.w(TAG, "Event or start time is null");
            return;
        }

        long eventStartTimeMillis = event.getStartTime().toDate().getTime();

        if (event.getReminders() != null && !event.getReminders().isEmpty()) {
            for (int i = 0; i < event.getReminders().size(); i++) {
                Event.EventReminder reminder = event.getReminders().get(i);
                
                if (reminder.getMinutesBefore() == null) {
                    continue;
                }

                long notificationTimeMillis = eventStartTimeMillis - 
                        (reminder.getMinutesBefore() * 60 * 1000);

                // Don't schedule if notification time is in the past
                if (notificationTimeMillis < System.currentTimeMillis()) {
                    Log.d(TAG, "Skipping reminder in the past for event: " + event.getId());
                    continue;
                }

                scheduleReminderAtTime(event, reminder, i, notificationTimeMillis);
            }
        }
    }

    /**
     * Schedule a single reminder for a specific time
     */
    private void scheduleReminderAtTime(Event event, Event.EventReminder reminder, 
                                        int reminderIndex, long notificationTimeMillis) {
        Intent intent = new Intent(context, EventNotificationReceiver.class);
        intent.setAction("com.timed.EVENT_REMINDER");
        intent.putExtra("event_id", event.getId());
        intent.putExtra("event_title", event.getTitle());
        intent.putExtra("event_description", event.getDescription());
        intent.putExtra("event_location", event.getLocation());
        intent.putExtra("reminder_type", reminder.getType());
        intent.putExtra("reminder_minutes_before", reminder.getMinutesBefore());
        intent.putExtra("reminder_index", reminderIndex);

        int requestCode = (event.getId().hashCode() + reminderIndex) % 20000;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = 
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        notificationTimeMillis,
                        pendingIntent
                );
                Log.d(TAG, "Scheduled reminder for event: " + event.getId() + 
                        " at " + new Date(notificationTimeMillis));
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when scheduling reminder: " + e.getMessage());
            }
        }
    }

    /**
     * Cancel all reminders for an event
     */
    public void cancelEventReminders(Event event) {
        if (event == null || event.getReminders() == null) {
            return;
        }

        for (int i = 0; i < event.getReminders().size(); i++) {
            cancelReminderAtIndex(event.getId(), i);
        }
    }

    /**
     * Cancel a reminder by index
     */
    private void cancelReminderAtIndex(String eventId, int reminderIndex) {
        Intent intent = new Intent(context, EventNotificationReceiver.class);
        intent.setAction("com.timed.EVENT_REMINDER");

        int requestCode = (eventId.hashCode() + reminderIndex) % 20000;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = 
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "Cancelled reminder for event: " + eventId);
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling reminder: " + e.getMessage());
            }
        }
    }

    /**
     * Show event reminder notification
     */
    public void showEventReminder(String eventId, String title, String description, 
                                  String location, String reminderType) {
        String contentText = description;
        if (location != null && !location.isEmpty()) {
            contentText = "📍 " + location + "\n" + description;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVibrate(new long[]{0, 200, 200, 200})
                .setLights(0xFF0099FF, 1000, 1000);

        int notificationId = NOTIFICATION_ID_BASE + (int) (eventId.hashCode() % 10000);
        notificationManager.notify(notificationId, builder.build());
        
        Log.d(TAG, "Showed notification for event: " + eventId);
    }

    /**
     * Cancel a notification
     */
    public void cancelNotification(String eventId) {
        int notificationId = NOTIFICATION_ID_BASE + (int) (eventId.hashCode() % 10000);
        notificationManager.cancel(notificationId);
    }

    /**
     * Show urgent event notification with action buttons
     */
    public void showUrgentEventNotification(String eventId, String title, String description) {
        Intent acceptIntent = new Intent(context, EventNotificationReceiver.class);
        acceptIntent.setAction("com.timed.EVENT_ACCEPT");
        acceptIntent.putExtra("event_id", eventId);

        int requestCodeAccept = (eventId.hashCode() + 1) % 20000;
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeAccept,
                acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent declineIntent = new Intent(context, EventNotificationReceiver.class);
        declineIntent.setAction("com.timed.EVENT_DECLINE");
        declineIntent.putExtra("event_id", eventId);

        int requestCodeDecline = (eventId.hashCode() + 2) % 20000;
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeDecline,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(description)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .addAction(0, "Accept", acceptPendingIntent)
                .addAction(0, "Decline", declinePendingIntent)
                .setVibrate(new long[]{0, 200, 200, 200})
                .setLights(0xFF0099FF, 1000, 1000);

        int notificationId = NOTIFICATION_ID_BASE + (int) (eventId.hashCode() % 10000);
        notificationManager.notify(notificationId, builder.build());
    }
}
