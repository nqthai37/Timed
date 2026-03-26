package com.example.firebasetestapp.managers;

import com.example.firebasetestapp.models.User;

public class UserManager {
    private static UserManager instance;

    private User currentUser;

    private UserManager() {}

    public static synchronized UserManager getInstance() {
        if (instance == null) instance = new UserManager();

        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        currentUser = user;
    }

    public void clearSession() {
        currentUser = null;
    }

    public boolean isUserLoggedIn() {
        return currentUser != null;
    }
}
