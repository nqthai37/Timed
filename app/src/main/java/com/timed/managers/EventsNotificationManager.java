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

import java.util.Date;

public class EventsNotificationManager {
    private static final String TAG = "EventsNotificationManager";
    private final Context context;
    private final NotificationManager notificationManager;
    private static final String CHANNEL_ID = "events_reminders_channel";
    private static final int NOTIFICATION_ID_BASE = 2000;

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

    // HÀM XÓA THÔNG BÁO
    public void cancelNotification(String eventId) {
        if (eventId != null) {
            notificationManager.cancel(NOTIFICATION_ID_BASE + (int) (eventId.hashCode() % 10000));
        }
    }

    // HÀM HỦY THÔNG BÁO SỰ KIỆN (EVENT)
    public void cancelEventReminders(Event event) {
        if (event != null && event.getId() != null) {
            cancelNotification(event.getId());
            // Also cancel any alarms if they exist
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                Intent intent = new Intent(context, EventNotificationReceiver.class);
                intent.setAction("ACTION_SHOW_EVENT_NOTIFICATION");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, event.getId().hashCode(), intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.cancel(pendingIntent);
            }
        }
    }

    // ==============================================================
    // HÀM MỚI: HIỂN THỊ THÔNG BÁO CHO CÔNG VIỆC (TASK)
    // ==============================================================
    public void showTaskNotification(String taskId, String title, String description) {
        if (taskId == null) return;

        // Intent khi nhấn vào thân thông báo (Mở app)
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, taskId.hashCode(), openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent cho nút "Xong"
        Intent doneIntent = new Intent(context, EventNotificationReceiver.class);
        doneIntent.setAction("ACTION_TASK_DONE");
        doneIntent.putExtra("TASK_ID", taskId);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context, taskId.hashCode() + 1, doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Xây dựng thông báo Pop-up
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar) // Đổi thành icon thật của app bạn
                .setContentTitle(title)
                .setContentText(description != null && !description.isEmpty() ? description : "Đến hạn hoàn thành công việc!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setFullScreenIntent(pendingIntent, true) // Lệnh quan trọng nhất để bung Pop-up
                .addAction(R.drawable.ic_check_gray, "Đã xong", donePendingIntent);

        // Hiển thị ra màn hình
        notificationManager.notify(taskId.hashCode(), builder.build());
    }

    public void scheduleEventReminders(Event event) {
        // 1. Kiểm tra nếu sự kiện không có thời gian bắt đầu hoặc không có nhắc nhở thì bỏ qua
        if (event == null || event.getStartTime() == null || event.getReminders() == null || event.getReminders().isEmpty()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Lấy thời gian bắt đầu của sự kiện (đổi ra mili-giây)
        long eventStartTimeMs = event.getStartTime().toDate().getTime();
        long currentTimeMs = System.currentTimeMillis();

        // 2. Duyệt qua từng chuông báo (nhắc nhở) của sự kiện này
        for (int i = 0; i < event.getReminders().size(); i++) {

            Event.EventReminder reminder = event.getReminders().get(i);
            if (reminder == null) continue;

            long minutesBefore = reminder.getMinutesBefore() != null ? reminder.getMinutesBefore() : 10L;
            String method = reminder.getType() != null ? reminder.getType() : "push";

            // Tính thời gian đổ chuông = Thời gian bắt đầu - Số phút báo trước
            long triggerTimeMs = eventStartTimeMs - (minutesBefore * 60 * 1000L);

            // 3. CHỈ đặt báo thức nếu thời gian đổ chuông nằm trong tương lai (chưa trôi qua)
            if (triggerTimeMs > currentTimeMs) {

                // Tạo Intent gửi đến EventNotificationReceiver mà bạn đã viết
                Intent intent = new Intent(context, EventNotificationReceiver.class);
                intent.setAction("com.timed.EVENT_REMINDER"); // Action này phải khớp với Receiver

                // Gửi kèm dữ liệu để khi Receiver nhận được có thể vẽ lên Pop-up
                intent.putExtra("event_id", event.getId());
                intent.putExtra("event_title", event.getTitle() != null ? event.getTitle() : "Sự kiện");
                intent.putExtra("event_description", event.getDescription());
                intent.putExtra("event_location", event.getLocation());
                intent.putExtra("reminder_type", method);

                // TẠO MÃ UNIQUE CHO REQUEST CODE: Để các chuông báo không đè lên nhau
                // (Ví dụ: Sự kiện A báo trước 10p là 1 mã, báo trước 30p là 1 mã khác)
                int requestCode = (event.getId() + "_" + minutesBefore).hashCode();

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // 4. Yêu cầu hệ thống Android hẹn giờ chính xác
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Cho phép rung chuông ngay cả khi điện thoại đang ngủ (Doze Mode)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent);
                }

                Log.d(TAG, "Đã hẹn giờ sự kiện: " + event.getTitle() + " lúc " + new Date(triggerTimeMs).toString());
            }
        }
    }
}