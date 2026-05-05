package com.timed.repositories;

import android.content.Context;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Calendar;
import java.util.Date;

public class OfflineSyncEventsRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_NAME = "events";
    
    public OfflineSyncEventsRepository(Context context) {}
    
    public Task<DocumentReference> createEvent(com.timed.models.Event event) {
        DocumentReference docRef = db.collection(COLLECTION_NAME).document();
        event.setId(docRef.getId());

        docRef.set(event);

        TaskCompletionSource<DocumentReference> tcs = new TaskCompletionSource<>();
        tcs.setResult(docRef);
        return tcs.getTask();
    }
    
    public Task<Void> updateEvent(String eventId, com.timed.models.Event event) {
        event.setId(eventId);
        db.collection(COLLECTION_NAME).document(eventId).set(event);
        
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        tcs.setResult(null);
        return tcs.getTask();
    }
    
    public Task<Void> deleteEvent(String eventId) {
        db.collection(COLLECTION_NAME).document(eventId).delete();
        
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        tcs.setResult(null);
        return tcs.getTask();
    }

    public Task<QuerySnapshot> getEventsByUser(String userId) {
        return db.collection(COLLECTION_NAME).whereEqualTo("created_by", userId).get();
    }
    
    public Task<QuerySnapshot> getTodaysEvents(String userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59);
        long endOfDay = calendar.getTimeInMillis();

        com.google.firebase.Timestamp startTimestamp = new com.google.firebase.Timestamp(new Date(startOfDay));
        com.google.firebase.Timestamp endTimestamp = new com.google.firebase.Timestamp(new Date(endOfDay));

        return db.collection(COLLECTION_NAME)
                .whereEqualTo("created_by", userId)
                .whereGreaterThanOrEqualTo("start_time", startTimestamp)
                .whereLessThanOrEqualTo("end_time", endTimestamp)
                .get();
    }
}