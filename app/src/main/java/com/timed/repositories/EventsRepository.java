package com.timed.repositories;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.timed.Features.ConflictResolver.ConflictEvent;
import com.timed.Features.FreeSlotFinder.FreeSlot;
import com.timed.Setting.SyncStorage.SyncStorageActivity;
import com.timed.managers.UserManager;
import com.timed.models.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsRepository {
    private final FirebaseFirestore db;
    private static final String EVENTS_COLLECTION = "events";
    private static final String TAG = "EventsRepository";

    public EventsRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnConflictCheckListener {
        void onConflictsFound(List<ConflictEvent> conflicts);
        void onError(Exception e);
    }

    public interface OnFreeSlotsFoundListener {
        void onSlotsFound(List<FreeSlot> freeSlots);
        void onError(Exception e);
    }

    public void findFreeSlots(long dateMillis, long minDurationMs, String timeBound, String calendarId, OnFreeSlotsFoundListener listener) {
        if (calendarId == null || calendarId.isEmpty()) {
            listener.onError(new Exception("No calendar selected"));
            return;
        }

        String uid = UserManager.getInstance().getCurrentUser().getUid();
        if (uid == null) return;

        // 1. Calculate the start and end of the chosen day
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        Date startOfDay = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
        Date endOfDay = cal.getTime();

        // 2. Build the Firebase Query
        Query query = db.collection("events")
                .whereEqualTo("created_by", uid)
                .whereGreaterThanOrEqualTo("start_time", startOfDay)
                .whereLessThan("start_time", endOfDay)
                .orderBy("start_time", Query.Direction.ASCENDING);

        query.get().addOnSuccessListener(snapshots -> {
            List<FreeSlot> availableSlots = new ArrayList<>();

            // 3. Define the Search Window based on the "Time" chip
            long searchStartMillis = getSearchStartBound(dateMillis, timeBound);
            long searchEndMillis = getSearchEndBound(dateMillis, timeBound);

            // Default to 30 mins if "Any duration" is selected
            long durationRequired = (minDurationMs == 0) ? (30 * 60000L) : minDurationMs;

            // 4. THE GAP FINDING ALGORITHM
            long currentPointer = searchStartMillis;
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

            for (DocumentSnapshot doc : snapshots) {
                if (!doc.contains("start_time") || !doc.contains("end_time")) continue;

                long eventStart = doc.getTimestamp("start_time").toDate().getTime();
                long eventEnd = doc.getTimestamp("end_time").toDate().getTime();

                // Is there a gap before this event?
                if (eventStart > currentPointer) {
                    long gapSize = eventStart - currentPointer;

                    if (gapSize >= durationRequired && currentPointer < searchEndMillis) {
                        // We found a valid slot! Format it for the UI.
                        long slotEnd = Math.min(eventStart, searchEndMillis);
                        String timeRange = timeFormat.format(new Date(currentPointer)) + " - " + timeFormat.format(new Date(slotEnd));

                        // Calculate hours/mins for the subtitle
                        String durationText = formatDurationCleanly(slotEnd - currentPointer);

                        availableSlots.add(new FreeSlot(timeRange, durationText, doc.getId(), currentPointer, slotEnd));
                    }
                }
                // Move the pointer forward (Math.max handles overlapping events beautifully)
                currentPointer = Math.max(currentPointer, eventEnd);
            }

            // 5. Check the final gap after the very last event of the day
            if (searchEndMillis - currentPointer >= durationRequired) {
                String timeRange = timeFormat.format(new Date(currentPointer)) + " - " + timeFormat.format(new Date(searchEndMillis));
                String durationText = formatDurationCleanly(searchEndMillis - currentPointer);

                availableSlots.add(new FreeSlot(timeRange, durationText, "end_slot", currentPointer, searchEndMillis));
            }

            listener.onSlotsFound(availableSlots);

        }).addOnFailureListener(listener::onError);
    }

    private String formatDurationCleanly(long durationMillis) {
        long mins = durationMillis / 60000;
        if (mins < 60) {
            return mins + " mins";
        } else {
            double hours = mins / 60.0;
            String formatted = String.format(Locale.getDefault(), "%.1f", hours).replace(".0", "");
            return formatted + (formatted.equals("1") ? " hour" : " hours");
        }
    }

    private long getSearchStartBound(long dateMillis, String timeBound) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMillis);

        if (timeBound.contains("Afternoon")) cal.set(Calendar.HOUR_OF_DAY, 12);
        else if (timeBound.contains("Evening")) cal.set(Calendar.HOUR_OF_DAY, 17);
        else cal.set(Calendar.HOUR_OF_DAY, 5); // Default/Morning starts at 5 AM

        cal.set(Calendar.MINUTE, 0);

        return cal.getTimeInMillis();
    }

    private long getSearchEndBound(long dateMillis, String timeBound) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMillis);

        if (timeBound.contains("Morning")) cal.set(Calendar.HOUR_OF_DAY, 12);
        else if (timeBound.contains("Afternoon")) cal.set(Calendar.HOUR_OF_DAY, 17);
        else cal.set(Calendar.HOUR_OF_DAY, 23); // Default/Evening ends at 11 PM

        cal.set(Calendar.MINUTE, 0);

        return cal.getTimeInMillis();
    }

    public void checkConflictsOnDay(long newStart, long newEnd, String newEventTitle, OnConflictCheckListener listener) {
        String uid = UserManager.getInstance().getCurrentUser().getUid();

        if (uid == null) {
            listener.onError(new Exception("User not logged in"));
            return;
        }

        // Calculate the absolute start and end of the day
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(newStart);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        Date startOfDay = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        Date endOfDay = cal.getTime();

        // Query: Get events for THIS user, on THIS day
        db.collection("events")
                .whereEqualTo("created_by", uid)
                .whereGreaterThanOrEqualTo("start_time", startOfDay)
                .whereLessThanOrEqualTo("start_time", endOfDay)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<ConflictEvent> conflicts = new ArrayList<>();
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Date existingStart = doc.getTimestamp("start_time").toDate();
                        Date existingEnd = doc.getTimestamp("end_time").toDate();
                        String existingTitle = doc.getString("title");

                        if (newStart < existingEnd.getTime() && newEnd > existingStart.getTime()) {

                            String timeRange = timeFormat.format(existingStart) + " - " + timeFormat.format(existingEnd);

                            // TODO: change syncstorage activity to event detail
                            conflicts.add(new ConflictEvent(
                                    existingTitle,
                                    timeRange,
                                    newEventTitle,
                                    doc.getId(),
                                    true,
                                    SyncStorageActivity.class
                            ));
                        }
                    }
                    // Send the finalized list back to the UI
                    listener.onConflictsFound(conflicts);

                })
                .addOnFailureListener(listener::onError);
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
