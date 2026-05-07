package com.timed.Setting.Main;

import android.os.Bundle;
import android.widget.ImageView;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import com.timed.R;
import com.timed.activities.BaseBottomNavActivity;
import com.timed.Features.AI.AiSchedulingActivity;
import com.timed.Features.Analytics.AnalyticsActivity;
import com.timed.Features.FocusMode.FocusModeActivity;
import com.timed.Features.FreeSlotFinder.FreeSlotFinderActivity;
import com.timed.Features.HabitTracker.HabitTrackerActivity;
import com.timed.Setting.Profile.ProfileActivity;
import com.timed.Setting.Timezone.TimezoneSettingActivity;
import com.timed.Setting.Themes.ThemeActivity;

public class SettingActivity extends BaseBottomNavActivity {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<SettingItem> settingList;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_setting);
        setupInsets();

        ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.rv_settings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        settingList = new ArrayList<>();
        settingList.add(new SettingItem("Time & Timezone", R.drawable.ic_time_zone, "Timezone Setting",
                TimezoneSettingActivity.class));
        settingList
                .add(new SettingItem(R.drawable.ic_free_slot_finder, "Free Slot Finder", FreeSlotFinderActivity.class));
        settingList.add(new SettingItem("Productivity & Focus", R.drawable.ic_habit_tracker, "Habit Tracker",
                HabitTrackerActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_focus, "Focus Mode", FocusModeActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_focus, "AI Scheduling", AiSchedulingActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_analytics, "Analytics", AnalyticsActivity.class));
        settingList.add(new SettingItem("System & Security", R.drawable.ic_security, "Profile", ProfileActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_theme, "Theme & Appearance", ThemeActivity.class)); // Placeholder
                                                                                                          // for future
                                                                                                          // implementation
        adapter = new SettingsAdapter(this, settingList);
        recyclerView.setAdapter(adapter);
        setupBottomNavigation();
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.nav_settings;
    }

    private void setupInsets() {
        View root = findViewById(R.id.settingsRoot);
        if (root == null) {
            return;
        }
        final int baseTop = root.getPaddingTop();
        final int baseBottom = root.getPaddingBottom();
        final int baseLeft = root.getPaddingLeft();
        final int baseRight = root.getPaddingRight();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseLeft + bars.left, baseTop + bars.top,
                    baseRight + bars.right, baseBottom + bars.bottom);
            return insets;
        });
    }
}
