package com.example.firebasetestapp.repositories;

import android.util.Log;

import com.example.firebasetestapp.managers.UserManager;
import com.example.firebasetestapp.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthRepository {
    private final FirebaseAuth mAuth;
    private final UserRepository userRepository;

    private final String defaultAvatar = "https://res.cloudinary.com/dpsqhztqa/image/upload/v1774512640/default_person_drlyqj.webp";

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

                    if (!authTask.getResult().getUser().isEmailVerified()) {
                        mAuth.signOut();
                        throw new Exception("Please verify your email before logging in.");
                    }

                    String uid = authTask.getResult().getUser().getUid();
                    return userRepository.getUser(uid);
                })
                .continueWithTask(dbTask -> {
                    if (!dbTask.isSuccessful()) {
                        throw dbTask.getException();
                    }

                    com.google.firebase.firestore.DocumentSnapshot snapshot = dbTask.getResult();

                    User loggedInUser = snapshot.toObject(User.class);
                    String uid = loggedInUser.getUid();

                    Timestamp now = Timestamp.now();
                    if (loggedInUser.getSecurity() != null) {
                        loggedInUser.getSecurity().setLastLogin(now);
                    } else {
                        User.Security security = new User.Security();
                        security.setLastLogin(now);
                        loggedInUser.setSecurity(security);
                    }

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("security.last_login", now);

                    if (!loggedInUser.getEmailVerified()) {
                        loggedInUser.setEmailVerified(true);
                        updates.put("emailVerified", true);
                    }

                    UserManager.getInstance().setCurrentUser(loggedInUser);

                    return userRepository.updateUser(uid, updates)
                            .continueWith(updateTask -> {
                                if (!updateTask.isSuccessful()) {
                                    Log.e("AuthRepo", "Failed to update last_login", updateTask.getException());
                                }
                                return loggedInUser;
                            });
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
                    newUser.setAvatar(defaultAvatar);
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

    public Task<User> signInWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        return mAuth.signInWithCredential(credential)
                .continueWithTask(authTask -> {
                    if (!authTask.isSuccessful()) {
                        throw authTask.getException();
                    }

                    boolean isNewUser = authTask.getResult().getAdditionalUserInfo().isNewUser();
                    com.google.firebase.auth.FirebaseUser firebaseUser = authTask.getResult().getUser();
                    String uid = firebaseUser.getUid();

                    if (isNewUser) {
                        Timestamp now = Timestamp.now();
                        User newUser = new User();

                        newUser.setUid(firebaseUser.getUid());
                        newUser.setName(firebaseUser.getDisplayName());

                        String actualEmail = firebaseUser.getEmail();

                        if (actualEmail == null) {
                            for (com.google.firebase.auth.UserInfo userInfo : firebaseUser.getProviderData()) {
                                if (userInfo.getEmail() != null) {
                                    actualEmail = userInfo.getEmail();
                                    break;
                                }
                            }
                        }
                        newUser.setEmail(actualEmail);

                        newUser.setEmailVerified(true);
                        newUser.setAuthProvider("google.com");

                        if (firebaseUser.getPhotoUrl() != null) {
                            newUser.setAvatar(firebaseUser.getPhotoUrl().toString());
                        } else {
                            newUser.setAvatar(defaultAvatar);
                        }

                        newUser.setCreatedAt(now);
                        newUser.setUpdatedAt(now);

                        User.Settings settings = new User.Settings();
                        settings.setTheme("light");
                        User.Notifications notifs = new User.Notifications();
                        notifs.setEmail(true);
                        notifs.setPush(true);
                        notifs.setSnoozeDefaultMinutes(5);
                        settings.setNotifications(notifs);
                        newUser.setSettings(settings);

                        User.Security security = new User.Security();
                        security.setTwoFactorEnabled(false);
                        security.setLastLogin(now);
                        newUser.setSecurity(security);

                        return userRepository.createUser(uid, newUser).continueWith(saveTask -> {
                            if (!saveTask.isSuccessful())
                                throw saveTask.getException();
                            UserManager.getInstance().setCurrentUser(newUser);
                            return newUser;
                        });

                    } else {
                        return userRepository.getUser(uid).continueWithTask(dbTask -> {
                            if (!dbTask.isSuccessful())
                                throw dbTask.getException();
                            User existingUser = dbTask.getResult().toObject(User.class);

                            Timestamp now = Timestamp.now();
                            if (existingUser.getSecurity() != null) {
                                existingUser.getSecurity().setLastLogin(now);
                            }

                            UserManager.getInstance().setCurrentUser(existingUser);

                            java.util.Map<String, Object> updates = new java.util.HashMap<>();
                            updates.put("security.last_login", now);

                            return userRepository.updateUser(uid, updates)
                                    .continueWith(updateTask -> {
                                        if (!updateTask.isSuccessful()) {
                                            Log.e("AuthRepo", "Failed to update last_login", updateTask.getException());
                                        }
                                        return existingUser;
                                    });
                        });
                    }
                });
    }

    public Task<Void> sendVerificationEmail() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().sendEmailVerification();
        }
        return Tasks.forException(new Exception("No user logged in"));
    }

    public Task<Void> markEmailAsVerifiedInFirestore() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            User currentUser = UserManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                currentUser.setEmailVerified(true);
            }

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("emailVerified", true);

            return userRepository.updateUser(uid, updates);
        }
        return Tasks.forException(new Exception("No user logged in"));
    }

    public Task<Void> sendPasswordResetEmail(String email) {
        return mAuth.sendPasswordResetEmail(email);
    }

    public Task<Void> reloadUser() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().reload();
        }
        return com.google.android.gms.tasks.Tasks.forException(new Exception("No user logged in"));
    }

    public boolean isCurrentUserVerified() {
        return mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified();
    }

    public Task<User> restoreSession() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            return userRepository.getUser(uid).continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                com.google.firebase.firestore.DocumentSnapshot snapshot = task.getResult();
                User restoredUser = snapshot.toObject(User.class);

                UserManager.getInstance().setCurrentUser(restoredUser);
                return restoredUser;
            });
        }
        return com.google.android.gms.tasks.Tasks.forException(new Exception("No saved session found"));
    }

    public void logout() {
        mAuth.signOut();
        UserManager.getInstance().setCurrentUser(null);
    }
}
