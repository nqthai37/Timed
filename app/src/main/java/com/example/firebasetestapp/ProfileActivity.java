package com.example.firebasetestapp;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.firebasetestapp.models.User;
import com.example.firebasetestapp.repositories.UserRepository;

public class ProfileActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private LinearLayout profileDataContainer;
    private TextView tvName, tvEmail, tvTheme, tvNotifications;

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        progressBar = findViewById(R.id.progressBar);
        profileDataContainer = findViewById(R.id.profileDataContainer);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvTheme = findViewById(R.id.tvTheme);
        tvNotifications = findViewById(R.id.tvNotifications);

        userRepository = new UserRepository();

        fetchUserProfile("JrBHoYhi1xQEsIwcgBFf");
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
