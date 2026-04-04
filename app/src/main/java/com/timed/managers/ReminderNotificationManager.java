package com.timed.managers;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.timed.models.Reminder;
import com.timed.services.ReminderNotificationReceiver;

import java.util.Calendar;
import java.util.List;

public class ReminderNotificationManager {
    private final Context context;
    private final android.app.NotificationManager notificationManager;
    private static final String CHANNEL_ID = "reminders_channel";
    private static final String CHANNEL_NAME = "Reminders";
    private static final int NOTIFICATION_ID_BASE = 1000;

    public ReminderNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (android.app.NotificationManager) 
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
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for reminders");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Schedule a reminder notification
     */
    public void scheduleReminder(Reminder reminder) {
        if (reminder == null || reminder.getDueDate() == null) {
            return;
        }

        long dueTimeMillis = reminder.getDueDate().toDate().getTime();
        long notificationTimeMillis = dueTimeMillis - 
                (reminder.getNotificationTimeBefore() * 60 * 1000);

        // Don't schedule if notification time is in the past
        if (notificationTimeMillis < System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, ReminderNotificationReceiver.class);
        intent.setAction("com.timed.REMINDER_NOTIFICATION");
        intent.putExtra("reminder_id", reminder.getId());
        intent.putExtra("reminder_title", reminder.getTitle());
        intent.putExtra("reminder_description", reminder.getDescription());

        int requestCode = (int) (reminder.getId().hashCode() % 10000);
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
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Cancel a scheduled reminder notification
     */
    public void cancelReminder(String reminderId) {
        Intent intent = new Intent(context, ReminderNotificationReceiver.class);
        intent.setAction("com.timed.REMINDER_NOTIFICATION");

        int requestCode = (int) (reminderId.hashCode() % 10000);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = 
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    /**
     * Show a notification immediately
     */
    public void showNotification(String reminderId, String title, String description) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(description)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVibrate(new long[]{0, 200, 200, 200})
                .setLights(0xFF00FF00, 1000, 1000);

        int notificationId = NOTIFICATION_ID_BASE + (int) (reminderId.hashCode() % 10000);
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Cancel a notification
     */
    public void cancelNotification(String reminderId) {
        int notificationId = NOTIFICATION_ID_BASE + (int) (reminderId.hashCode() % 10000);
        notificationManager.cancel(notificationId);
    }

    /**
     * Show an urgent notification with action buttons
     */
    public void showUrgentNotification(String reminderId, String title, String description) {
        Intent completeIntent = new Intent(context, ReminderNotificationReceiver.class);
        completeIntent.setAction("com.timed.REMINDER_COMPLETE");
        completeIntent.putExtra("reminder_id", reminderId);

        int requestCode = (int) (reminderId.hashCode() % 10000);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(description)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVibrate(new long[]{0, 300, 100, 300, 100, 300})
                .setLights(0xFFFF0000, 1000, 1000)
                .addAction(android.R.drawable.ic_dialog_info, "Complete", completePendingIntent);

        int notificationId = NOTIFICATION_ID_BASE + (int) (reminderId.hashCode() % 10000);
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Reschedule all reminders (useful when app starts)
     */
    public void rescheduleAllReminders(List<Reminder> reminders) {
        for (Reminder reminder : reminders) {
            if (reminder.getStatus().equals("pending") && 
                !reminder.getIsCompleted() && 
                !reminder.getNotificationSent()) {
                scheduleReminder(reminder);
            }
        }
    }

    /**
     * Get notification manager instance
     */
    public android.app.NotificationManager getNotificationManager() {
        return notificationManager;
    }
}
