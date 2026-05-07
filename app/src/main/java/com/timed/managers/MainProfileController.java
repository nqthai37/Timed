package com.timed.managers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.timed.Auth.LoginActivity;
import com.timed.BuildConfig;
import com.timed.R;
import com.timed.models.User;
import com.timed.repositories.AuthRepository;
import com.timed.repositories.CalendarOwnerRepository;
import com.timed.repositories.UserRepository;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

public class MainProfileController {
    public interface Listener {
        void onUserLoaded(User user);
    }

    private final Activity activity;
    private final ImageView profileImage;
    private final UserRepository userRepository;
    private final CalendarOwnerRepository calendarOwnerRepository;
    private final Listener listener;
    private ActivityResultLauncher<String> avatarPickerLauncher;
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String DEFAULT_AVATAR =
            "https://res.cloudinary.com/dpsqhztqa/image/upload/v1774512640/default_person_drlyqj.webp";

    public MainProfileController(Activity activity, ImageView profileImage, UserRepository userRepository,
            CalendarOwnerRepository calendarOwnerRepository, Listener listener) {
        this.activity = activity;
        this.profileImage = profileImage;
        this.userRepository = userRepository;
        this.calendarOwnerRepository = calendarOwnerRepository;
        this.listener = listener;
    }

    public void setup() {
        if (activity instanceof AppCompatActivity) {
            avatarPickerLauncher = ((AppCompatActivity) activity).registerForActivityResult(
                    new ActivityResultContracts.GetContent(), this::uploadSelectedAvatar);
        }
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
        menu.getMenu().add("Choose avatar");
        menu.getMenu().add("Use default avatar");
        menu.getMenu().add("Sign out");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if ("Choose avatar".equals(title)) {
                if (avatarPickerLauncher != null) {
                    avatarPickerLauncher.launch("image/*");
                } else {
                    Toast.makeText(activity, "Image picker unavailable", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            if ("Use default avatar".equals(title)) {
                updateAvatarUrl(DEFAULT_AVATAR);
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

    private void uploadSelectedAvatar(Uri uri) {
        if (uri == null) {
            return;
        }
        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isEmpty() || BuildConfig.CLOUDINARY_UPLOAD_PRESET.isEmpty()) {
            Toast.makeText(activity, "Cloudinary is not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] bytes = readUriBytes(uri);
            RequestBody fileBody = RequestBody.create(bytes, MediaType.parse("image/*"));
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "avatar.jpg", fileBody)
                    .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                    .build();
            String url = "https://api.cloudinary.com/v1_1/" + BuildConfig.CLOUDINARY_CLOUD_NAME + "/image/upload";
            Request request = new Request.Builder().url(url).post(requestBody).build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    activity.runOnUiThread(() -> Toast.makeText(activity,
                            "Failed to upload avatar", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        activity.runOnUiThread(() -> Toast.makeText(activity,
                                "Failed to upload avatar", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    try {
                        String uploadedUrl = new JSONObject(body).optString("secure_url");
                        activity.runOnUiThread(() -> updateAvatarUrl(uploadedUrl));
                    } catch (Exception e) {
                        activity.runOnUiThread(() -> Toast.makeText(activity,
                                "Invalid upload response", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(activity, "Cannot read selected image", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] readUriBytes(Uri uri) throws java.io.IOException {
        java.io.InputStream input = activity.getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new java.io.IOException("Cannot open image");
        }
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
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
