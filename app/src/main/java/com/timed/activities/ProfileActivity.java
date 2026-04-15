package com.timed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.timed.R;
import com.timed.Auth.LoginActivity;
import com.timed.managers.UserManager;
import com.timed.models.User;
import com.timed.repositories.AuthRepository;
import com.timed.repositories.UserRepository;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private LinearLayout profileDataContainer;
    private ImageView ivAvatar;
    private TextView tvName, tvEmail, tvTheme, tvNotifications;
    private MaterialButton btnTempLogout;

    private UserRepository userRepository;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        progressBar = findViewById(R.id.progressBar);
        profileDataContainer = findViewById(R.id.profileDataContainer);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvTheme = findViewById(R.id.tvTheme);
        tvNotifications = findViewById(R.id.tvNotifications);
        btnTempLogout = findViewById(R.id.btnTempLogout);

        userRepository = new UserRepository();
        authRepository = new AuthRepository();

        fetchUserProfile(UserManager.getInstance().getCurrentUser().getUid());
        setupClickListeners();
    }

    private void setupClickListeners() {
        btnTempLogout.setOnClickListener(v -> {
            authRepository.logout();

            getSharedPreferences("TimedAppPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("REMEMBER_ME", false)
                    .apply();

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void fetchUserProfile(String uid) {
        progressBar.setVisibility(View.VISIBLE);
        profileDataContainer.setVisibility(View.GONE);

        userRepository.getUser(uid).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {

                User myUser = documentSnapshot.toObject(User.class);

                if (myUser != null) {
                    tvName.setText("Name: " + myUser.getName());
                    tvEmail.setText("Email: " + myUser.getEmail());

                    if (myUser.getAvatar() != null && !myUser.getAvatar().isEmpty()) {
                        Glide.with(this)
                                .load(myUser.getAvatar())
                                .circleCrop()
                                .into(ivAvatar);
                    }

                    if (myUser.getSettings() != null) {
                        tvTheme.setText("Theme: " + myUser.getSettings().getTheme());

                        boolean pushEnabled = myUser.getSettings().getNotifications().isPush();
                        tvNotifications.setText("Push Notifications: " + (pushEnabled ? "ON" : "OFF"));
                    }

                    progressBar.setVisibility(View.GONE);
                    profileDataContainer.setVisibility(View.VISIBLE);
                }
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show();
            }

        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
