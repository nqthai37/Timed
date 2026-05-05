package com.timed.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.timed.models.CalendarModel;
import com.timed.utils.FirebaseInitializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persists visible calendar selections per user for fast UI filtering.
 */
public class CalendarVisibilityManager {
    private static final String PREFS_NAME = "calendar_visibility_prefs";
    private static final String PREF_VISIBLE_IDS_PREFIX = "visible_calendar_ids_";

    private final SharedPreferences preferences;
    private final FirebaseInitializer firebaseInitializer;

    public CalendarVisibilityManager(Context context) {
        Context appContext = context.getApplicationContext();
        this.preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firebaseInitializer = FirebaseInitializer.getInstance();
    }

    public void setCalendarVisible(String calendarId, boolean visible) {
        if (calendarId == null || calendarId.isEmpty()) {
            return;
        }

        Set<String> current = getVisibleCalendarIds();
        if (visible) {
            current.add(calendarId);
        } else {
            current.remove(calendarId);
        }
        saveVisibleCalendarIds(current);
    }

    public boolean isCalendarVisible(String calendarId) {
        if (calendarId == null || calendarId.isEmpty()) {
            return false;
        }
        return getVisibleCalendarIds().contains(calendarId);
    }

    public Set<String> getVisibleCalendarIds() {
        String key = getVisibleIdsKey();
        if (key == null) {
            return new LinkedHashSet<>();
        }

        Set<String> saved = preferences.getStringSet(key, null);
        if (saved == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(saved);
    }

    public void saveVisibleCalendarIds(Collection<String> calendarIds) {
        String key = getVisibleIdsKey();
        if (key == null) {
            return;
        }

        Set<String> sanitized = new LinkedHashSet<>();
        if (calendarIds != null) {
            for (String id : calendarIds) {
                if (id != null && !id.isEmpty()) {
                    sanitized.add(id);
                }
            }
        }

        preferences.edit().putStringSet(key, sanitized).apply();
    }

    /**
     * Resolves visibility state for current calendars.
     * Default behavior: if user has never saved visibility, show all calendars.
     */
    public List<String> resolveVisibleCalendarIds(List<CalendarModel> calendars, String fallbackCalendarId) {
        List<String> availableIds = new ArrayList<>();
        if (calendars != null) {
            for (CalendarModel calendar : calendars) {
                if (calendar != null && calendar.getId() != null && !calendar.getId().isEmpty()) {
                    availableIds.add(calendar.getId());
                }
            }
        }

        if (availableIds.isEmpty()) {
            return new ArrayList<>();
        }

        String key = getVisibleIdsKey();
        boolean hasSavedState = key != null && preferences.contains(key);

        if (!hasSavedState) {
            return new ArrayList<>(availableIds);
        }

        Set<String> availableSet = new HashSet<>(availableIds);
        Set<String> saved = getVisibleCalendarIds();
        List<String> resolved = new ArrayList<>();

        for (String id : availableIds) {
            if (saved.contains(id)) {
                resolved.add(id);
            }
        }

        if (resolved.isEmpty()) {
            if (fallbackCalendarId != null && availableSet.contains(fallbackCalendarId)) {
                resolved.add(fallbackCalendarId);
            } else {
                resolved.add(availableIds.get(0));
            }
        }

        return resolved;
    }

    private String getVisibleIdsKey() {
        String userId = firebaseInitializer.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        return PREF_VISIBLE_IDS_PREFIX + userId;
    }
}
