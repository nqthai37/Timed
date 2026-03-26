package com.timed.Setting.Data;

import java.util.ArrayList;
import java.util.List;

// Chuyển toàn bộ mớ import bòng bong của bạn sang đây
import com.timed.R;
import com.timed.Setting.AI.AiSchedulingActivity;
import com.timed.Setting.Analytics.AnalyticsActivity;
import com.timed.Setting.ConflictResolver.ConflictResolverActivity;
import com.timed.Setting.FocusMode.FocusModeActivity;
import com.timed.Setting.FreeSlotFinder.FreeSlotFinderActivity;
import com.timed.Setting.HabitTracker.HabitTrackerActivity;
import com.timed.Setting.Main.SettingItem;
import com.timed.Setting.Security.SecurityActivity;
import com.timed.Setting.SyncStorage.SyncStorageActivity;
import com.timed.Setting.Timezone.TimezoneSettingActivity;

public class SettingsRepository {
    
    // Tạo một hàm chuyên cung cấp dữ liệu
    public static List<SettingItem> getSettingsData() {
        List<SettingItem> settingList = new ArrayList<>();
        
        settingList.add(new SettingItem("Time & Timezone", R.drawable.ic_time_zone, "Timezone Setting", TimezoneSettingActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_conflict_resolver, "Conflict Resolver", ConflictResolverActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_free_slot_finder, "Free Slot Finder", FreeSlotFinderActivity.class));
        settingList.add(new SettingItem("Productivity & Focus",R.drawable.ic_habit_tracker, "Habit Tracker", HabitTrackerActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_focus, "Focus Mode", FocusModeActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_focus, "AI Scheduling", AiSchedulingActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_analytics, "Analytics", AnalyticsActivity.class));
        settingList.add(new SettingItem("System & Security",R.drawable.ic_security, "Security", SecurityActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_cloud, "Sync & Storage", SyncStorageActivity.class));
        
        return settingList;
    }
}