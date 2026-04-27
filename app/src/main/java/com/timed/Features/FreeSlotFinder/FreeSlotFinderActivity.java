package com.timed.Features.FreeSlotFinder;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FreeSlotFinderActivity extends AppCompatActivity {

    private RecyclerView rvSlots;
    private FreeSlotAdapter adapter;
    private FreeSlot currentSelectedSlot = null;

    private RecyclerView rvDateSelector;
    private DateAdapter dateAdapter;
    private Date currentSelectedDate;
    private TextView tvBestSlots;

    private long filterDurationMs = 0; // 0 means "Any"
    private String filterTimeBound = "Any";
    private String filterCalendarId = "Any";

    private Chip chipDuration, chipTime, chipCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_slot_finder);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        setupDateSelector();
        setupFilterChips();
        setupRecyclerView();
        setupConfirmButton();
    }

    private void setupDateSelector() {
        rvDateSelector = findViewById(R.id.rv_date_selector);
        tvBestSlots = findViewById(R.id.tv_best_slots);

        List<Date> upcomingDates = generateUpcomingDates(14);
        currentSelectedDate = upcomingDates.get(0);

        updateBestSlotsLabel(currentSelectedDate.getDisplayTitle());

        dateAdapter = new DateAdapter(this, upcomingDates, selectedDate -> {
            currentSelectedDate = selectedDate;
            updateBestSlotsLabel(selectedDate.getDisplayTitle());

            // TODO: Trigger your algorithm here to fetch new slots for this date!
            // fetchFreeSlotsForDate(selectedDate.getStartOfDayMillis());
        });

        rvDateSelector.setAdapter(dateAdapter);
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

                    // refreshSlots();
                })
                .show();
    }

    private void showTimePicker() {
        String[] options = {
                "Any time",
                "Morning (8 AM - 12 PM)",
                "Afternoon (12 PM - 5 PM)",
                "Evening (5 PM - 9 PM)"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Preferred time of day")
                .setItems(options, (dialog, which) -> {
                    chipTime.setText(options[which].split(" \\(")[0]);
                    filterTimeBound = options[which];
                })
                .show();
    }

    private void showCalendarPicker() {
        String[] options = {"Any calendar", "Personal", "Work", "School"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Scan specific calendar")
                .setItems(options, (dialog, which) -> {
                    chipCalendar.setText(options[which]);
                    filterCalendarId = options[which];
                })
                .show();
    }

    private void setupRecyclerView() {
        rvSlots = findViewById(R.id.rv_free_slots);
        if (rvSlots != null) {
            rvSlots.setLayoutManager(new LinearLayoutManager(this));

            List<FreeSlot> slotList = new ArrayList<>();
            slotList.add(new FreeSlot("10:30 AM - 11:30 AM", "1 hour • Perfect fit", "slot_1"));
            slotList.add(new FreeSlot("2:00 PM - 3:30 PM", "1.5 hours • Afternoon", "slot_2"));
            slotList.add(new FreeSlot("09:00 AM - 10:00 AM", "1 hour • Morning Sync", "slot_3"));
            slotList.add(new FreeSlot("4:00 PM - 5:00 PM", "1 hour • End of week", "slot_4"));

            adapter = new FreeSlotAdapter(this, slotList, slot -> {
                currentSelectedSlot = slot;
            });

            rvSlots.setAdapter(adapter);
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

                Toast.makeText(this, "Successfully selected: " + currentSelectedSlot.getTimeRange(), Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }
}
