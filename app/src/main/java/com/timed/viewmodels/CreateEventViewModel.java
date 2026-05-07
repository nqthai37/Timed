package com.timed.viewmodels;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.timed.Features.ConflictResolver.ConflictEvent;
import com.timed.managers.EventsManager;
import com.timed.managers.UserManager;
import com.timed.models.CalendarModel;
import com.timed.models.Event;
import com.timed.repositories.EventsRepository;
import com.timed.repositories.UserRepository;
import com.timed.utils.AlarmHelper;
import com.timed.utils.CalendarIntegrationService;
import com.timed.utils.CalendarSelectorHelper;
import com.timed.utils.FirebaseInitializer;
import com.timed.utils.RecurrenceConfig;
import com.timed.utils.RecurrenceTextFormatter;
import com.timed.utils.RecurrenceUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel cho màn hình Tạo/Chỉnh sửa sự kiện.
 * Chứa toàn bộ State và Business Logic, tách biệt khỏi Activity.
 */
public class CreateEventViewModel extends AndroidViewModel {
    private static final String TAG = "CreateEventVM";
    private static final long AUTO_FIX_END_DURATION_MS = 24 * 60 * 60 * 1000L;

    // --- Services ---
    private EventsManager eventsManager;
    private FirebaseInitializer firebaseInitializer;
    private CalendarIntegrationService calendarIntegrationService;
    private CalendarSelectorHelper calendarSelectorHelper;
    private EventsRepository eventsRepository;

    // --- State ---
    private final MutableLiveData<Long> startTime = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> endTime = new MutableLiveData<>(0L);
    private final MutableLiveData<String> calendarId = new MutableLiveData<>();
    private final MutableLiveData<String> calendarLabel = new MutableLiveData<>("Calendar");
    private final MutableLiveData<List<Long>> selectedReminderMinutes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> reminderDisplayText = new MutableLiveData<>("No reminders");
    private final MutableLiveData<RecurrenceConfig> recurrenceConfig = new MutableLiveData<>(RecurrenceConfig.disabled());
    private final MutableLiveData<String> repeatSummary = new MutableLiveData<>("No repeat");

    // Sự kiện 1 lần (Single Event) dùng để thông báo UI
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();
    private final MutableLiveData<ConflictCheckResult> conflictResult = new MutableLiveData<>();

    private String mode = "create";
    private String eventId;
    private Event editingEvent;
    private final List<CalendarModel> calendarOptions = new ArrayList<>();
    private final Map<String, CalendarModel> calendarsById = new HashMap<>();

    private boolean initialized = false;

    public CreateEventViewModel(@NonNull Application application) {
        super(application);
    }

    // --- Khởi tạo (gọi 1 lần từ Activity.onCreate) ---

    public void initialize(Context context) {
        if (initialized) {
            return;
        }
        initialized = true;

        firebaseInitializer = FirebaseInitializer.getInstance();
        firebaseInitializer.initialize(context);
        eventsManager = EventsManager.getInstance(context);
        calendarIntegrationService = new CalendarIntegrationService();
        UserRepository userRepository = new UserRepository();
        calendarSelectorHelper = new CalendarSelectorHelper(calendarIntegrationService, userRepository);
        eventsRepository = new EventsRepository();

        // Default reminders
        List<Long> defaults = new ArrayList<>();
        defaults.add(10L);
        defaults.add(30L);
        selectedReminderMinutes.setValue(defaults);
        updateReminderDisplayText();
    }

    // ======================= GETTERS (LiveData) =======================

    public LiveData<Long> getStartTime() { return startTime; }
    public LiveData<Long> getEndTime() { return endTime; }
    public LiveData<String> getCalendarLabel() { return calendarLabel; }
    public LiveData<String> getReminderDisplayText() { return reminderDisplayText; }
    public LiveData<RecurrenceConfig> getRecurrenceConfig() { return recurrenceConfig; }
    public LiveData<String> getRepeatSummary() { return repeatSummary; }
    public LiveData<SaveResult> getSaveResult() { return saveResult; }
    public LiveData<ConflictCheckResult> getConflictResult() { return conflictResult; }
    public List<CalendarModel> getCalendarOptions() { return calendarOptions; }
    public CalendarSelectorHelper getCalendarSelectorHelper() { return calendarSelectorHelper; }
    public String getMode() { return mode; }
    public String getEventId() { return eventId; }
    public Event getEditingEvent() { return editingEvent; }
    public EventsManager getEventsManager() { return eventsManager; }

