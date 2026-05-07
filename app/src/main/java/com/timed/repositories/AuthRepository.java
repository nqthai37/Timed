package com.timed.repositories;

import android.util.Log;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.timed.managers.UserManager;
import com.timed.models.User;
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

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();

                    if (!loggedInUser.getEmailVerified()) {
                        loggedInUser.setEmailVerified(true);
                        updates.put("emailVerified", true);
                    }

                    UserManager.getInstance().setCurrentUser(loggedInUser);

                    if (updates.isEmpty()) {
                        return Tasks.forResult(loggedInUser);
                    } else {
                        return userRepository.updateUser(uid, updates)
                                .continueWith(updateTask -> {
                                    if (!updateTask.isSuccessful()) {
                                        Log.e("AuthRepo", "Failed to update last_login", updateTask.getException());
                                    }
                                    return loggedInUser;
                                });
                    }
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

                        return userRepository.createUser(uid, newUser).continueWith(saveTask -> {
                            if (!saveTask.isSuccessful())
                                throw saveTask.getException();
                            UserManager.getInstance().setCurrentUser(newUser);
                            return newUser;
                        });

                    } else {
                        return userRepository.getUser(uid).continueWith(dbTask -> {
                            if (!dbTask.isSuccessful())
                                throw dbTask.getException();

                            User existingUser = dbTask.getResult().toObject(User.class);
                            UserManager.getInstance().setCurrentUser(existingUser);

                            return existingUser;
                        });
                    }
                });
    }

    public Task<Void> updatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            return user.reauthenticate(credential).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return user.updatePassword(newPassword);
            });
        }
        return Tasks.forException(new Exception("No user logged in."));
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
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            if (firebaseUser.isAnonymous()) {
                mAuth.signOut();
                UserManager.getInstance().setCurrentUser(null);
                return Tasks.forException(new Exception("Anonymous sessions cannot be restored"));
            }

            String uid = firebaseUser.getUid();

            return userRepository.getUser(uid).continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                com.google.firebase.firestore.DocumentSnapshot snapshot = task.getResult();
                if (snapshot == null || !snapshot.exists()) {
                    mAuth.signOut();
                    UserManager.getInstance().setCurrentUser(null);
                    throw new Exception("Saved user profile was not found");
                }

                User restoredUser = snapshot.toObject(User.class);
                if (restoredUser == null) {
                    mAuth.signOut();
                    UserManager.getInstance().setCurrentUser(null);
                    throw new Exception("Saved user profile could not be loaded");
                }

                if (restoredUser.getUid() == null || restoredUser.getUid().isEmpty()) {
                    restoredUser.setUid(uid);
                }

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
