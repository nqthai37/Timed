package com.timed.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.timed.Features.ConflictResolver.ConflictEvent;
import com.timed.R;
import com.google.firebase.Timestamp;
import com.timed.managers.EventsManager;
import com.timed.models.CalendarModel;
import com.timed.models.Event;
import com.timed.repositories.EventsRepository;
import com.timed.utils.CalendarIntegrationService;
import com.timed.utils.FirebaseInitializer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity để tạo hoặc chỉnh sửa sự kiện
 */
public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEvent";
    private static final long DEFAULT_EVENT_DURATION_MS = 60 * 60 * 1000L;
    private static final long AUTO_FIX_END_DURATION_MS = 24 * 60 * 60 * 1000L;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

    private EditText etTitle, etDescription, etLocation;
    private SwitchCompat cbAllDay;
    private Button btnStartDate, btnStartTime, btnEndDate, btnEndTime;
    private Button btnSave, btnCancel;
    private TextView tvDeleteEvent, tvScreenTitle, tvCalendarValue, tvAlertValue;
    private View layoutCalendarSelector;

    private long startTime = 0;
    private long endTime = 0;
    private EventsManager eventsManager;
    private FirebaseInitializer firebaseInitializer;
    private CalendarIntegrationService calendarIntegrationService;
    private ActivityResultLauncher<Intent> conflictResolverLauncher;
    private EventsRepository eventsRepository;
    private String mode = "create";
    private String eventId;
    private String calendarId;
    private final List<CalendarModel> calendarOptions = new ArrayList<>();
    private Event editingEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_create_event);

        initializeViews();
        setupInsets();
        initializeServices();
        readIntentData();
        setupListeners();

        if (startTime <= 0 || endTime <= 0) {
            Calendar calendar = Calendar.getInstance();
            startTime = calendar.getTimeInMillis();
            calendar.add(Calendar.HOUR, 1);
            endTime = calendar.getTimeInMillis();
        }

        ensureValidTimeRange();

        updateStartDateButton();
        updateStartTimeButton();
        updateEndDateButton();
        updateEndTimeButton();

        eventsRepository = new EventsRepository();
        conflictResolverLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            long newStart = data.getLongExtra("RESOLVED_START", -1);
                            long newEnd = data.getLongExtra("RESOLVED_END", -1);
                            if (newStart != -1 && newEnd != -1) {
                                startTime = newStart;
                                endTime = newEnd;
                                refreshDateTimeButtons();
                            }
                        }
                        proceedToSaveEvent();
                    }
                }
        );

        configureModeUI();
        loadCalendars();
    }

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
    }

    /**
     * Khởi tạo services
     */
    private void initializeServices() {
        firebaseInitializer = FirebaseInitializer.getInstance();
        firebaseInitializer.initialize(this);
        eventsManager = EventsManager.getInstance(this);
        calendarIntegrationService = new CalendarIntegrationService();
    }

    private void readIntentData() {
        mode = getIntent().getStringExtra("mode");
        if (mode == null || mode.isEmpty()) {
            mode = "create";
        }

        eventId = getIntent().getStringExtra("eventId");
        String incomingCalendarId = getIntent().getStringExtra("calendarId");
        if (incomingCalendarId != null && !incomingCalendarId.isEmpty()) {
            calendarId = incomingCalendarId;
        }

        String incomingTitle = getIntent().getStringExtra("title");
        String incomingDescription = getIntent().getStringExtra("description");
        String incomingLocation = getIntent().getStringExtra("location");
        startTime = getIntent().getLongExtra("startTime", 0L);
        endTime = getIntent().getLongExtra("endTime", 0L);
        boolean incomingAllDay = getIntent().getBooleanExtra("allDay", false);

        if (incomingTitle != null)
            etTitle.setText(incomingTitle);
        if (incomingDescription != null)
            etDescription.setText(incomingDescription);
        if (incomingLocation != null)
            etLocation.setText(incomingLocation);
        cbAllDay.setChecked(incomingAllDay);

        if (isEditMode() && eventId != null && !eventId.isEmpty()) {
            eventsManager.getEventById(eventId)
                    .addOnSuccessListener(event -> {
                        editingEvent = event;
                        if (event == null) {
                            return;
                        }
                        if (etTitle.getText().toString().trim().isEmpty())
                            etTitle.setText(event.getTitle());
                        if (etDescription.getText().toString().trim().isEmpty())
                            etDescription.setText(event.getDescription());
                        if (etLocation.getText().toString().trim().isEmpty())
                            etLocation.setText(event.getLocation());
                        if (event.getCalendarId() != null && !event.getCalendarId().isEmpty()) {
                            calendarId = event.getCalendarId();
                        }
                        if (startTime <= 0 && event.getStartTime() != null)
                            startTime = event.getStartTime().toDate().getTime();
                        if (endTime <= 0 && event.getEndTime() != null)
                            endTime = event.getEndTime().toDate().getTime();
                        cbAllDay.setChecked(event.getAllDay() != null && event.getAllDay());
                        ensureValidTimeRange();
                        updateStartDateButton();
                        updateStartTimeButton();
                        updateEndDateButton();
                        updateEndTimeButton();
                        updateCalendarLabel();
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Cannot load event detail for edit: " + e.getMessage()));
        }
    }

    private void configureModeUI() {
        if (tvScreenTitle != null) {
            tvScreenTitle.setText(isEditMode() ? "Edit Event" : "New Event");
        }
        if (btnSave != null) {
            btnSave.setText("Save");
        }
        if (tvDeleteEvent != null) {
            tvDeleteEvent.setVisibility(isEditMode() ? android.view.View.VISIBLE : android.view.View.GONE);
        }

        if (tvCalendarValue != null) {
            updateCalendarLabel();
        }

        if (tvAlertValue != null) {
            tvAlertValue.setText("30 minutes before");
        }

        if (etLocation != null && !isEditMode() && etLocation.getText().toString().trim().isEmpty()) {
            etLocation.setHint("Add location");
        }

        if (etDescription != null && !isEditMode() && etDescription.getText().toString().trim().isEmpty()) {
            etDescription.setHint("Add notes or event details...");
        }

        if (etTitle != null) {
            if (isEditMode()) {
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

    private boolean isEditMode() {
        return "edit".equalsIgnoreCase(mode);
    }

    /**
     * Setup button listeners
     */
    private void setupListeners() {
        if (btnStartDate != null) {
            btnStartDate.setOnClickListener(v -> showStartDatePicker());
        }
        if (btnStartTime != null) {
            btnStartTime.setOnClickListener(v -> showStartTimePicker());
        }
        if (btnEndDate != null) {
            btnEndDate.setOnClickListener(v -> showEndDatePicker());
        }
        if (btnEndTime != null) {
            btnEndTime.setOnClickListener(v -> showEndTimePicker());
        }
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveEvent());
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
        if (tvDeleteEvent != null) {
            tvDeleteEvent.setOnClickListener(v -> deleteEvent());
        }
        if (layoutCalendarSelector != null) {
            layoutCalendarSelector.setOnClickListener(v -> showCalendarLabelPicker());
        }
        if (cbAllDay != null) {
            cbAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllDayState());
        }
    }

    private void showCalendarLabelPicker() {
        if (calendarOptions.isEmpty()) {
            Toast.makeText(this, "No calendars available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[calendarOptions.size()];
        int selectedIndex = 0;
        for (int i = 0; i < calendarOptions.size(); i++) {
            CalendarModel calendar = calendarOptions.get(i);
            String name = calendar != null && calendar.getName() != null ? calendar.getName() : "Calendar";
            labels[i] = name;
            if (calendar != null && calendarId != null && calendarId.equals(calendar.getId())) {
                selectedIndex = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose Calendar")
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    CalendarModel selected = calendarOptions.get(which);
                    if (selected != null) {
                        calendarId = selected.getId();
                        calendarIntegrationService.setCachedDefaultCalendarId(this, calendarId);
                    }
                    updateCalendarLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCalendarLabel() {
        if (tvCalendarValue == null) {
            return;
        }
        String label = "Calendar";
        for (CalendarModel calendar : calendarOptions) {
            if (calendar != null && calendarId != null && calendarId.equals(calendar.getId())) {
                if (calendar.getName() != null && !calendar.getName().isEmpty()) {
                    label = calendar.getName();
                }
                break;
            }
        }
        tvCalendarValue.setText(label);
    }

    private void loadCalendars() {
        calendarIntegrationService.ensureDefaultCalendar(this,
                new CalendarIntegrationService.DefaultCalendarListener() {
                    @Override
                    public void onReady(String defaultId, List<CalendarModel> calendars) {
                        calendarOptions.clear();
                        if (calendars != null) {
                            calendarOptions.addAll(calendars);
                        }

                        if (calendarId == null || calendarId.isEmpty()) {
                            calendarId = defaultId;
                        } else if (!containsCalendar(calendarId)) {
                            calendarId = defaultId;
                        }

                        if (calendarId != null && !calendarId.isEmpty()) {
                            calendarIntegrationService.setCachedDefaultCalendarId(CreateEventActivity.this, calendarId);
                        }

                        updateCalendarLabel();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Failed to load calendars: " + errorMessage);
                    }
                });
    }

    private boolean containsCalendar(String id) {
        if (id == null) {
            return false;
        }
        for (CalendarModel calendar : calendarOptions) {
            if (calendar != null && id.equals(calendar.getId())) {
                return true;
            }
        }
        return false;
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

    /**
     * Đảm bảo khoảng thời gian hợp lệ (end luôn sau start)
     */
    private void ensureValidTimeRange() {
        if (startTime <= 0) {
            startTime = System.currentTimeMillis();
        }

        if (endTime <= startTime) {
            endTime = startTime + AUTO_FIX_END_DURATION_MS;
        }
    }

    private void refreshDateTimeButtons() {
        updateStartDateButton();
        updateStartTimeButton();
        updateEndDateButton();
        updateEndTimeButton();
    }

    /**
     * Hiển thị date picker cho start date
     */
    private void showStartDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    calendar.set(year, monthOfYear, dayOfMonth);
                    startTime = calendar.getTimeInMillis();
                    ensureValidTimeRange();
                    refreshDateTimeButtons();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    /**
     * Hiển thị time picker cho start time
     */
    private void showStartTimePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    startTime = calendar.getTimeInMillis();
                    ensureValidTimeRange();
                    refreshDateTimeButtons();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);

        timePickerDialog.show();
    }

    /**
     * Hiển thị date picker cho end date
     */
    private void showEndDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(endTime);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    calendar.set(year, monthOfYear, dayOfMonth);
                    endTime = calendar.getTimeInMillis();
                    ensureValidTimeRange();
                    refreshDateTimeButtons();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    /**
     * Hiển thị time picker cho end time
     */
    private void showEndTimePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(endTime);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    endTime = calendar.getTimeInMillis();
                    ensureValidTimeRange();
                    refreshDateTimeButtons();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);

        timePickerDialog.show();
    }

    /**
     * Cập nhật text của start date button
     */
    private void updateStartDateButton() {
        if (btnStartDate != null) {
            btnStartDate.setText(DATE_FORMAT.format(startTime));
        }
    }

    /**
     * Cập nhật text của start time button
     */
    private void updateStartTimeButton() {
        if (btnStartTime != null) {
            btnStartTime.setText(TIME_FORMAT.format(startTime));
        }
    }

    /**
     * Cập nhật text của end date button
     */
    private void updateEndDateButton() {
        if (btnEndDate != null) {
            btnEndDate.setText(DATE_FORMAT.format(endTime));
        }
    }

    /**
     * Cập nhật text của end time button
     */
    private void updateEndTimeButton() {
        if (btnEndTime != null) {
            btnEndTime.setText(TIME_FORMAT.format(endTime));
        }
    }

    /**
     * Lưu sự kiện
     */
    private void saveEvent() {
        String title = etTitle != null ? etTitle.getText().toString().trim() : "";

        if (title.isEmpty()) {
            if (etTitle != null) {
                etTitle.setError("Vui lòng nhập tiêu đề sự kiện");
                etTitle.requestFocus();
            }
            return;
        }

        ensureValidTimeRange();

        if (startTime >= endTime) {
            return;
        }

        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Checking...");
        }

        // SILENT BACKGROUND CHECK
        eventsRepository.checkConflictsOnDay(startTime, endTime, title, new EventsRepository.OnConflictCheckListener() {
            @Override
            public void onConflictsFound(List<ConflictEvent> conflicts) {
                if (btnSave != null) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                }

                if (conflicts.isEmpty()) {
                    proceedToSaveEvent();
                } else {
                    Intent intent = new Intent(CreateEventActivity.this, com.timed.Features.ConflictResolver.ConflictResolverActivity.class);
                    intent.putExtra("NEW_EVENT_START", startTime);
                    intent.putExtra("NEW_EVENT_END", endTime);
                    intent.putExtra("NEW_EVENT_TITLE", title);
                    conflictResolverLauncher.launch(intent);
                }
            }

            @Override
            public void onError(Exception e) {
                if (btnSave != null) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                }
                Log.e(TAG, "Error checking conflicts: " + e.getMessage());
                proceedToSaveEvent();
            }
        });
    }

    private void proceedToSaveEvent() {
        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation != null ? etLocation.getText().toString().trim() : "";
        boolean isAllDay = cbAllDay != null && cbAllDay.isChecked();

        if (isEditMode()) {
            updateExistingEvent(title, description, location, isAllDay);
            return;
        }

        ensureCalendarReadyAndSave(title, description, location, isAllDay);
    }

    private void ensureCalendarReadyAndSave(String title, String description, String location, boolean isAllDay) {
        if (calendarId == null || calendarId.isEmpty()) {
            calendarId = calendarIntegrationService.getCachedDefaultCalendarId(this);
        }
        if (calendarId != null && !calendarId.isEmpty()) {
            saveEventWithCalendar(title, description, location, isAllDay, calendarId);
            return;
        }

        calendarIntegrationService.ensureDefaultCalendar(this,
                new CalendarIntegrationService.DefaultCalendarListener() {
                    @Override
                    public void onReady(String calendarId, List<CalendarModel> calendars) {
                        CreateEventActivity.this.calendarId = calendarId;
                        saveEventWithCalendar(title, description, location, isAllDay, calendarId);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(CreateEventActivity.this,
                                "Calendar is not ready: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveEventWithCalendar(String title, String description, String location, boolean isAllDay,
            String calendarId) {
        Event newEvent = new Event();
        newEvent.setCalendarId(calendarId);
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setLocation(location);
        newEvent.setAllDay(isAllDay);
        newEvent.setStartTime(new Timestamp(new Date(startTime)));
        newEvent.setEndTime(new Timestamp(new Date(endTime)));

        String userId = firebaseInitializer.getCurrentUserId();
        if (userId != null) {
            newEvent.setCreatedBy(userId);
        }

        calendarIntegrationService.setCachedDefaultCalendarId(this, calendarId);

        eventsManager.createEvent(newEvent)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Event saved: " + docRef.getId());
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving event: " + e.getMessage(), e);
                    Toast.makeText(CreateEventActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExistingEvent(String title, String description, String location, boolean isAllDay) {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sự kiện để cập nhật", Toast.LENGTH_SHORT).show();
            return;
        }

        if (calendarId == null || calendarId.isEmpty()) {
            calendarId = calendarIntegrationService.getCachedDefaultCalendarId(this);
        }
        if (calendarId == null || calendarId.isEmpty()) {
            calendarIntegrationService.ensureDefaultCalendar(this,
                    new CalendarIntegrationService.DefaultCalendarListener() {
                        @Override
                        public void onReady(String calendarId, List<CalendarModel> calendars) {
                            CreateEventActivity.this.calendarId = calendarId;
                            calendarIntegrationService.setCachedDefaultCalendarId(CreateEventActivity.this, calendarId);
                            updateExistingEvent(title, description, location, isAllDay);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(CreateEventActivity.this,
                                    "Calendar is not ready: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
            return;
        }

        Event target = editingEvent != null ? editingEvent : new Event();
        target.setId(eventId);
        target.setCalendarId(calendarId);
        target.setTitle(title);
        target.setDescription(description);
        target.setLocation(location);
        target.setAllDay(isAllDay);
        target.setStartTime(new Timestamp(new Date(startTime)));
        target.setEndTime(new Timestamp(new Date(endTime)));

        eventsManager.updateEvent(eventId, target)
                .addOnSuccessListener(aVoid -> finish())
                .addOnFailureListener(e -> Toast.makeText(CreateEventActivity.this,
                        "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteEvent() {
        if (!isEditMode() || eventId == null || eventId.isEmpty()) {
            return;
        }

        eventsManager.deleteEvent(eventId)
                .addOnSuccessListener(aVoid -> finish())
                .addOnFailureListener(e -> Toast.makeText(CreateEventActivity.this,
                        "Lỗi xóa sự kiện: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