    public long getStartTimeValue() {
        Long val = startTime.getValue();
        return val != null ? val : 0L;
    }

    public long getEndTimeValue() {
        Long val = endTime.getValue();
        return val != null ? val : 0L;
    }

    public String getCalendarIdValue() {
        return calendarId.getValue();
    }

    public List<Long> getSelectedReminderMinutesValue() {
        List<Long> val = selectedReminderMinutes.getValue();
        return val != null ? val : new ArrayList<>();
    }

    public RecurrenceConfig getRecurrenceConfigValue() {
        RecurrenceConfig val = recurrenceConfig.getValue();
        return val != null ? val : RecurrenceConfig.disabled();
    }

    // ======================= SETTERS =======================

    public void setStartTime(long time) {
        startTime.setValue(time);
        ensureValidTimeRange();
    }

    public void setEndTime(long time) {
        endTime.setValue(time);
        ensureValidTimeRange();
    }

    public void setCalendarId(String id) {
        calendarId.setValue(id);
        if (calendarIntegrationService != null) {
            calendarIntegrationService.setCachedDefaultCalendarId(getApplication(), id);
        }
        updateCalendarLabel();
    }

    public void setMode(String mode) {
        this.mode = mode != null ? mode : "create";
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setSelectedReminderMinutes(List<Long> minutes) {
        selectedReminderMinutes.setValue(minutes != null ? minutes : new ArrayList<>());
        updateReminderDisplayText();
    }

    // ======================= LOGIC: Thời gian =======================

    public void ensureValidTimeRange() {
        long start = getStartTimeValue();
        long end = getEndTimeValue();

        if (start <= 0) {
            start = System.currentTimeMillis();
        }
        if (end < start) {
            end = start + AUTO_FIX_END_DURATION_MS;
        }
        // Chỉ setValue nếu thực sự thay đổi, tránh loop
        if (start != getStartTimeValue()) {
            startTime.setValue(start);
        }
        if (end != getEndTimeValue()) {
            endTime.setValue(end);
        }
    }

    // ======================= LOGIC: Calendar =======================

    public void loadCalendars(Context context) {
        calendarSelectorHelper.loadCalendars(context, new CalendarSelectorHelper.OnCalendarsLoadedListener() {
            @Override
            public void onLoaded(String defaultId, List<CalendarModel> calendars) {
                calendarOptions.clear();
                calendarsById.clear();
                if (calendars != null) {
                    calendarOptions.addAll(calendars);
                    for (CalendarModel calendar : calendars) {
                        if (calendar != null && calendar.getId() != null) {
                            calendarsById.put(calendar.getId(), calendar);
                        }
                    }
                }

                String currentCalId = calendarId.getValue();
                if (currentCalId == null || currentCalId.isEmpty()) {
                    calendarId.setValue(defaultId);
                } else if (!CalendarSelectorHelper.containsCalendar(calendarOptions, currentCalId)) {
                    calendarId.setValue(defaultId);
                }

                String resolvedId = calendarId.getValue();
                if (resolvedId != null && !resolvedId.isEmpty()) {
                    calendarIntegrationService.setCachedDefaultCalendarId(context, resolvedId);
                }

                updateCalendarLabel();
                calendarSelectorHelper.loadCalendarOwnerNames(calendarOptions, () -> updateCalendarLabel());
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to load calendars: " + errorMessage);
            }
        });
    }

    private void updateCalendarLabel() {
        String label = "Calendar";
        String currentCalId = calendarId.getValue();
        for (CalendarModel calendar : calendarOptions) {
            if (calendar != null && currentCalId != null && currentCalId.equals(calendar.getId())) {
                label = CalendarSelectorHelper.formatCalendarLabel(calendar);
                break;
            }
        }
        calendarLabel.setValue(label);
    }

    // ======================= LOGIC: Reminder =======================

    private void updateReminderDisplayText() {
        List<Long> minutes = getSelectedReminderMinutesValue();

        if (minutes.isEmpty()) {
            reminderDisplayText.setValue("No reminders");
            return;
        }

        List<Long> sorted = new ArrayList<>(minutes);
        sorted.sort(Long::compareTo);

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            long mins = sorted.get(i);
            if (i > 0) text.append(", ");

            if (mins < 60) {
                text.append(mins).append(" min");
            } else if (mins == 60) {
                text.append("1 hour");
            } else if (mins == 120) {
                text.append("2 hours");
            } else if (mins == 1440) {
                text.append("1 day");
            } else {
                text.append(mins / 60).append(" hours");
            }
        }

        reminderDisplayText.setValue(text.toString());
    }

