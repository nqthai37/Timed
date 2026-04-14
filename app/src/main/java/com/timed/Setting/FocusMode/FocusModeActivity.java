package com.timed.Setting.FocusMode;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.timed.R;
import com.timed.repositories.FocusPresetRepository;

public class FocusModeActivity extends AppCompatActivity {

    private RecyclerView rvPresets;
    private FocusPresetAdapter adapter;
    private List<FocusPreset> presetList;
    private ImageView ivBack, ivAddPreset;
    private CountDownTimer focusTimer;
    private Button btnStartFocus;
    private TextView tvMainTimer;
    private TextView tvSubtitle;
    private CircularProgressIndicator progressTimer;

    private int currentSelectedMinutes = 25;
    private int currentSelectedSeconds = 0;
    private boolean isTimerRunning = false;

    private FocusPresetRepository repository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_mode);

        currentSelectedMinutes = getSharedPreferences("FocusPrefs", MODE_PRIVATE).getInt("SAVED_MINUTES", 25);
        currentSelectedSeconds = getSharedPreferences("FocusPrefs", MODE_PRIVATE).getInt("SAVED_SECONDS", 0);

        tvMainTimer = findViewById(R.id.tv_main_timer);
        tvSubtitle = findViewById(R.id.tv_timer_subtitle);
        progressTimer = findViewById(R.id.progress_timer);
        btnStartFocus = findViewById(R.id.btn_start_focus);
        rvPresets = findViewById(R.id.rv_focus_presets);
        ivAddPreset = findViewById(R.id.iv_add_preset);

        ivBack = findViewById(R.id.iv_back);

        setupClickListeners();

        updateTimerText(currentSelectedMinutes, currentSelectedSeconds);
        updateSubtitle(currentSelectedMinutes, currentSelectedSeconds);

        int totalSeconds = (currentSelectedMinutes * 60) + currentSelectedSeconds;
        progressTimer.setMax(totalSeconds);
        progressTimer.setProgress(totalSeconds);

        if (rvPresets != null) {
            rvPresets.setLayoutManager(new LinearLayoutManager(this));

            presetList = new ArrayList<>();

            adapter = new FocusPresetAdapter(presetList, new FocusPresetAdapter.OnPresetClickListener() {
                @Override
                public void onPresetClick(FocusPreset preset) {
                    if (isTimerRunning) {
                        Toast.makeText(FocusModeActivity.this, "Cannot change preset while running!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentSelectedMinutes = preset.getMinutes();
                    currentSelectedSeconds = preset.getSeconds();

                    updateTimerText(currentSelectedMinutes, currentSelectedSeconds);
                    updateSubtitle(currentSelectedMinutes, currentSelectedSeconds);

                    progressTimer.setMax((currentSelectedMinutes * 60) + currentSelectedSeconds);
                    progressTimer.setProgress((currentSelectedMinutes * 60) + currentSelectedSeconds);

                    saveUserPreferences(currentSelectedMinutes, currentSelectedSeconds);
                }

                @Override
                public void onPresetDeleteClick(FocusPreset preset) {
                    repository.deletePreset(preset.getId(), new FocusPresetRepository.OnPresetDeletedListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(FocusModeActivity.this, "Preset deleted!", Toast.LENGTH_SHORT).show();
                            refreshPresetList();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(FocusModeActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            rvPresets.setAdapter(adapter);

            repository = new FocusPresetRepository();

            refreshPresetList();
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnStartFocus.setOnClickListener(v -> {
            if (isTimerRunning) {
                if (focusTimer != null) focusTimer.cancel();
                isTimerRunning = false;
                btnStartFocus.setText("Start");

                updateTimerText(currentSelectedMinutes, currentSelectedSeconds);
                progressTimer.setProgress(progressTimer.getMax());
            } else {
                startFocusSession(currentSelectedMinutes, currentSelectedSeconds);
            }
        });

        ivAddPreset.setOnClickListener(v -> showCreatePresetDialog());

        tvMainTimer.setOnClickListener(v -> {
            if (!isTimerRunning) {
                showCustomTimeDialog();
            }
        });
    }

    private void updateTimerText(int minutes, int seconds) {
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvMainTimer.setText(timeFormatted);
    }

    private void updateSubtitle(int minutes, int seconds) {
        if (tvSubtitle == null) return;

        if (seconds == 0) {
            tvSubtitle.setText("Stay focused for " + minutes + " minutes");
        } else {
            tvSubtitle.setText("Stay focused for " + minutes + " min " + seconds + " sec");
        }
    }

    private void showCreatePresetDialog() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 40, 50, 10);

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Preset Name (e.g., Reading)");
        mainLayout.addView(inputTitle);

        final EditText inputDesc = new EditText(this);
        inputDesc.setHint("Description (e.g., 45 mins uninterrupted)");
        mainLayout.addView(inputDesc);

        TextView tvIconLabel = new TextView(this);
        tvIconLabel.setText("Select Icon:");
        tvIconLabel.setPadding(0, 30, 0, 10);
        mainLayout.addView(tvIconLabel);

        LinearLayout iconLayout = new LinearLayout(this);
        iconLayout.setOrientation(LinearLayout.HORIZONTAL);
        iconLayout.setGravity(Gravity.CENTER);

        final String[] selectedIconName = {"ic_focus"};

        // The icons we want to offer the user
        String[] availableIcons = {"ic_clock", "ic_focus", "ic_theme"};
        ImageView[] iconViews = new ImageView[availableIcons.length];

        for (int i = 0; i < availableIcons.length; i++) {
            ImageView iv = new ImageView(this);
            int resId = getResources().getIdentifier(availableIcons[i], "drawable", getPackageName());
            iv.setImageResource(resId != 0 ? resId : R.drawable.ic_clock);
            iv.setPadding(20, 20, 20, 20);

            if (availableIcons[i].equals(selectedIconName[0])) {
                iv.setAlpha(1.0f);
                iv.setBackgroundColor(android.graphics.Color.parseColor("#E2E8F0")); // Light gray highlight
            } else {
                iv.setAlpha(0.4f);
                iv.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(120, 120);
            params.setMargins(16, 0, 16, 0);
            iv.setLayoutParams(params);

            final int index = i;
            iv.setOnClickListener(v -> {
                selectedIconName[0] = availableIcons[index];

                for (int j = 0; j < iconViews.length; j++) {
                    if (j == index) {
                        iconViews[j].setAlpha(1.0f);
                        iconViews[j].setBackgroundColor(android.graphics.Color.parseColor("#E2E8F0"));
                    } else {
                        iconViews[j].setAlpha(0.4f);
                        iconViews[j].setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    }
                }
            });

            iconViews[i] = iv;
            iconLayout.addView(iv);
        }
        mainLayout.addView(iconLayout);

        TextView tvTimeLabel = new TextView(this);
        tvTimeLabel.setText("Select Focus Time:");
        tvTimeLabel.setPadding(0, 30, 0, 10);
        mainLayout.addView(tvTimeLabel);

        LinearLayout timeLayout = new LinearLayout(this);
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeLayout.setGravity(Gravity.CENTER);

        NumberPicker minutePicker = new NumberPicker(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(720);
        minutePicker.setValue(25);

        TextView tvColon = new TextView(this);
        tvColon.setText(" : ");
        tvColon.setTextSize(24f);
        tvColon.setTypeface(null, Typeface.BOLD);

        NumberPicker secondPicker = new NumberPicker(this);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        secondPicker.setValue(0);

        timeLayout.addView(minutePicker);
        timeLayout.addView(tvColon);
        timeLayout.addView(secondPicker);

        mainLayout.addView(timeLayout);

        new AlertDialog.Builder(this)
                .setTitle("Create New Preset")
                .setView(mainLayout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = inputTitle.getText().toString().trim();
                    String desc = inputDesc.getText().toString().trim();
                    int mins = minutePicker.getValue();
                    int secs = secondPicker.getValue();

                    if (title.isEmpty()) {
                        Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (mins == 0 && secs == 0) {
                        Toast.makeText(this, "Time cannot be zero!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FocusPreset newPreset = new FocusPreset(null, title, desc, mins, secs, selectedIconName[0]);


                    FocusPresetRepository repository = new FocusPresetRepository();
                    repository.addPreset(newPreset, new FocusPresetRepository.OnPresetAddedListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(FocusModeActivity.this, "Preset created!", Toast.LENGTH_SHORT).show();
                            refreshPresetList();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(FocusModeActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshPresetList() {
        repository.fetchUserPresets(new FocusPresetRepository.OnPresetsLoadedListener() {
            @Override
            public void onSuccess(List<FocusPreset> presets) {
                presetList.clear();
                presetList.addAll(presets);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(FocusModeActivity.this, "Failed to load presets: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showCustomTimeDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        NumberPicker minutePicker = new NumberPicker(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(720); // Max 12 hours
        minutePicker.setValue(currentSelectedMinutes);

        TextView tvColon = new TextView(this);
        tvColon.setText(" : ");
        tvColon.setTextSize(24f);
        tvColon.setTypeface(null, Typeface.BOLD);

        NumberPicker secondPicker = new NumberPicker(this);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59); // Max 59 seconds
        secondPicker.setValue(currentSelectedSeconds);

        layout.addView(minutePicker);
        layout.addView(tvColon);
        layout.addView(secondPicker);

        new AlertDialog.Builder(this)
                .setTitle("Set Custom Focus Time")
                .setView(layout)
                .setPositiveButton("Set", (dialog, which) -> {
                    currentSelectedMinutes = minutePicker.getValue();
                    currentSelectedSeconds = secondPicker.getValue();

                    if (currentSelectedMinutes == 0 && currentSelectedSeconds == 0) {
                        Toast.makeText(this, "Time cannot be zero!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateTimerText(currentSelectedMinutes, currentSelectedSeconds);
                    updateSubtitle(currentSelectedMinutes, currentSelectedSeconds);

                    int totalSeconds = (currentSelectedMinutes * 60) + currentSelectedSeconds;
                    progressTimer.setMax(totalSeconds);
                    progressTimer.setProgress(totalSeconds);

                    saveUserPreferences(currentSelectedMinutes, currentSelectedSeconds);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startFocusSession(int minutes, int seconds) {
        isTimerRunning = true;
        btnStartFocus.setText("Stop");

        long durationInMillis = (minutes * 60L * 1000L) + (seconds * 1000L);
        int totalSeconds = (int) (durationInMillis / 1000);

        progressTimer.setMax(totalSeconds);
        progressTimer.setProgress(totalSeconds);

        if (focusTimer != null) {
            focusTimer.cancel();
        }

        focusTimer = new CountDownTimer(durationInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                progressTimer.setProgress(secondsRemaining);

                int mins = secondsRemaining / 60;
                int secs = secondsRemaining % 60;
                updateTimerText(mins, secs);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnStartFocus.setText("Start");
                progressTimer.setProgress(0);
                updateTimerText(0, 0);
                Toast.makeText(FocusModeActivity.this, "Focus Session Complete!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void saveUserPreferences(int minutes, int seconds) {
        getSharedPreferences("FocusPrefs", MODE_PRIVATE)
                .edit()
                .putInt("SAVED_MINUTES", minutes)
                .putInt("SAVED_SECONDS", seconds)
                .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (focusTimer != null) focusTimer.cancel();
    }
}