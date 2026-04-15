package com.timed.managers;

import android.content.Context;
import android.util.Log;

import com.timed.models.Event;
import com.timed.repositories.EventsRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.timed.utils.RecurrenceUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventsManager {
    private static EventsManager instance;
    private final EventsRepository eventsRepository;
    private final EventsNotificationManager notificationManager;
    private final Context context;
    private static final String TAG = "EventsManager";
    private static final int MAX_RANGE_OCCURRENCES = 512;

    private EventsManager(Context context) {
        this.context = context;
        this.eventsRepository = new EventsRepository();
        this.notificationManager = new EventsNotificationManager(context);
    }

    public static synchronized EventsManager getInstance(Context context) {
        if (instance == null) {
            instance = new EventsManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Create a new event and schedule reminders
     */
    public Task<DocumentReference> createEvent(Event event) {
        if (event.getId() == null) {
            event.setId(UUID.randomUUID().toString());
        }
        
        event.setCreatedAt(Timestamp.now());
        event.setUpdatedAt(Timestamp.now());

        final String eventId = event.getId();
        
        return eventsRepository.createEvent(event)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event created successfully: " + eventId);
                    // Schedule reminders
                    notificationManager.scheduleEventReminders(event);
                })
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        DocumentReference docRef = 
                                FirebaseFirestore.getInstance()
                                .collection("events")
                                .document(eventId);
                        return Tasks.forResult(docRef);
                    }
                    return Tasks.forException(task.getException());
                });
    }

    /**
     * Update an event and reschedule reminders
     */
    public Task<Void> updateEvent(String eventId, Event event) {
        event.setUpdatedAt(Timestamp.now());
        
        return eventsRepository.getEventById(eventId)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        // Cancel old reminders
                        com.google.firebase.firestore.DocumentSnapshot oldDoc = task.getResult();
                        if (oldDoc.exists()) {
                            Event oldEvent = oldDoc.toObject(Event.class);
                            if (oldEvent != null) {
                                notificationManager.cancelEventReminders(oldEvent);
                            }
                        }
                        
                        // Update event in database
                        return eventsRepository.updateEvent(eventId, event);
                    }
                    return Tasks.forException(task.getException());
                })
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event updated successfully: " + eventId);
                    // Schedule new reminders
                    notificationManager.scheduleEventReminders(event);
                });
    }

    /**
     * Get all events for a calendar
     */
    public Task<List<Event>> getEventsByCalendarId(String calendarId) {
        return eventsRepository.getEventsByCalendarId(calendarId)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    
                    List<Event> events = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        events.add(event);
                    }
                    events.sort(Comparator.comparingLong(this::getStartMillisForSort));
                    Log.d(TAG, "Retrieved " + events.size() + " events for calendar: " + calendarId);
                    return events;
                });
    }

    /**
     * Get events within a date range
     */
    public Task<List<Event>> getEventsByDateRange(String calendarId, Timestamp startDate, Timestamp endDate) {
        return eventsRepository.getEventsByDateRange(calendarId, startDate, endDate)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    
                    long rangeStart = startDate.toDate().getTime();
                    long rangeEnd = endDate.toDate().getTime();
                    List<Event> events = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        events.addAll(expandEventForRange(event, rangeStart, rangeEnd));
                    }
                    events.sort(Comparator.comparingLong(this::getStartMillisForSort));
                    return events;
                });
    }

    private List<Event> expandEventForRange(Event event, long rangeStart, long rangeEnd) {
        List<Event> expanded = new ArrayList<>();
        if (event == null || event.getStartTime() == null) {
            return expanded;
        }

        String recurrenceRule = event.getRecurrenceRule();
        if (recurrenceRule == null || recurrenceRule.trim().isEmpty()) {
            if (isEventOverlappingRange(event, rangeStart, rangeEnd)) {
                expanded.add(event);
            }
            return expanded;
        }

        long eventStart = event.getStartTime().toDate().getTime();
        long eventEnd = event.getEndTime() != null
                ? event.getEndTime().toDate().getTime()
                : eventStart;
        long duration = Math.max(0L, eventEnd - eventStart);

        RecurrenceUtils.RecurrenceRule parsedRule = RecurrenceUtils.parseRRule(recurrenceRule);
        List<Long> occurrenceStarts = RecurrenceUtils.generateOccurrencesInRange(
                eventStart,
                parsedRule,
                rangeStart,
                rangeEnd,
                event.getRecurrenceExceptions(),
                MAX_RANGE_OCCURRENCES
        );

        for (Long occurrenceStart : occurrenceStarts) {
            if (occurrenceStart == null) {
                continue;
            }
            long occurrenceEnd = occurrenceStart + duration;
            expanded.add(copyEventForOccurrence(event, occurrenceStart, occurrenceEnd));
        }

        return expanded;
    }

    private Event copyEventForOccurrence(Event source, long occurrenceStart, long occurrenceEnd) {
        Event occurrence = new Event();

        occurrence.setId(source.getId());
        occurrence.setTitle(source.getTitle());
        occurrence.setDescription(source.getDescription());
        occurrence.setLocation(source.getLocation());
        occurrence.setAllDay(source.getAllDay());
        occurrence.setTimezone(source.getTimezone());
        occurrence.setCreatedBy(source.getCreatedBy());
        occurrence.setCalendarId(source.getCalendarId());
        occurrence.setVisibility(source.getVisibility());
        occurrence.setCreatedAt(source.getCreatedAt());
        occurrence.setUpdatedAt(source.getUpdatedAt());
        occurrence.setRecurrenceRule(source.getRecurrenceRule());

        if (source.getRecurrenceExceptions() != null) {
            occurrence.setRecurrenceExceptions(new ArrayList<>(source.getRecurrenceExceptions()));
        } else {
            occurrence.setRecurrenceExceptions(new ArrayList<>());
        }

        if (source.getParticipantId() != null) {
            occurrence.setParticipantId(new ArrayList<>(source.getParticipantId()));
        }

        if (source.getParticipantStatus() != null) {
            occurrence.setParticipantStatus(new HashMap<>(source.getParticipantStatus()));
        }

        if (source.getReminders() != null) {
            List<Event.EventReminder> reminders = new ArrayList<>();
            for (Event.EventReminder reminder : source.getReminders()) {
                if (reminder == null) {
                    continue;
                }
                reminders.add(new Event.EventReminder(reminder.getMinutesBefore(), reminder.getType()));
            }
            occurrence.setReminders(reminders);
        }

        if (source.getAttachments() != null) {
            List<Event.EventAttachment> attachments = new ArrayList<>();
            for (Event.EventAttachment attachment : source.getAttachments()) {
                if (attachment == null) {
                    continue;
                }
                attachments.add(new Event.EventAttachment(attachment.getName(), attachment.getUrl()));
            }
            occurrence.setAttachments(attachments);
        }

        String parentId = source.getInstanceOf();
        if (parentId == null || parentId.isEmpty()) {
            parentId = source.getId();
        }
        occurrence.setInstanceOf(parentId);

        occurrence.setStartTime(new Timestamp(new java.util.Date(occurrenceStart)));
        occurrence.setEndTime(new Timestamp(new java.util.Date(occurrenceEnd)));
        return occurrence;
    }

    private boolean isEventOverlappingRange(Event event, long rangeStart, long rangeEnd) {
        if (event == null || event.getStartTime() == null) {
            return false;
        }

        long eventStart = event.getStartTime().toDate().getTime();
        long eventEnd = event.getEndTime() != null
                ? event.getEndTime().toDate().getTime()
                : eventStart;

        if (eventEnd < eventStart) {
            eventEnd = eventStart;
        }

        return eventStart <= rangeEnd && eventEnd >= rangeStart;
    }

    private long getStartMillisForSort(Event event) {
        if (event == null || event.getStartTime() == null) {
            return Long.MAX_VALUE;
        }
        return event.getStartTime().toDate().getTime();
    }

    /**
     * Get upcoming events for current user
     */
    public Task<List<Event>> getUpcomingEventsForUser(String userId) {
        Timestamp now = Timestamp.now();
        
        return eventsRepository.getUpcomingEventsByParticipant(userId, now)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    
                    List<Event> events = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        events.add(event);
                    }
                    return events;
                });
    }

    /**
     * Get events that need reminder scheduling
     */
    public Task<List<Event>> getEventsThatNeedReminders(String userId) {
        // Calculate time window: now to 24 hours from now
        long timeWindow = 24 * 60 * 60 * 1000; // 24 hours in ms
        Timestamp beforeDate = new Timestamp(new java.util.Date(System.currentTimeMillis() + timeWindow));
        
        return eventsRepository.getEventsThatNeedReminders(userId, beforeDate)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    
                    List<Event> events = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        events.add(event);
                    }
                    return events;
                });
    }

    /**
     * Get event by ID
     */
    public Task<Event> getEventById(String eventId) {
        return eventsRepository.getEventById(eventId)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    
                    Event event = task.getResult().toObject(Event.class);
                    if (event != null) {
                        event.setId(task.getResult().getId());
                    }
                    return event;
                });
    }

    /**
     * Delete an event
     */
    public Task<Void> deleteEvent(String eventId) {
        return eventsRepository.getEventById(eventId)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        Event event = task.getResult().toObject(Event.class);
                        if (event != null) {
                            // Cancel reminders
                            notificationManager.cancelEventReminders(event);
                        }
                    }
                    return eventsRepository.deleteEvent(eventId);
                })
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event deleted successfully: " + eventId);
                });
    }

    /**
     * Add participant to event
     */
    public Task<Void> addParticipant(String eventId, String userId, String status) {
        return eventsRepository.getEventById(eventId)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Event event = task.getResult().toObject(Event.class);
                        if (event != null) {
                            if (!event.getParticipantId().contains(userId)) {
                                event.getParticipantId().add(userId);
                            }
                            event.getParticipantStatus().put(userId, status != null ? status : "pending");
                            return eventsRepository.updateEvent(eventId, event);
                        }
                    }
                    return Tasks.forException(new Exception("Event not found"));
                });
    }

    /**
     * Update participant status
     */
    public Task<Void> updateParticipantStatus(String eventId, String userId, String status) {
        return eventsRepository.getEventById(eventId)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Event event = task.getResult().toObject(Event.class);
                        if (event != null) {
                            event.getParticipantStatus().put(userId, status);
                            return eventsRepository.updateEvent(eventId, event);
                        }
                    }
                    return Tasks.forException(new Exception("Event not found"));
                });
    }

    /**
     * Schedule reminders for events that need them
     * Call this periodically (e.g., on app startup, in a worker)
     */
    public void rescheduleAllReminders(String userId) {
        eventsRepository.getEventsByParticipant(userId)
                .addOnSuccessListener(snapshot -> {
                    int scheduled = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Event event = doc.toObject(Event.class);
                        if (event == null) {
                            continue;
                        }
                        event.setId(doc.getId());
                        notificationManager.scheduleEventReminders(event);
                        scheduled++;
                    }
                    Log.d(TAG, "Rescheduled reminders for " + scheduled + " events");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to reschedule reminders: " + e.getMessage()));
    }
}
