package com.timed.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.timed.R;
import com.timed.managers.TasksManager;
import com.timed.models.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CreateTaskActivity extends AppCompatActivity {

    private EditText etTaskTitle, etTaskDescription;
    private SwitchCompat cbTaskAllDay;
    private com.google.android.material.button.MaterialButton btnTaskDueDate, btnTaskDueTime;
    
    private Calendar dueCalendar;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private TasksManager tasksManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task); // Đảm bảo bạn đã lưu file XML ở câu trước tên là activity_create_task.xml

        tasksManager = TasksManager.getInstance(this);
        dueCalendar = Calendar.getInstance();

        initViews();
        setupListeners();
        updateDateTimeUI();
    }

    private void initViews() {
        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        cbTaskAllDay = findViewById(R.id.cbTaskAllDay);
        btnTaskDueDate = findViewById(R.id.btnTaskDueDate);
        btnTaskDueTime = findViewById(R.id.btnTaskDueTime);
    }

    private void setupListeners() {
        findViewById(R.id.btnCancelTask).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveTask).setOnClickListener(v -> saveTask());

        btnTaskDueDate.setOnClickListener(v -> showDatePicker());
        btnTaskDueTime.setOnClickListener(v -> showTimePicker());

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

        // Tạo mảng nhắc nhở (Mặc định báo trước 15p)
        ArrayList<Task.TaskReminder> reminders = new ArrayList<>();
        reminders.add(new Task.TaskReminder("popup", 15));

        // Khởi tạo Task model
        Task newTask = new Task(title, description, dueTimestamp, cbTaskAllDay.isChecked(), 
                                "Medium", userId, "default_list", reminders);

        // Gọi Manager để lưu
        tasksManager.createTask(newTask)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Đã lưu công việc!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}