package com.timed.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
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

import com.timed.R;
import com.google.firebase.Timestamp;
import com.timed.managers.EventsManager;
import com.timed.models.Event;
import com.timed.utils.FirebaseInitializer;
import com.timed.utils.RecurrenceUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Activity để tạo hoặc chỉnh sửa sự kiện
 */
public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEvent";
    private static final long DEFAULT_EVENT_DURATION_MS = 60 * 60 * 1000L;
    private static final long AUTO_FIX_END_DURATION_MS = 24 * 60 * 60 * 1000L;
    private static final String[] CALENDAR_LABELS = new String[]{"Work", "Personal", "Shared", "Study"};
    private static final String[] CALENDAR_IDS = new String[]{"default_calendar", "personal_calendar", "shared_calendar", "study_calendar"};
    private static final int REPEAT_NONE = 0;
    private static final int REPEAT_DAILY = 1;
    private static final int REPEAT_WEEKDAY = 2;
    private static final int REPEAT_WEEKLY_ON_DAY = 3;
    private static final int REPEAT_BI_WEEKLY_ON_DAY = 4;
    private static final int REPEAT_MONTHLY_DAY_OF_MONTH = 5;
    private static final int REPEAT_MONTHLY_ORDINAL_WEEKDAY = 6;
    private static final int REPEAT_YEARLY = 7;
    private static final int REPEAT_CUSTOM = 8;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private static final String DATE_INPUT_REGEX = "^\\d{4}-\\d{2}-\\d{2}$";
    private static final String CUSTOM_RULE_HINT = "FREQ=MONTHLY;BYDAY=MO;BYSETPOS=1";

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
    private String mode = "create";
    private String eventId;
    private String calendarId = "default_calendar";
    private String recurrenceRule = "";
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

        configureModeUI();
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

        if (incomingTitle != null) etTitle.setText(incomingTitle);
        if (incomingDescription != null) etDescription.setText(incomingDescription);
        if (incomingLocation != null) etLocation.setText(incomingLocation);
        cbAllDay.setChecked(incomingAllDay);

        if (isEditMode() && eventId != null && !eventId.isEmpty()) {
            eventsManager.getEventById(eventId)
                    .addOnSuccessListener(event -> {
                        editingEvent = event;
                        if (event == null) {
                            return;
                        }
                        if (etTitle.getText().toString().trim().isEmpty()) etTitle.setText(event.getTitle());
                        if (etDescription.getText().toString().trim().isEmpty()) etDescription.setText(event.getDescription());
                        if (etLocation.getText().toString().trim().isEmpty()) etLocation.setText(event.getLocation());
                        if (event.getCalendarId() != null && !event.getCalendarId().isEmpty()) {
                            calendarId = event.getCalendarId();
                        }
                        if (startTime <= 0 && event.getStartTime() != null) startTime = event.getStartTime().toDate().getTime();
                        if (endTime <= 0 && event.getEndTime() != null) endTime = event.getEndTime().toDate().getTime();
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
        if (cbAllDay != null) {
            cbAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllDayState());
        }
    }

    private void showCalendarLabelPicker() {
        int selectedIndex = findCalendarIndex(calendarId);
        new AlertDialog.Builder(this)
            .setTitle("Choose Label")
            .setSingleChoiceItems(CALENDAR_LABELS, selectedIndex, (dialog, which) -> {
                calendarId = CALENDAR_IDS[which];
                updateCalendarLabel();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private int findCalendarIndex(String value) {
        if (value == null) {
            return 0;
        }

        for (int i = 0; i < CALENDAR_IDS.length; i++) {
            if (CALENDAR_IDS[i].equalsIgnoreCase(value)) {
                return i;
            }
        }

        return 0;
    }

    private void updateCalendarLabel() {
        if (tvCalendarValue == null) {
            return;
        }

        int index = findCalendarIndex(calendarId);
        tvCalendarValue.setText(CALENDAR_LABELS[index]);
    }

    private void showRepeatPicker() {
        String[] repeatOptions = buildRepeatOptions();
        int selectedIndex = findRepeatIndex(recurrenceRule);
        if (selectedIndex < 0 || selectedIndex >= repeatOptions.length) {
            selectedIndex = REPEAT_NONE;
        }

        new AlertDialog.Builder(this)
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
                .show();
    }

    private void showCustomRuleDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint(CUSTOM_RULE_HINT);
        input.setText(recurrenceRule == null ? "" : recurrenceRule);
        input.setMinLines(2);

        new AlertDialog.Builder(this)
                .setTitle("Custom recurrence rule")
                .setMessage("Enter RRULE format (for example: FREQ=MONTHLY;BYDAY=MO;BYSETPOS=1)")
                .setView(input)
                .setPositiveButton("Apply", (dialog, which) -> {
                    recurrenceRule = normalizeRecurrenceRule(input.getText().toString());
                    updateRepeatLabel();
                })
                .setNeutralButton("Clear", (dialog, which) -> {
                    recurrenceRule = "";
                    updateRepeatLabel();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String[] buildRepeatOptions() {
        String weekdayLabel = getWeekdayLabelFromStart();
        int dayOfMonth = getDayOfMonthFromStart();
        int weekOrdinal = getWeekOrdinalFromStart();
        String yearDateLabel = new SimpleDateFormat("MMM d", Locale.ENGLISH).format(new Date(startTime));

        return new String[]{
                "Does not repeat",
                "Every day",
                "Every weekday Mon-Fri",
                "Every week on " + weekdayLabel,
                "Every 2 weeks on " + weekdayLabel,
                "Every month on the " + dayOfMonth + getDaySuffix(dayOfMonth),
                "Every month on the " + getOrdinalLabel(weekOrdinal) + " " + weekdayLabel,
                "Every year on " + yearDateLabel,
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
        int dayOfMonth = getDayOfMonthFromStart();
        int weekOrdinal = getWeekOrdinalFromStart();

        if (RecurrenceUtils.FREQ_DAILY.equals(frequency) && parsed.interval == 1) {
            return REPEAT_DAILY;
        }

        if (RecurrenceUtils.FREQ_WEEKLY.equals(frequency)) {
            if (parsed.interval == 1 && isWeekdaySet(parsed.byDay)) {
                return REPEAT_WEEKDAY;
            }

            if (isSingleByDayMatch(parsed.byDay, weekdayCode)) {
                if (parsed.interval == 1) {
                    return REPEAT_WEEKLY_ON_DAY;
                }
                if (parsed.interval == 2) {
                    return REPEAT_BI_WEEKLY_ON_DAY;
                }
            }
        }

        if (RecurrenceUtils.FREQ_MONTHLY.equals(frequency) && parsed.interval == 1) {
            if (containsOnlyByMonthDay(parsed.byMonthDay, dayOfMonth)) {
                return REPEAT_MONTHLY_DAY_OF_MONTH;
            }

            if (matchesMonthlyOrdinalWeekdayRule(parsed, weekdayCode, weekOrdinal)) {
                return REPEAT_MONTHLY_ORDINAL_WEEKDAY;
            }
        }

        if (RecurrenceUtils.FREQ_YEARLY.equals(frequency) && parsed.interval == 1) {
            return REPEAT_YEARLY;
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
        return weekdayCode.equalsIgnoreCase(byDay.get(0));
    }

    private boolean containsOnlyByMonthDay(List<Integer> byMonthDay, int dayOfMonth) {
        if (byMonthDay == null || byMonthDay.size() != 1 || byMonthDay.get(0) == null) {
            return false;
        }
        return byMonthDay.get(0) == dayOfMonth;
    }

    private boolean matchesMonthlyOrdinalWeekdayRule(RecurrenceUtils.RecurrenceRule rule,
                                                     String weekdayCode,
                                                     int weekOrdinal) {
        if (rule.byDayEntries != null && rule.byDayEntries.size() == 1) {
            RecurrenceUtils.ByDayEntry entry = rule.byDayEntries.get(0);
            if (entry != null && weekdayCode.equalsIgnoreCase(entry.dayCode)) {
                if (entry.ordinal != null) {
                    return entry.ordinal == weekOrdinal;
                }
                return rule.bySetPos != null
                        && rule.bySetPos.size() == 1
                        && rule.bySetPos.get(0) != null
                        && rule.bySetPos.get(0) == weekOrdinal;
            }
        }

        return false;
    }

    private String buildRuleForRepeatIndex(int index) {
        switch (index) {
            case REPEAT_DAILY:
                return "FREQ=DAILY;INTERVAL=1";
            case REPEAT_WEEKDAY:
                return "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR";
            case REPEAT_WEEKLY_ON_DAY:
                return "FREQ=WEEKLY;INTERVAL=1;BYDAY=" + getWeekdayCodeFromStart();
            case REPEAT_BI_WEEKLY_ON_DAY:
                return "FREQ=WEEKLY;INTERVAL=2;BYDAY=" + getWeekdayCodeFromStart();
            case REPEAT_MONTHLY_DAY_OF_MONTH:
                int dayOfMonth = getDayOfMonthFromStart();
                return "FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=" + dayOfMonth;
            case REPEAT_MONTHLY_ORDINAL_WEEKDAY:
                return "FREQ=MONTHLY;INTERVAL=1;BYDAY=" + getWeekdayCodeFromStart() + ";BYSETPOS=" + getWeekOrdinalFromStart();
            case REPEAT_YEARLY:
                return "FREQ=YEARLY;INTERVAL=1";
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

    private int getWeekOrdinalFromStart() {
        int dayOfMonth = getDayOfMonthFromStart();
        return ((dayOfMonth - 1) / 7) + 1;
    }

    private String getWeekdayLabelFromStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "Mon";
            case Calendar.TUESDAY:
                return "Tue";
            case Calendar.WEDNESDAY:
                return "Wed";
            case Calendar.THURSDAY:
                return "Thu";
            case Calendar.FRIDAY:
                return "Fri";
            case Calendar.SATURDAY:
                return "Sat";
            case Calendar.SUNDAY:
            default:
                return "Sun";
        }
    }

    private String getDaySuffix(int dayOfMonth) {
        int mod100 = dayOfMonth % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return "th";
        }

        switch (dayOfMonth % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    private String getOrdinalLabel(int value) {
        return value + getDaySuffix(value);
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

    private void refreshRuleForMovedStartDate() {
        int repeatIndex = findRepeatIndex(recurrenceRule);
        if (repeatIndex == REPEAT_WEEKLY_ON_DAY
                || repeatIndex == REPEAT_BI_WEEKLY_ON_DAY
                || repeatIndex == REPEAT_MONTHLY_DAY_OF_MONTH
                || repeatIndex == REPEAT_MONTHLY_ORDINAL_WEEKDAY) {
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
                etTitle.setError("Vui lòng nhập tiêu đề sự kiện");
                etTitle.requestFocus();
            }
            return;
        }

        ensureValidTimeRange();

        if (startTime >= endTime) {
            return;
        }

        if (isEditMode()) {
            updateExistingEvent(title, description, location, isAllDay);
            return;
        }

        Event newEvent = new Event();
        newEvent.setCalendarId(calendarId);
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setLocation(location);
        newEvent.setAllDay(isAllDay);
        newEvent.setStartTime(new Timestamp(new Date(startTime)));
        newEvent.setEndTime(new Timestamp(new Date(endTime)));
        newEvent.setRecurrenceRule(recurrenceRule.isEmpty() ? null : recurrenceRule);
        newEvent.setRecurrenceExceptions(recurrenceExceptions);

        String userId = firebaseInitializer.getCurrentUserId();
        if (userId != null) {
            newEvent.setCreatedBy(userId);
            if (newEvent.getParticipantId() != null && !newEvent.getParticipantId().contains(userId)) {
                newEvent.getParticipantId().add(userId);
            }
            if (newEvent.getParticipantStatus() != null) {
                newEvent.getParticipantStatus().put(userId, "accepted");
            }
        }

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

        List<String> recurrenceExceptions = parseExceptionDatesInput();
        if (recurrenceExceptions == null) {
            return;
        }

        if (recurrenceRule == null || recurrenceRule.isEmpty()) {
            recurrenceExceptions.clear();
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
        target.setRecurrenceRule(recurrenceRule.isEmpty() ? null : recurrenceRule);
        target.setRecurrenceExceptions(recurrenceExceptions);

        eventsManager.updateEvent(eventId, target)
                .addOnSuccessListener(aVoid -> finish())
                .addOnFailureListener(e -> Toast.makeText(CreateEventActivity.this,
                        "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private List<String> parseExceptionDatesInput() {
        List<String> dates = new ArrayList<>();
        if (etExceptionDates == null) {
            return dates;
        }

        String rawInput = etExceptionDates.getText().toString().trim();
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
                etExceptionDates.setError("Date must follow yyyy-MM-dd");
                etExceptionDates.requestFocus();
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
                        "Lỗi xóa sự kiện: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}

