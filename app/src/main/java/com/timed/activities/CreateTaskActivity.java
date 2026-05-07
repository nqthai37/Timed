package com.timed.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.timed.R;
import com.timed.dialogs.ReminderPickerDialog;
import com.timed.managers.TasksManager;
import com.timed.models.CalendarModel;
import com.timed.models.Task;
import com.timed.utils.CalendarIntegrationService;
import com.timed.utils.CalendarPermissionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateTaskActivity extends AppCompatActivity {
    private EditText etTaskTitle;
    private EditText etTaskDescription;
    private TextView tvTaskReminder;
    private TextView tvScreenTitle;
    private TextView tvDeleteTask;
    private TextView tvTaskListValue;
    private View layoutTaskAlert;
    private View layoutTaskListSelector;
    private Button btnSave;
    private Button btnCancel;
    private com.google.android.material.button.MaterialButton btnTaskDueDate;
    private com.google.android.material.button.MaterialButton btnTaskDueTime;

    private final Calendar dueCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private final List<Long> selectedReminderMinutes = new ArrayList<>();
    private final List<CalendarModel> writableCalendars = new ArrayList<>();

    private TasksManager tasksManager;
    private CalendarIntegrationService calendarIntegrationService;
    private String taskId;
    private String calendarId;
    private Task editingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_create_task);

        tasksManager = TasksManager.getInstance(this);
        calendarIntegrationService = new CalendarIntegrationService();
        taskId = getIntent().getStringExtra("taskId");
        calendarId = getIntent().getStringExtra("calendarId");

        initViews();
        setupInsets();
        setupListeners();

        selectedReminderMinutes.add(15L);
        updateReminderDisplay();
        updateDateTimeUI();
        loadCalendars();
        loadTaskForEdit();
    }

    private void initViews() {
        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        btnTaskDueDate = findViewById(R.id.btnTaskDueDate);
        btnTaskDueTime = findViewById(R.id.btnTaskDueTime);
        btnSave = findViewById(R.id.btnSaveTask);
        btnCancel = findViewById(R.id.btnCancelTask);
        tvTaskReminder = findViewById(R.id.tvTaskAlertValue);
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        tvDeleteTask = findViewById(R.id.tvDeleteTask);
        tvTaskListValue = findViewById(R.id.tvTaskListValue);
        layoutTaskAlert = findViewById(R.id.layoutTaskAlert);
        layoutTaskListSelector = findViewById(R.id.layoutTaskListSelector);

        if (isEditMode()) {
            tvScreenTitle.setText("Edit Task");
            tvDeleteTask.setVisibility(View.VISIBLE);
        }
    }

    private void setupInsets() {
        View root = findViewById(R.id.rootCreateTask);
        View topBar = findViewById(R.id.layoutTopBar);
        if (root == null || topBar == null) {
            return;
        }

        final int baseTopBarHeight = dpToPx(56);
        final int baseTopPadding = topBar.getPaddingTop();
        final int baseBottomPadding = topBar.getPaddingBottom();
        final int baseLeftPadding = topBar.getPaddingLeft();
        final int baseRightPadding = topBar.getPaddingRight();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topBar.setPadding(baseLeftPadding, baseTopPadding + bars.top, baseRightPadding, baseBottomPadding);
            ViewGroup.LayoutParams lp = topBar.getLayoutParams();
            lp.height = baseTopBarHeight + bars.top;
            topBar.setLayoutParams(lp);
            v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveTask());
        btnTaskDueDate.setOnClickListener(v -> showDatePicker());
        btnTaskDueTime.setOnClickListener(v -> showTimePicker());
        layoutTaskAlert.setOnClickListener(v -> showReminderPicker());
        layoutTaskListSelector.setOnClickListener(v -> showCalendarPicker());
        tvDeleteTask.setOnClickListener(v -> deleteTask());
    }

    private boolean isEditMode() {
        return taskId != null && !taskId.isEmpty();
    }

    private void loadTaskForEdit() {
        if (!isEditMode()) {
            return;
        }
        tasksManager.getTaskById(taskId)
                .addOnSuccessListener(snapshot -> {
                    editingTask = snapshot.toObject(Task.class);
                    if (editingTask == null) {
                        return;
                    }
                    editingTask.setId(snapshot.getId());
                    etTaskTitle.setText(editingTask.getTitle());
                    etTaskDescription.setText(editingTask.getDescription());
                    if (editingTask.getDue_date() != null) {
                        dueCalendar.setTime(editingTask.getDue_date().toDate());
                    }
                    if (editingTask.getList_id() != null && !editingTask.getList_id().isEmpty()) {
                        calendarId = editingTask.getList_id();
                    }
                    selectedReminderMinutes.clear();
                    if (editingTask.getReminders() != null) {
                        for (Task.TaskReminder reminder : editingTask.getReminders()) {
                            selectedReminderMinutes.add((long) reminder.getMinutes_before());
                        }
                    }
                    updateDateTimeUI();
                    updateReminderDisplay();
                    updateCalendarLabel();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to load task", Toast.LENGTH_SHORT).show());
    }

    private void loadCalendars() {
        calendarIntegrationService.ensureDefaultCalendar(this, new CalendarIntegrationService.DefaultCalendarListener() {
            @Override
            public void onReady(String defaultId, List<CalendarModel> calendars) {
                writableCalendars.clear();
                String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                if (calendars != null) {
                    for (CalendarModel calendar : calendars) {
                        if (CalendarPermissionUtils.canWrite(calendar, userId)) {
                            writableCalendars.add(calendar);
                        }
                    }
                }
                if (!containsWritableCalendar(calendarId)) {
                    calendarId = writableCalendars.isEmpty() ? null : writableCalendars.get(0).getId();
                }
                updateCalendarLabel();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(CreateTaskActivity.this, "Cannot load calendars", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCalendarPicker() {
        if (writableCalendars.isEmpty()) {
            Toast.makeText(this, "No editable calendars available", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[writableCalendars.size()];
        int selectedIndex = 0;
        for (int i = 0; i < writableCalendars.size(); i++) {
            CalendarModel calendar = writableCalendars.get(i);
            labels[i] = calendar.getName() == null ? "Calendar" : calendar.getName();
            if (calendarId != null && calendarId.equals(calendar.getId())) {
                selectedIndex = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose List")
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    calendarId = writableCalendars.get(which).getId();
                    updateCalendarLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean containsWritableCalendar(String id) {
        if (id == null) {
            return false;
        }
        for (CalendarModel calendar : writableCalendars) {
            if (calendar != null && id.equals(calendar.getId())) {
                return true;
            }
        }
        return false;
    }

    private void updateCalendarLabel() {
        if (tvTaskListValue == null) {
            return;
        }
        for (CalendarModel calendar : writableCalendars) {
            if (calendar != null && calendarId != null && calendarId.equals(calendar.getId())) {
                tvTaskListValue.setText(calendar.getName() == null ? "Calendar" : calendar.getName());
                return;
            }
        }
        tvTaskListValue.setText(writableCalendars.isEmpty() ? "No editable calendars" : "Personal");
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
            selectedReminderMinutes.clear();
            selectedReminderMinutes.addAll(selectedMinutes);
            updateReminderDisplay();
        });
    }

    private void updateReminderDisplay() {
        if (tvTaskReminder == null) {
            return;
        }
        if (selectedReminderMinutes.isEmpty()) {
            tvTaskReminder.setText("No reminders set");
            return;
        }

        List<Long> sorted = new ArrayList<>(selectedReminderMinutes);
        sorted.sort(Long::compareTo);
        StringBuilder text = new StringBuilder("Reminders: ");
        for (int i = 0; i < sorted.size(); i++) {
            long mins = sorted.get(i);
            if (i > 0) {
                text.append(", ");
            }
            if (mins < 60) {
                text.append(mins).append(" min");
            } else if (mins == 60) {
                text.append("1 hour");
            } else if (mins == 1440) {
                text.append("1 day");
            } else {
                text.append(mins / 60).append(" hours");
            }
        }
        tvTaskReminder.setText(text.toString());
    }

    private void saveTask() {
        String title = etTaskTitle.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etTaskTitle.setError("Enter a task title");
            etTaskTitle.requestFocus();
            return;
        }
        if (calendarId == null || calendarId.isEmpty()) {
            Toast.makeText(this, "Choose an editable calendar", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        ArrayList<Task.TaskReminder> reminders = new ArrayList<>();
        for (Long minutes : selectedReminderMinutes) {
            reminders.add(new Task.TaskReminder("popup", minutes.intValue()));
        }

        Task task = new Task(title, description, new Timestamp(dueCalendar.getTime()), false,
                "Medium", userId, calendarId, reminders);

        if (isEditMode()) {
            if (editingTask != null) {
                task.setCreated_at(editingTask.getCreated_at());
                task.setIs_completed(editingTask.isIs_completed());
            }
            task.setUpdated_at(Timestamp.now());
            tasksManager.updateTask(taskId, task).addOnCompleteListener(result -> finish());
        } else {
            tasksManager.createTask(task).addOnCompleteListener(result -> finish());
        }
    }

    private void deleteTask() {
        if (isEditMode()) {
            tasksManager.deleteTask(taskId).addOnCompleteListener(task -> finish());
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
