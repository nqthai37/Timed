package com.timed.Features.AI;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.timed.Features.FreeSlotFinder.FreeSlot;
import com.timed.Features.FreeSlotFinder.FreeSlotAdapter;
import com.timed.R;
import com.timed.managers.UserManager;
import com.timed.models.CalendarModel;
import com.timed.repositories.AiRepository;
import com.timed.repositories.CalendarRepository;
import com.timed.repositories.EventsRepository;
import com.timed.repositories.RepositoryCallback;
import com.timed.repositories.TemplateRepository;

public class AiSchedulingActivity extends AppCompatActivity {

    private RecyclerView rvSchedules;
    private AiScheduleAdapter adapter;
    private List<AiSchedule> scheduleList;
    private FreeSlot currentSelectedAiSlot = null;
    private EventsRepository eventsRepository;
    private TextView tvSelectedCalendar;
    private String selectedCalendarId = "";
    private BottomSheetDialog aiSheetDialog;
    private View sheetView;
    private CalendarRepository calendarRepository;
    private TemplateRepository templateRepository;
    private List<String> calendarNames = new ArrayList<>();
    private List<String> calendarIds = new ArrayList<>();
    private EditText etAiInput;
    private ImageView ivSend;
    private AiRepository aiRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_scheduling);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        etAiInput = findViewById(R.id.et_ai_input);
        ivSend = findViewById(R.id.iv_send);
        rvSchedules = findViewById(R.id.rv_ai_schedules);

        aiRepository = new AiRepository();
        eventsRepository = new EventsRepository();
        calendarRepository = new CalendarRepository();
        templateRepository = new TemplateRepository();


        setupPromptButton();
        setupChipListeners();
        setupCalendarSelector();

        loadSavedTemplates();
    }

    private void loadSavedTemplates() {
        String currentUserId = UserManager.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) return;

        if (rvSchedules != null) {
            rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        }

        templateRepository.getUserTemplates(currentUserId, new RepositoryCallback<List<AiPromptTemplate>>() {
            @Override
            public void onSuccess(List<AiPromptTemplate> templates) {
                runOnUiThread(() -> {
                    TextView tvEmpty = findViewById(R.id.tv_empty_templates);
                    if (templates.isEmpty()) {
                        rvSchedules.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvSchedules.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);

                        scheduleList = new ArrayList<>();

                        for (AiPromptTemplate qt : templates) {
                            scheduleList.add(new AiSchedule(qt.getId(), qt.getPromptText(), qt.getTitle(), true));
                        }

                        adapter = new AiScheduleAdapter(scheduleList,
                                clickedTemplate -> {
                            etAiInput.setText(clickedTemplate.getTitle());
                            ivSend.performClick();
                        }, longClickedTemplate -> {
                            new android.app.AlertDialog.Builder(AiSchedulingActivity.this)
                                    .setTitle("Delete Template")
                                    .setMessage("Are you sure you want to delete '" + longClickedTemplate.getStatus() + "'?")
                                    .setPositiveButton("Delete", (dialog, which) -> {

                                        templateRepository.deleteTemplate(currentUserId, longClickedTemplate.getId(), new RepositoryCallback<Void>() {
                                            @Override
                                            public void onSuccess(Void data) {
                                                runOnUiThread(() -> {
                                                    Toast.makeText(AiSchedulingActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                                    loadSavedTemplates();
                                                });
                                            }

                                            @Override
                                            public void onFailure(String e) {
                                                runOnUiThread(() -> Toast.makeText(AiSchedulingActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show());
                                            }
                                        });

                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });

                        rvSchedules.setAdapter(adapter);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(AiSchedulingActivity.this, "Failed to load templates", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupPromptButton() {
        ivSend.setOnClickListener(v -> {
            String prompt = etAiInput.getText().toString().trim();
            if (!prompt.isEmpty()) {
                openBottomSheetInLoadingState();
                etAiInput.setText("");

                aiRepository.extractScheduleFromText(prompt, new AiRepository.AiExtractionCallback() {
                    @Override
                    public void onSuccess(AiScheduleRequest extractedData) {
                        runOnUiThread(() -> {
                            long targetDateMillis = convertKeywordToMillis(extractedData.dayKeyword);

                            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
                            String displayDate = sdf.format(new Date(targetDateMillis));

                            eventsRepository.findFreeSlots(
                                    targetDateMillis,
                                    extractedData.durationMs,
                                    extractedData.timeBound,
                                    selectedCalendarId,
                                    new EventsRepository.OnFreeSlotsFoundListener() {
                                        @Override
                                        public void onSlotsFound(List<FreeSlot> freeSlots) {
                                            if (freeSlots.isEmpty()) {
                                                Toast.makeText(AiSchedulingActivity.this, "No free slots found for that time.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                List<FreeSlot> top3 = carveProposedSlots(freeSlots, targetDateMillis, extractedData);

                                                if (top3.isEmpty()) {
                                                    Toast.makeText(AiSchedulingActivity.this, "Couldn't fit the event in your schedule.", Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                etAiInput.setText(extractedData.title);
                                                updateBottomSheetWithResults(displayDate, top3);
                                            }
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            if (aiSheetDialog != null) aiSheetDialog.dismiss();
                                            Toast.makeText(AiSchedulingActivity.this, "Error finding slots", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            if (aiSheetDialog != null) aiSheetDialog.dismiss();
                            Toast.makeText(AiSchedulingActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
    }

    private void setupCalendarSelector() {
        tvSelectedCalendar = findViewById(R.id.tv_selected_calendar);

        String currentUserId = UserManager.getInstance().getCurrentUser().getUid();

        if (currentUserId == null) {
            tvSelectedCalendar.setText("📅 Not Logged In ▼");
            return;
        }

        calendarNames.clear();
        calendarIds.clear();

        calendarRepository.getCalendarsByUser(currentUserId, new RepositoryCallback<List<CalendarModel>>() {
            @Override
            public void onSuccess(List<CalendarModel> calendars) {
                runOnUiThread(() -> {
                    if (calendars != null && !calendars.isEmpty()) {
                        for (com.timed.models.CalendarModel cal : calendars) {
                            calendarNames.add(cal.getName());
                            calendarIds.add(cal.getId());
                        }

                        tvSelectedCalendar.setText("📅 " + calendarNames.get(0) + " ▼");
                        selectedCalendarId = calendarIds.get(0);
                    } else {
                        tvSelectedCalendar.setText("📅 No Calendars ▼");
                    }
                });

            }

            @Override
            public void onFailure(String e) {
                Toast.makeText(AiSchedulingActivity.this, "Failed to load calendars", Toast.LENGTH_SHORT).show();
                tvSelectedCalendar.setText("📅 Error ▼");
            }
        });

        tvSelectedCalendar.setOnClickListener(v -> {
            if (calendarNames.isEmpty()) {
                Toast.makeText(this, "Loading calendars...", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] namesArray = calendarNames.toArray(new String[0]);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Select Calendar")
                    .setItems(namesArray, (dialog, which) -> {
                        String chosenName = calendarNames.get(which);
                        selectedCalendarId = calendarIds.get(which);
                        tvSelectedCalendar.setText("📅 " + chosenName + " ▼");
                    })
                    .show();
        });
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

    private long convertKeywordToMillis(String keyword) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (keyword == null) return cal.getTimeInMillis();

        String lower = keyword.toLowerCase();

        if (lower.contains("tomorrow")) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if (lower.contains("monday")) {
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.MONDAY) cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if (lower.contains("tuesday")) {
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.TUESDAY) cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if (lower.contains("wednesday")) {
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.WEDNESDAY) cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if (lower.contains("thursday")) {
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.THURSDAY) cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if (lower.contains("friday")) {
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.FRIDAY) cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if (lower.contains("saturday")) {
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SATURDAY) cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if (lower.contains("sunday")) {
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY) cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }

        return cal.getTimeInMillis();
    }

    private List<FreeSlot> carveProposedSlots(List<FreeSlot> availableGaps, long baseDateMillis, AiScheduleRequest aiData) {
        List<FreeSlot> proposed = new ArrayList<>();
        long duration = aiData.durationMs > 0 ? aiData.durationMs : 3600000L; // Default 1 hour

        for (FreeSlot gap : availableGaps) {
            long gapStart = gap.getStartMillis();
            long gapEnd = gap.getEndMillis();

            if (aiData.exactTime != null && !aiData.exactTime.isEmpty() && !aiData.exactTime.equals("null")) {
                try {
                    String[] parts = aiData.exactTime.split(":");
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);

                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(baseDateMillis);
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    cal.set(Calendar.SECOND, 0);

                    long proposedStart = cal.getTimeInMillis();
                    long proposedEnd = proposedStart + duration;

                    if (proposedStart >= gapStart && proposedEnd <= gapEnd) {
                        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                        String timeString = sdf.format(new Date(proposedStart)) + " - " + sdf.format(new Date(proposedEnd));

                        proposed.add(new FreeSlot(timeString, (duration / 60000) + " mins", "ai_slot_exact", proposedStart, proposedEnd));
                        continue;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                long currentStart = gapStart;

                while (currentStart + duration <= gapEnd && proposed.size() < 3) {
                    long proposedEnd = currentStart + duration;

                    SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    String timeString = sdf.format(new Date(currentStart)) + " - " + sdf.format(new Date(proposedEnd));

                    proposed.add(new FreeSlot(timeString, (duration / 60000) + " mins", "ai_slot_" + currentStart, currentStart, proposedEnd));

                    currentStart += 1800000L;
                }
            }

            if (proposed.size() >= 3) break;
        }
        return proposed;
    }

    private void openBottomSheetInLoadingState() {
        if (aiSheetDialog == null) {
            aiSheetDialog = new BottomSheetDialog(this);
            sheetView = getLayoutInflater().inflate(R.layout.layout_ai_bottom_sheet, null);
            aiSheetDialog.setContentView(sheetView);
        }

        ((TextView) sheetView.findViewById(R.id.tv_sheet_title)).setText("AI is thinking...");
        ((TextView) sheetView.findViewById(R.id.tv_sheet_subtitle)).setText("Scanning your calendar for the best times...");
        sheetView.findViewById(R.id.ll_loading_state).setVisibility(View.VISIBLE);
        sheetView.findViewById(R.id.ll_results_state).setVisibility(View.GONE);

        animateDot(sheetView.findViewById(R.id.dot_blue), 0);
        animateDot(sheetView.findViewById(R.id.dot_yellow), 150);
        animateDot(sheetView.findViewById(R.id.dot_red), 300);

        aiSheetDialog.show();
    }

    private void updateBottomSheetWithResults(String dateString, List<FreeSlot> generatedSlots) {
        if (aiSheetDialog == null || !aiSheetDialog.isShowing()) return;

        TransitionManager.beginDelayedTransition((ViewGroup) sheetView);

        sheetView.findViewById(R.id.ll_loading_state).setVisibility(View.GONE);
        sheetView.findViewById(R.id.ll_results_state).setVisibility(View.VISIBLE);

        aiSheetDialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

        ((TextView) sheetView.findViewById(R.id.tv_sheet_title)).setText("Select a Time");
        ((TextView) sheetView.findViewById(R.id.tv_sheet_subtitle)).setText("Best available slots for " + dateString + ":");

        RecyclerView rvGeneratedSlots = sheetView.findViewById(R.id.rv_ai_generated_slots);
        Button btnConfirm = sheetView.findViewById(R.id.btn_confirm_ai);
        Button btnSaveTemplate = sheetView.findViewById(R.id.btn_save_template);

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
            scheduleEventDirectly(currentSelectedAiSlot);
        });

        btnSaveTemplate.setOnClickListener(v -> {
            String currentPrompt = etAiInput.getText().toString().trim();

            android.widget.EditText inputTitle = new android.widget.EditText(this);
            inputTitle.setHint("e.g., Focus Time");

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Save Template")
                    .setMessage("Give this prompt a quick name:")
                    .setView(inputTitle)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String title = inputTitle.getText().toString().trim();
                        if (title.isEmpty()) return;

                        AiPromptTemplate newTemplate = new AiPromptTemplate(title, currentPrompt);

                        templateRepository.addTemplate(
                                UserManager.getInstance().getCurrentUser().getUid(),
                                newTemplate,
                                new RepositoryCallback<String>() {
                                    @Override
                                    public void onSuccess(String data) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(AiSchedulingActivity.this, "Template saved!", Toast.LENGTH_SHORT).show();
                                            loadSavedTemplates();
                                            btnSaveTemplate.setVisibility(View.GONE);
                                        });
                                    }
                                    @Override
                                    public void onFailure(String e) {
                                        runOnUiThread(() -> Toast.makeText(AiSchedulingActivity.this, "Failed to save", Toast.LENGTH_SHORT).show());
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void animateDot(View dot, long delay) {
        dot.animate()
                .translationY(-24f)
                .setDuration(300)
                .setStartDelay(delay)
                .withEndAction(() -> {
                    dot.animate()
                            .translationY(0f)
                            .setDuration(300)
                            .setStartDelay(0)
                            .withEndAction(() -> animateDot(dot, 400))
                            .start();
                })
                .start();
    }

    private void scheduleEventDirectly(FreeSlot slot) {
        String title = etAiInput.getText().toString().trim();
        if (title.isEmpty()) {
            title = "AI Scheduled Event";
        }

        com.timed.models.Event newEvent = new com.timed.models.Event();
        newEvent.setTitle(title);
        newEvent.setCalendarId(selectedCalendarId);
        newEvent.setAllDay(false);

        newEvent.setStartTime(new com.google.firebase.Timestamp(new java.util.Date(slot.getStartMillis())));
        newEvent.setEndTime(new com.google.firebase.Timestamp(new java.util.Date(slot.getEndMillis())));

        String currentUserId = com.timed.managers.UserManager.getInstance().getCurrentUser().getUid();
        if (currentUserId != null) {
            newEvent.setCreatedBy(currentUserId);
        }

        Toast.makeText(this, "Saving to calendar...", Toast.LENGTH_SHORT).show();

        eventsRepository.createEvent(newEvent)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        if (aiSheetDialog != null) aiSheetDialog.dismiss();
                        Toast.makeText(AiSchedulingActivity.this, "🎉 Event scheduled!", Toast.LENGTH_LONG).show();
                        etAiInput.setText("");
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(AiSchedulingActivity.this, "Failed to schedule: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }
}