package com.timed.Setting.AI;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;

import com.timed.R;
import com.timed.Setting.Main.GenericSettingAdapter;

public class AiSchedulingActivity extends AppCompatActivity {

    private RecyclerView rvSchedules;
    private GenericSettingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_scheduling);

        rvSchedules = findViewById(R.id.rv_ai_schedules);
        if (rvSchedules != null) {
            rvSchedules.setLayoutManager(new LinearLayoutManager(this));

            List<GenericSettingAdapter.SettingItemModel> scheduleList = new ArrayList<>();
            scheduleList.add(new GenericSettingAdapter.SettingItemModel("Optimize Daily Task", "AI-powered scheduling", "sched_1"));
            scheduleList.add(new GenericSettingAdapter.SettingItemModel("Smart Reminder", "Get notified at best time", "sched_2"));

            adapter = new GenericSettingAdapter(scheduleList, item -> {
                // Handle schedule selection
            });
            rvSchedules.setAdapter(adapter);
        }
    }
}
