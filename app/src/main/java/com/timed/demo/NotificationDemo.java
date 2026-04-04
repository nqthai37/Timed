package com.timed.demo;

import android.content.Context;
import android.util.Log;

import com.timed.managers.RemindersManager;
import com.timed.managers.ReminderNotificationManager;
import com.timed.models.Reminder;

import java.util.Calendar;

public class NotificationDemo {
    private static final String TAG = "NotificationDemo";

    /**
     * Hiển thị một notification ngay lập tức
     */
    public static void showSampleNotification(Context context) {
        ReminderNotificationManager notificationManager = new ReminderNotificationManager(context);
        notificationManager.showNotification(
                "sample_reminder_1",
                "Tiêu đề mẫu",
                "Đây là nội dung thông báo mẫu"
        );
        Log.d(TAG, "Sample notification displayed");
    }

    /**
     * Tạo một reminder mẫu và lên lịch thông báo
     */
    public static void createSampleReminder(Context context, String userId) {
        RemindersManager manager = RemindersManager.getInstance(context);
        
        // Tạo reminder cho 5 phút sau từ bây giờ
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        
        manager.createReminder(
                userId,
                "Nhắc nhở mẫu",
                "Đây là một reminder mẫu sẽ hiển thị sau 5 phút",
                calendar.getTime(),
                "high",
                "personal"
        ).addOnSuccessListener(docRef -> {
            Log.d(TAG, "Sample reminder created: " + docRef.getId());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to create sample reminder", e);
        });
    }

    /**
     * Hiển thị notification khẩn cấp mẫu
     */
    public static void showSampleUrgentNotification(Context context) {
        ReminderNotificationManager notificationManager = new ReminderNotificationManager(context);
        notificationManager.showUrgentNotification(
                "urgent_sample_1",
                "Thông báo khẩn cấp",
                "Đây là một thông báo khẩn cấp mẫu"
        );
        Log.d(TAG, "Urgent sample notification displayed");
    }

    /**
     * Hiển thị multiple notifications
     */
    public static void showMultipleSampleNotifications(Context context) {
        ReminderNotificationManager notificationManager = new ReminderNotificationManager(context);
        
        notificationManager.showNotification(
                "sample_1",
                "Thông báo 1",
                "Lần thứ nhất"
        );
        
        notificationManager.showNotification(
                "sample_2",
                "Thông báo 2",
                "Lần thứ hai"
        );
        
        notificationManager.showNotification(
                "sample_3",
                "Thông báo 3",
                "Lần thứ ba"
        );
        
        Log.d(TAG, "Multiple sample notifications displayed");
    }

    /**
     * Tạo reminder cho hôm nay lúc sau 1 phút
     */
    public static void createReminderForNow(Context context, String userId) {
        RemindersManager manager = RemindersManager.getInstance(context);
        
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        
        manager.createReminder(
                userId,
                "Thử nghiệm ngay bây giờ",
                "Bạn sẽ nhận được thông báo sau 1 phút",
                calendar.getTime(),
                "medium",
                "personal"
        ).addOnSuccessListener(docRef -> {
            Log.d(TAG, "Quick test reminder created");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error creating test reminder", e);
        });
    }
}
