package com.timed.Features.FreeSlotFinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.timed.R;
import com.timed.Setting.Main.GenericSettingAdapter;
import com.timed.managers.UserManager;
import com.timed.models.CalendarModel;
import com.timed.repositories.CalendarRepository;
import com.timed.repositories.EventsRepository;
import com.timed.repositories.RepositoryCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FreeSlotFinderActivity extends AppCompatActivity {

    private RecyclerView rvSlots;
    private FreeSlotAdapter adapter;
    private FreeSlot currentSelectedSlot = null;

    private boolean isResolverMode = false;

    private List<CalendarModel> userCalendars = new ArrayList<>();
    private CalendarRepository calendarRepository;
    private EventsRepository eventsRepository;

    private RecyclerView rvDateSelector;
    private DateAdapter dateAdapter;
    private Date currentSelectedDate;
    private TextView tvBestSlots;

    private long filterDurationMs = 0; // 0 means "Any"
    private String filterTimeBound = "Any";
    private String filterCalendarId = "Any";

    private long eventDurationMs = 0;
    private long originalStartMs = 0;
    private long originalEndMs = 0;

    private Chip chipDuration, chipTime, chipCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_slot_finder);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        eventsRepository = new EventsRepository();
        calendarRepository = new CalendarRepository();

        setupDateSelector();
        setupFilterChips();
        setupRecyclerView();
        setupConfirmButton();

        isResolverMode = "RESOLVER".equals(getIntent().getStringExtra("MODE"));

        if (isResolverMode) {
            eventDurationMs = getIntent().getLongExtra("EVENT_DURATION", 0);
            originalStartMs = getIntent().getLongExtra("ORIGINAL_START", 0);
            originalEndMs = getIntent().getLongExtra("ORIGINAL_END", 0);
            if (eventDurationMs > 0) {
                filterDurationMs = eventDurationMs;

                chipDuration.setText(formatDurationCleanly(eventDurationMs));
                chipDuration.setEnabled(false);
            }
        }

        fetchUserCalendars();
    }

    private void setupDateSelector() {
        rvDateSelector = findViewById(R.id.rv_date_selector);
        tvBestSlots = findViewById(R.id.tv_best_slots);

        List<Date> upcomingDates = generateUpcomingDates(14);
        currentSelectedDate = upcomingDates.get(0);

        updateBestSlotsLabel(currentSelectedDate.getDisplayTitle());

        dateAdapter = new DateAdapter(this, upcomingDates, selectedDate -> {
            currentSelectedDate = selectedDate;
            currentSelectedSlot = null;
            updateBestSlotsLabel(selectedDate.getDisplayTitle());

            fetchFreeSlotsForDate();
        });

        rvDateSelector.setAdapter(dateAdapter);
    }

    private void fetchFreeSlotsForDate() {
        if (currentSelectedDate == null) return;

        rvSlots.setAdapter(null);
        currentSelectedSlot = null;

        eventsRepository.findFreeSlots(
                currentSelectedDate.getStartOfDayMillis(),
                filterDurationMs,
                filterTimeBound,
                filterCalendarId,
                new EventsRepository.OnFreeSlotsFoundListener() {
                    @Override
                    public void onSlotsFound(List<FreeSlot> freeSlots) {
                        adapter = new FreeSlotAdapter(FreeSlotFinderActivity.this, freeSlots, slot -> {
                            currentSelectedSlot = slot;
                        });
                        rvSlots.setAdapter(adapter);

                        if (freeSlots.isEmpty()) {
                            Toast.makeText(FreeSlotFinderActivity.this, "No free time found matching these filters.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(FreeSlotFinderActivity.this, "Error finding slots: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void fetchUserCalendars() {
        String currentUserId = UserManager.getInstance().getCurrentUser().getUid();

        calendarRepository.getCalendarsByUser(currentUserId, new RepositoryCallback<List<CalendarModel>>() {
            @Override
            public void onSuccess(List<CalendarModel> calendars) {
                userCalendars = calendars;

                if (!userCalendars.isEmpty()) {
                    // Set the default filter to their first calendar
                    CalendarModel defaultCal = userCalendars.get(0);
                    chipCalendar.setText(defaultCal.getName());
                    filterCalendarId = defaultCal.getId();

                    // NOW we can safely fetch the slots for the first time!
                    fetchFreeSlotsForDate();
                } else {
                    chipCalendar.setText("No Calendars");
                    Toast.makeText(FreeSlotFinderActivity.this, "You need a calendar to find free slots.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String e) {
                Toast.makeText(FreeSlotFinderActivity.this, "Failed to load calendars", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBestSlotsLabel(String dateString) {
        tvBestSlots.setText("Best slots for " + dateString);
    }

    private List<Date> generateUpcomingDates(int daysToGenerate) {
        List<Date> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat format = new SimpleDateFormat("EEE - dd/MM", Locale.getDefault());
        SimpleDateFormat onlyDateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (int i = 0; i < daysToGenerate; i++) {
            String title;
            if (i == 0) {
                // Example: "Today - 27/04"
                title = "Today - " + onlyDateFormat.format(calendar.getTime());
            } else if (i == 1) {
                // Example: "Tomorrow - 28/04"
                title = "Tomorrow - " + onlyDateFormat.format(calendar.getTime());
            } else {
                // Example: "Wed - 29/04"
                title = format.format(calendar.getTime());
            }

            dates.add(new Date(title, calendar.getTimeInMillis()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return dates;
    }

    private void setupFilterChips() {
        chipDuration = findViewById(R.id.chip_duration);
        chipTime = findViewById(R.id.chip_time);
        chipCalendar = findViewById(R.id.chip_calendar);

        chipDuration.setOnClickListener(v -> showDurationPicker());
        chipTime.setOnClickListener(v -> showTimePicker());
        chipCalendar.setOnClickListener(v -> showCalendarPicker());
    }

    private void showDurationPicker() {
        String[] options = {"Any duration", "30 minutes", "1 hour", "1.5 hours", "2 hours"};

        long[] valuesMs = {0, 30 * 60000L, 60 * 60000L, 90 * 60000L, 120 * 60000L};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Minimum free time needed")
                .setItems(options, (dialog, which) -> {
                    chipDuration.setText(options[which]);
                    filterDurationMs = valuesMs[which];

                    fetchFreeSlotsForDate();
                })
                .show();
    }

    private void showTimePicker() {
        String[] options = {
                "Any time",
                "Morning (5 AM - 12 PM)",
                "Afternoon (12 PM - 5 PM)",
                "Evening (5 PM - 11 PM)"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Preferred time of day")
                .setItems(options, (dialog, which) -> {
                    chipTime.setText(options[which].split(" \\(")[0]);
                    filterTimeBound = options[which];
                    fetchFreeSlotsForDate();
                })
                .show();
    }

    private void showCalendarPicker() {
        if (userCalendars == null || userCalendars.isEmpty()) {
            Toast.makeText(this, "Loading calendars...", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = new String[userCalendars.size()];
        for (int i = 0; i < userCalendars.size(); i++) {
            options[i] = userCalendars.get(i).getName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Scan specific calendar")
                .setItems(options, (dialog, which) -> {
                    CalendarModel selectedCalendar = userCalendars.get(which);
                    chipCalendar.setText(selectedCalendar.getName());
                    filterCalendarId = selectedCalendar.getId();
                    fetchFreeSlotsForDate();
                })
                .show();
    }

    private void setupRecyclerView() {
        rvSlots = findViewById(R.id.rv_free_slots);
        rvSlots.setLayoutManager(new LinearLayoutManager(this));
    }

    private String formatDurationCleanly(long durationMillis) {
        long mins = durationMillis / 60000;
        if (mins < 60) {
            return mins + " mins";
        } else {
            double hours = mins / 60.0;
            String formatted = String.format(Locale.getDefault(), "%.1f", hours).replace(".0", "");
            return formatted + (formatted.equals("1") ? " hour" : " hours");
        }
    }

    private void setupConfirmButton() {
        Button btnConfirm = findViewById(R.id.btn_confirm);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (currentSelectedSlot == null) {
                    Toast.makeText(this, "Please select a time slot first", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isResolverMode) {
                    long freeStart = currentSelectedSlot.getStartMillis();
                    long freeEnd = currentSelectedSlot.getEndMillis();

                    long finalStart;
                    long finalEnd;

                    // SCENARIO 1: Does the original time fit entirely inside this free slot?
                    if (originalStartMs >= freeStart && originalEndMs <= freeEnd) {
                        // Yes! Keep the original times.
                        finalStart = originalStartMs;
                        finalEnd = originalEndMs;
                    }
                    // SCENARIO 2: It doesn't fit perfectly. Snap to the start of the free slot.
                    else {
                        finalStart = freeStart;
                        finalEnd = freeStart + eventDurationMs;

                        // Safety check: Make sure we didn't accidentally spill over the end of the free slot
                        if (finalEnd > freeEnd) {
                            finalEnd = freeEnd;
                        }
                    }

                    // Send the timestamps back to the Resolver!
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("RESOLVED_START", finalStart);
                    resultIntent.putExtra("RESOLVED_END", finalEnd);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    // Normal independent mode behavior
                    Toast.makeText(this, "Successfully selected: " + currentSelectedSlot.getTimeRange(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }
}
