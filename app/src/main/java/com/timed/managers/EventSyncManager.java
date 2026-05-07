package com.timed.managers;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.models.Event;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventSyncManager {
    private static final String TAG = "EventSyncManager";
    private final Context context;
    private final EventsManager eventsManager;

    public interface EventsLoadCallback {
        void onLoaded(List<Event> events);
    }

    public EventSyncManager(Context context) {
        this.context = context;
        this.eventsManager = EventsManager.getInstance(context);
    }

    public void fetchEventsForCalendars(LocalDate startDate, LocalDate endDate, List<String> calendarIds, EventsLoadCallback callback) {
        if (calendarIds == null || calendarIds.isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return;
        }
        List<Event> merged = new ArrayList<>();
        fetchEventsForCalendarsSequential(startDate, endDate, calendarIds, 0, merged, callback);
    }

    private void fetchEventsForCalendarsSequential(LocalDate startDate, LocalDate endDate, List<String> calendarIds,
                                                   int index, List<Event> merged, EventsLoadCallback callback) {
        if (index >= calendarIds.size()) {
            callback.onLoaded(sortEvents(merged));
            return;
        }

        String calendarId = calendarIds.get(index);
        fetchEventsForRange(startDate, endDate, calendarId, events -> {
            mergeEvents(merged, events);
            fetchEventsForCalendarsSequential(startDate, endDate, calendarIds, index + 1, merged, callback);
        });
    }

    private void fetchEventsForRange(LocalDate startDate, LocalDate endDate, String calendarId, EventsLoadCallback callback) {
        if (eventsManager == null) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        ZoneId userZone = TimezoneHelper.getSelectedZoneId(context);
        Timestamp startTimestamp = toTimestamp(startDate.atStartOfDay(userZone).toInstant().toEpochMilli());
        long endMillis = endDate.plusDays(1).atStartOfDay(userZone).toInstant().toEpochMilli() - 1;
        Timestamp endTimestamp = toTimestamp(endMillis);

        eventsManager.getEventsByDateRange(calendarId, startTimestamp, endTimestamp)
                .addOnSuccessListener(callback::onLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events: " + e.getMessage(), e);
                    callback.onLoaded(new ArrayList<>());
                });
    }

    private void mergeEvents(List<Event> target, List<Event> incoming) {
        if (incoming == null || incoming.isEmpty()) return;

        Map<String, Event> map = new HashMap<>();
        for (Event event : target) {
            if (event != null && event.getId() != null) {
                map.put(event.getId(), event);
            }
        }

        for (Event event : incoming) {
            if (event == null) continue;
            String id = event.getId();
            if (id == null || !map.containsKey(id)) {
                target.add(event);
                if (id != null) map.put(id, event);
            }
        }
    }

    private List<Event> sortEvents(List<Event> events) {
        if (events == null || events.size() <= 1) {
            return events == null ? new ArrayList<>() : events;
        }

        Collections.sort(events, (a, b) -> {
            if (a == null || a.getStartTime() == null) return -1;
            if (b == null || b.getStartTime() == null) return 1;
            return a.getStartTime().toDate().compareTo(b.getStartTime().toDate());
        });
        return events;
    }

    private Timestamp toTimestamp(long millis) {
        return new Timestamp(new Date(millis));
    }
}
