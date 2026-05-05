package com.timed.repositories;

import com.timed.models.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.List;

public class EventsRepository {
    private final FirebaseFirestore db;
    private static final String EVENTS_COLLECTION = "events";
    private static final String TAG = "EventsRepository";

    public EventsRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private void logRepoError(String context, Exception e) {
        try {
            Log.e(TAG, context + " -> " + e.toString());
            // Log stacktrace manually as BuildConfig may be unavailable or in a different
            // package
            Log.e(TAG, context + " -> stacktrace: " + Log.getStackTraceString(e));
            if (e instanceof FirebaseFirestoreException) {
                Log.e(TAG, context + " -> firestore code: " + ((FirebaseFirestoreException) e).getCode());
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to log repo error", ex);
        }
    }

    /**
     * Create a new event
     */
    public Task<Void> createEvent(Event event) {
        if (event == null) {
            Exception error = new IllegalArgumentException("Event is null");
            logRepoError("createEvent", error);
            return Tasks.forException(error);
        }

        String eventId = event.getId();
        if (eventId == null || eventId.isEmpty()) {
            eventId = db.collection(EVENTS_COLLECTION).document().getId();
            event.setId(eventId);
        }

        Task<Void> t = db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .set(event);
        t.addOnFailureListener(e -> logRepoError("createEvent", e));
        return t;
    }

    /**
     * Get all events for a specific calendar
     */
    public Task<QuerySnapshot> getEventsByCalendarId(String calendarId) {
        Task<QuerySnapshot> t = db.collection(EVENTS_COLLECTION)
                .whereEqualTo("calendar_id", calendarId)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
        t.addOnFailureListener(e -> logRepoError("getEventsByCalendarId", e));
        return t;
    }

    /**
     * Get events by calendar ID and date range
     */
    public Task<QuerySnapshot> getEventsByDateRange(String calendarId, Timestamp startDate, Timestamp endDate) {
        // Query by start_time only so recurring parent events (which may start before rangeStart)
        // can still be loaded and expanded on the client side.
        if (endDate == null) {
            return db.collection(EVENTS_COLLECTION)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
    }

        return db.collection(EVENTS_COLLECTION)
            .whereLessThanOrEqualTo("start_time", endDate)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
        t.addOnFailureListener(e -> logRepoError("getEventsByDateRange", e));
        return t;
    }

    /**
     * Get events by creator and a list of calendar IDs (legacy migration)
     */
    public Task<QuerySnapshot> getEventsByCreatorAndCalendarIds(String userId, List<String> calendarIds) {
        if (userId == null || userId.isEmpty() || calendarIds == null || calendarIds.isEmpty()) {
            Exception error = new IllegalArgumentException("Invalid migration query inputs");
            logRepoError("getEventsByCreatorAndCalendarIds", error);
            return Tasks.forException(error);
        }

        Task<QuerySnapshot> t = db.collection(EVENTS_COLLECTION)
                .whereEqualTo("created_by", userId)
                .whereIn("calendar_id", calendarIds)
                .get();
        t.addOnFailureListener(e -> logRepoError("getEventsByCreatorAndCalendarIds", e));
        return t;
    }

    /**
     * Get upcoming events for a participant
     */
    public Task<QuerySnapshot> getUpcomingEventsByParticipant(String userId, Timestamp now) {
        Task<QuerySnapshot> t = db.collection(EVENTS_COLLECTION)
                .whereArrayContains("participant_id", userId)
                .whereGreaterThanOrEqualTo("start_time", now)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
        t.addOnFailureListener(e -> logRepoError("getUpcomingEventsByParticipant", e));
        return t;
    }

    /**
     * Get events that need reminders scheduled within a time window
     */
    public Task<QuerySnapshot> getEventsThatNeedReminders(String userId, Timestamp beforeDate) {
        Timestamp now = Timestamp.now();
        Task<QuerySnapshot> t = db.collection(EVENTS_COLLECTION)
                .whereArrayContains("participant_id", userId)
                .whereGreaterThanOrEqualTo("start_time", now)
                .whereLessThanOrEqualTo("start_time", beforeDate)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
        t.addOnFailureListener(e -> logRepoError("getEventsThatNeedReminders", e));
        return t;
    }

    /**
     * Get event by ID
     */
    public Task<com.google.firebase.firestore.DocumentSnapshot> getEventById(String eventId) {
        Task<com.google.firebase.firestore.DocumentSnapshot> t = db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get();
        t.addOnFailureListener(e -> logRepoError("getEventById", e));
        return t;
    }

    /**
     * Update an event
     */
    public Task<Void> updateEvent(String eventId, Event event) {
        Task<Void> t = db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .set(event);
        t.addOnFailureListener(e -> logRepoError("updateEvent", e));
        return t;
    }

    /**
     * Delete an event
     */
    public Task<Void> deleteEvent(String eventId) {
        Task<Void> t = db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete();
        t.addOnFailureListener(e -> logRepoError("deleteEvent", e));
        return t;
    }
}
