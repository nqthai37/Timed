package com.example.firebasetestapp.repositories;

import com.example.firebasetestapp.managers.UserManager;
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

    public Task<User> loginWithEmail(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password)
                .continueWithTask(authTask -> {
                    if (!authTask.isSuccessful()) {
                        throw authTask.getException();
                    }

                    String uid = authTask.getResult().getUser().getUid();
                    return userRepository.getUser(uid);
                })
                .continueWith(dbTask -> {
                    if (!dbTask.isSuccessful()) {
                        throw dbTask.getException();
                    }

                    com.google.firebase.firestore.DocumentSnapshot snapshot = dbTask.getResult();

                    User loggedInUser = snapshot.toObject(User.class);
                    UserManager.getInstance().setCurrentUser(loggedInUser);

                    return loggedInUser;
                });
    }

    public Task<Void> registerWithEmail(String name, String email, String password) {

        return mAuth.createUserWithEmailAndPassword(email, password)
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    String uid = task.getResult().getUser().getUid();

                    User newUser = new User();
                    newUser.setUid(uid);
                    newUser.setName(name);
                    newUser.setEmail(email);
                    newUser.setAvatar("https://res.cloudinary.com/dpsqhztqa/image/upload/v1774512640/default_person_drlyqj.webp");
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

                    UserManager.getInstance().setCurrentUser(newUser);

                    return userRepository.createUser(uid, newUser);
                });
    }

}
