package com.timed.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.Date;

public class TasksRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_NAME = "tasks";

    public Task<DocumentReference> createTask(com.timed.models.Task task) {
        return db.collection(COLLECTION_NAME).add(task);
    }

    public Task<Void> updateTask(String taskId, com.timed.models.Task task) {
        return db.collection(COLLECTION_NAME).document(taskId).set(task);
    }

    public Task<Void> deleteTask(String taskId) {
        return db.collection(COLLECTION_NAME).document(taskId).delete();
    }

    public Task<DocumentSnapshot> getTaskById(String taskId) {
        return db.collection(COLLECTION_NAME).document(taskId).get();
    }

    // Lấy tất cả Task chưa hoàn thành của một user
    public Task<QuerySnapshot> getPendingTasksByUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("created_by", userId)
                .whereEqualTo("is_completed", false)
                .get();
    }

    // Lấy tất cả Task của một user (cả hoàn thành và chưa hoàn thành)
    public Task<QuerySnapshot> getAllTasksByUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("created_by", userId)
                .get();
    }

    // Lấy các Task có due date hôm nay
    public Task<QuerySnapshot> getTodaysTasks(String userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
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
