package com.timed;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<SettingItem> settingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        recyclerView = findViewById(R.id.rvSettings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo danh sách các mục cài đặt
        settingList = new ArrayList<>();

        settingList.add(new SettingItem("Sync & Storage", SyncStorageActivity.class));
        settingList.add(new SettingItem("Timezone Setting", TimezoneSettingActivity.class));
        settingList.add(new SettingItem("AI Scheduling", AiSchedulingActivity.class));
        settingList.add(new SettingItem("Analytics", AnalyticsActivity.class));
        settingList.add(new SettingItem("Conflict Resolver", ConflictResolverActivity.class));
        settingList.add(new SettingItem("Focus Mode", FocusModeActivity.class));
        settingList.add(new SettingItem("Free Slot Finder", FreeSlotFinderActivity.class));
        settingList.add(new SettingItem("Habit Tracker", HabitTrackerActivity.class));
        settingList.add(new SettingItem("Security", SecurityActivity.class));

        // Thiết lập Adapter
        adapter = new SettingsAdapter(this, settingList);
        recyclerView.setAdapter(adapter);
    }
}