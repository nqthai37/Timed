package com.timed.utils;

import com.timed.Event;
import com.timed.data.models.EventModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter để convert giữa UI Event Model và Backend EventModel
 */
public class EventModelAdapter {
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    /**
     * Convert UI Event → Backend EventModel
     */
    public static EventModel toBackendModel(Event uiEvent, String calendarId, String userId) {
        EventModel backendEvent = new EventModel();
        backendEvent.setTitle(uiEvent.getTitle());
        backendEvent.setDescription(uiEvent.getDetails());
        backendEvent.setLocation("");
        backendEvent.setNotes(uiEvent.getDetails());
        backendEvent.setCalendarId(calendarId);
        backendEvent.setCreatedBy(userId);
        backendEvent.setVisibility("private");
        // Note: createdAt and updatedAt will be set by Firestore @ServerTimestamp
        // backendEvent.setCreatedAt(System.currentTimeMillis());
        // backendEvent.setUpdatedAt(System.currentTimeMillis());

        return backendEvent;
    }

    /**
     * Convert Backend EventModel → UI Event
     */
    public static Event toUIEvent(EventModel backendEvent) {
        String timeStr = formatTime(backendEvent.getStartTime());
        return new Event(
            backendEvent.getId(),
            backendEvent.getCalendarId(),
            timeStr,
            backendEvent.getTitle(),
            backendEvent.getDescription(),
            backendEvent.getLocation(),
            backendEvent.getStartTime(),
            backendEvent.getEndTime(),
            backendEvent.isAllDay()
        );
    }

    /**
     * Convert List của Backend Events → UI Events
     */
    public static List<Event> toUIEventList(List<EventModel> backendEvents) {
        List<Event> uiEvents = new ArrayList<>();
        for (EventModel backendEvent : backendEvents) {
            uiEvents.add(toUIEvent(backendEvent));
        }
        return uiEvents;
    }

    /**
     * Convert List của UI Events → Backend Events
     */
    public static List<EventModel> toBackendModelList(List<Event> uiEvents, String calendarId, String userId) {
        List<EventModel> backendEvents = new ArrayList<>();
        for (Event uiEvent : uiEvents) {
            backendEvents.add(toBackendModel(uiEvent, calendarId, userId));
        }
        return backendEvents;
    }

    /**
     * Format timestamp thành giờ
     */
    private static String formatTime(long timestamp) {
        try {
            return timeFormat.format(new Date(timestamp));
        } catch (Exception e) {
            return "00:00";
        }
    }

    /**
     * Format timestamp thành ngày
     */
    public static String formatDate(long timestamp) {
        try {
            return dateFormat.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Format timestamp thành datetime
     */
    public static String formatDateTime(long timestamp) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return dateTimeFormat.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }
}

