package com.timed.Features.AI;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.timed.Features.FreeSlotFinder.FreeSlot;
import com.timed.Features.FreeSlotFinder.FreeSlotAdapter;
import com.timed.R;

public class AiSchedulingActivity extends AppCompatActivity {

    private RecyclerView rvSchedules;
    private AiScheduleAdapter adapter;
    private List<AiSchedule> scheduleList;
    private FreeSlot currentSelectedAiSlot = null;
    private EditText etAiInput;
    private ImageView ivSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_scheduling);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        etAiInput = findViewById(R.id.et_ai_input);
        ivSend = findViewById(R.id.iv_send);
        rvSchedules = findViewById(R.id.rv_ai_schedules);

        ivSend.setOnClickListener(v -> {
            String prompt = etAiInput.getText().toString().trim();
            if (!prompt.isEmpty()) {
                Toast.makeText(this, "AI is thinking...", Toast.LENGTH_SHORT).show();
                etAiInput.setText(""); // Clear the input

                // MOCK DATA: Simulate a 1.5-second network delay
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {

                    // 1. Create some fake base timestamps for tomorrow
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1); // Set to tomorrow

                    // Slot 1: 10:00 AM - 11:00 AM
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 10);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    long start1 = cal.getTimeInMillis();
                    long end1 = start1 + (60 * 60 * 1000L); // + 1 hour

                    // Slot 2: 11:30 AM - 12:30 PM
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 11);
                    cal.set(java.util.Calendar.MINUTE, 30);
                    long start2 = cal.getTimeInMillis();
                    long end2 = start2 + (60 * 60 * 1000L);

                    // Slot 3: 2:00 PM - 3:00 PM
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 14);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    long start3 = cal.getTimeInMillis();
                    long end3 = start3 + (60 * 60 * 1000L);

                    // 2. Build the fake list of FreeSlots
                    List<FreeSlot> fakeSlots = new ArrayList<>();
                    fakeSlots.add(new FreeSlot("10:00 AM - 11:00 AM", "1 hour", "mock_id_1", start1, end1));
                    fakeSlots.add(new FreeSlot("11:30 AM - 12:30 PM", "1 hour", "mock_id_2", start2, end2));
                    fakeSlots.add(new FreeSlot("2:00 PM - 3:00 PM", "1 hour", "mock_id_3", start3, end3));

                    SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault());
                    String displayDate = sdf.format(cal.getTime());
                    showAiOptionsBottomSheet(displayDate, fakeSlots);

                }, 1500);
            }
        });

        setupChipListeners();

        if (rvSchedules != null) {
            rvSchedules.setLayoutManager(new LinearLayoutManager(this));

            scheduleList = new ArrayList<>();
            scheduleList.add(new AiSchedule("Team sync tomorrow at 10 AM", "Scheduled successfully", true));
            scheduleList.add(new AiSchedule("Doctor appointment on Friday", "Need confirmation", false));
            scheduleList.add(new AiSchedule("Gym every Monday 7am", "Recurring event added", true));

            adapter = new AiScheduleAdapter(scheduleList);
            rvSchedules.setAdapter(adapter);
        }
    }

    private void setupChipListeners() {
        ChipGroup chipGroup = findViewById(R.id.chip_group_try_these);

        if (chipGroup == null) return;

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);

            if (child instanceof Chip) {
                Chip chip = (Chip) child;

                chip.setOnClickListener(v -> {
                    String chipText = chip.getText().toString().replace("\"", "");

                    etAiInput.setText(chipText);

                    etAiInput.setSelection(chipText.length());
                });
            }
        }
    }

    private void showAiOptionsBottomSheet(String dateString, List<FreeSlot> generatedSlots) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        View sheetView = getLayoutInflater().inflate(R.layout.layout_ai_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvSubtitle = sheetView.findViewById(R.id.tv_sheet_subtitle);
        tvSubtitle.setText("Best available slots for " + dateString + ":");

        RecyclerView rvGeneratedSlots = sheetView.findViewById(R.id.rv_ai_generated_slots);
        Button btnConfirm = sheetView.findViewById(R.id.btn_confirm_ai);

        currentSelectedAiSlot = null;

        FreeSlotAdapter adapter = new FreeSlotAdapter(this, generatedSlots, selectedSlot -> {
            currentSelectedAiSlot = selectedSlot;
        });

        rvGeneratedSlots.setAdapter(adapter);

        btnConfirm.setOnClickListener(v -> {
            if (currentSelectedAiSlot == null) {
                Toast.makeText(this, "Please select a time slot first", Toast.LENGTH_SHORT).show();
                return;
            }

            bottomSheetDialog.dismiss();
            navigateToCreateEvent(currentSelectedAiSlot);
        });

        bottomSheetDialog.show();
    }

    private void navigateToCreateEvent(FreeSlot selectedSlot) {
        Intent intent = new Intent(this, com.timed.activities.CreateEventActivity.class);

        long startMs = selectedSlot.getStartMillis();
        long endMs = selectedSlot.getEndMillis();

        intent.putExtra("mode", "create");
        intent.putExtra("startTime", startMs);
        intent.putExtra("endTime", endMs);

        String aiGeneratedTitle = etAiInput.getText().toString().trim();
        if (!aiGeneratedTitle.isEmpty()) {
            intent.putExtra("title", aiGeneratedTitle);
        }

        startActivity(intent);

        finish();
    }
}