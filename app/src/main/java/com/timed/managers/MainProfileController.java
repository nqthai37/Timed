package com.timed.managers;

import android.app.Activity;
import android.content.Intent;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.timed.Auth.LoginActivity;
import com.timed.R;
import com.timed.models.User;
import com.timed.repositories.AuthRepository;
import com.timed.repositories.CalendarOwnerRepository;
import com.timed.repositories.UserRepository;

import java.util.HashMap;
import java.util.Map;

public class MainProfileController {
    public interface Listener {
        void onUserLoaded(User user);
    }

    private final Activity activity;
    private final ImageView profileImage;
    private final UserRepository userRepository;
    private final CalendarOwnerRepository calendarOwnerRepository;
    private final Listener listener;

    public MainProfileController(Activity activity, ImageView profileImage, UserRepository userRepository,
            CalendarOwnerRepository calendarOwnerRepository, Listener listener) {
        this.activity = activity;
        this.profileImage = profileImage;
        this.userRepository = userRepository;
        this.calendarOwnerRepository = calendarOwnerRepository;
        this.listener = listener;
    }

    public void setup() {
        if (profileImage != null) {
            profileImage.setOnClickListener(this::showProfileMenu);
        }
    }

    public void refreshProfileAvatar() {
        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            calendarOwnerRepository.cacheOwnerName(currentUser);
            notifyUserLoaded(currentUser);
            if (isValidAvatarUrl(currentUser.getAvatar())) {
                loadAvatar(currentUser.getAvatar());
                return;
            }
        }

        String userId = currentUser != null ? currentUser.getUid() : null;
        if (userId == null) {
            return;
        }

        userRepository.getUser(userId)
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        UserManager.getInstance().setCurrentUser(user);
                        calendarOwnerRepository.cacheOwnerName(user);
                        if (isValidAvatarUrl(user.getAvatar())) {
                            loadAvatar(user.getAvatar());
                        }
                        notifyUserLoaded(user);
                    }
                });
    }

    private void showProfileMenu(View anchor) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        menu.getMenu().add("Change avatar");
        menu.getMenu().add("Sign out");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if ("Change avatar".equals(title)) {
                User currentUser = UserManager.getInstance().getCurrentUser();
                String currentUrl = currentUser != null && currentUser.getAvatar() != null
                        ? currentUser.getAvatar()
                        : "";
                AccountActionManager.showChangeAvatarDialog(activity, currentUrl, this::updateAvatarUrl);
                return true;
            }
            if ("Sign out".equals(title)) {
                AccountActionManager.showSignOutDialog(activity, this::handleSignOut);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void updateAvatarUrl(String url) {
        User currentUser = UserManager.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : null;
        if (userId == null) {
            Toast.makeText(activity, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("avatar", url);
        userRepository.updateUser(userId, updates)
                .addOnSuccessListener(unused -> {
                    currentUser.setAvatar(url);
                    loadAvatar(url);
                    notifyUserLoaded(currentUser);
                })
                .addOnFailureListener(e -> Toast.makeText(activity,
                        "Failed to update avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadAvatar(String url) {
        if (profileImage == null) {
            return;
        }
        Glide.with(activity)
                .load(url)
                .circleCrop()
                .error(R.drawable.ic_account_circle)
                .into(profileImage);
    }

    private void handleSignOut() {
        new AuthRepository().logout();
        activity.getSharedPreferences("TimedAppPrefs", Activity.MODE_PRIVATE)
                .edit()
                .putBoolean("REMEMBER_ME", false)
                .apply();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private boolean isValidAvatarUrl(String url) {
        return url != null && Patterns.WEB_URL.matcher(url).matches();
    }

    private void notifyUserLoaded(User user) {
        if (listener != null) {
            listener.onUserLoaded(user);
        }
    }
}
