package com.timed.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

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
    private TextView tvTaskReminder;  // 🔔 Reminder display
    private com.google.android.material.button.MaterialButton btnTaskDueDate, btnTaskDueTime;
    
    private Calendar dueCalendar;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private TasksManager tasksManager;
    
    // 🔔 REMINDERS: Track user-selected reminders
    private List<Long> selectedReminderMinutes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        tasksManager = TasksManager.getInstance(this);
        dueCalendar = Calendar.getInstance();

        initViews();
        setupListeners();
        
        // 🔔 Initialize default reminder (15 minutes)
        selectedReminderMinutes.add(15L);
        updateReminderDisplay();
        
        updateDateTimeUI();
    }

    private void initViews() {
        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        cbTaskAllDay = findViewById(R.id.cbTaskAllDay);
        btnTaskDueDate = findViewById(R.id.btnTaskDueDate);
        btnTaskDueTime = findViewById(R.id.btnTaskDueTime);
        
        // 🔔 Try to find reminder view, if not found, create it or skip
        tvTaskReminder = findViewById(R.id.tvTaskAlertValue);
        if (tvTaskReminder == null) {
            // If tvTaskReminder doesn't exist in layout, create a simple one
            tvTaskReminder = new TextView(this);
            Log.w(TAG, "tvTaskReminder not found in layout, created programmatically");
        }
    }

    private void setupListeners() {
        findViewById(R.id.btnCancelTask).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveTask).setOnClickListener(v -> saveTask());

        btnTaskDueDate.setOnClickListener(v -> showDatePicker());
        btnTaskDueTime.setOnClickListener(v -> showTimePicker());
        
        // 🔔 Add reminder picker listener
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

    /**
     * 🔔 Show reminder picker dialog
     */
    private void showReminderPicker() {
        ReminderPickerDialog.show(this, selectedReminderMinutes, selectedMinutes -> {
            selectedReminderMinutes = selectedMinutes;
            updateReminderDisplay();
        });
    }

    /**
     * 🔔 Update reminder display text
     */
    private void updateReminderDisplay() {
        if (tvTaskReminder == null) {
            return;
        }
        
        if (selectedReminderMinutes.isEmpty()) {
            tvTaskReminder.setText("No reminders set");
            return;
        }
        
        // Sort reminders
        List<Long> sorted = new ArrayList<>(selectedReminderMinutes);
        sorted.sort(Long::compareTo);
        
        // Format display text
        StringBuilder text = new StringBuilder("Reminders: ");
        for (int i = 0; i < sorted.size(); i++) {
            long mins = sorted.get(i);
            if (i > 0) text.append(", ");
            
            if (mins < 60) {
                text.append(mins).append(" min");
            } else if (mins == 60) {
                text.append("1 hour");
            } else if (mins == 120) {
                text.append("2 hours");
            } else if (mins == 1440) {
                text.append("1 day");
            } else {
                text.append(mins / 60).append(" hours");
            }
        }
        
        tvTaskReminder.setText(text.toString());
        Log.d(TAG, text.toString());
    }

    private void saveTask() {
        String title = etTaskTitle.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề công việc", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        Timestamp dueTimestamp = new Timestamp(dueCalendar.getTime());

        // 🔔 Create reminders from user selection
        ArrayList<Task.TaskReminder> reminders = new ArrayList<>();
        for (Long minutes : selectedReminderMinutes) {
            reminders.add(new Task.TaskReminder("popup", minutes.intValue()));
        }

        // Khởi tạo Task model
        Task newTask = new Task(title, description, dueTimestamp, cbTaskAllDay.isChecked(), 
                                "Medium", userId, "default_list", reminders);

        Log.d(TAG, "Saving task with " + reminders.size() + " reminders");

        // Gọi Manager để lưu
        tasksManager.createTask(newTask)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "✅ Task saved! Reminders: " + selectedReminderMinutes.size(), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}