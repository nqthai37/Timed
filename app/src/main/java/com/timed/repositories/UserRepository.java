package com.timed.repositories;

import com.timed.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class UserRepository {
    private final FirebaseFirestore db;

    private static final String COLLECTION_NAME = "users";

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<Void> createUser(String uid, User user) {
        return db.collection(COLLECTION_NAME)
                .document(uid)
                .set(user);
    }

    public Task<DocumentSnapshot> getUser(String uid) {
        return db.collection(COLLECTION_NAME)
                .document(uid)
                .get();
    }

    public Task<Void> updateUser(String uid, Map<String, Object> updates) {
        return db.collection(COLLECTION_NAME)
                .document(uid)
                .update(updates);
    }

    public Task<Void> deleteUser(String uid) {
        return db.collection(COLLECTION_NAME)
                .document(uid)
                .delete();
    }
}
