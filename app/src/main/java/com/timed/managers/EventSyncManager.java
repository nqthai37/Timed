package com.timed.managers;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.models.Event;
import com.timed.utils.RecurrenceUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        eventsManager.getEventsByCalendarId(calendarId)
                .addOnSuccessListener(events -> callback.onLoaded(expandAndFilterEvents(events, startDate, endDate)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events: " + e.getMessage(), e);
                    callback.onLoaded(new ArrayList<>());
                });
    }

    private List<Event> expandAndFilterEvents(List<Event> events, LocalDate startDate, LocalDate endDate) {
        List<Event> results = new ArrayList<>();
        if (events == null || events.isEmpty()) {
            return results;
        }

        ZoneId userZone = TimezoneHelper.getSelectedZoneId(context);
        for (Event event : events) {
            if (event == null || event.getStartTime() == null) {
                continue;
            }

            String rrule = event.getRecurrenceRule();
            if (rrule == null || rrule.trim().isEmpty()) {
                LocalDate eventDate = toLocalDate(event.getStartTime());
                if (eventDate != null && !eventDate.isBefore(startDate) && !eventDate.isAfter(endDate)) {
                    results.add(event);
                }
                continue;
            }

            LocalDate eventStartDate = toLocalDate(event.getStartTime());
            if (eventStartDate == null || eventStartDate.isAfter(endDate)) {
                continue;
            }

            RecurrenceUtils.RecurrenceRule rule = RecurrenceUtils.parseRRule(rrule);
            Set<LocalDate> exceptionDates = parseExceptionDates(event.getRecurrenceExceptions());

            int occurrenceCount = 0;
            LocalDate cursor = eventStartDate;
            while (!cursor.isAfter(endDate)) {
                if (!exceptionDates.contains(cursor) && occursOnDate(event, rule, cursor, userZone)) {
                    occurrenceCount++;
                    if (rule.count > 0 && occurrenceCount > rule.count) {
                        break;
                    }
                    if (!cursor.isBefore(startDate)) {
                        results.add(buildOccurrenceEvent(event, cursor, userZone));
                    }
                }
                cursor = cursor.plusDays(1);
            }
        }

        return sortEvents(results);
    }

    private Set<LocalDate> parseExceptionDates(List<String> exceptionList) {
        Set<LocalDate> exceptionDates = new HashSet<>();
        if (exceptionList == null) {
            return exceptionDates;
        }

        for (String raw : exceptionList) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            try {
                exceptionDates.add(LocalDate.parse(raw.trim()));
            } catch (Exception ignored) {
            }
        }
        return exceptionDates;
    }

    private boolean occursOnDate(Event event, RecurrenceUtils.RecurrenceRule rule, LocalDate date, ZoneId zoneId) {
        if (event == null || event.getStartTime() == null || rule == null || rule.frequency == null) {
            return false;
        }

        java.util.Calendar startCal = TimezoneHelper.getCalendarInSelectedTz(context,
                event.getStartTime().toDate());
        int hour = startCal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = startCal.get(java.util.Calendar.MINUTE);
        int second = startCal.get(java.util.Calendar.SECOND);

        long occurrenceMillis = date.atTime(hour, minute, second)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli();

        if (rule.until != null && occurrenceMillis > rule.until.getTime()) {
            return false;
        }

        return RecurrenceUtils.isOccurrence(event.getStartTime().toDate().getTime(), rule, occurrenceMillis, null);
    }

    private Event buildOccurrenceEvent(Event source, LocalDate date, ZoneId zoneId) {
        Event copy = new Event();
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setLocation(source.getLocation());
        copy.setAllDay(source.getAllDay());
        copy.setCalendarId(source.getCalendarId());
        copy.setCalendarName(source.getCalendarName());
        copy.setColor(source.getColor());
        copy.setRecurrenceRule(source.getRecurrenceRule());
        copy.setRecurrenceExceptions(source.getRecurrenceExceptions());

        java.util.Calendar startCal = TimezoneHelper.getCalendarInSelectedTz(context, source.getStartTime().toDate());
        int hour = startCal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = startCal.get(java.util.Calendar.MINUTE);
        int second = startCal.get(java.util.Calendar.SECOND);
        long startMillis = date.atTime(hour, minute, second)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli();

        long durationMillis = 0L;
        if (source.getEndTime() != null) {
            durationMillis = Math.max(0L,
                    source.getEndTime().toDate().getTime() - source.getStartTime().toDate().getTime());
        }

        copy.setStartTime(new Timestamp(new Date(startMillis)));
        copy.setEndTime(new Timestamp(new Date(startMillis + durationMillis)));

        String baseId = source.getId();
        copy.setId(baseId != null ? baseId + "_" + date : date.toString());
        copy.setInstanceOf(baseId);
        return copy;
    }

    private LocalDate toLocalDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        ZoneId userZone = TimezoneHelper.getSelectedZoneId(context);
        return timestamp.toDate().toInstant().atZone(userZone).toLocalDate();
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
