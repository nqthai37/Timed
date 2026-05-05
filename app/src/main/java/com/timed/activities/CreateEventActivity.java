package com.timed.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.timed.R;
import com.google.firebase.Timestamp;
import com.timed.managers.EventsManager;
import com.timed.managers.UserManager;
import com.timed.models.CalendarModel;
import com.timed.models.Event;
import com.timed.models.User;
import com.timed.repositories.UserRepository;
import com.timed.utils.CalendarIntegrationService;
import com.timed.utils.FirebaseInitializer;
import com.timed.dialogs.ReminderPickerDialog;
import com.timed.utils.RecurrenceUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Set;
import java.util.TimeZone;

/**
 * Activity để tạo hoặc chỉnh sửa sự kiện
 */
public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEvent";
    private static final long DEFAULT_END_DURATION_MS = 60 * 60 * 1000L;
    private static final int REPEAT_NONE = 0;
    private static final int REPEAT_DAILY = 1;
    private static final int REPEAT_WEEKLY_ON_DAY = 2;
    private static final int REPEAT_MONTHLY_LAST_WEEKDAY = 3;
    private static final int REPEAT_YEARLY_ON_DATE = 4;
    private static final int REPEAT_WEEKDAY = 5;
    private static final int REPEAT_CUSTOM = 6;
    private static final int END_NEVER = 0;
    private static final int END_ON_DATE = 1;
    private static final int END_AFTER_COUNT = 2;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private static final String DATE_INPUT_REGEX = "^\\d{4}-\\d{2}-\\d{2}$";
    private static final String RRULE_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    private static final Locale EN_LOCALE = Locale.US;
    private static final String[] CUSTOM_UNIT_LABELS = new String[]{"day", "week", "month", "year"};
    private static final String[] CUSTOM_UNIT_CODES = new String[]{
        RecurrenceUtils.FREQ_DAILY,
        RecurrenceUtils.FREQ_WEEKLY,
        RecurrenceUtils.FREQ_MONTHLY,
        RecurrenceUtils.FREQ_YEARLY
    };
    private static final String[] CUSTOM_DAY_CODES = new String[]{"SU", "MO", "TU", "WE", "TH", "FR", "SA"};
    private static final int[] CUSTOM_DAY_VIEW_IDS = new int[]{
        R.id.chipSun,
        R.id.chipMon,
        R.id.chipTue,
        R.id.chipWed,
        R.id.chipThu,
        R.id.chipFri,
        R.id.chipSat
    };

    private EditText etTitle, etDescription, etLocation;
    private EditText etExceptionDates;
    private SwitchCompat cbAllDay;
    private Button btnStartDate, btnStartTime, btnEndDate, btnEndTime;
    private Button btnSave, btnCancel;
    private TextView tvDeleteEvent, tvScreenTitle, tvCalendarValue, tvAlertValue, tvRepeatValue;
    private View layoutCalendarSelector;
    private View layoutRepeatSelector;

    private long startTime = 0;
    private long endTime = 0;
    private EventsManager eventsManager;
    private FirebaseInitializer firebaseInitializer;
    private CalendarIntegrationService calendarIntegrationService;
    private String mode = "create";
    private String eventId;
    private String calendarId;
    private final List<CalendarModel> calendarOptions = new ArrayList<>();
    private final Map<String, CalendarModel> calendarsById = new HashMap<>();
    private final Map<String, String> ownerNameCache = new HashMap<>();
    private UserRepository userRepository;
    private String recurrenceRule = "";
    private Event editingEvent;

    private static class CustomRecurrenceState {
        int interval = 1;
        int unitIndex = 1;
        boolean[] selectedDays = new boolean[7];
        int endMode = END_NEVER;
        long endDateMillis;
        int endCount = 1;
    }
    
    // 🔔 REMINDERS: Track user-selected reminders
    private List<Long> selectedReminderMinutes = new ArrayList<>();

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
        
        // � Initialize default reminders (10 & 30 minutes)
        selectedReminderMinutes.add(10L);
        selectedReminderMinutes.add(30L);
        updateReminderDisplay();
        
        // �🔍 DEBUG: Check notification permissions & settings
        debugNotificationSetup();

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
        etExceptionDates = findViewById(R.id.etExceptionDates);
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
        tvRepeatValue = findViewById(R.id.tvRepeatValue);
        layoutCalendarSelector = findViewById(R.id.layoutCalendarSelector);
        layoutRepeatSelector = findViewById(R.id.layoutRepeatSelector);
    }

    /**
     * Khởi tạo services
     */
    private void initializeServices() {
        firebaseInitializer = FirebaseInitializer.getInstance();
        firebaseInitializer.initialize(this);
        eventsManager = EventsManager.getInstance(this);
        calendarIntegrationService = new CalendarIntegrationService();
        userRepository = new UserRepository();
        cacheOwnerName(UserManager.getInstance().getCurrentUser());
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
                        recurrenceRule = normalizeRecurrenceRule(event.getRecurrenceRule());

                        if (etExceptionDates != null) {
                            List<String> exceptions = event.getRecurrenceExceptions();
                            if (exceptions != null && !exceptions.isEmpty()) {
                                etExceptionDates.setText(String.join(", ", exceptions));
                            }
                        }

                        cbAllDay.setChecked(event.getAllDay() != null && event.getAllDay());
                        ensureValidTimeRange();
                        updateStartDateButton();
                        updateStartTimeButton();
                        updateEndDateButton();
                        updateEndTimeButton();
                        updateCalendarLabel();
                        updateRepeatLabel();
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

        if (etExceptionDates != null && !isEditMode()) {
            etExceptionDates.setText("");
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
        updateRepeatLabel();
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
        if (layoutRepeatSelector != null) {
            layoutRepeatSelector.setOnClickListener(v -> showRepeatPicker());
        }
        // 🔔 ADD REMINDER PICKER LISTENER
        if (tvAlertValue != null) {
            tvAlertValue.setOnClickListener(v -> showReminderPicker());
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
            labels[i] = formatCalendarLabel(calendar);
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
                label = formatCalendarLabel(calendar);
                break;
            }
        }
        tvCalendarValue.setText(label);
    }

    private String formatCalendarLabel(CalendarModel calendar) {
        if (calendar == null) {
            return "Calendar";
        }
        String name = calendar.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Calendar";
        } else {
            name = name.trim();
        }

        String ownerName = calendar.getOwnerName();
        if (ownerName != null) {
            ownerName = ownerName.trim();
        }
        if (ownerName != null && !ownerName.isEmpty()) {
            return name + " - " + ownerName;
        }
        return name;
    }

    private void loadCalendars() {
        calendarIntegrationService.ensureDefaultCalendar(this,
                new CalendarIntegrationService.DefaultCalendarListener() {
                    @Override
                    public void onReady(String defaultId, List<CalendarModel> calendars) {
                        calendarOptions.clear();
                        calendarsById.clear();
                        if (calendars != null) {
                            calendarOptions.addAll(calendars);
                            for (CalendarModel calendar : calendars) {
                                if (calendar != null && calendar.getId() != null) {
                                    calendarsById.put(calendar.getId(), calendar);
                                }
                            }
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
                        loadCalendarOwnerNames(calendarOptions, CreateEventActivity.this::updateCalendarLabel);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Failed to load calendars: " + errorMessage);
                    }
                });
    }

    private void cacheOwnerName(User user) {
        if (user == null) {
            return;
        }
        String userId = user.getUid();
        String name = user.getName();
        if (userId == null || name == null) {
            return;
        }
        String normalized = name.trim();
        if (!normalized.isEmpty()) {
            ownerNameCache.put(userId, normalized);
        }
    }

    private void applyOwnerNameToCalendars(List<CalendarModel> calendars, String ownerId, String ownerName) {
        if (calendars == null || ownerId == null || ownerName == null) {
            return;
        }
        for (CalendarModel calendar : calendars) {
            if (calendar != null && ownerId.equals(calendar.getOwnerId())) {
                calendar.setOwnerName(ownerName);
            }
        }
    }

    private void loadCalendarOwnerNames(List<CalendarModel> calendars, Runnable onComplete) {
        if (calendars == null || calendars.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        cacheOwnerName(UserManager.getInstance().getCurrentUser());

        if (userRepository == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        Set<String> ownerIdsToFetch = new HashSet<>();
        for (CalendarModel calendar : calendars) {
            if (calendar == null) {
                continue;
            }
            String ownerId = calendar.getOwnerId();
            if (ownerId == null || ownerId.isEmpty()) {
                continue;
            }
            String cachedName = ownerNameCache.get(ownerId);
            if (cachedName != null) {
                calendar.setOwnerName(cachedName);
            } else {
                ownerIdsToFetch.add(ownerId);
            }
        }

        if (ownerIdsToFetch.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        AtomicInteger remaining = new AtomicInteger(ownerIdsToFetch.size());
        for (String ownerId : ownerIdsToFetch) {
            userRepository.getUser(ownerId)
                    .addOnSuccessListener(snapshot -> {
                        User owner = snapshot.toObject(User.class);
                        String ownerName = owner != null ? owner.getName() : null;
                        if (ownerName != null) {
                            ownerName = ownerName.trim();
                        }
                        if (ownerName != null && !ownerName.isEmpty()) {
                            ownerNameCache.put(ownerId, ownerName);
                            applyOwnerNameToCalendars(calendars, ownerId, ownerName);
                        }
                        if (remaining.decrementAndGet() == 0 && onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0 && onComplete != null) {
                            onComplete.run();
                        }
                    });
        }
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

    private void showRepeatPicker() {
        String[] repeatOptions = buildRepeatOptions();
        int selectedIndex = findRepeatIndex(recurrenceRule);
        if (selectedIndex < 0 || selectedIndex >= repeatOptions.length) {
            selectedIndex = REPEAT_NONE;
        }

        AlertDialog repeatDialog = new AlertDialog.Builder(this)
            .setTitle("Repeat")
                .setSingleChoiceItems(repeatOptions, selectedIndex, (dialog, which) -> {
                    if (which == REPEAT_CUSTOM) {
                        dialog.dismiss();
                        showCustomRuleDialog();
                        return;
                    }

                    recurrenceRule = buildRuleForRepeatIndex(which);
                    updateRepeatLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .create();

        repeatDialog.show();
        if (repeatDialog.getWindow() != null) {
            repeatDialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_repeat_picker_dialog);
            repeatDialog.getWindow().setDimAmount(0.22f);
        }
    }

    private void showCustomRuleDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_recurrence, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.15f);
        }

        CustomRecurrenceState state = parseRuleToCustomState(recurrenceRule);

        EditText etInterval = dialogView.findViewById(R.id.etCustomInterval);
        Spinner spFrequencyUnit = dialogView.findViewById(R.id.spCustomFrequencyUnit);
        LinearLayout sectionByDay = dialogView.findViewById(R.id.sectionCustomByDay);
        RadioButton rbEndNever = dialogView.findViewById(R.id.rbEndNever);
        RadioButton rbEndOnDate = dialogView.findViewById(R.id.rbEndOnDate);
        RadioButton rbEndAfterCount = dialogView.findViewById(R.id.rbEndAfterCount);
        LinearLayout rowEndNever = dialogView.findViewById(R.id.rowEndNever);
        LinearLayout rowEndOnDate = dialogView.findViewById(R.id.rowEndOnDate);
        LinearLayout rowEndAfterCount = dialogView.findViewById(R.id.rowEndAfterCount);
        TextView tvEndDateValue = dialogView.findViewById(R.id.tvEndDateValue);
        EditText etEndAfterValue = dialogView.findViewById(R.id.etEndAfterValue);
        EditText etCustomExceptionDates = dialogView.findViewById(R.id.etCustomExceptionDates);
        TextView btnCancelCustom = dialogView.findViewById(R.id.btnCustomCancel);
        Button btnDoneCustom = dialogView.findViewById(R.id.btnCustomDone);

        etInterval.setText(String.valueOf(Math.max(1, state.interval)));
        etEndAfterValue.setText(String.valueOf(Math.max(1, state.endCount)));
        tvEndDateValue.setText(formatDialogDate(state.endDateMillis));
        if (etExceptionDates != null && etCustomExceptionDates != null) {
            etCustomExceptionDates.setText(etExceptionDates.getText().toString().trim());
        }

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CUSTOM_UNIT_LABELS) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(0xFF0F172A);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(0xFF111827);
                }
                return view;
            }
        };
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrequencyUnit.setAdapter(unitAdapter);
        spFrequencyUnit.setSelection(Math.max(0, Math.min(state.unitIndex, CUSTOM_UNIT_LABELS.length - 1)), false);

        TextView[] dayChips = new TextView[CUSTOM_DAY_VIEW_IDS.length];
        for (int i = 0; i < CUSTOM_DAY_VIEW_IDS.length; i++) {
            dayChips[i] = dialogView.findViewById(CUSTOM_DAY_VIEW_IDS[i]);
            final int index = i;
            dayChips[i].setSelected(state.selectedDays[index]);
            dayChips[i].setOnClickListener(v -> {
                state.selectedDays[index] = !state.selectedDays[index];
                dayChips[index].setSelected(state.selectedDays[index]);
            });
        }

        spFrequencyUnit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                state.unitIndex = position;
                sectionByDay.setVisibility(position == getUnitIndexFromFrequency(RecurrenceUtils.FREQ_WEEKLY)
                        ? View.VISIBLE
                        : View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No-op.
            }
        });

        View.OnClickListener setNeverMode = v -> {
            state.endMode = END_NEVER;
            syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);
        };
        View.OnClickListener setDateMode = v -> {
            state.endMode = END_ON_DATE;
            syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);
        };
        View.OnClickListener setCountMode = v -> {
            state.endMode = END_AFTER_COUNT;
            syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);
        };

        rowEndNever.setOnClickListener(setNeverMode);
        rowEndOnDate.setOnClickListener(setDateMode);
        rowEndAfterCount.setOnClickListener(setCountMode);
        rbEndNever.setOnClickListener(setNeverMode);
        rbEndOnDate.setOnClickListener(setDateMode);
        rbEndAfterCount.setOnClickListener(setCountMode);

        tvEndDateValue.setOnClickListener(v -> {
            state.endMode = END_ON_DATE;
            syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(state.endDateMillis > 0 ? state.endDateMillis : startTime);
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, monthOfYear, dayOfMonth) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.setTimeInMillis(startTime);
                        picked.set(Calendar.YEAR, year);
                        picked.set(Calendar.MONTH, monthOfYear);
                        picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        state.endDateMillis = picked.getTimeInMillis();
                        tvEndDateValue.setText(formatDialogDate(state.endDateMillis));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        btnCancelCustom.setOnClickListener(v -> dialog.dismiss());
        btnDoneCustom.setOnClickListener(v -> {
            int parsedInterval = parsePositiveInt(etInterval.getText().toString().trim(), 1);
            if (parsedInterval <= 0) {
                etInterval.setError("Value must be greater than 0");
                etInterval.requestFocus();
                return;
            }

            state.interval = parsedInterval;
            state.unitIndex = spFrequencyUnit.getSelectedItemPosition();

            int parsedCount = parsePositiveInt(etEndAfterValue.getText().toString().trim(), 1);
            if (state.endMode == END_AFTER_COUNT && parsedCount <= 0) {
                etEndAfterValue.setError("Occurrences must be greater than 0");
                etEndAfterValue.requestFocus();
                return;
            }
            state.endCount = Math.max(1, parsedCount);

            List<String> customExceptionDates = parseExceptionDatesInput(
                    etCustomExceptionDates.getText().toString().trim(),
                    etCustomExceptionDates
            );
            if (customExceptionDates == null) {
                return;
            }

            if (etExceptionDates != null) {
                etExceptionDates.setText(customExceptionDates.isEmpty()
                        ? ""
                        : String.join(", ", customExceptionDates));
            }

            recurrenceRule = buildCustomRule(state);
            updateRepeatLabel();
            dialog.dismiss();
        });

        syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);
        sectionByDay.setVisibility(state.unitIndex == getUnitIndexFromFrequency(RecurrenceUtils.FREQ_WEEKLY)
                ? View.VISIBLE
                : View.GONE);

        dialog.show();
    }

    private void syncEndModeViews(CustomRecurrenceState state,
                                  RadioButton rbEndNever,
                                  RadioButton rbEndOnDate,
                                  RadioButton rbEndAfterCount,
                                  TextView tvEndDateValue,
                                  EditText etEndAfterValue) {
        rbEndNever.setChecked(state.endMode == END_NEVER);
        rbEndOnDate.setChecked(state.endMode == END_ON_DATE);
        rbEndAfterCount.setChecked(state.endMode == END_AFTER_COUNT);

        boolean dateEnabled = state.endMode == END_ON_DATE;
        boolean countEnabled = state.endMode == END_AFTER_COUNT;

        tvEndDateValue.setEnabled(dateEnabled);
        tvEndDateValue.setAlpha(dateEnabled ? 1f : 0.5f);
        etEndAfterValue.setEnabled(countEnabled);
        etEndAfterValue.setAlpha(countEnabled ? 1f : 0.5f);
    }

    private String buildCustomRule(CustomRecurrenceState state) {
        String frequency = getFrequencyCodeAt(state.unitIndex);
        int interval = Math.max(1, state.interval);

        StringBuilder builder = new StringBuilder();
        builder.append("FREQ=").append(frequency);
        builder.append(";INTERVAL=").append(interval);

        if (RecurrenceUtils.FREQ_WEEKLY.equals(frequency)) {
            List<String> selectedDayCodes = getSelectedByDayCodes(state.selectedDays);
            if (selectedDayCodes.isEmpty()) {
                selectedDayCodes.add(getWeekdayCodeFromStart());
            }
            builder.append(";BYDAY=").append(joinWithComma(selectedDayCodes));
        }

        if (state.endMode == END_ON_DATE) {
            builder.append(";UNTIL=").append(formatUntilUtc(state.endDateMillis));
        } else if (state.endMode == END_AFTER_COUNT) {
            builder.append(";COUNT=").append(Math.max(1, state.endCount));
        }

        return builder.toString();
    }

    private String joinWithComma(List<String> values) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                joined.append(",");
            }
            joined.append(values.get(i));
        }
        return joined.toString();
    }

    private CustomRecurrenceState parseRuleToCustomState(String rule) {
        CustomRecurrenceState state = new CustomRecurrenceState();
        state.endDateMillis = startTime;
        state.endCount = 1;
        int startDayIndex = dayCodeToIndex(getWeekdayCodeFromStart());
        if (startDayIndex >= 0) {
            state.selectedDays[startDayIndex] = true;
        }

        String normalized = normalizeRecurrenceRule(rule);
        if (normalized.isEmpty()) {
            return state;
        }

        RecurrenceUtils.RecurrenceRule parsed = RecurrenceUtils.parseRRule(normalized);
        if (parsed == null || parsed.frequency == null) {
            return state;
        }

        state.interval = Math.max(1, parsed.interval);
        state.unitIndex = getUnitIndexFromFrequency(parsed.frequency);

        if (parsed.byDay != null && !parsed.byDay.isEmpty()) {
            Arrays.fill(state.selectedDays, false);
            applyByDayCodesToSelection(parsed.byDay, state.selectedDays);
            if (!hasAnySelectedDay(state.selectedDays) && startDayIndex >= 0) {
                state.selectedDays[startDayIndex] = true;
            }
        }

        if (parsed.count > 0) {
            state.endMode = END_AFTER_COUNT;
            state.endCount = parsed.count;
        } else if (parsed.until != null) {
            state.endMode = END_ON_DATE;
            state.endDateMillis = parsed.until.getTime();
        }

        return state;
    }

    private int getUnitIndexFromFrequency(String frequency) {
        if (frequency == null) {
            return 1;
        }

        String normalized = frequency.toUpperCase(Locale.US);
        for (int i = 0; i < CUSTOM_UNIT_CODES.length; i++) {
            if (CUSTOM_UNIT_CODES[i].equalsIgnoreCase(normalized)) {
                return i;
            }
        }
        return 1;
    }

    private String getFrequencyCodeAt(int unitIndex) {
        if (unitIndex < 0 || unitIndex >= CUSTOM_UNIT_CODES.length) {
            return RecurrenceUtils.FREQ_WEEKLY;
        }
        return CUSTOM_UNIT_CODES[unitIndex];
    }

    private boolean hasAnySelectedDay(boolean[] selectedDays) {
        if (selectedDays == null) {
            return false;
        }
        for (boolean selected : selectedDays) {
            if (selected) {
                return true;
            }
        }
        return false;
    }

    private void applyByDayCodesToSelection(List<String> byDayValues, boolean[] selectedDays) {
        if (selectedDays == null || selectedDays.length != 7 || byDayValues == null) {
            return;
        }

        for (String rawValue : byDayValues) {
            if (rawValue == null) {
                continue;
            }
            String token = rawValue.trim().toUpperCase(Locale.US);
            if (token.length() > 2) {
                token = token.substring(token.length() - 2);
            }
            int index = dayCodeToIndex(token);
            if (index >= 0) {
                selectedDays[index] = true;
            }
        }
    }

    private List<String> getSelectedByDayCodes(boolean[] selectedDays) {
        List<String> selected = new ArrayList<>();
        if (selectedDays == null || selectedDays.length != 7) {
            return selected;
        }
        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                selected.add(CUSTOM_DAY_CODES[i]);
            }
        }
        return selected;
    }

    private int dayCodeToIndex(String dayCode) {
        if (dayCode == null) {
            return -1;
        }
        for (int i = 0; i < CUSTOM_DAY_CODES.length; i++) {
            if (CUSTOM_DAY_CODES[i].equalsIgnoreCase(dayCode)) {
                return i;
            }
        }
        return -1;
    }

    private String[] buildRepeatOptions() {
        String weekdayLabel = getWeekdayLabelViFromStart();
        String yearlyLabel = getYearlyLabelViFromStart();

        return new String[]{
                "Does not repeat",
                "Daily",
                "Weekly on " + weekdayLabel,
                "Monthly on the last " + weekdayLabel,
                "Annually on " + yearlyLabel,
                "Every weekday (Monday to Friday)",
                "Custom..."
        };
    }

    private void updateRepeatLabel() {
        if (tvRepeatValue == null) {
            return;
        }

        String[] repeatOptions = buildRepeatOptions();
        int index = findRepeatIndex(recurrenceRule);
        if (index >= 0 && index < repeatOptions.length) {
            tvRepeatValue.setText(repeatOptions[index]);
            return;
        }

        if (recurrenceRule == null || recurrenceRule.trim().isEmpty()) {
            tvRepeatValue.setText(repeatOptions[REPEAT_NONE]);
            return;
        }

        tvRepeatValue.setText(repeatOptions[REPEAT_CUSTOM]);
    }

    private int findRepeatIndex(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return REPEAT_NONE;
        }

        String normalizedRule = rule.trim().toUpperCase(Locale.US);
        if (!normalizedRule.contains("FREQ=")) {
            return REPEAT_CUSTOM;
        }

        RecurrenceUtils.RecurrenceRule parsed = RecurrenceUtils.parseRRule(rule);
        if (parsed == null || parsed.frequency == null) {
            return REPEAT_CUSTOM;
        }

        String frequency = parsed.frequency.toUpperCase(Locale.US);
        String weekdayCode = getWeekdayCodeFromStart();

        if (RecurrenceUtils.FREQ_DAILY.equals(frequency) && parsed.interval == 1) {
            return REPEAT_DAILY;
        }

        if (RecurrenceUtils.FREQ_WEEKLY.equals(frequency)) {
            if (parsed.interval == 1 && isWeekdaySet(parsed.byDay)) {
                return REPEAT_WEEKDAY;
            }

            if (parsed.interval == 1 && isSingleByDayMatch(parsed.byDay, weekdayCode)) {
                return REPEAT_WEEKLY_ON_DAY;
            }
        }

        if (RecurrenceUtils.FREQ_MONTHLY.equals(frequency) && parsed.interval == 1) {
            if (matchesMonthlyLastWeekdayRule(parsed, weekdayCode)) {
                return REPEAT_MONTHLY_LAST_WEEKDAY;
            }
        }

        if (RecurrenceUtils.FREQ_YEARLY.equals(frequency) && parsed.interval == 1) {
            return REPEAT_YEARLY_ON_DATE;
        }

        return REPEAT_CUSTOM;
    }

    private boolean isWeekdaySet(List<String> byDay) {
        if (byDay == null || byDay.size() != 5) {
            return false;
        }

        Set<String> expected = new HashSet<>(Arrays.asList("MO", "TU", "WE", "TH", "FR"));
        Set<String> actual = new HashSet<>();

        for (String value : byDay) {
            if (value == null) {
                return false;
            }
            String normalized = value.trim().toUpperCase(Locale.US);
            if (normalized.length() != 2) {
                return false;
            }
            actual.add(normalized);
        }

        return actual.equals(expected);
    }

    private boolean isSingleByDayMatch(List<String> byDay, String weekdayCode) {
        if (byDay == null || byDay.size() != 1) {
            return false;
        }
        String token = byDay.get(0);
        if (token == null) {
            return false;
        }
        String normalized = token.trim().toUpperCase(Locale.US);
        if (normalized.length() > 2) {
            normalized = normalized.substring(normalized.length() - 2);
        }
        return weekdayCode.equalsIgnoreCase(normalized);
    }

    private boolean matchesMonthlyLastWeekdayRule(RecurrenceUtils.RecurrenceRule rule, String weekdayCode) {
        if (rule.byDayEntries != null && rule.byDayEntries.size() == 1) {
            RecurrenceUtils.ByDayEntry entry = rule.byDayEntries.get(0);
            if (entry != null && weekdayCode.equalsIgnoreCase(entry.dayCode)) {
                if (entry.ordinal != null) {
                    return entry.ordinal == -1;
                }
                return rule.bySetPos != null
                        && rule.bySetPos.size() == 1
                        && rule.bySetPos.get(0) != null
                        && rule.bySetPos.get(0) == -1;
            }
        }

        return false;
    }

    private String buildRuleForRepeatIndex(int index) {
        switch (index) {
            case REPEAT_DAILY:
                return "FREQ=DAILY;INTERVAL=1";
            case REPEAT_WEEKLY_ON_DAY:
                return "FREQ=WEEKLY;INTERVAL=1;BYDAY=" + getWeekdayCodeFromStart();
            case REPEAT_MONTHLY_LAST_WEEKDAY:
                return "FREQ=MONTHLY;INTERVAL=1;BYDAY=" + getWeekdayCodeFromStart() + ";BYSETPOS=-1";
            case REPEAT_YEARLY_ON_DATE:
                return "FREQ=YEARLY;INTERVAL=1";
            case REPEAT_WEEKDAY:
                return "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR";
            case REPEAT_NONE:
            default:
                return "";
        }
    }

    private int getDayOfMonthFromStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    private String getWeekdayLabelViFromStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            case Calendar.SUNDAY:
            default:
                return "Sunday";
        }
    }

    private String getYearlyLabelViFromStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        return month + "/" + day;
    }

    private String getWeekdayCodeFromStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "MO";
            case Calendar.TUESDAY:
                return "TU";
            case Calendar.WEDNESDAY:
                return "WE";
            case Calendar.THURSDAY:
                return "TH";
            case Calendar.FRIDAY:
                return "FR";
            case Calendar.SATURDAY:
                return "SA";
            case Calendar.SUNDAY:
            default:
                return "SU";
        }
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String formatDialogDate(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy", EN_LOCALE);
        return format.format(new Date(millis));
    }

    private String formatUntilUtc(long millis) {
        SimpleDateFormat format = new SimpleDateFormat(RRULE_DATE_FORMAT, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis));
    }

    private void refreshRuleForMovedStartDate() {
        int repeatIndex = findRepeatIndex(recurrenceRule);
        if (repeatIndex == REPEAT_WEEKLY_ON_DAY
                || repeatIndex == REPEAT_MONTHLY_LAST_WEEKDAY) {
            recurrenceRule = buildRuleForRepeatIndex(repeatIndex);
        }
        updateRepeatLabel();
    }

    private String normalizeRecurrenceRule(String rule) {
        if (rule == null) {
            return "";
        }
        return rule.trim();
    }

    private void showRepeatPicker() {
        String[] repeatOptions = buildRepeatOptions();
        int selectedIndex = findRepeatIndex(recurrenceRule);
        if (selectedIndex < 0 || selectedIndex >= repeatOptions.length) {
            selectedIndex = REPEAT_NONE;
        }

        AlertDialog repeatDialog = new AlertDialog.Builder(this)
            .setTitle("Repeat")
                .setSingleChoiceItems(repeatOptions, selectedIndex, (dialog, which) -> {
                    if (which == REPEAT_CUSTOM) {
                        dialog.dismiss();
                        showCustomRuleDialog();
                        return;
                    }

                    recurrenceRule = buildRuleForRepeatIndex(which);
                    updateRepeatLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .create();

        repeatDialog.show();
        if (repeatDialog.getWindow() != null) {
            repeatDialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_repeat_picker_dialog);
            repeatDialog.getWindow().setDimAmount(0.22f);
        }
    }

    private void showCustomRuleDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_recurrence, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.15f);
        }

        CustomRecurrenceState state = parseRuleToCustomState(recurrenceRule);

        EditText etInterval = dialogView.findViewById(R.id.etCustomInterval);
        Spinner spFrequencyUnit = dialogView.findViewById(R.id.spCustomFrequencyUnit);
        LinearLayout sectionByDay = dialogView.findViewById(R.id.sectionCustomByDay);
        RadioButton rbEndNever = dialogView.findViewById(R.id.rbEndNever);
        RadioButton rbEndOnDate = dialogView.findViewById(R.id.rbEndOnDate);
        RadioButton rbEndAfterCount = dialogView.findViewById(R.id.rbEndAfterCount);
        LinearLayout rowEndNever = dialogView.findViewById(R.id.rowEndNever);
        LinearLayout rowEndOnDate = dialogView.findViewById(R.id.rowEndOnDate);
        LinearLayout rowEndAfterCount = dialogView.findViewById(R.id.rowEndAfterCount);
        TextView tvEndDateValue = dialogView.findViewById(R.id.tvEndDateValue);
        EditText etEndAfterValue = dialogView.findViewById(R.id.etEndAfterValue);
        EditText etCustomExceptionDates = dialogView.findViewById(R.id.etCustomExceptionDates);
        TextView btnCancelCustom = dialogView.findViewById(R.id.btnCustomCancel);
        Button btnDoneCustom = dialogView.findViewById(R.id.btnCustomDone);

        etInterval.setText(String.valueOf(Math.max(1, state.interval)));
        etEndAfterValue.setText(String.valueOf(Math.max(1, state.endCount)));
        tvEndDateValue.setText(formatDialogDate(state.endDateMillis));
        if (etExceptionDates != null && etCustomExceptionDates != null) {
            etCustomExceptionDates.setText(etExceptionDates.getText().toString().trim());
        }

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CUSTOM_UNIT_LABELS) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(0xFF0F172A);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(0xFF111827);
                }
                return view;
            }
        };
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrequencyUnit.setAdapter(unitAdapter);
        spFrequencyUnit.setSelection(Math.max(0, Math.min(state.unitIndex, CUSTOM_UNIT_LABELS.length - 1)), false);

        TextView[] dayChips = new TextView[CUSTOM_DAY_VIEW_IDS.length];
        for (int i = 0; i < CUSTOM_DAY_VIEW_IDS.length; i++) {
            dayChips[i] = dialogView.findViewById(CUSTOM_DAY_VIEW_IDS[i]);
            final int index = i;
            dayChips[i].setSelected(state.selectedDays[index]);
            dayChips[i].setOnClickListener(v -> {
                state.selectedDays[index] = !state.selectedDays[index];
                dayChips[index].setSelected(state.selectedDays[index]);
            });
        }

        spFrequencyUnit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                state.unitIndex = position;
                sectionByDay.setVisibility(position == getUnitIndexFromFrequency(RecurrenceUtils.FREQ_WEEKLY)
                        ? View.VISIBLE
                        : View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No-op.
            }
        });

        View.OnClickListener setNeverMode = v -> {
            state.endMode = END_NEVER;
            syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);
        };
        View.OnClickListener setDateMode = v -> {
            state.endMode = END_ON_DATE;
            syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);
        };
        View.OnClickListener setCountMode = v -> {
            state.endMode = END_AFTER_COUNT;
            syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);
        };

        rowEndNever.setOnClickListener(setNeverMode);
        rowEndOnDate.setOnClickListener(setDateMode);
        rowEndAfterCount.setOnClickListener(setCountMode);
        rbEndNever.setOnClickListener(setNeverMode);
        rbEndOnDate.setOnClickListener(setDateMode);
        rbEndAfterCount.setOnClickListener(setCountMode);

        tvEndDateValue.setOnClickListener(v -> {
            state.endMode = END_ON_DATE;
            syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(state.endDateMillis > 0 ? state.endDateMillis : startTime);
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, monthOfYear, dayOfMonth) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.setTimeInMillis(startTime);
                        picked.set(Calendar.YEAR, year);
                        picked.set(Calendar.MONTH, monthOfYear);
                        picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        state.endDateMillis = picked.getTimeInMillis();
                        tvEndDateValue.setText(formatDialogDate(state.endDateMillis));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        btnCancelCustom.setOnClickListener(v -> dialog.dismiss());
        btnDoneCustom.setOnClickListener(v -> {
            int parsedInterval = parsePositiveInt(etInterval.getText().toString().trim(), 1);
            if (parsedInterval <= 0) {
                etInterval.setError("Value must be greater than 0");
                etInterval.requestFocus();
                return;
            }

            state.interval = parsedInterval;
            state.unitIndex = spFrequencyUnit.getSelectedItemPosition();

            int parsedCount = parsePositiveInt(etEndAfterValue.getText().toString().trim(), 1);
            if (state.endMode == END_AFTER_COUNT && parsedCount <= 0) {
                etEndAfterValue.setError("Occurrences must be greater than 0");
                etEndAfterValue.requestFocus();
                return;
            }
            state.endCount = Math.max(1, parsedCount);

            List<String> customExceptionDates = parseExceptionDatesInput(
                    etCustomExceptionDates.getText().toString().trim(),
                    etCustomExceptionDates
            );
            if (customExceptionDates == null) {
                return;
            }

            if (etExceptionDates != null) {
                etExceptionDates.setText(customExceptionDates.isEmpty()
                        ? ""
                        : String.join(", ", customExceptionDates));
            }

            recurrenceRule = buildCustomRule(state);
            updateRepeatLabel();
            dialog.dismiss();
        });

        syncEndModeViews(state, rbEndNever, rbEndOnDate, rbEndAfterCount, tvEndDateValue, etEndAfterValue);
        sectionByDay.setVisibility(state.unitIndex == getUnitIndexFromFrequency(RecurrenceUtils.FREQ_WEEKLY)
                ? View.VISIBLE
                : View.GONE);

        dialog.show();
    }

    private void syncEndModeViews(CustomRecurrenceState state,
                                  RadioButton rbEndNever,
                                  RadioButton rbEndOnDate,
                                  RadioButton rbEndAfterCount,
                                  TextView tvEndDateValue,
                                  EditText etEndAfterValue) {
        rbEndNever.setChecked(state.endMode == END_NEVER);
        rbEndOnDate.setChecked(state.endMode == END_ON_DATE);
        rbEndAfterCount.setChecked(state.endMode == END_AFTER_COUNT);

        boolean dateEnabled = state.endMode == END_ON_DATE;
        boolean countEnabled = state.endMode == END_AFTER_COUNT;

        tvEndDateValue.setEnabled(dateEnabled);
        tvEndDateValue.setAlpha(dateEnabled ? 1f : 0.5f);
        etEndAfterValue.setEnabled(countEnabled);
        etEndAfterValue.setAlpha(countEnabled ? 1f : 0.5f);
    }

    private String buildCustomRule(CustomRecurrenceState state) {
        String frequency = getFrequencyCodeAt(state.unitIndex);
        int interval = Math.max(1, state.interval);

        StringBuilder builder = new StringBuilder();
        builder.append("FREQ=").append(frequency);
        builder.append(";INTERVAL=").append(interval);

        if (RecurrenceUtils.FREQ_WEEKLY.equals(frequency)) {
            List<String> selectedDayCodes = getSelectedByDayCodes(state.selectedDays);
            if (selectedDayCodes.isEmpty()) {
                selectedDayCodes.add(getWeekdayCodeFromStart());
            }
            builder.append(";BYDAY=").append(joinWithComma(selectedDayCodes));
        }

        if (state.endMode == END_ON_DATE) {
            builder.append(";UNTIL=").append(formatUntilUtc(state.endDateMillis));
        } else if (state.endMode == END_AFTER_COUNT) {
            builder.append(";COUNT=").append(Math.max(1, state.endCount));
        }

        return builder.toString();
    }

    private String joinWithComma(List<String> values) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                joined.append(",");
            }
            joined.append(values.get(i));
        }
        return joined.toString();
    }

    private CustomRecurrenceState parseRuleToCustomState(String rule) {
        CustomRecurrenceState state = new CustomRecurrenceState();
        state.endDateMillis = startTime;
        state.endCount = 1;
        int startDayIndex = dayCodeToIndex(getWeekdayCodeFromStart());
        if (startDayIndex >= 0) {
            state.selectedDays[startDayIndex] = true;
        }

        String normalized = normalizeRecurrenceRule(rule);
        if (normalized.isEmpty()) {
            return state;
        }

        RecurrenceUtils.RecurrenceRule parsed = RecurrenceUtils.parseRRule(normalized);
        if (parsed == null || parsed.frequency == null) {
            return state;
        }

        state.interval = Math.max(1, parsed.interval);
        state.unitIndex = getUnitIndexFromFrequency(parsed.frequency);

        if (parsed.byDay != null && !parsed.byDay.isEmpty()) {
            Arrays.fill(state.selectedDays, false);
            applyByDayCodesToSelection(parsed.byDay, state.selectedDays);
            if (!hasAnySelectedDay(state.selectedDays) && startDayIndex >= 0) {
                state.selectedDays[startDayIndex] = true;
            }
        }

        if (parsed.count > 0) {
            state.endMode = END_AFTER_COUNT;
            state.endCount = parsed.count;
        } else if (parsed.until != null) {
            state.endMode = END_ON_DATE;
            state.endDateMillis = parsed.until.getTime();
        }

        return state;
    }

    private int getUnitIndexFromFrequency(String frequency) {
        if (frequency == null) {
            return 1;
        }

        String normalized = frequency.toUpperCase(Locale.US);
        for (int i = 0; i < CUSTOM_UNIT_CODES.length; i++) {
            if (CUSTOM_UNIT_CODES[i].equalsIgnoreCase(normalized)) {
                return i;
            }
        }
        return 1;
    }

    private String getFrequencyCodeAt(int unitIndex) {
        if (unitIndex < 0 || unitIndex >= CUSTOM_UNIT_CODES.length) {
            return RecurrenceUtils.FREQ_WEEKLY;
        }
        return CUSTOM_UNIT_CODES[unitIndex];
    }

    private boolean hasAnySelectedDay(boolean[] selectedDays) {
        if (selectedDays == null) {
            return false;
        }
        for (boolean selected : selectedDays) {
            if (selected) {
                return true;
            }
        }
        return false;
    }

    private void applyByDayCodesToSelection(List<String> byDayValues, boolean[] selectedDays) {
        if (selectedDays == null || selectedDays.length != 7 || byDayValues == null) {
            return;
        }

        for (String rawValue : byDayValues) {
            if (rawValue == null) {
                continue;
            }
            String token = rawValue.trim().toUpperCase(Locale.US);
            if (token.length() > 2) {
                token = token.substring(token.length() - 2);
            }
            int index = dayCodeToIndex(token);
            if (index >= 0) {
                selectedDays[index] = true;
            }
        }
    }

    private List<String> getSelectedByDayCodes(boolean[] selectedDays) {
        List<String> selected = new ArrayList<>();
        if (selectedDays == null || selectedDays.length != 7) {
            return selected;
        }
        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                selected.add(CUSTOM_DAY_CODES[i]);
            }
        }
        return selected;
    }

    private int dayCodeToIndex(String dayCode) {
        if (dayCode == null) {
            return -1;
        }
        for (int i = 0; i < CUSTOM_DAY_CODES.length; i++) {
            if (CUSTOM_DAY_CODES[i].equalsIgnoreCase(dayCode)) {
                return i;
            }
        }
        return -1;
    }

    private String[] buildRepeatOptions() {
        String weekdayLabel = getWeekdayLabelViFromStart();
        String yearlyLabel = getYearlyLabelViFromStart();

        return new String[]{
                "Does not repeat",
                "Daily",
                "Weekly on " + weekdayLabel,
                "Monthly on the last " + weekdayLabel,
                "Annually on " + yearlyLabel,
                "Every weekday (Monday to Friday)",
                "Custom..."
        };
    }

    private void updateRepeatLabel() {
        if (tvRepeatValue == null) {
            return;
        }

        String[] repeatOptions = buildRepeatOptions();
        int index = findRepeatIndex(recurrenceRule);
        if (index >= 0 && index < repeatOptions.length) {
            tvRepeatValue.setText(repeatOptions[index]);
            return;
        }

        if (recurrenceRule == null || recurrenceRule.trim().isEmpty()) {
            tvRepeatValue.setText(repeatOptions[REPEAT_NONE]);
            return;
        }

        tvRepeatValue.setText(repeatOptions[REPEAT_CUSTOM]);
    }

    private int findRepeatIndex(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return REPEAT_NONE;
        }

        String normalizedRule = rule.trim().toUpperCase(Locale.US);
        if (!normalizedRule.contains("FREQ=")) {
            return REPEAT_CUSTOM;
        }

        RecurrenceUtils.RecurrenceRule parsed = RecurrenceUtils.parseRRule(rule);
        if (parsed == null || parsed.frequency == null) {
            return REPEAT_CUSTOM;
        }

        String frequency = parsed.frequency.toUpperCase(Locale.US);
        String weekdayCode = getWeekdayCodeFromStart();

        if (RecurrenceUtils.FREQ_DAILY.equals(frequency) && parsed.interval == 1) {
            return REPEAT_DAILY;
        }

        if (RecurrenceUtils.FREQ_WEEKLY.equals(frequency)) {
            if (parsed.interval == 1 && isWeekdaySet(parsed.byDay)) {
                return REPEAT_WEEKDAY;
            }

            if (parsed.interval == 1 && isSingleByDayMatch(parsed.byDay, weekdayCode)) {
                return REPEAT_WEEKLY_ON_DAY;
            }
        }

        if (RecurrenceUtils.FREQ_MONTHLY.equals(frequency) && parsed.interval == 1) {
            if (matchesMonthlyLastWeekdayRule(parsed, weekdayCode)) {
                return REPEAT_MONTHLY_LAST_WEEKDAY;
            }
        }

        if (RecurrenceUtils.FREQ_YEARLY.equals(frequency) && parsed.interval == 1) {
            return REPEAT_YEARLY_ON_DATE;
        }

        return REPEAT_CUSTOM;
    }

    private boolean isWeekdaySet(List<String> byDay) {
        if (byDay == null || byDay.size() != 5) {
            return false;
        }

        Set<String> expected = new HashSet<>(Arrays.asList("MO", "TU", "WE", "TH", "FR"));
        Set<String> actual = new HashSet<>();

        for (String value : byDay) {
            if (value == null) {
                return false;
            }
            String normalized = value.trim().toUpperCase(Locale.US);
            if (normalized.length() != 2) {
                return false;
            }
            actual.add(normalized);
        }

        return actual.equals(expected);
    }

    private boolean isSingleByDayMatch(List<String> byDay, String weekdayCode) {
        if (byDay == null || byDay.size() != 1) {
            return false;
        }
        String token = byDay.get(0);
        if (token == null) {
            return false;
        }
        String normalized = token.trim().toUpperCase(Locale.US);
        if (normalized.length() > 2) {
            normalized = normalized.substring(normalized.length() - 2);
        }
        return weekdayCode.equalsIgnoreCase(normalized);
    }

    private boolean matchesMonthlyLastWeekdayRule(RecurrenceUtils.RecurrenceRule rule, String weekdayCode) {
        if (rule.byDayEntries != null && rule.byDayEntries.size() == 1) {
            RecurrenceUtils.ByDayEntry entry = rule.byDayEntries.get(0);
            if (entry != null && weekdayCode.equalsIgnoreCase(entry.dayCode)) {
                if (entry.ordinal != null) {
                    return entry.ordinal == -1;
                }
                return rule.bySetPos != null
                        && rule.bySetPos.size() == 1
                        && rule.bySetPos.get(0) != null
                        && rule.bySetPos.get(0) == -1;
            }
        }

        return false;
    }

    private String buildRuleForRepeatIndex(int index) {
        switch (index) {
            case REPEAT_DAILY:
                return "FREQ=DAILY;INTERVAL=1";
            case REPEAT_WEEKLY_ON_DAY:
                return "FREQ=WEEKLY;INTERVAL=1;BYDAY=" + getWeekdayCodeFromStart();
            case REPEAT_MONTHLY_LAST_WEEKDAY:
                return "FREQ=MONTHLY;INTERVAL=1;BYDAY=" + getWeekdayCodeFromStart() + ";BYSETPOS=-1";
            case REPEAT_YEARLY_ON_DATE:
                return "FREQ=YEARLY;INTERVAL=1";
            case REPEAT_WEEKDAY:
                return "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR";
            case REPEAT_NONE:
            default:
                return "";
        }
    }

    private int getDayOfMonthFromStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    private String getWeekdayLabelViFromStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            case Calendar.SUNDAY:
            default:
                return "Sunday";
        }
    }

    private String getYearlyLabelViFromStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        return month + "/" + day;
    }

    private String getWeekdayCodeFromStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "MO";
            case Calendar.TUESDAY:
                return "TU";
            case Calendar.WEDNESDAY:
                return "WE";
            case Calendar.THURSDAY:
                return "TH";
            case Calendar.FRIDAY:
                return "FR";
            case Calendar.SATURDAY:
                return "SA";
            case Calendar.SUNDAY:
            default:
                return "SU";
        }
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String formatDialogDate(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy", EN_LOCALE);
        return format.format(new Date(millis));
    }

    private String formatUntilUtc(long millis) {
        SimpleDateFormat format = new SimpleDateFormat(RRULE_DATE_FORMAT, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis));
    }

    private void refreshRuleForMovedStartDate() {
        int repeatIndex = findRepeatIndex(recurrenceRule);
        if (repeatIndex == REPEAT_WEEKLY_ON_DAY
                || repeatIndex == REPEAT_MONTHLY_LAST_WEEKDAY) {
            recurrenceRule = buildRuleForRepeatIndex(repeatIndex);
        }
        updateRepeatLabel();
    }

    private String normalizeRecurrenceRule(String rule) {
        if (rule == null) {
            return "";
        }
        return rule.trim();
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
     * Đảm bảo khoảng thời gian hợp lệ (end không trước start)
     */
    private void ensureValidTimeRange() {
        if (startTime <= 0) {
            startTime = System.currentTimeMillis();
        }

        if (endTime <= 0) {
            endTime = startTime + DEFAULT_END_DURATION_MS;
        }

        if (endTime < startTime) {
            endTime = startTime + DEFAULT_END_DURATION_MS;
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
                    refreshRuleForMovedStartDate();
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

                // End date cannot be before start date-time; minimum is equal.
                if (endTime < startTime) {
                    endTime = startTime;
                }

                refreshDateTimeButtons();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH));
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

                long candidateEndTime = calendar.getTimeInMillis();

                // If edited end time is earlier than start, roll end to next day.
                if (candidateEndTime < startTime) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    candidateEndTime = calendar.getTimeInMillis();
                }

                endTime = candidateEndTime;
                refreshDateTimeButtons();
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true);
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
        if (tvAlertValue == null) {
            return;
        }
        
        if (selectedReminderMinutes.isEmpty()) {
            tvAlertValue.setText("No reminders");
            return;
        }
        
        // Sort reminders
        List<Long> sorted = new ArrayList<>(selectedReminderMinutes);
        sorted.sort(Long::compareTo);
        
        // Format display text
        StringBuilder text = new StringBuilder();
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
        
        tvAlertValue.setText(text.toString());
        Log.d(TAG, "📌 Reminders: " + text);
    }

    /**
     * Lưu sự kiện
     */
    private void saveEvent() {
        String title = etTitle != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation != null ? etLocation.getText().toString().trim() : "";
        boolean isAllDay = cbAllDay != null && cbAllDay.isChecked();
        List<String> recurrenceExceptions = parseExceptionDatesInput();

        if (recurrenceExceptions == null) {
            return;
        }

        if (recurrenceRule == null || recurrenceRule.isEmpty()) {
            recurrenceExceptions.clear();
        }

        if (title.isEmpty()) {
            if (etTitle != null) {
                etTitle.setError("Please enter an event title");
                etTitle.requestFocus();
            }
            return;
        }

        ensureValidTimeRange();

        // Minimum allowed duration is zero; start and end can be equal.
        if (startTime > endTime) {
            endTime = startTime;
        }

        if (isEditMode()) {
            updateExistingEvent(title, description, location, isAllDay);
            return;
        }

        String userId = firebaseInitializer.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Please sign in to create events", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseInitializer.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Please sign in to create events", Toast.LENGTH_SHORT).show();
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
        CalendarModel selectedCalendar = calendarsById.get(calendarId);

        newEvent.setCalendarId(calendarId);
        if (selectedCalendar != null) {
            newEvent.setCalendarName(selectedCalendar.getName());
            newEvent.setColor(selectedCalendar.getColor());
        }
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setLocation(location);
        newEvent.setAllDay(isAllDay);
        newEvent.setStartTime(new Timestamp(new Date(startTime)));
        newEvent.setEndTime(new Timestamp(new Date(endTime)));
        newEvent.setRecurrenceRule(recurrenceRule.isEmpty() ? null : recurrenceRule);
        newEvent.setRecurrenceExceptions(recurrenceExceptions);
        newEvent.setCreatedBy(userId);

        String userId = firebaseInitializer.getCurrentUserId();
        if (userId != null) {
            newEvent.setCreatedBy(userId);
            newEvent.getParticipantId().add(userId);
            newEvent.getParticipantStatus().put(userId, "accepted");
        }

        // 🔔 ADD USER-SELECTED REMINDERS
        List<Event.EventReminder> reminders = new ArrayList<>();
        for (Long minutes : selectedReminderMinutes) {
            reminders.add(new Event.EventReminder(minutes, "push"));
        }
        newEvent.setReminders(reminders);

        Log.d(TAG, "📌 Event to save:");
        Log.d(TAG, "   Title: " + title);
        Log.d(TAG, "   Start: " + new Date(startTime));
        Log.d(TAG, "   End: " + new Date(endTime));
        Log.d(TAG, "   Reminders: " + reminders.size());
        for (Event.EventReminder r : reminders) {
            Log.d(TAG, "      - " + r.getMinutesBefore() + " min before");
        }

        calendarIntegrationService.setCachedDefaultCalendarId(this, calendarId);

        eventsManager.createEvent(newEvent)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅ Event saved: " + docRef.getId());
                    Toast.makeText(CreateEventActivity.this, 
                            "✅ Event created! Reminders set: " + selectedReminderMinutes.size(), 
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error saving event: " + e.getMessage(), e);
                    Toast.makeText(CreateEventActivity.this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExistingEvent(String title, String description, String location, boolean isAllDay) {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event not found for update", Toast.LENGTH_SHORT).show();
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

        List<String> recurrenceExceptions = parseExceptionDatesInput();
        if (recurrenceExceptions == null) {
            return;
        }

        if (recurrenceRule == null || recurrenceRule.isEmpty()) {
            recurrenceExceptions.clear();
        }

        Event target = editingEvent != null ? editingEvent : new Event();
        CalendarModel selectedCalendar = calendarsById.get(calendarId);

        String userId = firebaseInitializer.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Please sign in to update events", Toast.LENGTH_SHORT).show();
            return;
        }

        target.setId(eventId);
        target.setCalendarId(calendarId);
        if (selectedCalendar != null) {
            target.setCalendarName(selectedCalendar.getName());
            target.setColor(selectedCalendar.getColor());
        }
        target.setTitle(title);
        target.setDescription(description);
        target.setLocation(location);
        target.setAllDay(isAllDay);
        target.setStartTime(new Timestamp(new Date(startTime)));
        target.setEndTime(new Timestamp(new Date(endTime)));
        target.setRecurrenceRule(recurrenceRule.isEmpty() ? null : recurrenceRule);
        target.setRecurrenceExceptions(recurrenceExceptions);
        if (target.getCreatedBy() == null || target.getCreatedBy().trim().isEmpty()) {
            target.setCreatedBy(userId);
        }

        eventsManager.updateEvent(eventId, target)
                .addOnSuccessListener(aVoid -> finish())
                .addOnFailureListener(e -> Toast.makeText(CreateEventActivity.this,
                "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private List<String> parseExceptionDatesInput() {
        if (etExceptionDates == null) {
            return new ArrayList<>();
        }

        return parseExceptionDatesInput(etExceptionDates.getText().toString().trim(), etExceptionDates);
    }

    private List<String> parseExceptionDatesInput(String rawInput, EditText errorTarget) {
        List<String> dates = new ArrayList<>();
        if (rawInput.isEmpty()) {
            return dates;
        }

        String[] split = rawInput.split(",");
        for (String part : split) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }

            if (!value.matches(DATE_INPUT_REGEX)) {
                if (errorTarget != null) {
                    errorTarget.setError("Date must follow yyyy-MM-dd (comma-separated)");
                    errorTarget.requestFocus();
                }
                return null;
            }
            dates.add(value);
        }

        return dates;
    }

    private void deleteEvent() {
        if (!isEditMode() || eventId == null || eventId.isEmpty()) {
            return;
        }

        eventsManager.deleteEvent(eventId)
                .addOnSuccessListener(aVoid -> finish())
                .addOnFailureListener(e -> Toast.makeText(CreateEventActivity.this,
                "Error deleting event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 🔍 DEBUG: Kiểm tra permissions & settings
    private void debugNotificationSetup() {
        Log.d(TAG, "========== DEBUG NOTIFICATION SETUP ==========");
        
        // 1. Check POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int permission = ContextCompat.checkSelfPermission(this, 
                    Manifest.permission.POST_NOTIFICATIONS);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ POST_NOTIFICATIONS: GRANTED");
            } else {
                Log.e(TAG, "❌ POST_NOTIFICATIONS: DENIED - Request it!");
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    101);
            }
        } else {
            Log.d(TAG, "✅ POST_NOTIFICATIONS: Not needed (API < 33)");
        }
        
        // 2. Check SCHEDULE_EXACT_ALARM permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int permission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.SCHEDULE_EXACT_ALARM);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ SCHEDULE_EXACT_ALARM: GRANTED");
            } else {
                Log.e(TAG, "❌ SCHEDULE_EXACT_ALARM: DENIED - Request it!");
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM},
                    102);
            }
        } else {
            Log.d(TAG, "✅ SCHEDULE_EXACT_ALARM: Not needed (API < 31)");
        }
        
        // 3. Check time settings
        long nowMs = System.currentTimeMillis();
        Log.d(TAG, "Current time: " + new Date(nowMs));
        Log.d(TAG, "Event start time: " + new Date(startTime));
        
        if (startTime <= nowMs) {
            Log.e(TAG, "❌ EVENT TIME IN PAST! Set future time!");
        } else {
            long diffMs = startTime - nowMs;
            long diffMins = diffMs / (60 * 1000);
            Log.d(TAG, "✅ Event in future (" + diffMins + " minutes away)");
        }
        
        Log.d(TAG, "========================================");
    }

    // 🔍 DEBUG: Kiểm tra permissions & settings
    private void debugNotificationSetup() {
        Log.d(TAG, "========== DEBUG NOTIFICATION SETUP ==========");
        
        // 1. Check POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int permission = ContextCompat.checkSelfPermission(this, 
                    Manifest.permission.POST_NOTIFICATIONS);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ POST_NOTIFICATIONS: GRANTED");
            } else {
                Log.e(TAG, "❌ POST_NOTIFICATIONS: DENIED - Request it!");
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    101);
            }
        } else {
            Log.d(TAG, "✅ POST_NOTIFICATIONS: Not needed (API < 33)");
        }
        
        // 2. Check SCHEDULE_EXACT_ALARM permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int permission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.SCHEDULE_EXACT_ALARM);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ SCHEDULE_EXACT_ALARM: GRANTED");
            } else {
                Log.e(TAG, "❌ SCHEDULE_EXACT_ALARM: DENIED - Request it!");
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM},
                    102);
            }
        } else {
            Log.d(TAG, "✅ SCHEDULE_EXACT_ALARM: Not needed (API < 31)");
        }
        
        // 3. Check time settings
        long nowMs = System.currentTimeMillis();
        Log.d(TAG, "Current time: " + new Date(nowMs));
        Log.d(TAG, "Event start time: " + new Date(startTime));
        
        if (startTime <= nowMs) {
            Log.e(TAG, "❌ EVENT TIME IN PAST! Set future time!");
        } else {
            long diffMs = startTime - nowMs;
            long diffMins = diffMs / (60 * 1000);
            Log.d(TAG, "✅ Event in future (" + diffMins + " minutes away)");
        }
        
        Log.d(TAG, "========================================");
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
