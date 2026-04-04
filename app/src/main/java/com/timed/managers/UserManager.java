package com.timed.managers;

import com.timed.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserManager {
    private static UserManager instance;
    private FirebaseAuth firebaseAuth;
    private User currentUser;

    private UserManager() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Get current user from Firebase Auth
     */
    public User getCurrentUser() {
        if (currentUser == null) {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                currentUser = new User();
                currentUser.setUid(firebaseUser.getUid());
                currentUser.setEmail(firebaseUser.getEmail());
                currentUser.setName(firebaseUser.getDisplayName());
            }
        }
        return currentUser;
    }

    /**
     * Set current user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Get current user's UID
     */
    public String getCurrentUserId() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }
        return null;
    }

    /**
     * Check if user is logged in
     */
    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Clear user session
     */
    public void clearSession() {
        currentUser = null;
        firebaseAuth.signOut();
    }

    /**
     * Get Firebase Auth instance
     */
    public FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }
}