    // ======================= LOGIC: Recurrence =======================

    public void applyQuickRecurrence(int index) {
        RecurrenceConfig config = new RecurrenceConfig();
        config.enabled = true;
        config.interval = 1;
        config.endType = RecurrenceConfig.EndType.NEVER;
        config.exceptions = new ArrayList<>();

        if (index == 0) {
            config.frequency = RecurrenceUtils.FREQ_DAILY;
        } else if (index == 1) {
            config.frequency = RecurrenceUtils.FREQ_WEEKLY;
        } else if (index == 2) {
            config.frequency = RecurrenceUtils.FREQ_MONTHLY;
        } else {
            config.frequency = RecurrenceUtils.FREQ_YEARLY;
        }

        config.applyStartDefaults(getStartTimeValue());
        recurrenceConfig.setValue(config);
        updateRepeatSummary();
    }

    public void setRecurrenceConfig(RecurrenceConfig config) {
        recurrenceConfig.setValue(config);
        updateRepeatSummary();
    }

    private void updateRepeatSummary() {
        RecurrenceConfig config = getRecurrenceConfigValue();
        String summary = RecurrenceTextFormatter.formatSummary(config, getStartTimeValue(), false);
        repeatSummary.setValue(summary);
    }

    public void applyRecurrenceFromEvent(Event event) {
        if (event == null) {
            return;
        }
        String rule = event.getRecurrenceRule();
        RecurrenceConfig config;
        if (rule != null && !rule.trim().isEmpty()) {
            config = RecurrenceConfig.fromRRule(rule);
            config.enabled = true;
        } else {
            config = RecurrenceConfig.disabled();
        }
        if (event.getRecurrenceExceptions() != null) {
            config.exceptions = new ArrayList<>(event.getRecurrenceExceptions());
        }
        recurrenceConfig.setValue(config);
        updateRepeatSummary();
    }

    private void applyRecurrenceToEvent(Event event) {
        if (event == null) {
            return;
        }
        RecurrenceConfig config = getRecurrenceConfigValue();
        if (config.enabled) {
            config.applyStartDefaults(getStartTimeValue());
            String rule = config.toRRuleString();
            event.setRecurrenceRule(rule != null && !rule.isEmpty() ? rule : null);
            List<String> exceptions = new ArrayList<>();
            if (config.exceptions != null) {
                exceptions.addAll(config.exceptions);
            }
            event.setRecurrenceExceptions(exceptions);
        } else {
            event.setRecurrenceRule(null);
            event.setRecurrenceExceptions(new ArrayList<>());
        }
    }

    // ======================= LOGIC: Lưu sự kiện =======================

