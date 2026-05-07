package com.timed.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.timed.services.EventNotificationReceiver;

import java.util.Date;
import java.util.List;

/**
 * Helper class quản lý logic đặt báo thức (AlarmManager) và kiểm tra permissions.
 * Dùng chung cho toàn bộ app, không chỉ riêng CreateEventActivity.
 */
public class AlarmHelper {
    private static final String TAG = "AlarmHelper";

    /**
     * Đặt báo thức offline cho một sự kiện.
     *
     * @param context          Context hiện tại
     * @param eventId          ID của sự kiện
     * @param title            Tiêu đề sự kiện
     * @param eventStartTimeMs Thời gian bắt đầu sự kiện (millis)
     * @param minutesBefore    Số phút nhắc trước khi sự kiện bắt đầu
     */
    public static void scheduleEventAlarm(Context context, String eventId, String title,
                                          long eventStartTimeMs, int minutesBefore) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        long triggerTime = eventStartTimeMs - (minutesBefore * 60 * 1000L);

        // Chỉ đặt nếu giờ báo thức nằm ở tương lai
        if (triggerTime > System.currentTimeMillis()) {
            Intent intent = new Intent(context, EventNotificationReceiver.class);
            intent.setAction("com.timed.EVENT_REMINDER");
            intent.putExtra("event_id", eventId);
            intent.putExtra("event_title", title);

            // Mã hóa ID báo thức để không bị đè nhau
            int requestCode = (eventId + minutesBefore).hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Đặt báo thức tương thích với Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent); // Fallback
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            Log.d(TAG, "⏰ Đã đặt báo thức: " + title + " trước " + minutesBefore + " phút");
        }
    }

    /**
     * Đặt báo thức cho tất cả các mốc nhắc nhở của một sự kiện.
     *
     * @param context          Context hiện tại
     * @param eventId          ID sự kiện
     * @param title            Tiêu đề sự kiện
     * @param eventStartTimeMs Thời gian bắt đầu sự kiện (millis)
     * @param reminderMinutes  Danh sách các mốc nhắc nhở (phút trước sự kiện)
     */
    public static void scheduleAllAlarms(Context context, String eventId, String title,
                                         long eventStartTimeMs, List<Long> reminderMinutes) {
        if (reminderMinutes == null) {
            return;
        }
        for (Long mins : reminderMinutes) {
            scheduleEventAlarm(context, eventId, title, eventStartTimeMs, mins.intValue());
        }
    }

    /**
     * Debug: Kiểm tra trạng thái permissions liên quan đến thông báo và báo thức.
     *
     * @param context   Context hiện tại
     * @param startTime Thời gian bắt đầu sự kiện (dùng để kiểm tra có nằm trong tương lai không)
     */
    public static void debugNotificationSetup(Context context, long startTime) {
        Log.d(TAG, "========== DEBUG NOTIFICATION SETUP ==========");

        // 1. Check POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int permission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ POST_NOTIFICATIONS: GRANTED");
            } else {
                Log.e(TAG, "❌ POST_NOTIFICATIONS: DENIED - Request it!");
            }
        } else {
            Log.d(TAG, "✅ POST_NOTIFICATIONS: Not needed (API < 33)");
        }

        // 2. Check SCHEDULE_EXACT_ALARM permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int permission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.SCHEDULE_EXACT_ALARM);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ SCHEDULE_EXACT_ALARM: GRANTED");
            } else {
                Log.e(TAG, "❌ SCHEDULE_EXACT_ALARM: DENIED - Request it!");
            }
        } else {
            Log.d(TAG, "✅ SCHEDULE_EXACT_ALARM: Not needed (API < 31)");
        }

        // 3. Check time settings
        long nowMs = System.currentTimeMillis();
        Log.d(TAG, "Current time: " + new Date(nowMs));
        Log.d(TAG, "Event start time: " + new Date(startTime));

        if (startTime <= nowMs) {
            Log.e(TAG, "❌ EVENT TIME IN PAST! Set future time!");
        } else {
            long diffMs = startTime - nowMs;
            long diffMins = diffMs / (60 * 1000);
            Log.d(TAG, "✅ Event in future (" + diffMins + " minutes away)");
        }

        Log.d(TAG, "========================================");
    }

    /**
     * Yêu cầu các permissions cần thiết cho thông báo (Android 12+, 13+).
     * Lưu ý: Hàm này cần gọi từ Activity vì cần requestPermissions.
     *
     * @param context Context (phải là Activity)
     * @return true nếu tất cả permissions đã được cấp, false nếu cần yêu cầu
     */
    public static boolean hasRequiredPermissions(Context context) {
        boolean hasPostNotif = true;
        boolean hasAlarm = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPostNotif = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasAlarm = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.SCHEDULE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED;
        }

        return hasPostNotif && hasAlarm;
    }
}
