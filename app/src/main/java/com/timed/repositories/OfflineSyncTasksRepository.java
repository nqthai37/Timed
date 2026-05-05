package com.timed.repositories;

import android.content.Context;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Calendar;
import java.util.Date;

public class OfflineSyncTasksRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_NAME = "tasks";
    
    public OfflineSyncTasksRepository(Context context) {}
    
    public Task<DocumentReference> createTask(com.timed.models.Task task) {
        DocumentReference docRef = db.collection(COLLECTION_NAME).document();
        task.setId(docRef.getId());

        docRef.set(task);

        TaskCompletionSource<DocumentReference> tcs = new TaskCompletionSource<>();
        tcs.setResult(docRef);
        return tcs.getTask();
    }
    
    public Task<Void> updateTask(String taskId, com.timed.models.Task task) {
        task.setId(taskId);
        task.setUpdated_at(new com.google.firebase.Timestamp(new Date()));
        
        db.collection(COLLECTION_NAME).document(taskId).set(task);
        
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        tcs.setResult(null);
        return tcs.getTask();
    }
    
    public Task<Void> deleteTask(String taskId) {
        db.collection(COLLECTION_NAME).document(taskId).delete();
        
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        tcs.setResult(null);
        return tcs.getTask();
    }
    
    public Task<QuerySnapshot> getPendingTasksByUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("created_by", userId)
                .whereEqualTo("is_completed", false)
                .get();
    }
    
    public Task<QuerySnapshot> getAllTasksByUser(String userId) {
        return db.collection(COLLECTION_NAME).whereEqualTo("created_by", userId).get();
    }
    
    public Task<QuerySnapshot> getTodaysTasks(String userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59);
        long endOfDay = calendar.getTimeInMillis();

        com.google.firebase.Timestamp startTimestamp = new com.google.firebase.Timestamp(new Date(startOfDay));
        com.google.firebase.Timestamp endTimestamp = new com.google.firebase.Timestamp(new Date(endOfDay));

        return db.collection(COLLECTION_NAME)
                .whereEqualTo("created_by", userId)
                .whereGreaterThanOrEqualTo("due_date", startTimestamp)
                .whereLessThanOrEqualTo("due_date", endTimestamp)
                .get();
    }
}