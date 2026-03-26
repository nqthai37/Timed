package com.example.firebasetestapp.repositories;

import com.example.firebasetestapp.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;

import java.util.Collections;

public class AuthRepository {
    private final FirebaseAuth mAuth;
    private final UserRepository userRepository;

    public AuthRepository() {
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
    }

    public Task<Void> registerWithEmail(String name, String email, String password) {

        return mAuth.createUserWithEmailAndPassword(email, password)
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    String uid = task.getResult().getUser().getUid();

                    User newUser = new User();
                    newUser.setName(name);
                    newUser.setEmail(email);
                    newUser.setEmailVerified(false);
                    newUser.setAuthProvider("password");

                    User.Settings settings = new User.Settings();
                    settings.setTheme("light");

                    User.Notifications notifications = new User.Notifications();
                    notifications.setEmail(true);
                    notifications.setPush(true);
                    notifications.setSnoozeDefaultMinutes(5);
                    settings.setNotifications(notifications);

                    newUser.setSettings(settings);

                    Timestamp now = Timestamp.now();

                    User.Security security = new User.Security();
                    security.setTwoFactorEnabled(false);
                    security.setLastLogin(now);
                    newUser.setSecurity(security);

                    newUser.setCreatedAt(now);
                    newUser.setUpdatedAt(now);

                    return userRepository.createUser(uid, newUser);
                });
    }

}
