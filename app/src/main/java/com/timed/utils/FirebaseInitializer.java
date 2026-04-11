package com.timed.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Firebase Initializer - Khởi tạo và cấu hình Firebase cho ứng dụng
 * Cung cấp singleton instances cho Firestore và Auth
 */
public class FirebaseInitializer {
    private static final String TAG = "FirebaseInitializer";
    private static FirebaseInitializer instance;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private FirebaseInitializer() {}

    /**
     * Lấy singleton instance
     */
    public static FirebaseInitializer getInstance() {
        if (instance == null) {
            instance = new FirebaseInitializer();
        }
        return instance;
    }

    /**
     * Khởi tạo Firebase - gọi lần đầu trong MainActivity.onCreate()
     */
    public void initialize(Context context) {
        try {
            // Khởi tạo Firebase App
            FirebaseApp.initializeApp(context);
            Log.d(TAG, "Firebase App initialized");

            // Lấy Firestore instance
            db = FirebaseFirestore.getInstance();

            // Cấu hình Firestore settings
            configureFirestore();

            // Lấy Auth instance
            auth = FirebaseAuth.getInstance();

            Log.d(TAG, "Firebase initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
        }
    }

    /**
     * Cấu hình Firestore settings
     * - Bật persistence để hỗ trợ offline
     * - Cấu hình cache size
     */
    private void configureFirestore() {
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)  // Bật offline persistence
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();

            db.setFirestoreSettings(settings);
            Log.d(TAG, "Firestore configured with offline persistence enabled");
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Firestore: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy Firestore instance
     */
    public FirebaseFirestore getFirestore() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }

    /**
     * Lấy Firebase Auth instance
     */
    public FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }

    /**
     * Kiểm tra xem người dùng có đã đăng nhập hay không
     */
    public boolean isUserLoggedIn() {
        return auth != null && auth.getCurrentUser() != null;
    }

    /**
     * Lấy UID của người dùng hiện tại
     */
    public String getCurrentUserId() {
        if (auth != null && auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    /**
     * Lấy email của người dùng hiện tại
     */
    public String getCurrentUserEmail() {
        if (auth != null && auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getEmail();
        }
        return null;
    }

    /**
     * Đăng xuất người dùng
     */
    public void logout() {
        if (auth != null) {
            auth.signOut();
            Log.d(TAG, "User logged out");
        }
    }

    /**
     * Reset Firebase (dùng cho testing)
     */
    public void reset() {
        db = null;
        auth = null;
        instance = null;
    }
}

