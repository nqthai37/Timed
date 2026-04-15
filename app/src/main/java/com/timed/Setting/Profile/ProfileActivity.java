package com.timed.Setting.Profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;
import com.timed.Auth.LoginActivity;
import com.timed.managers.UserManager;
import com.timed.models.User;
import com.timed.repositories.AuthRepository;
import com.timed.repositories.UserRepository;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private NestedScrollView profileDataContainer;
    private ImageView ivAvatar, ivBack;
    private TextView tvName, tvEmail;
    private RecyclerView rvProfileOptions;

    private ProfileAdapter adapter;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        progressBar = findViewById(R.id.progressBar);
        profileDataContainer = findViewById(R.id.profileDataContainer);
        ivAvatar = findViewById(R.id.ivAvatar);
        ivBack = findViewById(R.id.iv_back);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        rvProfileOptions = findViewById(R.id.rv_profile_options);

        userRepository = new UserRepository();

        if (UserManager.getInstance().getCurrentUser() != null) {
            fetchUserProfile(UserManager.getInstance().getCurrentUser().getUid());
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupRecyclerView();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        rvProfileOptions.setLayoutManager(new LinearLayoutManager(this));

        List<ProfileOption> options = new ArrayList<>();
        options.add(new ProfileOption("Change Password", R.drawable.ic_key, ProfileOption.TYPE_ARROW, false));

        adapter = new ProfileAdapter(options, new ProfileAdapter.OnItemClickListener() {
            @Override
            public void onClick(ProfileOption option) {
                if (option.getTitle().equals("Change Password")) {
                    // TODO: Replace Toast with Intent to ChangePasswordActivity
                    Toast.makeText(ProfileActivity.this, "Opening Change Password", Toast.LENGTH_SHORT).show();
                    // startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
                }
            }

            @Override
            public void onSwitchChange(ProfileOption option, boolean isChecked) {

            }
        });

        rvProfileOptions.setAdapter(adapter);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
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
