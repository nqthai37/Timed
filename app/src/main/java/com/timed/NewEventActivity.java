package com.timed;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

public class NewEventActivity extends AppCompatActivity {

    private EditText etEventTitle, etLocation, etDescription;
    private TextView tvStartDate, tvStartTime, tvEndDate, tvEndTime;
    private SwitchCompat switchAllDay;

    private LocalDate startDate = LocalDate.now();
    private LocalTime startTime = LocalTime.now().withMinute(0).plusHours(1);
    private LocalDate endDate = LocalDate.now();
    private LocalTime endTime = LocalTime.now().withMinute(0).plusHours(2);

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_new_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNav), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupDateTimePickers();

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        findViewById(R.id.btnSave).setOnClickListener(v -> saveEvent());
    }

    private void initViews() {
        etEventTitle = findViewById(R.id.etEventTitle);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvEndTime = findViewById(R.id.tvEndTime);
        switchAllDay = findViewById(R.id.switchAllDay);

        updateDateTimeDisplay();

        switchAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvStartTime.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            tvEndTime.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });
    }

    private void updateDateTimeDisplay() {
        tvStartDate.setText(startDate.format(dateFormatter));
        tvStartTime.setText(startTime.format(timeFormatter));
        tvEndDate.setText(endDate.format(dateFormatter));
        tvEndTime.setText(endTime.format(timeFormatter));
    }

    private void setupDateTimePickers() {
        findViewById(R.id.layoutStarts).setOnClickListener(v -> showDatePicker(true));
        tvStartTime.setOnClickListener(v -> showTimePicker(true));
        findViewById(R.id.layoutEnds).setOnClickListener(v -> showDatePicker(false));
        tvEndTime.setOnClickListener(v -> showTimePicker(false));
    }

    private void showDatePicker(boolean isStart) {
        LocalDate initialDate = isStart ? startDate : endDate;
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            if (isStart) {
                startDate = selectedDate;
                if (endDate.isBefore(startDate)) endDate = startDate;
            } else {
                endDate = selectedDate;
                if (endDate.isBefore(startDate)) startDate = endDate;
            }
            updateDateTimeDisplay();
        }, initialDate.getYear(), initialDate.getMonthValue() - 1, initialDate.getDayOfMonth());
        datePickerDialog.show();
    }

    private void showTimePicker(boolean isStart) {
        LocalTime initialTime = isStart ? startTime : endTime;
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            LocalTime selectedTime = LocalTime.of(hourOfDay, minute);
            if (isStart) {
                startTime = selectedTime;
                if (startDate.equals(endDate) && endTime.isBefore(startTime)) {
                    endTime = startTime.plusHours(1);
                }
            } else {
                endTime = selectedTime;
                if (startDate.equals(endDate) && endTime.isBefore(startTime)) {
                    startTime = endTime.minusHours(1);
                }
            }
            updateDateTimeDisplay();
        }, initialTime.getHour(), initialTime.getMinute(), false);
        timePickerDialog.show();
    }

    private void saveEvent() {
        String title = etEventTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        String location = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        String timeString;
        if (switchAllDay.isChecked()) {
            timeString = "All-day";
        } else {
            timeString = startTime.format(timeFormatter);
        }

        StringBuilder detailsBuilder = new StringBuilder();
        if (!location.isEmpty()) detailsBuilder.append(location);
        if (!description.isEmpty()) {
            if (detailsBuilder.length() > 0) detailsBuilder.append(" · ");
            detailsBuilder.append(description);
        }
        if (detailsBuilder.length() == 0) {
            detailsBuilder.append(startDate.format(dateFormatter));
        }

        // We'll use a static list in MainActivity for temporary storage
        Event newEvent = new Event(timeString, title, detailsBuilder.toString());
        MainActivity.addTemporaryEvent(newEvent);

        Toast.makeText(this, "Event saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}