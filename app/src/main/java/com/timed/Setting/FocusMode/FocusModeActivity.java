package com.timed.Setting.FocusMode;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
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

public class FocusModeActivity extends AppCompatActivity {

    private RecyclerView rvPresets;
    private FocusPresetAdapter adapter;
    private List<FocusPreset> presetList;
    private ImageView ivBack;
    private CountDownTimer focusTimer;
    private Button btnStartFocus;
    private TextView tvMainTimer;
    private TextView tvSubtitle;
    private CircularProgressIndicator progressTimer;

    private int currentSelectedMinutes = 25;
    private int currentSelectedSeconds = 0;
    private boolean isTimerRunning = false;


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
            presetList.add(new FocusPreset("Pomodoro", "25 min focus, 5 min break", R.drawable.ic_clock ));
            presetList.add(new FocusPreset("Deep Work", "90 min focus, 15 min break", R.drawable.ic_focus));
            presetList.add(new FocusPreset("Quick Break", "5 min rest", R.drawable.ic_theme ));

            adapter = new FocusPresetAdapter(presetList, preset -> {
                if (isTimerRunning) {
                    Toast.makeText(this, "Cannot change preset while running!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (preset.getTitle().equals("Pomodoro")) {
                    currentSelectedMinutes = 25;
                    currentSelectedSeconds = 0;
                } else if (preset.getTitle().equals("Deep Work")) {
                    currentSelectedMinutes = 90;
                    currentSelectedSeconds = 0;
                } else if (preset.getTitle().equals("Quick Break")) {
                    currentSelectedMinutes = 5;
                    currentSelectedSeconds = 0;
                }

                updateTimerText(currentSelectedMinutes, currentSelectedSeconds);
                updateSubtitle(currentSelectedMinutes, currentSelectedSeconds);

                progressTimer.setMax((currentSelectedMinutes * 60) + currentSelectedSeconds);
                progressTimer.setProgress((currentSelectedMinutes * 60) + currentSelectedSeconds);

                saveUserPreferences(currentSelectedMinutes, currentSelectedSeconds);

                Toast.makeText(this, "Selected: " + preset.getTitle(), Toast.LENGTH_SHORT).show();
            });
            rvPresets.setAdapter(adapter);
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
        tvColon.setTypeface(null, android.graphics.Typeface.BOLD);

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