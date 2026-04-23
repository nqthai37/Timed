package com.timed.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.timed.adapters.EventAdapter;
import com.timed.R;
import com.timed.managers.EventsManager;
import com.timed.models.Event;
import com.timed.utils.FirebaseInitializer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchFilterActivity extends AppCompatActivity {
    private static final String TAG = "SearchFilterActivity";
    private static final String[] CALENDAR_LABELS = new String[] {
            "All", "Work", "Personal", "Shared", "Study"
    };
    private static final String[] CALENDAR_IDS = new String[] {
            "", "default_calendar", "personal_calendar", "shared_calendar", "study_calendar"
    };
    private static final String[] STATUS_LABELS = new String[] {
            "Any", "Completed", "Pending"
    };

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    private EventsManager eventsManager;
    private FirebaseInitializer firebaseInitializer;

    private EditText etQuery;
    private EditText etLocation;
    private Button btnStartDate;
    private Button btnEndDate;
    private Spinner spCalendar;
    private Spinner spStatus;
    private Button btnApply;
    private Button btnClear;
    private TextView tvResultsSummary;
    private RecyclerView rvResults;

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();
    private EventAdapter eventAdapter;

    private long startDateMillis;
    private long endDateMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_filter);

        eventsManager = EventsManager.getInstance(this);
        firebaseInitializer = FirebaseInitializer.getInstance();
        firebaseInitializer.initialize(this);

        bindViews();
        setupSpinners();
        setupDefaultDates();
        setupListeners();

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter(filteredEvents, this::openEditEvent);
        rvResults.setAdapter(eventAdapter);

        applyFilters();
    }

    private void bindViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        etQuery = findViewById(R.id.etQuery);
        etLocation = findViewById(R.id.etLocation);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        spCalendar = findViewById(R.id.spCalendar);
        spStatus = findViewById(R.id.spStatus);
        btnApply = findViewById(R.id.btnApply);
        btnClear = findViewById(R.id.btnClear);
        tvResultsSummary = findViewById(R.id.tvResultsSummary);
        rvResults = findViewById(R.id.rvResults);
    }

    private void setupSpinners() {
        ArrayAdapter<String> calendarAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CALENDAR_LABELS);
        spCalendar.setAdapter(calendarAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, STATUS_LABELS);
        spStatus.setAdapter(statusAdapter);
    }

    private void setupDefaultDates() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        startDateMillis = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        endDateMillis = calendar.getTimeInMillis();
        updateDateButtons();
    }

    private void setupListeners() {
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));

        btnApply.setOnClickListener(v -> applyFilters());
        btnClear.setOnClickListener(v -> {
            etQuery.setText("");
            etLocation.setText("");
            spCalendar.setSelection(0);
            spStatus.setSelection(0);
            setupDefaultDates();
            applyFilters();
        });
    }

    private void showDatePicker(boolean isStart) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(isStart ? startDateMillis : endDateMillis);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth, 0, 0, 0);
                    picked.set(Calendar.MILLISECOND, 0);
                    if (isStart) {
                        startDateMillis = picked.getTimeInMillis();
                        if (endDateMillis < startDateMillis) {
                            endDateMillis = startDateMillis;
                        }
                    } else {
                        endDateMillis = picked.getTimeInMillis();
                        if (endDateMillis < startDateMillis) {
                            startDateMillis = endDateMillis;
                        }
                    }
                    updateDateButtons();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updateDateButtons() {
        btnStartDate.setText(dateFormat.format(new Date(startDateMillis)));
        btnEndDate.setText(dateFormat.format(new Date(endDateMillis)));
    }

    private void applyFilters() {
        loadEventsFromSelection();
    }

    private void loadEventsFromSelection() {
        allEvents.clear();
        String calendarId = CALENDAR_IDS[spCalendar.getSelectedItemPosition()];

        if (calendarId == null || calendarId.isEmpty()) {
            List<String> ids = new ArrayList<>();
            for (int i = 1; i < CALENDAR_IDS.length; i++) {
                ids.add(CALENDAR_IDS[i]);
            }
            loadEventsSequential(ids, 0);
        } else {
            loadEventsForCalendar(calendarId, () -> applyFiltersToResults());
        }
    }

    private void loadEventsSequential(List<String> ids, int index) {
        if (index >= ids.size()) {
            applyFiltersToResults();
            return;
        }

        loadEventsForCalendar(ids.get(index), () -> loadEventsSequential(ids, index + 1));
    }

    private void loadEventsForCalendar(String calendarId, Runnable onDone) {
        Timestamp startTimestamp = new Timestamp(new Date(startDateMillis));
        Timestamp endTimestamp = new Timestamp(new Date(endDateMillis + (24 * 60 * 60 * 1000L) - 1));

        eventsManager.getEventsByDateRange(calendarId, startTimestamp, endTimestamp)
                .addOnSuccessListener(events -> {
                    if (events != null) {
                        allEvents.addAll(events);
                    }
                    onDone.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events for calendar " + calendarId + ": " + e.getMessage(), e);
                    onDone.run();
                });
    }

    private void applyFiltersToResults() {
        String query = etQuery.getText() != null ? etQuery.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";
        String statusFilter = STATUS_LABELS[spStatus.getSelectedItemPosition()];
        String userId = firebaseInitializer.getCurrentUserId();

        filteredEvents.clear();
        for (Event event : allEvents) {
            if (!matchesQuery(event, query)) {
                continue;
            }
            if (!matchesLocation(event, location)) {
                continue;
            }
            if (!matchesStatus(event, statusFilter, userId)) {
                continue;
            }
            filteredEvents.add(event);
        }

        if (tvResultsSummary != null) {
            tvResultsSummary.setText("Results: " + filteredEvents.size());
        }
        eventAdapter.notifyDataSetChanged();
    }

    private boolean matchesQuery(Event event, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String title = safeLower(event.getTitle());
        String description = safeLower(event.getDescription());
        String location = safeLower(event.getLocation());

        return title.contains(query) || description.contains(query) || location.contains(query);
    }

    private boolean matchesLocation(Event event, String location) {
        if (location.isEmpty()) {
            return true;
        }
        return safeLower(event.getLocation()).contains(location);
    }

    private boolean matchesStatus(Event event, String statusFilter, String userId) {
        if (statusFilter == null || "Any".equalsIgnoreCase(statusFilter)) {
            return true;
        }

        String status = null;
        if (event.getParticipantStatus() != null && userId != null) {
            status = event.getParticipantStatus().get(userId);
        }

        if ("Completed".equalsIgnoreCase(statusFilter)) {
            return status != null && ("completed".equalsIgnoreCase(status)
                    || "done".equalsIgnoreCase(status)
                    || "accepted".equalsIgnoreCase(status));
        }

        if ("Pending".equalsIgnoreCase(statusFilter)) {
            return status == null || "pending".equalsIgnoreCase(status) || "invited".equalsIgnoreCase(status);
        }

        return true;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.getDefault());
    }

    private void openEditEvent(Event event) {
        Intent intent = new Intent(this, CreateEventActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("eventId", event.getId());
        intent.putExtra("calendarId", event.getCalendarId() != null ? event.getCalendarId() : "default_calendar");
        intent.putExtra("title", event.getTitle());
        intent.putExtra("description", event.getDescription());
        intent.putExtra("location", event.getLocation());
        intent.putExtra("startTime", event.getStartTime() != null ? event.getStartTime().toDate().getTime() : 0L);
        intent.putExtra("endTime", event.getEndTime() != null ? event.getEndTime().toDate().getTime() : 0L);
        intent.putExtra("allDay", event.getAllDay() != null && event.getAllDay());
        startActivity(intent);
    }
}
