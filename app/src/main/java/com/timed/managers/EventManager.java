package com.timed.managers;

import android.util.Log;

import com.timed.data.models.Attachment;
import com.timed.data.models.EventModel;
import com.timed.data.models.Reminder;
import com.timed.data.repository.EventRepository;
import com.timed.utils.RecurrenceUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Manager class for handling all event-related operations
 * Acts as a bridge between the UI and the repository layer
 */
public class EventManager {
    private static final String TAG = "EventManager";
    private final EventRepository eventRepository;

    public EventManager() {
        this.eventRepository = new EventRepository();
    }

    /**
     * Create a new single event
     */
    public void createSingleEvent(String calendarId, String title, long startTime, long endTime,
                                 String description, String location, boolean allDay,
                                 EventRepository.OnEventListener<String> callback) {
        EventModel event = new EventModel();
        event.setCalendarId(calendarId);
        event.setTitle(title);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setDescription(description);
        event.setLocation(location);
        event.setAllDay(allDay);
        event.setVisibility("private");

        eventRepository.createEvent(event, callback);
    }

    /**
     * Create a recurring event
     */
    public void createRecurringEvent(String calendarId, String title, long startTime, long endTime,
                                     String recurrenceRule, String description, String location,
                                     boolean allDay, EventRepository.OnEventListener<String> callback) {
        EventModel event = new EventModel();
        event.setCalendarId(calendarId);
        event.setTitle(title);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setRecurrenceRule(recurrenceRule);
        event.setDescription(description);
        event.setLocation(location);
        event.setAllDay(allDay);
        event.setVisibility("private");

        eventRepository.createEvent(event, callback);
    }

    /**
     * Create a modified instance of a recurring event
     */
    public void createRecurringInstance(String parentEventId, String calendarId, String title,
                                       long startTime, long endTime, String description,
                                       EventRepository.OnEventListener<String> callback) {
        EventModel event = new EventModel();
        event.setCalendarId(calendarId);
        event.setTitle(title);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setDescription(description);
        event.setInstanceOf(parentEventId);
        event.setVisibility("private");

        eventRepository.createEvent(event, callback);
    }

    /**
     * Update an event
     */
    public void updateEvent(String eventId, EventModel eventModel,
                           EventRepository.OnEventListener<Void> callback) {
        eventRepository.updateEvent(eventId, eventModel, callback);
    }

    /**
     * Delete an event
     */
    public void deleteEvent(String eventId, EventRepository.OnEventListener<Void> callback) {
        eventRepository.deleteEvent(eventId, callback);
    }

    /**
     * Get event details
     */
    public void getEvent(String eventId, EventRepository.OnEventListener<EventModel> callback) {
        eventRepository.getEventById(eventId, callback);
    }

    /**
     * Get all events for a calendar
     */
    public void getCalendarEvents(String calendarId,
                                 EventRepository.OnEventListener<List<EventModel>> callback) {
        eventRepository.getEventsByCalendar(calendarId, callback);
    }

