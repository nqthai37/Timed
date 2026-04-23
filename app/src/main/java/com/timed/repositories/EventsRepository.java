package com.timed.repositories;

import androidx.annotation.NonNull;

import com.timed.models.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;

public class EventsRepository {
    private final FirebaseFirestore db;
    private static final String EVENTS_COLLECTION = "events";

    public EventsRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new event
     */
    public Task<Void> createEvent(Event event) {
        return db.collection(EVENTS_COLLECTION)
                .document(event.getId())
                .set(event);
    }

    /**
     * Get all events for a specific calendar
     */
    public Task<QuerySnapshot> getEventsByCalendarId(String calendarId) {
        return db.collection(EVENTS_COLLECTION)
                .whereEqualTo("calendar_id", calendarId)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
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
    }

    /**
     * Get event by ID
     */
    public Task<com.google.firebase.firestore.DocumentSnapshot> getEventById(String eventId) {
        return db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get();
    }

    /**
     * Get events for a user (participant)
     */
    public Task<QuerySnapshot> getEventsByParticipant(String userId) {
        return db.collection(EVENTS_COLLECTION)
                .whereArrayContains("participant_id", userId)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
    }

    /**
     * Get upcoming events for a user
     */
    public Task<QuerySnapshot> getUpcomingEventsByParticipant(String userId, Timestamp afterDate) {
        return db.collection(EVENTS_COLLECTION)
                .whereArrayContains("participant_id", userId)
                .whereGreaterThanOrEqualTo("start_time", afterDate)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
    }

    /**
     * Get events with reminders that need to be processed
     */
    public Task<QuerySnapshot> getEventsThatNeedReminders(String userId, Timestamp beforeDate) {
        return db.collection(EVENTS_COLLECTION)
                .whereArrayContains("participant_id", userId)
                .whereLessThanOrEqualTo("start_time", beforeDate)
                .whereGreaterThan("start_time", Timestamp.now())
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
    }

    /**
     * Update an event
     */
    public Task<Void> updateEvent(String eventId, Event event) {
        event.setUpdatedAt(Timestamp.now());
        return db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .set(event);
    }

    /**
     * Delete an event
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete();
    }

    /**
     * Get events created by a specific user
     */
    public Task<QuerySnapshot> getEventsByCreator(String userId) {
        return db.collection(EVENTS_COLLECTION)
                .whereEqualTo("created_by", userId)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get();
    }

    /**
     * Get all events (for admin purposes)
     */
    public Task<QuerySnapshot> getAllEvents() {
        return db.collection(EVENTS_COLLECTION)
                .orderBy("start_time", Query.Direction.DESCENDING)
                .get();
    }
}
