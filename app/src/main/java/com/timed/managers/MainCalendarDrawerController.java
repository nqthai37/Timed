package com.timed.managers;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.timed.R;
import com.timed.adapters.CalendarDrawerAdapter;
import com.timed.adapters.ColorPickerAdapter;
import com.timed.models.CalendarModel;
import com.timed.models.User;
import com.timed.repositories.CalendarOwnerRepository;
import com.timed.utils.CalendarIntegrationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainCalendarDrawerController {
    public interface Listener {
        void onCalendarsChanged();
    }

    private final Activity activity;
    private final CalendarIntegrationService calendarIntegrationService;
    private final CalendarOwnerRepository ownerRepository;
    private final Listener listener;
    private final List<String> visibleCalendarIds = new ArrayList<>();
    private final Map<String, CalendarModel> calendarsById = new HashMap<>();

    private NavigationView navigationView;
    private android.widget.TextView drawerName;
    private CalendarDrawerAdapter calendarDrawerAdapter;
    private String defaultCalendarId;

    public MainCalendarDrawerController(Activity activity,
            CalendarIntegrationService calendarIntegrationService,
            CalendarOwnerRepository ownerRepository,
            Listener listener) {
        this.activity = activity;
        this.calendarIntegrationService = calendarIntegrationService;
        this.ownerRepository = ownerRepository;
        this.listener = listener;
        this.defaultCalendarId = calendarIntegrationService.getCachedDefaultCalendarId(activity);
    }

    public void setup(NavigationView navView, DrawerLayout drawerLayout) {
        this.navigationView = navView;
        if (navView == null) {
            return;
        }

        View header = navView.getHeaderView(0);
        if (header == null) {
            return;
        }

        drawerName = header.findViewById(R.id.tvDrawerName);
        updateDrawerHeader(UserManager.getInstance().getCurrentUser());

        RecyclerView rvCalendarDrawer = header.findViewById(R.id.rvDrawerCalendars);
        if (rvCalendarDrawer != null) {
            rvCalendarDrawer.setLayoutManager(new LinearLayoutManager(activity));
            calendarDrawerAdapter = new CalendarDrawerAdapter(
                    new ArrayList<>(calendarsById.values()),
                    visibleCalendarIds,
                    new CalendarDrawerAdapter.OnCalendarActionListener() {
                        @Override
                        public void onCalendarToggle(CalendarModel calendar, boolean isVisible) {
                            toggleCalendarVisibility(calendar.getId());
                        }

                        @Override
                        public void onEditCalendar(CalendarModel calendar) {
                            showEditCalendarDialog(calendar);
                        }
                    });
            rvCalendarDrawer.setAdapter(calendarDrawerAdapter);
        }

        com.google.android.material.button.MaterialButton btnAddCalendar = header.findViewById(R.id.btnAddCalendar);
        if (btnAddCalendar != null) {
            btnAddCalendar.setOnClickListener(v -> {
                showCreateCalendarDialog();
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }
    }

    public void updateDrawerHeader(User user) {
        if (user != null && drawerName != null && user.getName() != null) {
            drawerName.setText(user.getName());
        }
    }

    public String getActiveCalendarId() {
        if (defaultCalendarId != null && !defaultCalendarId.isEmpty()) {
            return defaultCalendarId;
        }
        if (!visibleCalendarIds.isEmpty()) {
            defaultCalendarId = visibleCalendarIds.get(0);
            return defaultCalendarId;
        }
        defaultCalendarId = calendarIntegrationService.getCachedDefaultCalendarId(activity);
        return defaultCalendarId;
    }

    public List<String> getVisibleCalendarIds() {
        if (!visibleCalendarIds.isEmpty()) {
            return new ArrayList<>(visibleCalendarIds);
        }

        String calendarId = getActiveCalendarId();
        if (calendarId != null && !calendarId.isEmpty()) {
            List<String> ids = new ArrayList<>();
            ids.add(calendarId);
            return ids;
        }
        return new ArrayList<>();
    }

    public void ensureDefaultCalendarReady(Runnable onReady) {
        calendarIntegrationService.ensureDefaultCalendar(activity,
                new CalendarIntegrationService.DefaultCalendarListener() {
                    @Override
                    public void onReady(String calendarId, List<CalendarModel> calendars) {
                        defaultCalendarId = calendarId;
                        updateVisibleCalendarIds(calendars);
                        updateDrawerCalendars(calendars);
                        if (onReady != null) {
                            onReady.run();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        android.util.Log.e("MainCalendarDrawer", "Failed to ensure default calendar: " + errorMessage);
                    }
                });
    }

    private void updateVisibleCalendarIds(List<CalendarModel> calendars) {
        visibleCalendarIds.clear();

        List<String> resolvedVisibleIds = calendarIntegrationService.resolveVisibleCalendarIds(activity, calendars,
                defaultCalendarId);
        if (resolvedVisibleIds != null) {
            visibleCalendarIds.addAll(resolvedVisibleIds);
        }

        if (visibleCalendarIds.isEmpty() && defaultCalendarId != null && !defaultCalendarId.isEmpty()) {
            visibleCalendarIds.add(defaultCalendarId);
        }

        if (!visibleCalendarIds.isEmpty()) {
            if (defaultCalendarId == null || defaultCalendarId.isEmpty() || !visibleCalendarIds.contains(defaultCalendarId)) {
                defaultCalendarId = visibleCalendarIds.get(0);
                calendarIntegrationService.setCachedDefaultCalendarId(activity, defaultCalendarId);
            }
            calendarIntegrationService.saveVisibleCalendarIds(activity, visibleCalendarIds);
        }
    }

    private void updateDrawerCalendars(List<CalendarModel> calendars) {
        if (calendars == null) {
            return;
        }

        calendarsById.clear();
        List<CalendarModel> sortedCalendars = new ArrayList<>();
        for (CalendarModel calendar : calendars) {
            if (calendar != null && calendar.getId() != null && !calendar.getId().isEmpty()) {
                sortedCalendars.add(calendar);
                calendarsById.put(calendar.getId(), calendar);
            }
        }

        Collections.sort(sortedCalendars, (c1, c2) -> {
            int byOrder = Integer.compare(c1.getSortOrder(), c2.getSortOrder());
            if (byOrder != 0) {
                return byOrder;
            }
            String name1 = c1.getName() != null ? c1.getName() : "";
            String name2 = c2.getName() != null ? c2.getName() : "";
            return name1.compareToIgnoreCase(name2);
        });

        if (calendarDrawerAdapter != null) {
            calendarDrawerAdapter.updateData(sortedCalendars, visibleCalendarIds);
        }

        ownerRepository.loadOwnerNames(sortedCalendars, () -> {
            if (calendarDrawerAdapter != null) {
                calendarDrawerAdapter.updateData(sortedCalendars, visibleCalendarIds);
            }
        });
    }

    private void toggleCalendarVisibility(String calendarId) {
        if (calendarId == null || calendarId.isEmpty()) {
            return;
        }

        boolean currentlyVisible = visibleCalendarIds.contains(calendarId);
        if (currentlyVisible && visibleCalendarIds.size() == 1) {
            Toast.makeText(activity, "At least one calendar must remain visible", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentlyVisible) {
            visibleCalendarIds.remove(calendarId);
        } else {
            visibleCalendarIds.add(calendarId);
            defaultCalendarId = calendarId;
        }

        if (!visibleCalendarIds.isEmpty() && (defaultCalendarId == null || !visibleCalendarIds.contains(defaultCalendarId))) {
            defaultCalendarId = visibleCalendarIds.get(0);
        }

        if (defaultCalendarId != null && !defaultCalendarId.isEmpty()) {
            calendarIntegrationService.setCachedDefaultCalendarId(activity, defaultCalendarId);
        }
        calendarIntegrationService.saveVisibleCalendarIds(activity, visibleCalendarIds);

        boolean newVisibility = !currentlyVisible;
        calendarIntegrationService.updateCalendarVisibility(calendarId, newVisibility,
                new CalendarIntegrationService.CalendarSaveListener() {
                    @Override
                    public void onSuccess(String ignoredCalendarId) {
                    }

                    @Override
                    public void onError(String errorMessage) {
                    }
                });

        notifyCalendarsChanged();
    }

    private void showCreateCalendarDialog() {
        CalendarDialogManager.showCreateCalendarDialog(activity, (name, description) -> {
            if (isDuplicateCalendarName(name)) {
                Toast.makeText(activity, "Calendar name already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            showColorPickerForNewCalendar(name, description);
        });
    }

    private void showColorPickerForNewCalendar(String name, String description) {
        List<CalendarColorManager.CalendarColor> colors = calendarIntegrationService.getPresetColors();
        if (colors == null || colors.isEmpty()) {
            createCalendarFromDrawer(name, description, "#2B78E4");
            return;
        }

        String[] labels = new String[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            CalendarColorManager.CalendarColor color = colors.get(i);
            labels[i] = color.getName() + " (" + color.getHex() + ")";
        }

        final int[] selectedIndex = { 0 };
        new AlertDialog.Builder(activity)
                .setTitle("Choose color")
                .setSingleChoiceItems(labels, selectedIndex[0], (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("Create", (dialog, which) -> {
                    CalendarColorManager.CalendarColor selected = colors.get(selectedIndex[0]);
                    createCalendarFromDrawer(name, description, selected.getHex());
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private void createCalendarFromDrawer(String name, String description, String colorHex) {
        calendarIntegrationService.createCalendar(name, description, colorHex, false,
                new CalendarIntegrationService.CalendarSaveListener() {
                    @Override
                    public void onSuccess(String calendarId) {
                        if (calendarId != null && !calendarId.isEmpty()) {
                            defaultCalendarId = calendarId;
                            calendarIntegrationService.setCachedDefaultCalendarId(activity, calendarId);
                            if (!visibleCalendarIds.contains(calendarId)) {
                                visibleCalendarIds.add(calendarId);
                            }
                            calendarIntegrationService.saveVisibleCalendarIds(activity, visibleCalendarIds);
                        }

                        ensureDefaultCalendarReady(() -> {
                            Toast.makeText(activity, "Calendar created", Toast.LENGTH_SHORT).show();
                            notifyCalendarsChanged();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(activity, "Failed to create calendar: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isDuplicateCalendarName(String candidateName) {
        if (candidateName == null || candidateName.trim().isEmpty()) {
            return false;
        }
        String normalized = candidateName.trim();
        for (CalendarModel calendar : calendarsById.values()) {
            if (calendar != null && calendar.getName() != null
                    && normalized.equalsIgnoreCase(calendar.getName().trim())) {
                return true;
            }
        }
        return false;
    }

    private void showEditCalendarDialog(CalendarModel calendar) {
        if (calendar == null) {
            return;
        }

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_calendar, null);
        EditText etName = dialogView.findViewById(R.id.etCalendarName);
        EditText etDescription = dialogView.findViewById(R.id.etCalendarDescription);
        RecyclerView rvColorPicker = dialogView.findViewById(R.id.rvColorPicker);

        etName.setText(calendar.getName());
        etDescription.setText(calendar.getDescription());

        final String[] selectedColor = { calendar.getColor() };
        List<CalendarColorManager.CalendarColor> colors = calendarIntegrationService.getPresetColors();
        if (colors != null && !colors.isEmpty()) {
            rvColorPicker.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
            rvColorPicker.setAdapter(new ColorPickerAdapter(colors, calendar.getColor(),
                    colorHex -> selectedColor[0] = colorHex));
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Edit Calendar")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                String newDescription = etDescription.getText().toString().trim();

                if (newName.isEmpty()) {
                    etName.setError("Calendar name is required");
                    etName.requestFocus();
                    return;
                }

                if (!newName.equalsIgnoreCase(calendar.getName()) && isDuplicateCalendarName(newName)) {
                    etName.setError("Calendar name already exists");
                    etName.requestFocus();
                    return;
                }

                calendarIntegrationService.updateCalendar(calendar.getId(), newName, newDescription,
                        selectedColor[0], false, new CalendarIntegrationService.CalendarSaveListener() {
                            @Override
                            public void onSuccess(String calendarId) {
                                Toast.makeText(activity, "Calendar updated", Toast.LENGTH_SHORT).show();
                                ensureDefaultCalendarReady(() -> {
                                    notifyCalendarsChanged();
                                    dialog.dismiss();
                                });
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(activity, "Failed to update calendar: " + errorMessage,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                new AlertDialog.Builder(activity)
                        .setTitle("Delete Calendar")
                        .setMessage("Are you sure you want to delete this calendar? This cannot be undone.")
                        .setPositiveButton("Delete", (deleteDialog, which) -> {
                            calendarIntegrationService.deleteCalendar(calendar.getId(),
                                    new CalendarIntegrationService.CalendarSaveListener() {
                                        @Override
                                        public void onSuccess(String calendarId) {
                                            Toast.makeText(activity, "Calendar deleted", Toast.LENGTH_SHORT).show();
                                            visibleCalendarIds.remove(calendarId);
                                            calendarIntegrationService.saveVisibleCalendarIds(activity, visibleCalendarIds);
                                            ensureDefaultCalendarReady(() -> {
                                                notifyCalendarsChanged();
                                                dialog.dismiss();
                                            });
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            Toast.makeText(activity, "Failed to delete calendar: " + errorMessage,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });

        dialog.show();
    }

    private void notifyCalendarsChanged() {
        if (listener != null) {
            listener.onCalendarsChanged();
        }
    }
}
