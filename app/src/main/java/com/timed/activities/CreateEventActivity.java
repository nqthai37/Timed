package com.timed.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.timed.R;
import com.timed.dialogs.ReminderPickerDialog;
import com.timed.dialogs.RecurrenceRuleBottomSheet;
import com.timed.models.Event;
import com.timed.utils.AlarmHelper;
import com.timed.utils.DateTimePickerHelper;
import com.timed.utils.RecurrenceConfig;
import com.timed.viewmodels.CreateEventViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Activity để tạo hoặc chỉnh sửa sự kiện.
 * Chỉ chứa logic liên kết UI (View Binding) và lắng nghe sự kiện từ ViewModel.
 */
public class CreateEventActivity extends AppCompatActivity {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

    // --- Views ---
    private EditText etTitle, etDescription, etLocation;
    private SwitchCompat cbAllDay;
    private Button btnStartDate, btnStartTime, btnEndDate, btnEndTime;
    private Button btnSave, btnCancel;
    private TextView tvDeleteEvent, tvScreenTitle, tvCalendarValue, tvAlertValue;
    private View layoutCalendarSelector;
    private View layoutRepeatRow;
    private TextView tvRepeatValue;

    // --- ViewModel ---
    private CreateEventViewModel viewModel;

    // --- Activity Result Launcher ---
    private ActivityResultLauncher<Intent> conflictResolverLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_create_event);

        initializeViews();
        setupInsets();

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(CreateEventViewModel.class);
        viewModel.initialize(this);

        readIntentData();
        setupListeners();
        setupConflictResolver();
        observeViewModel();

        // Request permissions nếu cần
        requestNotificationPermissions();

        // Khởi tạo thời gian mặc định nếu chưa có
        if (viewModel.getStartTimeValue() <= 0 || viewModel.getEndTimeValue() <= 0) {
            Calendar calendar = Calendar.getInstance();
            viewModel.setStartTime(calendar.getTimeInMillis());
            calendar.add(Calendar.HOUR, 1);
            viewModel.setEndTime(calendar.getTimeInMillis());
        }

        viewModel.ensureValidTimeRange();
        configureModeUI();
        viewModel.loadCalendars(this);

        // 🔍 DEBUG
        AlarmHelper.debugNotificationSetup(this, viewModel.getStartTimeValue());
    }

    // ======================= Khởi tạo Views =======================

    /**
     * Khởi tạo views
     */
    private void initializeViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        cbAllDay = findViewById(R.id.cbAllDay);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnEndTime = findViewById(R.id.btnEndTime);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        tvDeleteEvent = findViewById(R.id.tvDeleteEvent);
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        tvCalendarValue = findViewById(R.id.tvCalendarValue);
        tvAlertValue = findViewById(R.id.tvAlertValue);
        layoutCalendarSelector = findViewById(R.id.layoutCalendarSelector);
        layoutRepeatRow = findViewById(R.id.layoutRepeatRow);
        tvRepeatValue = findViewById(R.id.tvRepeatValue);
    }

    private void setupInsets() {
        View root = findViewById(R.id.rootCreateEvent);
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

    // ======================= Đọc Intent =======================

    private void readIntentData() {
        String mode = getIntent().getStringExtra("mode");
        viewModel.setMode(mode);

        String eventId = getIntent().getStringExtra("eventId");
        viewModel.setEventId(eventId);

        String incomingCalendarId = getIntent().getStringExtra("calendarId");
        if (incomingCalendarId != null && !incomingCalendarId.isEmpty()) {
            viewModel.setCalendarId(incomingCalendarId);
        }

        String incomingTitle = getIntent().getStringExtra("title");
        String incomingDescription = getIntent().getStringExtra("description");
        String incomingLocation = getIntent().getStringExtra("location");
        long startTime = getIntent().getLongExtra("startTime", 0L);
        long endTime = getIntent().getLongExtra("endTime", 0L);
        boolean incomingAllDay = getIntent().getBooleanExtra("allDay", false);

        if (incomingTitle != null) etTitle.setText(incomingTitle);
        if (incomingDescription != null) etDescription.setText(incomingDescription);
        if (incomingLocation != null) etLocation.setText(incomingLocation);
        cbAllDay.setChecked(incomingAllDay);

        if (startTime > 0) viewModel.setStartTime(startTime);
        if (endTime > 0) viewModel.setEndTime(endTime);

        // Nếu đang edit, load dữ liệu sự kiện
        if (viewModel.isEditMode() && eventId != null && !eventId.isEmpty()) {
            viewModel.loadEventForEdit(event -> {
                if (event == null) return;
                if (etTitle.getText().toString().trim().isEmpty())
                    etTitle.setText(event.getTitle());
                if (etDescription.getText().toString().trim().isEmpty())
                    etDescription.setText(event.getDescription());
                if (etLocation.getText().toString().trim().isEmpty())
                    etLocation.setText(event.getLocation());
                cbAllDay.setChecked(event.getAllDay() != null && event.getAllDay());
            });
        }
    }

    // ======================= Setup Listeners =======================

    /**
     * Setup button listeners — dùng DateTimePickerHelper thay vì tự tạo dialog
     */
    private void setupListeners() {
        if (btnStartDate != null) {
            btnStartDate.setOnClickListener(v ->
                    DateTimePickerHelper.showDatePicker(this, viewModel.getStartTimeValue(), newTime -> {
                        viewModel.setStartTime(newTime);
                    }));
        }
        if (btnStartTime != null) {
            btnStartTime.setOnClickListener(v ->
                    DateTimePickerHelper.showTimePicker(this, viewModel.getStartTimeValue(), newTime -> {
                        viewModel.setStartTime(newTime);
                    }));
        }
        if (btnEndDate != null) {
            btnEndDate.setOnClickListener(v ->
                    DateTimePickerHelper.showDatePicker(this, viewModel.getEndTimeValue(), newTime -> {
                        viewModel.setEndTime(newTime);
                    }));
        }
        if (btnEndTime != null) {
            btnEndTime.setOnClickListener(v ->
                    DateTimePickerHelper.showTimePicker(this, viewModel.getEndTimeValue(), newTime -> {
                        viewModel.setEndTime(newTime);
                    }));
        }
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> onSaveClicked());
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
        if (tvDeleteEvent != null) {
            tvDeleteEvent.setOnClickListener(v -> {
                viewModel.deleteEvent();
            });
        }
        if (layoutCalendarSelector != null) {
            layoutCalendarSelector.setOnClickListener(v ->
                    viewModel.getCalendarSelectorHelper().showCalendarPicker(
                            this,
                            viewModel.getCalendarOptions(),
                            viewModel.getCalendarIdValue(),
                            calendarId -> viewModel.setCalendarId(calendarId)
                    ));
        }
        if (layoutRepeatRow != null) {
            layoutRepeatRow.setOnClickListener(v -> showRepeatPicker());
        }
        // 🔔 Reminder picker
        if (tvAlertValue != null) {
            tvAlertValue.setOnClickListener(v ->
                    ReminderPickerDialog.show(this, viewModel.getSelectedReminderMinutesValue(), selectedMinutes -> {
                        viewModel.setSelectedReminderMinutes(selectedMinutes);
                    }));
        }
        if (cbAllDay != null) {
            cbAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllDayState());
        }
    }

    private void setupConflictResolver() {
        conflictResolverLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            long newStart = data.getLongExtra("RESOLVED_START", -1);
                            long newEnd = data.getLongExtra("RESOLVED_END", -1);
                            if (newStart != -1 && newEnd != -1) {
                                viewModel.setStartTime(newStart);
                                viewModel.setEndTime(newEnd);
                            }
                        }
                        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
                        String description = etDescription != null ? etDescription.getText().toString().trim() : "";
                        String location = etLocation != null ? etLocation.getText().toString().trim() : "";
                        boolean isAllDay = cbAllDay != null && cbAllDay.isChecked();
                        viewModel.proceedToSave(title, description, location, isAllDay);
                    }
                }
        );
    }

    // ======================= Observe ViewModel =======================

    /**
     * Lắng nghe các thay đổi từ ViewModel để cập nhật UI
     */
    private void observeViewModel() {
        viewModel.getStartTime().observe(this, time -> {
            if (btnStartDate != null) btnStartDate.setText(DATE_FORMAT.format(time));
            if (btnStartTime != null) btnStartTime.setText(TIME_FORMAT.format(time));
        });

        viewModel.getEndTime().observe(this, time -> {
            if (btnEndDate != null) btnEndDate.setText(DATE_FORMAT.format(time));
            if (btnEndTime != null) btnEndTime.setText(TIME_FORMAT.format(time));
        });

        viewModel.getCalendarLabel().observe(this, label -> {
            if (tvCalendarValue != null) tvCalendarValue.setText(label);
        });

        viewModel.getReminderDisplayText().observe(this, text -> {
            if (tvAlertValue != null) tvAlertValue.setText(text);
        });

        viewModel.getRepeatSummary().observe(this, summary -> {
            if (tvRepeatValue != null) tvRepeatValue.setText(summary);
        });

        viewModel.getSaveResult().observe(this, result -> {
            if (result == null) return;
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            if (result.success) {
                finish();
            } else {
                if (btnSave != null) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                }
            }
        });

        viewModel.getConflictResult().observe(this, result -> {
            if (result == null) return;
            if (btnSave != null) {
                btnSave.setEnabled(true);
                btnSave.setText("Save");
            }
            Intent intent = new Intent(this, com.timed.Features.ConflictResolver.ConflictResolverActivity.class);
            intent.putExtra("NEW_EVENT_START", result.startTime);
            intent.putExtra("NEW_EVENT_END", result.endTime);
            intent.putExtra("NEW_EVENT_TITLE", result.title);
            conflictResolverLauncher.launch(intent);
        });
    }

    // ======================= UI Actions =======================

    private void onSaveClicked() {
        String title = etTitle != null ? etTitle.getText().toString().trim() : "";

        if (title.isEmpty()) {
            if (etTitle != null) {
                etTitle.setError("Vui lòng nhập tiêu đề sự kiện");
                etTitle.requestFocus();
            }
            return;
        }

        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Checking...");
        }

        String description = etDescription != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation != null ? etLocation.getText().toString().trim() : "";
        boolean isAllDay = cbAllDay != null && cbAllDay.isChecked();

        viewModel.saveEvent(title, description, location, isAllDay);
    }

    private void showRepeatPicker() {
        String[] options = new String[]{"Hàng ngày", "Hàng tuần", "Hàng tháng", "Hàng năm", "Tùy chỉnh"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Lặp lại")
                .setItems(options, (dialog, which) -> {
                    if (which == 4) {
                        showRecurrenceSheet();
                        return;
                    }
                    viewModel.applyQuickRecurrence(which);
                })
                .show();
    }

    private void showRecurrenceSheet() {
        RecurrenceRuleBottomSheet sheet = RecurrenceRuleBottomSheet.newInstance(
                viewModel.getRecurrenceConfigValue(), viewModel.getStartTimeValue());
        sheet.setOnConfirmedListener(config -> viewModel.setRecurrenceConfig(config));
        sheet.show(getSupportFragmentManager(), "recurrence_rule");
    }

    private void configureModeUI() {
        boolean editMode = viewModel.isEditMode();

        if (tvScreenTitle != null) {
            tvScreenTitle.setText(editMode ? "Edit Event" : "New Event");
        }
        if (btnSave != null) {
            btnSave.setText("Save");
        }
        if (tvDeleteEvent != null) {
            tvDeleteEvent.setVisibility(editMode ? View.VISIBLE : View.GONE);
        }

        if (etLocation != null && !editMode && etLocation.getText().toString().trim().isEmpty()) {
            etLocation.setHint("Add location");
        }
        if (etDescription != null && !editMode && etDescription.getText().toString().trim().isEmpty()) {
            etDescription.setHint("Add notes or event details...");
        }

        if (etTitle != null) {
            if (editMode) {
                etTitle.setBackground(null);
                etTitle.setTextSize(38f);
                etTitle.setPadding(0, dpToPx(8), 0, dpToPx(8));
            } else {
                etTitle.setTextSize(14f);
                etTitle.setBackgroundResource(R.drawable.bg_event_soft_field);
                etTitle.setPadding(dpToPx(14), 0, dpToPx(14), 0);
            }
        }

        updateAllDayState();
    }

    private void updateAllDayState() {
        boolean allDay = cbAllDay != null && cbAllDay.isChecked();
        if (btnStartTime != null) {
            btnStartTime.setEnabled(!allDay);
            btnStartTime.setAlpha(allDay ? 0.45f : 1f);
        }
        if (btnEndTime != null) {
            btnEndTime.setEnabled(!allDay);
            btnEndTime.setAlpha(allDay ? 0.45f : 1f);
        }
    }

    // ======================= Permissions =======================

    private void requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM}, 102);
            }
        }
    }

    // ======================= Utils =======================

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
