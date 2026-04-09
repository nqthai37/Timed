package com.mobile.timed.data.repository;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.mobile.timed.data.models.EventModel;
import com.mobile.timed.data.models.Reminder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Repository for managing Event operations with Firebase Firestore
 */
public class EventRepository {
    private static final String TAG = "EventRepository";
    private static final String EVENTS_COLLECTION = "events";

    private final FirebaseFirestore db;

    public EventRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new event
     */
    public void createEvent(EventModel event, OnEventListener<String> callback) {
        db.collection(EVENTS_COLLECTION)
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();
                    // Update the event with its generated ID
                    documentReference.update("id", eventId)
                            .addOnSuccessListener(unused -> {
                                callback.onSuccess(eventId);
                                Log.d(TAG, "Event created with ID: " + eventId);
                            })
                            .addOnFailureListener(e -> {
                                callback.onFailure(e.getMessage());
                                Log.e(TAG, "Failed to update event ID: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to create event: " + e.getMessage());
                });
    }

    /**
     * Get event by ID
     */
    public void getEventById(String eventId, OnEventListener<EventModel> callback) {
        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        EventModel event = documentSnapshot.toObject(EventModel.class);
                        callback.onSuccess(event);
                    } else {
                        callback.onFailure("Event not found");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to fetch event: " + e.getMessage());
                });
    }

    /**
     * Get all events for a calendar
     */
    public void getEventsByCalendar(String calendarId, OnEventListener<List<EventModel>> callback) {
        db.collection(EVENTS_COLLECTION)
                .whereEqualTo("calendar_id", calendarId)
                .orderBy("start_time")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<EventModel> events = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        EventModel event = querySnapshot.getDocuments().get(i).toObject(EventModel.class);
                        events.add(event);
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to fetch events: " + e.getMessage());
                });
    }

    /**
     * Get events within a date range
     */
    public void getEventsByDateRange(String calendarId, long startTime, long endTime,
                                     OnEventListener<List<EventModel>> callback) {
        db.collection(EVENTS_COLLECTION)
                .whereEqualTo("calendar_id", calendarId)
                .whereGreaterThanOrEqualTo("start_time", startTime)
                .whereLessThanOrEqualTo("end_time", endTime)
                .orderBy("start_time")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<EventModel> events = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        EventModel event = querySnapshot.getDocuments().get(i).toObject(EventModel.class);
                        events.add(event);
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to fetch events by date range: " + e.getMessage());
                });
    }

    /**
     * Update an event
     */
    public void updateEvent(String eventId, EventModel event, OnEventListener<Void> callback) {
        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .set(event)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(null);
                    Log.d(TAG, "Event updated: " + eventId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to update event: " + e.getMessage());
                });
    }

    /**
     * Delete an event
     */
    public void deleteEvent(String eventId, OnEventListener<Void> callback) {
        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(null);
                    Log.d(TAG, "Event deleted: " + eventId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to delete event: " + e.getMessage());
                });
    }

    /**
     * Add a reminder to an event
     */
    public void addReminder(String eventId, Reminder reminder, OnEventListener<Void> callback) {
        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update("reminders", com.google.firebase.firestore.FieldValue.arrayUnion(reminder))
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(null);
                    Log.d(TAG, "Reminder added to event: " + eventId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to add reminder: " + e.getMessage());
                });
    }

    /**
     * Get recurring event instances
     */
    public void getRecurringEvents(String calendarId, OnEventListener<List<EventModel>> callback) {
        db.collection(EVENTS_COLLECTION)
                .whereEqualTo("calendar_id", calendarId)
                .whereNotEqualTo("recurrence_rule", null)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<EventModel> events = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        EventModel event = querySnapshot.getDocuments().get(i).toObject(EventModel.class);
                        if (event.isRecurring()) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to fetch recurring events: " + e.getMessage());
                });
    }

    /**
     * Add an exception to a recurring event
     */
    public void addRecurrenceException(String eventId, long exceptionTimestamp, OnEventListener<Void> callback) {
        db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update("recurrence_exceptions", com.google.firebase.firestore.FieldValue.arrayUnion(exceptionTimestamp))
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(null);
                    Log.d(TAG, "Recurrence exception added to event: " + eventId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to add recurrence exception: " + e.getMessage());
                });
    }

    /**
     * Generic listener interface for asynchronous operations
     */
    public interface OnEventListener<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }
}

