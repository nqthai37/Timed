package com.timed.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.timed.managers.EventsNotificationManager;
import com.timed.repositories.EventsRepository;

public class EventNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "EventNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        EventsNotificationManager notificationManager = new EventsNotificationManager(context);

        switch (action) {
            // --- PHẦN CỦA EVENT (Giữ nguyên của bạn) ---
            case "com.timed.EVENT_REMINDER":
                handleEventReminder(context, intent, notificationManager);
                break;
            case "com.timed.EVENT_ACCEPT":
                handleEventAccept(context, intent.getStringExtra("event_id"), notificationManager);
                break;
            case "com.timed.EVENT_DECLINE":
                handleEventDecline(context, intent.getStringExtra("event_id"), notificationManager);
                break;

            // --- PHẦN MỚI THÊM CHO TASK ---
            case "ACTION_SHOW_TASK_NOTIFICATION":
                String taskId = intent.getStringExtra("TASK_ID");
                String title = intent.getStringExtra("TASK_TITLE");
                String desc = intent.getStringExtra("TASK_DESC");
                notificationManager.showTaskNotification(taskId, title, desc);
                break;

            case "ACTION_TASK_DONE":
                String doneTaskId = intent.getStringExtra("TASK_ID");
                if (doneTaskId != null) {
                    // Cập nhật Firebase
                    FirebaseFirestore.getInstance().collection("tasks").document(doneTaskId)
                            .update("is_completed", true)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Task marked as done"));

                    // Tắt thông báo
                    NotificationManager sysManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    sysManager.cancel(doneTaskId.hashCode());
                    Toast.makeText(context, "Đã hoàn thành công việc!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    // CÁC HÀM CŨ GIỮ NGUYÊN BÊN TRONG NÀY...
    private void handleEventReminder(Context context, Intent intent, EventsNotificationManager manager) {
        String eventId = intent.getStringExtra("event_id");
        String title = intent.getStringExtra("event_title");
        String description = intent.getStringExtra("event_description");
        String location = intent.getStringExtra("event_location");
        String type = intent.getStringExtra("reminder_type");
        manager.showEventReminder(eventId, title, description, location, type);
    }

    private void handleEventAccept(Context context, String eventId, EventsNotificationManager manager) {
        if (eventId == null) return;

        // Cập nhật trạng thái participant lên Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null 
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() 
                : null;

        if (userId != null) {
            db.collection("events").document(eventId)
                    .update("participant_status." + userId, "accepted")
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Event accepted: " + eventId);
                        Toast.makeText(context, "Đã chấp nhận sự kiện!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error accepting event: " + e.getMessage());
                        Toast.makeText(context, "Lỗi khi chấp nhận sự kiện", Toast.LENGTH_SHORT).show();
                    });
        }

        // Tắt thông báo
        manager.cancelNotification(eventId);
    }

    private void handleEventDecline(Context context, String eventId, EventsNotificationManager manager) {
        if (eventId == null) return;

        // Cập nhật trạng thái participant lên Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null 
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() 
                : null;

        if (userId != null) {
            db.collection("events").document(eventId)
                    .update("participant_status." + userId, "declined")
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Event declined: " + eventId);
                        Toast.makeText(context, "Đã từ chối sự kiện!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error declining event: " + e.getMessage());
                        Toast.makeText(context, "Lỗi khi từ chối sự kiện", Toast.LENGTH_SHORT).show();
                    });
        }

        // Tắt thông báo
        manager.cancelNotification(eventId);
    }
}