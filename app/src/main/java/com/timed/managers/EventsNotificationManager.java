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

import com.timed.R;
import com.timed.activities.MainActivity;
import com.timed.models.Event;
import com.timed.services.EventNotificationReceiver;
import com.timed.utils.RecurrenceUtils;

import java.util.Date;
import java.util.List;

public class EventsNotificationManager {
    private static final String TAG = "EventsNotificationManager";
    private final Context context;
    private final NotificationManager notificationManager;
    private static final String CHANNEL_ID = "events_reminders_channel";
    private static final int NOTIFICATION_ID_BASE = 2000;
    private static final long LOOKAHEAD_WINDOW_MS = 60L * 24L * 60L * 60L * 1000L;
    private static final int MAX_REMINDER_OCCURRENCES = 256;

    public EventsNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Event Reminders", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // HÀM HIỂN THỊ THÔNG BÁO CHO SỰ KIỆN (EVENT - ĐÃ CÓ TỪ TRƯỚC)
    public void showEventReminder(String eventId, String title, String description, String location, String type) {
        if (eventId == null) return;

        // Intent khi nhấn vào thân thông báo (Mở app)
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.putExtra("event_id", eventId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, eventId.hashCode(), openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent cho nút "Accept"
        Intent acceptIntent = new Intent(context, EventNotificationReceiver.class);
        acceptIntent.setAction("com.timed.EVENT_ACCEPT");
        acceptIntent.putExtra("event_id", eventId);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
                context, eventId.hashCode() + 1, acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent cho nút "Decline"
        Intent declineIntent = new Intent(context, EventNotificationReceiver.class);
        declineIntent.setAction("com.timed.EVENT_DECLINE");
        declineIntent.putExtra("event_id", eventId);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
                context, eventId.hashCode() + 2, declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(title != null ? title : "Sự kiện")
                .setContentText(description != null ? description : "Có sự kiện mới")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Địa điểm: " + (location != null ? location : "Không có")))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setFullScreenIntent(pendingIntent, true)
                .addAction(R.drawable.ic_check_gray, "Accept", acceptPendingIntent)
                .addAction(R.drawable.ic_close, "Decline", declinePendingIntent);

        // Hiển thị thông báo
        notificationManager.notify(NOTIFICATION_ID_BASE + (int) (eventId.hashCode() % 10000), builder.build());
    }

    /**
     * Schedule reminders for upcoming event occurrences (supports recurrence)
     */
    public void scheduleEventReminders(Event event) {
        if (event == null || event.getId() == null || event.getStartTime() == null) {
            return;
        }
        if (event.getReminders() == null || event.getReminders().isEmpty()) {
            return;
        }

        long occurrenceStartTimeMillis = resolveNextOccurrenceStart(event);
        if (occurrenceStartTimeMillis <= 0L) {
            Log.d(TAG, "No upcoming occurrence found for event: " + event.getId());
            return;
        }

        for (int i = 0; i < event.getReminders().size(); i++) {
            Event.EventReminder reminder = event.getReminders().get(i);
            if (reminder == null || reminder.getMinutesBefore() == null) {
                continue;
            }

            long notificationTimeMillis = occurrenceStartTimeMillis
                    - (reminder.getMinutesBefore() * 60L * 1000L);

            if (notificationTimeMillis < System.currentTimeMillis()) {
                Log.d(TAG, "Skipping reminder in the past for event: " + event.getId());
                continue;
            }

            scheduleReminderAtTime(event, reminder, i, notificationTimeMillis, occurrenceStartTimeMillis);
        }
    }

    private long resolveNextOccurrenceStart(Event event) {
        long eventStartTimeMillis = event.getStartTime().toDate().getTime();
        String recurrenceRuleText = event.getRecurrenceRule();

        if (recurrenceRuleText == null || recurrenceRuleText.trim().isEmpty()) {
            return eventStartTimeMillis;
        }

        RecurrenceUtils.RecurrenceRule rule = RecurrenceUtils.parseRRule(recurrenceRuleText);
        long now = System.currentTimeMillis();
        long searchStart = Math.max(eventStartTimeMillis, now - ONE_HOUR_MS);
        long searchEnd = now + LOOKAHEAD_WINDOW_MS;

        List<Long> upcoming = RecurrenceUtils.generateOccurrencesInRange(
                eventStartTimeMillis,
                rule,
                searchStart,
                searchEnd,
                event.getRecurrenceExceptions(),
                MAX_REMINDER_OCCURRENCES
        );

        for (Long occurrence : upcoming) {
            if (occurrence != null && occurrence >= now) {
                return occurrence;
            }
        }

        return -1L;
    }

    private static final long ONE_HOUR_MS = 60L * 60L * 1000L;

    /**
     * Schedule a single reminder for a specific time
     */
    private void scheduleReminderAtTime(Event event, Event.EventReminder reminder,
                                        int reminderIndex, long notificationTimeMillis,
                                        long occurrenceStartTimeMillis) {
        Intent intent = new Intent(context, EventNotificationReceiver.class);
        intent.setAction("com.timed.EVENT_REMINDER");
        intent.putExtra("event_id", event.getId());
        intent.putExtra("event_title", event.getTitle());
        intent.putExtra("event_description", event.getDescription());
        intent.putExtra("event_location", event.getLocation());
        intent.putExtra("occurrence_start_time", occurrenceStartTimeMillis);
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
                    " (occurrence at " + new Date(occurrenceStartTimeMillis) + ")" +
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

    private void cancelReminderAtIndex(String eventId, int reminderIndex) {
        if (eventId == null) {
            return;
        }

        Intent intent = new Intent(context, EventNotificationReceiver.class);
        intent.setAction("com.timed.EVENT_REMINDER");

        int requestCode = (eventId.hashCode() + reminderIndex) % 20000;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
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
     * Cancel notification by event ID
     */
    public void cancelNotification(String eventId) {
        if (eventId == null) {
            return;
        }
        int notificationId = NOTIFICATION_ID_BASE + (int) (eventId.hashCode() % 10000);
        notificationManager.cancel(notificationId);
    }

    /**
     * Show notification for tasks
     */
    public void showTaskNotification(String taskId, String title, String description) {
        if (taskId == null) return;

        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, taskId.hashCode(), openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent doneIntent = new Intent(context, EventNotificationReceiver.class);
        doneIntent.setAction("ACTION_TASK_DONE");
        doneIntent.putExtra("TASK_ID", taskId);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context, taskId.hashCode() + 1, doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(title)
                .setContentText(description != null && !description.isEmpty()
                        ? description
                        : "Đến hạn hoàn thành công việc!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setFullScreenIntent(pendingIntent, true)
                .addAction(R.drawable.ic_check_gray, "Đã xong", donePendingIntent);

        notificationManager.notify(taskId.hashCode(), builder.build());
    }
}