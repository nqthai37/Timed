package com.timed.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.Timestamp;
import com.google.gson.Gson;
import com.timed.models.Event;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class OfflineCacheManager {
    public static final String EXTRA_OFFLINE_MODE = "offline_mode";

    public static final String KEY_DAY_CACHE = "cache_day";
    public static final String KEY_3DAYS_CACHE = "cache_3days";
    public static final String KEY_WEEK_CACHE = "cache_week";
    public static final String KEY_MONTH_CACHE = "cache_month";

    private static final String PREFS_NAME = "offline_cache";
    private static final String KEY_VIEW_STATE = "cache_view_state";
    private static final String KEY_LAST_TAB = "cache_last_tab";

    private static final Gson gson = new Gson();

    private OfflineCacheManager() {
    }

    public static void saveRange(Context context, String key, LocalDate startDate, LocalDate endDate,
            List<Event> events) {
        if (context == null || key == null || startDate == null || endDate == null) {
            return;
        }
        CachedRange range = new CachedRange();
        range.startDate = startDate.toString();
        range.endDate = endDate.toString();
        range.events = toCachedEvents(events);
        getPrefs(context).edit().putString(key, gson.toJson(range)).apply();
    }

    public static CachedRange loadRange(Context context, String key) {
        if (context == null || key == null) {
            return null;
        }
        String json = getPrefs(context).getString(key, null);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, CachedRange.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Event> getEventsForRange(Context context, String key, LocalDate startDate,
            LocalDate endDate) {
        CachedRange cached = loadRange(context, key);
        if (cached == null) {
            return new ArrayList<>();
        }
        List<Event> events = toEvents(cached.events);
        if (startDate == null || endDate == null) {
            return events;
        }
        List<Event> filtered = new ArrayList<>();
        for (Event event : events) {
            if (event == null || event.getStartTime() == null) {
                continue;
            }
            LocalDate eventDate = event.getStartTime().toDate().toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDate();
            if (!eventDate.isBefore(startDate) && !eventDate.isAfter(endDate)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    public static void saveViewState(Context context, LocalDate selectedDate, LocalDate startDate3Days,
            LocalDate selectedWeekDate) {
        if (context == null) {
            return;
        }
        ViewState state = new ViewState();
        state.selectedDate = selectedDate != null ? selectedDate.toString() : null;
        state.startDate3Days = startDate3Days != null ? startDate3Days.toString() : null;
        state.selectedWeekDate = selectedWeekDate != null ? selectedWeekDate.toString() : null;
        getPrefs(context).edit().putString(KEY_VIEW_STATE, gson.toJson(state)).apply();
    }

    public static ViewState loadViewState(Context context) {
        if (context == null) {
            return null;
        }
        String json = getPrefs(context).getString(KEY_VIEW_STATE, null);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, ViewState.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveLastTab(Context context, int tabIndex) {
        if (context == null) {
            return;
        }
        getPrefs(context).edit().putInt(KEY_LAST_TAB, tabIndex).apply();
    }

    public static int getLastTab(Context context) {
        if (context == null) {
            return 3;
        }
        return getPrefs(context).getInt(KEY_LAST_TAB, 3);
    }

    public static boolean hasAnyCache(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences prefs = getPrefs(context);
        return prefs.contains(KEY_DAY_CACHE) || prefs.contains(KEY_3DAYS_CACHE) || prefs.contains(KEY_WEEK_CACHE)
                || prefs.contains(KEY_MONTH_CACHE);
    }

    public static void clearCache(Context context) {
        if (context == null) {
            return;
        }
        getPrefs(context).edit().clear().apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static List<CachedEvent> toCachedEvents(List<Event> events) {
        List<CachedEvent> cached = new ArrayList<>();
        if (events == null) {
            return cached;
        }
        for (Event event : events) {
            if (event == null) {
                continue;
            }
            CachedEvent item = new CachedEvent();
            item.id = event.getId();
            item.title = event.getTitle();
            item.description = event.getDescription();
            item.location = event.getLocation();
            item.allDay = event.getAllDay();
            item.calendarId = event.getCalendarId();

            if (event.getStartTime() != null) {
                item.startMillis = event.getStartTime().toDate().getTime();
            }
            if (event.getEndTime() != null) {
                item.endMillis = event.getEndTime().toDate().getTime();
            }
            cached.add(item);
        }
        return cached;
    }

    private static List<Event> toEvents(List<CachedEvent> cachedEvents) {
        List<Event> events = new ArrayList<>();
        if (cachedEvents == null) {
            return events;
        }
        for (CachedEvent cached : cachedEvents) {
            if (cached == null) {
                continue;
            }
            Event event = new Event();
            event.setId(cached.id);
            event.setTitle(cached.title);
            event.setDescription(cached.description);
            event.setLocation(cached.location);
            event.setAllDay(cached.allDay != null && cached.allDay);
            event.setCalendarId(cached.calendarId);

            if (cached.startMillis != null) {
                event.setStartTime(new Timestamp(new Date(cached.startMillis)));
            }
            if (cached.endMillis != null) {
                event.setEndTime(new Timestamp(new Date(cached.endMillis)));
            }
            events.add(event);
        }
        return events;
    }

    public static class CachedRange {
        String startDate;
        String endDate;
        List<CachedEvent> events;
    }

    private static class CachedEvent {
        String id;
        String title;
        String description;
        String location;
        Boolean allDay;
        String calendarId;
        Long startMillis;
        Long endMillis;
    }

    public static class ViewState {
        public String selectedDate;
        public String startDate3Days;
        public String selectedWeekDate;
    }
}
