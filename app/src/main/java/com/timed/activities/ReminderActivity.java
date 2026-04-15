package com.timed.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.timed.R;
import com.timed.models.Task;
import com.timed.services.EventNotificationReceiver;

import java.util.Date;

public class ReminderActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private EditText etTitle, etDesc;
    private long selectedTimeInMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder); // File XML giao diện của bạn

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Ánh xạ View (Dựa theo ID trong activity_reminder.xml)
        etTitle = findViewById(R.id.et_reminder_title);
        etDesc = findViewById(R.id.et_reminder_desc);
        TextView btnSave = findViewById(R.id.btn_save);
        View btnClose = findViewById(R.id.iv_back);

        // Đóng màn hình
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        // Tạm thời lấy thời gian hiện tại cộng thêm 1 phút để test (Bạn có thể tích hợp TimePicker lại sau)
        selectedTimeInMillis = System.currentTimeMillis() + (60 * 1000);

        // Bấm lưu
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveTaskToFirebase());
        }
    }

    private void saveTaskToFirebase() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String userId = auth.getCurrentUser().getUid();

        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Khởi tạo đối tượng Task
        Timestamp deadline = new Timestamp(new Date(selectedTimeInMillis));
        Task newTask = new Task(title, desc, deadline, "high", userId);

        // 2. Lưu lên Firestore vào collection "tasks"
        db.collection("tasks")
                .add(newTask)
                .addOnSuccessListener(documentReference -> {
                    String taskId = documentReference.getId();

                    // 3. Đặt báo thức (AlarmManager)
                    scheduleTaskAlarm(taskId, title, desc, selectedTimeInMillis);

                    Toast.makeText(this, "Đã lưu công việc!", Toast.LENGTH_SHORT).show();
                    finish(); // Trở về màn hình trước
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void scheduleTaskAlarm(String taskId, String title, String desc, long triggerTime) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, EventNotificationReceiver.class);
        intent.setAction("ACTION_SHOW_TASK_NOTIFICATION");
        intent.putExtra("TASK_ID", taskId);
        intent.putExtra("TASK_TITLE", title);
        intent.putExtra("TASK_DESC", desc);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
}