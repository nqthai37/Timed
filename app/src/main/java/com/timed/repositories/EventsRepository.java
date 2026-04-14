package com.timed.repositories;

import com.timed.models.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

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
            // Log stacktrace manually as BuildConfig may be unavailable or in a different package
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
        // Updated to use the correct model structure
        Task<Void> t = db.collection(EVENTS_COLLECTION)
            .document() // Auto ID
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
        Task<QuerySnapshot> t = db.collection(EVENTS_COLLECTION)
            .whereEqualTo("calendar_id", calendarId)
            .whereGreaterThanOrEqualTo("start_time", startDate)
            .whereLessThanOrEqualTo("start_time", endDate)
            .orderBy("start_time", Query.Direction.ASCENDING)
            .get();
        t.addOnFailureListener(e -> logRepoError("getEventsByDateRange", e));
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
