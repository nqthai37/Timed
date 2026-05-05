package com.timed.Setting.Timezone;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.timed.R;
import com.timed.repositories.UserRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Activity for selecting and managing the user's timezone.
 * Displays the current time in the selected timezone, the detected device timezone,
 * a searchable list of all UTC-standard timezones, and persists the selection
 * both locally (SharedPreferences) and remotely (Firestore).
 */
public class TimezoneSettingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "TimedAppPrefs";
    private static final String PREF_KEY_TIMEZONE = "selected_timezone";

    private RecyclerView rvTimezones;
    private TimezoneAdapter adapter;
    private EditText etSearch;
    private TextView tvCurrentTime;
    private TextView tvCurrentDate;
    private TextView tvDetectedLocation;
    private TextView tvSelectedTz;

    private List<TimezoneItem> allTimezones;
    private String selectedTimezoneId;
    private final Handler timeUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable timeUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timezone);

        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // Initialize views
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvCurrentDate = findViewById(R.id.tv_current_date);
        tvDetectedLocation = findViewById(R.id.tv_detected_location);
        tvSelectedTz = findViewById(R.id.tv_selected_tz);
        etSearch = findViewById(R.id.et_search_timezone);
        rvTimezones = findViewById(R.id.rv_timezone_settings);

        // Load saved timezone or use device default
        selectedTimezoneId = loadSavedTimezone();

        // Set up detected device timezone
        TimezoneItem deviceTz = TimezoneDataProvider.getDeviceTimezone();
        tvDetectedLocation.setText(deviceTz.getTimezoneId().replace("_", " ")
                .replace("/", " / "));

        // Load all timezones
        allTimezones = TimezoneDataProvider.getAllTimezones();

        // Set up RecyclerView
        if (rvTimezones != null) {
            rvTimezones.setLayoutManager(new LinearLayoutManager(this));
            adapter = new TimezoneAdapter(allTimezones, selectedTimezoneId, this::onTimezoneSelected);
            rvTimezones.setAdapter(adapter);

            // Scroll to the currently selected timezone
            scrollToSelected();
        }

        // Set up search functionality
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterTimezones(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Update the current time display
        updateTimeDisplay();
        startTimeUpdater();
    }

    /**
     * Handles timezone selection.
     */
    private void onTimezoneSelected(TimezoneItem item) {
        selectedTimezoneId = item.getTimezoneId();

        // Update display
        updateTimeDisplay();

        // Save locally (via TimezoneHelper so cache is updated)
        TimezoneHelper.setSelectedTimezoneId(this, selectedTimezoneId);

        // Save to Firestore
        saveTimezoneToFirestore(selectedTimezoneId);

        Toast.makeText(this,
                "Timezone set to " + item.getUtcOffset() + " (" + item.getDisplayName() + ")",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Filters the timezone list based on the search query.
     */
    private void filterTimezones(String query) {
        List<TimezoneItem> filtered = TimezoneDataProvider.filterTimezones(allTimezones, query);
        if (adapter != null) {
            adapter.updateList(filtered);
        }
    }

    /**
     * Updates the time and date display at the top of the screen.
     */
    private void updateTimeDisplay() {
        TimeZone tz = TimeZone.getTimeZone(selectedTimezoneId);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeFormat.setTimeZone(tz);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        dateFormat.setTimeZone(tz);

        Date now = new Date();

        if (tvCurrentTime != null) {
            tvCurrentTime.setText(timeFormat.format(now));
        }
        if (tvCurrentDate != null) {
            tvCurrentDate.setText(dateFormat.format(now));
        }

        // Update selected timezone label
        if (tvSelectedTz != null) {
            String offset = TimezoneDataProvider.formatOffset(tz.getRawOffset());
            tvSelectedTz.setText(offset + " · " + selectedTimezoneId.replace("_", " ")
                    .replace("/", " / "));
        }
    }

    /**
     * Starts a periodic task that updates the time display every second.
     */
    private void startTimeUpdater() {
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeDisplay();
                timeUpdateHandler.postDelayed(this, 1000);
            }
        };
        timeUpdateHandler.postDelayed(timeUpdateRunnable, 1000);
    }

    /**
     * Scrolls the RecyclerView to the currently selected timezone.
     */
    private void scrollToSelected() {
        if (allTimezones == null || selectedTimezoneId == null) return;

        for (int i = 0; i < allTimezones.size(); i++) {
            if (allTimezones.get(i).getTimezoneId().equals(selectedTimezoneId)) {
                int position = i;
                rvTimezones.post(() -> {
                    LinearLayoutManager lm = (LinearLayoutManager) rvTimezones.getLayoutManager();
                    if (lm != null) {
                        lm.scrollToPositionWithOffset(position, 200);
                    }
                });
                break;
            }
        }
    }

    /**
     * Loads the saved timezone from SharedPreferences.
     */
    private String loadSavedTimezone() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(PREF_KEY_TIMEZONE, TimeZone.getDefault().getID());
    }

    /**
     * Saves the selected timezone to SharedPreferences.
     */
    private void saveTimezone(String timezoneId) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_TIMEZONE, timezoneId)
                .apply();
    }

    /**
     * Saves the selected timezone to the user's Firestore document.
     */
    private void saveTimezoneToFirestore(String timezoneId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository userRepository = new UserRepository();
            Map<String, Object> updates = new HashMap<>();
            updates.put("timezone", timezoneId);
            userRepository.updateUser(currentUser.getUid(), updates);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeUpdateHandler != null && timeUpdateRunnable != null) {
            timeUpdateHandler.removeCallbacks(timeUpdateRunnable);
        }
    }
}