    /**
     * Bắt đầu quy trình lưu sự kiện. Sẽ kiểm tra conflict trước.
     */
    public void saveEvent(String title, String description, String location, boolean isAllDay) {
        ensureValidTimeRange();
        long start = getStartTimeValue();
        long end = getEndTimeValue();

        if (start > end) {
            return;
        }

        // SILENT BACKGROUND CHECK
        eventsRepository.checkConflictsOnDay(start, end, title, new EventsRepository.OnConflictCheckListener() {
            @Override
            public void onConflictsFound(List<ConflictEvent> conflicts) {
                if (conflicts.isEmpty()) {
                    proceedToSave(title, description, location, isAllDay);
                } else {
                    conflictResult.setValue(new ConflictCheckResult(conflicts, title, start, end));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking conflicts: " + e.getMessage());
                proceedToSave(title, description, location, isAllDay);
            }
        });
    }

    /**
     * Tiếp tục lưu sau khi đã xử lý conflict (hoặc không có conflict).
     */
    public void proceedToSave(String title, String description, String location, boolean isAllDay) {
        if (isEditMode()) {
            updateExistingEvent(title, description, location, isAllDay);
            return;
        }

        ensureCalendarReadyAndSave(title, description, location, isAllDay);
    }

    private void ensureCalendarReadyAndSave(String title, String description, String location, boolean isAllDay) {
        String calId = calendarId.getValue();
        if (calId == null || calId.isEmpty()) {
            calId = calendarIntegrationService.getCachedDefaultCalendarId(getApplication());
            calendarId.setValue(calId);
        }
        if (calId != null && !calId.isEmpty()) {
            saveEventWithCalendar(title, description, location, isAllDay, calId);
            return;
        }

        calendarIntegrationService.ensureDefaultCalendar(getApplication(),
                new CalendarIntegrationService.DefaultCalendarListener() {
                    @Override
                    public void onReady(String resolvedCalId, List<CalendarModel> calendars) {
                        calendarId.setValue(resolvedCalId);
                        saveEventWithCalendar(title, description, location, isAllDay, resolvedCalId);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        saveResult.setValue(new SaveResult(false, "Calendar is not ready: " + errorMessage));
                    }
                });
    }

    private void saveEventWithCalendar(String title, String description, String location,
                                       boolean isAllDay, String calId) {
        String generatedEventId = FirebaseFirestore.getInstance().collection("events").document().getId();

        Event newEvent = new Event();
        newEvent.setCalendarId(calId);

        CalendarModel selectedCalendar = calendarsById.get(calId);
        if (selectedCalendar != null) {
            newEvent.setCalendarName(selectedCalendar.getName());
            newEvent.setColor(selectedCalendar.getColor());
        }
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setLocation(location);
        newEvent.setAllDay(isAllDay);
        newEvent.setStartTime(new Timestamp(new Date(getStartTimeValue())));
        newEvent.setEndTime(new Timestamp(new Date(getEndTimeValue())));
        applyRecurrenceToEvent(newEvent);

        String userId = firebaseInitializer.getCurrentUserId();
        if (userId != null) {
            newEvent.setCreatedBy(userId);
            newEvent.getParticipantId().add(userId);
            newEvent.getParticipantStatus().put(userId, "accepted");
        }

        List<Event.EventReminder> reminders = new ArrayList<>();
        for (Long minutes : getSelectedReminderMinutesValue()) {
            reminders.add(new Event.EventReminder(minutes, "push"));
        }
        newEvent.setReminders(reminders);

        calendarIntegrationService.setCachedDefaultCalendarId(getApplication(), calId);

        // Ghi trực tiếp xuống Cache của Firestore
        FirebaseFirestore.getInstance().collection("events").document(generatedEventId).set(newEvent);

        // Đặt báo thức Offline
        AlarmHelper.scheduleAllAlarms(getApplication(), generatedEventId, title,
                getStartTimeValue(), getSelectedReminderMinutesValue());

        saveResult.setValue(new SaveResult(true, "✅ Đã lưu sự kiện!"));
    }

    private void updateExistingEvent(String title, String description, String location, boolean isAllDay) {
        if (eventId == null || eventId.isEmpty()) {
            saveResult.setValue(new SaveResult(false, "Không tìm thấy sự kiện để cập nhật"));
            return;
        }

        String calId = calendarId.getValue();
        if (calId == null || calId.isEmpty()) {
            calId = calendarIntegrationService.getCachedDefaultCalendarId(getApplication());
            calendarId.setValue(calId);
        }
        if (calId == null || calId.isEmpty()) {
            calendarIntegrationService.ensureDefaultCalendar(getApplication(),
                    new CalendarIntegrationService.DefaultCalendarListener() {
                        @Override
                        public void onReady(String resolvedCalId, List<CalendarModel> calendars) {
                            calendarId.setValue(resolvedCalId);
                            calendarIntegrationService.setCachedDefaultCalendarId(getApplication(), resolvedCalId);
                            updateExistingEvent(title, description, location, isAllDay);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            saveResult.setValue(new SaveResult(false, "Calendar is not ready: " + errorMessage));
                        }
                    });
            return;
        }

        Event target = editingEvent != null ? editingEvent : new Event();
        CalendarModel selectedCalendar = calendarsById.get(calId);

        target.setId(eventId);
        target.setCalendarId(calId);
        if (selectedCalendar != null) {
            target.setCalendarName(selectedCalendar.getName());
            target.setColor(selectedCalendar.getColor());
        }
        target.setTitle(title);
        target.setDescription(description);
        target.setLocation(location);
        target.setAllDay(isAllDay);
        target.setStartTime(new Timestamp(new Date(getStartTimeValue())));
        target.setEndTime(new Timestamp(new Date(getEndTimeValue())));
        applyRecurrenceToEvent(target);

        List<Event.EventReminder> reminders = new ArrayList<>();
        for (Long minutes : getSelectedReminderMinutesValue()) {
            reminders.add(new Event.EventReminder(minutes, "push"));
        }
        target.setReminders(reminders);

        FirebaseFirestore.getInstance().collection("events").document(eventId).set(target);

        AlarmHelper.scheduleAllAlarms(getApplication(), eventId, title,
                getStartTimeValue(), getSelectedReminderMinutesValue());

        saveResult.setValue(new SaveResult(true, "✅ Event updated!"));
    }

    /**
     * Xóa sự kiện
     */
    public void deleteEvent() {
        if (!isEditMode() || eventId == null || eventId.isEmpty()) {
            return;
        }
        eventsManager.deleteEvent(eventId);
        saveResult.setValue(new SaveResult(true, "✅ Event deleted!"));
    }

    /**
     * Load dữ liệu sự kiện khi ở chế độ Edit
     */
    public void loadEventForEdit(EditEventCallback callback) {
        if (!isEditMode() || eventId == null || eventId.isEmpty()) {
            return;
        }
        eventsManager.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    editingEvent = event;
                    if (event == null) {
                        return;
                    }
                    if (event.getCalendarId() != null && !event.getCalendarId().isEmpty()) {
                        calendarId.setValue(event.getCalendarId());
                    }
                    if (getStartTimeValue() <= 0 && event.getStartTime() != null) {
                        startTime.setValue(event.getStartTime().toDate().getTime());
                    }
                    if (getEndTimeValue() <= 0 && event.getEndTime() != null) {
                        endTime.setValue(event.getEndTime().toDate().getTime());
                    }
                    ensureValidTimeRange();
                    updateCalendarLabel();
                    applyRecurrenceFromEvent(event);

                    if (callback != null) {
                        callback.onEventLoaded(event);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Cannot load event detail for edit: " + e.getMessage()));
    }

    public boolean isEditMode() {
        return "edit".equalsIgnoreCase(mode);
    }

    // ======================= Result Classes =======================

    /**
     * Kết quả lưu sự kiện (dùng để Activity lắng nghe qua LiveData)
     */
    public static class SaveResult {
        public final boolean success;
        public final String message;

        public SaveResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /**
     * Kết quả kiểm tra conflict (dùng để Activity mở ConflictResolverActivity)
     */
    public static class ConflictCheckResult {
        public final List<ConflictEvent> conflicts;
        public final String title;
        public final long startTime;
        public final long endTime;

        public ConflictCheckResult(List<ConflictEvent> conflicts, String title, long startTime, long endTime) {
            this.conflicts = conflicts;
            this.title = title;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    /**
     * Callback khi load event để edit
     */
    public interface EditEventCallback {
        void onEventLoaded(Event event);
    }
}
