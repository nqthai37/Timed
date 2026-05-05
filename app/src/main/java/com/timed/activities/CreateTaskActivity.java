package com.timed.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.timed.R;
import com.timed.managers.TasksManager;
import com.timed.models.Task;
import com.timed.dialogs.ReminderPickerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateTaskActivity extends AppCompatActivity {

    private static final String TAG = "CreateTask";
    private EditText etTaskTitle, etTaskDescription;
    private SwitchCompat cbTaskAllDay;
    private Button btnSave, btnCancel;

    private TextView tvTaskReminder;  // 🔔 Hiển thị nhắc nhở
    private com.google.android.material.button.MaterialButton btnTaskDueDate, btnTaskDueTime;
    
    private Calendar dueCalendar;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private TasksManager tasksManager;
    
    // 🔔 Danh sách lưu các mốc nhắc nhở người dùng chọn
    private List<Long> selectedReminderMinutes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🛠️ Kích hoạt chế độ Edge-to-Edge (Giống CreateEventActivity)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        setContentView(R.layout.activity_create_task);

        tasksManager = TasksManager.getInstance(this);
        dueCalendar = Calendar.getInstance();

        initViews();
        setupInsets(); // 🛠️ Thiết lập padding cho status bar
        setupListeners();
        
        // 🔔 Khởi tạo nhắc nhở mặc định (15 phút)
        selectedReminderMinutes.add(15L);
        updateReminderDisplay();
        
        updateDateTimeUI();
    }

    /**
     * Khởi tạo các View và gán biến
     */
    private void initViews() {
        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        cbTaskAllDay = findViewById(R.id.cbTaskAllDay);
        btnTaskDueDate = findViewById(R.id.btnTaskDueDate);
        btnTaskDueTime = findViewById(R.id.btnTaskDueTime);
        
        // Gán các nút Save/Cancel chuẩn
        btnSave = findViewById(R.id.btnSaveTask);
        btnCancel = findViewById(R.id.btnCancelTask);

        tvTaskReminder = findViewById(R.id.tvTaskAlertValue);
        if (tvTaskReminder == null) {
            tvTaskReminder = new TextView(this);
            Log.w(TAG, "tvTaskReminder not found in layout, created programmatically");
        }
    }

    /**
     * Thiết lập xử lý lề hệ thống (Status bar & Navigation bar)
     */
    private void setupInsets() {
        // Lưu ý: Đảm bảo ID rootCreateTask và layoutTopBarTask tồn tại trong activity_create_task.xml
        View root = findViewById(R.id.rootCreateTask); 
        View topBar = findViewById(R.id.layoutTopBar);

        if (root == null || topBar == null) return;

        final int baseTopBarHeight = dpToPx(56);
        final int baseTopPadding = topBar.getPaddingTop();
        final int baseBottomPadding = topBar.getPaddingBottom();
        final int baseLeftPadding = topBar.getPaddingLeft();
        final int baseRightPadding = topBar.getPaddingRight();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Tự động thêm padding top bằng độ cao của Status Bar
            topBar.setPadding(baseLeftPadding, baseTopPadding + bars.top, baseRightPadding, baseBottomPadding);
            
            ViewGroup.LayoutParams lp = topBar.getLayoutParams();
            lp.height = baseTopBarHeight + bars.top;
            topBar.setLayoutParams(lp);

            v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    /**
     * Thiết lập các sự kiện Click (Chuẩn hóa logic theo Event)
     */
    private void setupListeners() {
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveTask());
        }

        btnTaskDueDate.setOnClickListener(v -> showDatePicker());
        btnTaskDueTime.setOnClickListener(v -> showTimePicker());
        
        if (tvTaskReminder != null) {
            tvTaskReminder.setOnClickListener(v -> showReminderPicker());
        }

        cbTaskAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnTaskDueTime.setVisibility(isChecked ? android.view.View.GONE : android.view.View.VISIBLE);
        });
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            dueCalendar.set(year, month, dayOfMonth);
            updateDateTimeUI();
        }, dueCalendar.get(Calendar.YEAR), dueCalendar.get(Calendar.MONTH), dueCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            dueCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            dueCalendar.set(Calendar.MINUTE, minute);
            updateDateTimeUI();
        }, dueCalendar.get(Calendar.HOUR_OF_DAY), dueCalendar.get(Calendar.MINUTE), true).show();
    }

    private void updateDateTimeUI() {
        btnTaskDueDate.setText(dateFormat.format(dueCalendar.getTime()));
        btnTaskDueTime.setText(timeFormat.format(dueCalendar.getTime()));
    }

    private void showReminderPicker() {
        ReminderPickerDialog.show(this, selectedReminderMinutes, selectedMinutes -> {
            selectedReminderMinutes = selectedMinutes;
            updateReminderDisplay();
        });
    }

    private void updateReminderDisplay() {
        if (tvTaskReminder == null) return;
        
        if (selectedReminderMinutes.isEmpty()) {
            tvTaskReminder.setText("No reminders set");
            return;
        }
        
        List<Long> sorted = new ArrayList<>(selectedReminderMinutes);
        sorted.sort(Long::compareTo);
        
        StringBuilder text = new StringBuilder("Reminders: ");
        for (int i = 0; i < sorted.size(); i++) {
            long mins = sorted.get(i);
            if (i > 0) text.append(", ");
            if (mins < 60) text.append(mins).append(" min");
            else if (mins == 60) text.append("1 hour");
            else if (mins == 1440) text.append("1 day");
            else text.append(mins / 60).append(" hours");
        }
        tvTaskReminder.setText(text.toString());
    }

    private void saveTask() {
        String title = etTaskTitle.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề công việc", Toast.LENGTH_SHORT).show();
            etTaskTitle.requestFocus();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        Timestamp dueTimestamp = new Timestamp(dueCalendar.getTime());
        ArrayList<Task.TaskReminder> reminders = new ArrayList<>();
        for (Long minutes : selectedReminderMinutes) {
            reminders.add(new Task.TaskReminder("popup", minutes.intValue()));
        }

        Task newTask = new Task(title, description, dueTimestamp, cbTaskAllDay.isChecked(), 
                                "Medium", userId, "default_list", reminders);

        tasksManager.createTask(newTask);
        Toast.makeText(this, "✅ Task saved! Reminders: " + selectedReminderMinutes.size(), Toast.LENGTH_SHORT).show();
        finish();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}