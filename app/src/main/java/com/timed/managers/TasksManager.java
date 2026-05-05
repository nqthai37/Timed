package com.timed.managers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.timed.repositories.TasksRepository;
import com.timed.services.EventNotificationReceiver;

public class TasksManager {
    private static TasksManager instance;
    private final TasksRepository tasksRepository;
    private final Context context;
    private static final String TAG = "TasksManager";

    private TasksManager(Context context) {
        this.context = context.getApplicationContext();
        this.tasksRepository = new TasksRepository();
    }

    public static synchronized TasksManager getInstance(Context context) {
        if (instance == null) {
            instance = new TasksManager(context);
        }
        return instance;
    }

    // TẠO MỚI TASK VÀ CÀI BÁO THỨC
    public Task<DocumentReference> createTask(com.timed.models.Task task) {
        return tasksRepository.createTask(task).addOnSuccessListener(docRef -> {
            String taskId = docRef.getId();
            scheduleTaskReminders(taskId, task);
        });
    }

    // CẬP NHẬT TASK
    public Task<Void> updateTask(String taskId, com.timed.models.Task task) {
        return tasksRepository.updateTask(taskId, task).addOnSuccessListener(aVoid -> {
            scheduleTaskReminders(taskId, task); // Cài lại báo thức mới
        });
    }

    // ĐÁNH DẤU HOÀN THÀNH
    public Task<Void> markTaskAsDone(String taskId) {
        cancelTaskAlarm(taskId); // Hủy báo thức
        // Update only the is_completed field without creating full Task object
        return com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("tasks")
                .document(taskId)
                .update("is_completed", true);
    }

    // ĐẶT BÁO THỨC CHO TASK
    private void scheduleTaskReminders(String taskId, com.timed.models.Task task) {
        if (task.getDue_date() == null || task.isIs_completed()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventNotificationReceiver.class);
        intent.setAction("ACTION_SHOW_TASK_NOTIFICATION");
        intent.putExtra("TASK_ID", taskId);
        intent.putExtra("TASK_TITLE", task.getTitle());
        intent.putExtra("TASK_DESC", task.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, taskId.hashCode(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Giả sử báo thức reo đúng lúc due_date (Nếu có mảng reminders thì bạn tính toán trừ đi số phút ở đây)
        long triggerTime = task.getDue_date().toDate().getTime();

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            Log.d(TAG, "Scheduled alarm for Task: " + task.getTitle());
        }
    }

    // HỦY BÁO THỨC KHI XÓA/HOÀN THÀNH
    private void cancelTaskAlarm(String taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventNotificationReceiver.class);
        intent.setAction("ACTION_SHOW_TASK_NOTIFICATION");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, taskId.hashCode(), intent, 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    // ===== PHƯƠNG THỨC RETRIEVE TASKS =====

    // LẤY TẤT CẢ TASKS CHƯA HOÀN THÀNH CỦA USER
    public Task<QuerySnapshot> getPendingTasks(String userId) {
        return tasksRepository.getPendingTasksByUser(userId);
    }

    // LẤY TẤT CẢ TASKS CỦA USER (CẢ HOÀN THÀNH VÀ CHƯA HOÀN THÀNH)
    public Task<QuerySnapshot> getAllTasks(String userId) {
        return tasksRepository.getAllTasksByUser(userId);
    }

    // LẤY TASKS CÓ DUE DATE HÔM NAY
    public Task<QuerySnapshot> getTodaysTasks(String userId) {
        return tasksRepository.getTodaysTasks(userId);
    }

    // XÓA TASK
    public Task<Void> deleteTask(String taskId) {
        cancelTaskAlarm(taskId); // Hủy báo thức trước khi xóa
        return tasksRepository.deleteTask(taskId);
    }
}