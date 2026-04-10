package com.mobile.timed.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobile.timed.MainActivity;
import com.mobile.timed.R;
import com.mobile.timed.utils.EventIntegrationService;
import com.mobile.timed.utils.EventModelAdapter;

import java.util.Calendar;

/**
 * Activity để tạo hoặc chỉnh sửa sự kiện
 */
public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEvent";

    private EditText etTitle, etDescription, etLocation;
    private CheckBox cbAllDay;
    private Button btnStartDate, btnStartTime, btnEndDate, btnEndTime;
    private Button btnSave, btnCancel;

    private long startTime = 0;
    private long endTime = 0;
    private EventIntegrationService eventService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Thay bằng layout của bạn

        initializeViews();
        initializeServices();
        setupListeners();

        // Set default times
        Calendar calendar = Calendar.getInstance();
        startTime = calendar.getTimeInMillis();
        calendar.add(Calendar.HOUR, 1);
        endTime = calendar.getTimeInMillis();
    }

    /**
     * Khởi tạo views
     */
    private void initializeViews() {
        // Note: These views will be defined in activity_create_event.xml layout file
        // For now, we use null checks in setupListeners to avoid crashes
    }

    /**
     * Khởi tạo services
     */
    private void initializeServices() {
        eventService = new EventIntegrationService();
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
                updateStartDateButton();
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
                updateStartTimeButton();
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
                updateEndDateButton();
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
                updateEndTimeButton();
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
            btnStartDate.setText(EventModelAdapter.formatDate(startTime));
        }
    }

    /**
     * Cập nhật text của start time button
     */
    private void updateStartTimeButton() {
        if (btnStartTime != null) {
            btnStartTime.setText(EventModelAdapter.formatDate(startTime).split(" ")[0]);
        }
    }

    /**
     * Cập nhật text của end date button
     */
    private void updateEndDateButton() {
        if (btnEndDate != null) {
            btnEndDate.setText(EventModelAdapter.formatDate(endTime));
        }
    }

    /**
     * Cập nhật text của end time button
     */
    private void updateEndTimeButton() {
        if (btnEndTime != null) {
            btnEndTime.setText(EventModelAdapter.formatDate(endTime).split(" ")[0]);
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

        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề sự kiện", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startTime >= endTime) {
            Toast.makeText(this, "Thời gian bắt đầu phải trước thời gian kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang lưu sự kiện...", Toast.LENGTH_SHORT).show();

        // Lấy calendar ID từ intent hoặc preference
        String calendarId = getIntent().getStringExtra("calendarId");
        if (calendarId == null) {
            calendarId = "default_calendar"; // Default calendar
        }

        eventService.createSingleEvent(calendarId, title, startTime, endTime,
            description, location, isAllDay,
            new EventIntegrationService.EventSaveListener() {
                @Override
                public void onSuccess(String eventId) {
                    Log.d(TAG, "Event saved: " + eventId);
                    Toast.makeText(CreateEventActivity.this, "Sự kiện đã được tạo", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error saving event: " + errorMessage);
                    Toast.makeText(CreateEventActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
    }
}

