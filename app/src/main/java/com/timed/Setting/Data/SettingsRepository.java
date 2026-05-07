package com.timed.Setting.Data;

import java.util.ArrayList;
import java.util.List;

// Chuyển toàn bộ mớ import bòng bong của bạn sang đây
import com.timed.R;
import com.timed.Features.AI.AiSchedulingActivity;
import com.timed.Features.Analytics.AnalyticsActivity;
import com.timed.Features.FocusMode.FocusModeActivity;
import com.timed.Features.FreeSlotFinder.FreeSlotFinderActivity;
import com.timed.Features.HabitTracker.HabitTrackerActivity;
import com.timed.Setting.Main.SettingItem;
import com.timed.Setting.Profile.ProfileActivity;
import com.timed.Setting.Timezone.TimezoneSettingActivity;
import com.timed.Setting.Themes.ThemeActivity;

public class SettingsRepository {
    
    // Tạo một hàm chuyên cung cấp dữ liệu
    public static List<SettingItem> getSettingsData() {
        List<SettingItem> settingList = new ArrayList<>();
        
        settingList.add(new SettingItem("Time & Timezone", R.drawable.ic_time_zone, "Timezone Setting", TimezoneSettingActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_free_slot_finder, "Free Slot Finder", FreeSlotFinderActivity.class));
        settingList.add(new SettingItem("Productivity & Focus",R.drawable.ic_habit_tracker, "Habit Tracker", HabitTrackerActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_focus, "Focus Mode", FocusModeActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_focus, "AI Scheduling", AiSchedulingActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_analytics, "Analytics", AnalyticsActivity.class));
        settingList.add(new SettingItem("System & Security",R.drawable.ic_security, "Profile", ProfileActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_analytics,"Theme", ThemeActivity.class));
        return settingList;
    }
}
