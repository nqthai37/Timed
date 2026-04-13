package com.timed.Setting.Main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import com.google.android.material.button.MaterialButton;
import com.timed.Auth.LoginActivity;
import com.timed.R;
import com.timed.Setting.AI.AiSchedulingActivity;
import com.timed.Setting.Analytics.AnalyticsActivity;
import com.timed.Setting.ConflictResolver.ConflictResolverActivity;
import com.timed.Setting.FocusMode.FocusModeActivity;
import com.timed.Setting.FreeSlotFinder.FreeSlotFinderActivity;
import com.timed.Setting.HabitTracker.HabitTrackerActivity;
import com.timed.Setting.Security.SecurityActivity;
import com.timed.Setting.SyncStorage.SyncStorageActivity;
import com.timed.Setting.Timezone.TimezoneSettingActivity;
import com.timed.Setting.Themes.ThemeActivity;
import com.timed.repositories.AuthRepository;

public class SettingActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<SettingItem> settingList;
    private MaterialButton btnLogout;
    private ImageView ivBack;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        authRepository = new AuthRepository();

        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> handleLogout());

        ivBack  = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rv_settings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        settingList = new ArrayList<>();
        settingList.add(new SettingItem("Time & Timezone",R.drawable.ic_time_zone, "Timezone Setting", TimezoneSettingActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_conflict_resolver, "Conflict Resolver", ConflictResolverActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_free_slot_finder, "Free Slot Finder", FreeSlotFinderActivity.class));
        settingList.add(new SettingItem("Productivity & Focus",R.drawable.ic_habit_tracker, "Habit Tracker", HabitTrackerActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_focus, "Focus Mode", FocusModeActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_focus, "AI Scheduling", AiSchedulingActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_analytics, "Analytics", AnalyticsActivity.class));
        settingList.add(new SettingItem("System & Security",R.drawable.ic_security, "Security", SecurityActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_cloud, "Sync & Storage", SyncStorageActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_theme, "Theme & Appearance", ThemeActivity.class)); // Placeholder for future implementation
        adapter = new SettingsAdapter(this, settingList);
        recyclerView.setAdapter(adapter);
    }

    private void handleLogout() {
        authRepository.logout();

        getSharedPreferences("TimedAppPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("REMEMBER_ME", false)
                .apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
