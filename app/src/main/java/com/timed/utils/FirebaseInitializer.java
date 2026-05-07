package com.timed.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.auth.FirebaseAuth;

public class FirebaseInitializer {
    private static final String TAG = "FirebaseInitializer";
    private static FirebaseInitializer instance;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private FirebaseInitializer() {
    }

    public static FirebaseInitializer getInstance() {
        if (instance == null) {
            instance = new FirebaseInitializer();
        }
        return instance;
    }

    public void initialize(Context context) {
        try {
            FirebaseApp.initializeApp(context);
            Log.d(TAG, "Firebase App initialized");

            db = FirebaseFirestore.getInstance();
            configureFirestore();

            auth = FirebaseAuth.getInstance();

            Log.d(TAG, "Firebase initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
        }
    }

    private void configureFirestore() {
        try {
            PersistentCacheSettings cacheSettings = PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();

            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(cacheSettings)
                    .build();

            db.setFirestoreSettings(settings);
            Log.d(TAG, "✅ Firestore configured with offline persistence enabled");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error configuring Firestore: " + e.getMessage(), e);
        }
    }

    public FirebaseFirestore getFirestore() {
        if (db == null)
            db = FirebaseFirestore.getInstance();
        return db;
    }

    public FirebaseAuth getAuth() {
        if (auth == null)
            auth = FirebaseAuth.getInstance();
        return auth;
    }

    public boolean isUserLoggedIn() {
        return auth != null && auth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        if (auth != null && auth.getCurrentUser() != null)
            return auth.getCurrentUser().getUid();
        return null;
    }

    public String getCurrentUserEmail() {
        if (auth != null && auth.getCurrentUser() != null)
            return auth.getCurrentUser().getEmail();
        return null;
    }

    public void logout() {
        if (auth != null) {
            auth.signOut();
            Log.d(TAG, "User logged out");
        }
    }

    public void reset() {
        db = null;
        auth = null;
        instance = null;
    }
}
