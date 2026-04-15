package com.timed.activities;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.timed.R;
import com.timed.services.EventNotificationReceiver;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReminderActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hỗ trợ vẽ tràn viền (Edge-to-Edge)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_reminder);

        db = FirebaseFirestore.getInstance();
        userId = getUserId();

        setupInsets();
        setupListeners();
    }

    private void setupListeners() {
        ImageButton ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish()); //
        }

        // Giả sử bạn có một nút để đặt giờ nhắc nhở trong layout
//        View btnSetTime = findViewById(R.id.btn_set_reminder);
//        if (btnSetTime != null) {
//            btnSetTime.setOnClickListener(v -> showTimePicker());
//        }
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, h, m) -> {
            Calendar target = Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY, h);
            target.set(Calendar.MINUTE, m);
            target.set(Calendar.SECOND, 0);

            if (target.before(Calendar.getInstance())) {
                target.add(Calendar.DATE, 1);
            }

            String eventId = UUID.randomUUID().toString();
            scheduleAlarm(target.getTimeInMillis(), eventId);
            saveToFirestore(h, m, eventId);
        }, hour, minute, true);

        timePickerDialog.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleAlarm(long timeInMillis, String eventId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Sử dụng Action chuẩn mà EventNotificationReceiver đang lắng nghe
        Intent intent = new Intent(this, EventNotificationReceiver.class);
        intent.setAction("com.timed.EVENT_REMINDER");

        // Truyền các dữ liệu cần thiết để Receiver có thể hiển thị thông báo chi tiết
        intent.putExtra("event_id", eventId);
        intent.putExtra("event_title", "Nhắc nhở công việc");
        intent.putExtra("event_description", "Đã đến thời gian bạn cài đặt trong ứng dụng TimeD.");
        intent.putExtra("reminder_type", "default");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            Toast.makeText(this, "Đã đặt nhắc nhở thành công", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFirestore(int h, int m, String eventId) {
        if (userId == null) return;

        Map<String, Object> reminder = new HashMap<>();
        reminder.put("hour", h);
        reminder.put("minute", m);
        reminder.put("eventId", eventId);
        reminder.put("enabled", true);

        db.collection("users").document(userId)
                .collection("settings").document("reminder")
                .set(reminder);
    }

    private void setupInsets() {
        View root = findViewById(R.id.topBar);
        if (root == null) return;

        // Sử dụng logic chiều cao 65dp chuẩn của bạn
        final int baseTopBarHeight = dpToPx(65);
        final int baseTopPadding = root.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Điều chỉnh padding và chiều cao dựa trên Status Bar
            root.setPadding(root.getPaddingLeft(), baseTopPadding + bars.top, root.getPaddingRight(), root.getPaddingBottom());
            ViewGroup.LayoutParams lp = root.getLayoutParams();
            lp.height = baseTopBarHeight + bars.top;
            root.setLayoutParams(lp);

            return insets;
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density); //
    }

    private String getUserId() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                return auth.getCurrentUser().getUid();
            }
        } catch (Exception ignored) {}
        return null;
    }
}