    /**
     * Get events within a specific date range
     */
    public void getEventsInDateRange(String calendarId, long startDate, long endDate,
                                    EventRepository.OnEventListener<List<EventModel>> callback) {
        // Adjust dates to full day boundaries
        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(startDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long adjustedStart = cal.getTimeInMillis();

        cal.setTimeInMillis(endDate);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long adjustedEnd = cal.getTimeInMillis();

        eventRepository.getEventsByDateRange(calendarId, adjustedStart, adjustedEnd, callback);
    }

    /**
     * Add a reminder to an event
     */
    public void addReminder(String eventId, int minutesBefore,
                           EventRepository.OnEventListener<Void> callback) {
        Reminder reminder = new Reminder("minutes", minutesBefore, true);
        eventRepository.addReminder(eventId, reminder, callback);
    }

    /**
     * Add a custom reminder
     */
    public void addCustomReminder(String eventId, String type, int value,
                                 EventRepository.OnEventListener<Void> callback) {
        Reminder reminder = new Reminder(type, value, true);
        eventRepository.addReminder(eventId, reminder, callback);
    }

    /**
     * Add attachment to an event
     */
    public void addAttachment(String eventId, String attachmentName, String filePath,
                             String fileType, long fileSize,
                             EventRepository.OnEventListener<Void> callback) {
        Attachment attachment = new Attachment(attachmentName, filePath, fileType,
                fileSize, System.currentTimeMillis());

        getEvent(eventId, new EventRepository.OnEventListener<EventModel>() {
            @Override
            public void onSuccess(EventModel event) {
                event.addAttachment(attachment);
                updateEvent(eventId, event, callback);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure("Failed to add attachment: " + errorMessage);
            }
        });
    }

    /**
     * Handle exception from recurring event (delete specific occurrence)
     */
    public void deleteRecurringEventOccurrence(String eventId, long occurrenceTime,
                                              EventRepository.OnEventListener<Void> callback) {
        eventRepository.addRecurrenceException(eventId, occurrenceTime, callback);
    }

    /**
     * Get all recurring events
     */
    public void getRecurringEvents(String calendarId,
                                  EventRepository.OnEventListener<List<EventModel>> callback) {
        eventRepository.getRecurringEvents(calendarId, callback);
    }

    /**
     * Generate all occurrences of a recurring event up to a certain count
     */
    public List<Long> generateRecurrenceOccurrences(String recurrenceRule, long startTime,
                                                    int maxOccurrences) {
        RecurrenceUtils.RecurrenceRule rule = RecurrenceUtils.parseRRule(recurrenceRule);
        return RecurrenceUtils.generateOccurrences(startTime, rule, maxOccurrences);
    }

    /**
     * Check if a timestamp matches a recurring pattern
     */
    public boolean isRecurrenceOccurrence(String recurrenceRule, long startTime, long timestamp,
                                         List<Long> exceptions) {
        RecurrenceUtils.RecurrenceRule rule = RecurrenceUtils.parseRRule(recurrenceRule);
        return RecurrenceUtils.isOccurrence(startTime, rule, timestamp, exceptions);
    }

    /**
     * Build iCal RRULE string for a recurring event
     */
    public String buildRecurrenceRule(String frequency, int interval, Integer count) {
        StringBuilder rule = new StringBuilder("FREQ=" + frequency);
        if (interval > 1) {
            rule.append(";INTERVAL=").append(interval);
        }
        if (count != null && count > 0) {
            rule.append(";COUNT=").append(count);
        }
        return rule.toString();
    }

    /**
     * Build RRULE for weekly recurring event (e.g., every Monday and Wednesday)
     */
    public String buildWeeklyRecurrenceRule(int interval, List<String> daysOfWeek, Integer count) {
        StringBuilder rule = new StringBuilder("FREQ=WEEKLY");
        if (interval > 1) {
            rule.append(";INTERVAL=").append(interval);
        }
        if (!daysOfWeek.isEmpty()) {
            rule.append(";BYDAY=").append(String.join(",", daysOfWeek));
        }
        if (count != null && count > 0) {
            rule.append(";COUNT=").append(count);
        }
        return rule.toString();
    }

    /**
     * Build RRULE for monthly recurring event
     */
    public String buildMonthlyRecurrenceRule(int interval, List<Integer> daysOfMonth, Integer count) {
        StringBuilder rule = new StringBuilder("FREQ=MONTHLY");
        if (interval > 1) {
            rule.append(";INTERVAL=").append(interval);
        }
        if (!daysOfMonth.isEmpty()) {
            StringBuilder days = new StringBuilder();
            for (Integer day : daysOfMonth) {
                if (days.length() > 0) days.append(",");
                days.append(day);
            }
            rule.append(";BYMONTHDAY=").append(days);
        }
        if (count != null && count > 0) {
            rule.append(";COUNT=").append(count);
        }
        return rule.toString();
    }

    /**
     * Add a participant to an event
     */
    public void addParticipant(String eventId, String participantUid, String status,
                              EventRepository.OnEventListener<Void> callback) {
        getEvent(eventId, new EventRepository.OnEventListener<EventModel>() {
            @Override
            public void onSuccess(EventModel event) {
                event.addParticipant(participantUid, status);
                updateEvent(eventId, event, callback);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure("Failed to add participant: " + errorMessage);
            }
        });
    }

    /**
     * Update participant status (accepted, declined, tentative)
     */
    public void updateParticipantStatus(String eventId, String participantUid, String status,
                                       EventRepository.OnEventListener<Void> callback) {
        getEvent(eventId, new EventRepository.OnEventListener<EventModel>() {
            @Override
            public void onSuccess(EventModel event) {
                if (event.getParticipantIds().contains(participantUid)) {
                    event.getParticipantStatuses().put(participantUid, status);
                    updateEvent(eventId, event, callback);
                } else {
                    callback.onFailure("Participant not found in event");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure("Failed to update participant status: " + errorMessage);
            }
        });
    }

    /**
     * Add notes to an event
     */
    public void addNotes(String eventId, String notes,
                        EventRepository.OnEventListener<Void> callback) {
        getEvent(eventId, new EventRepository.OnEventListener<EventModel>() {
            @Override
            public void onSuccess(EventModel event) {
                event.setNotes(notes);
                updateEvent(eventId, event, callback);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure("Failed to add notes: " + errorMessage);
            }
        });
    }
}

