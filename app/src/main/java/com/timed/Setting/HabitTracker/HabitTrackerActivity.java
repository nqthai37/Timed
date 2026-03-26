package com.timed.Setting.HabitTracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;

import com.timed.R;
import com.timed.Setting.Main.GenericSettingAdapter;

public class HabitTrackerActivity extends AppCompatActivity {

    private RecyclerView rvHabits;
    private GenericSettingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.habit_tracker);

        rvHabits = findViewById(R.id.rv_habits);
        if (rvHabits != null) {
            rvHabits.setLayoutManager(new LinearLayoutManager(this));

            List<GenericSettingAdapter.SettingItemModel> habitList = new ArrayList<>();
            habitList.add(new GenericSettingAdapter.SettingItemModel("Morning Workout", "7 day streak", "habit_1"));
            habitList.add(new GenericSettingAdapter.SettingItemModel("Reading", "15 day streak", "habit_2"));
            habitList.add(new GenericSettingAdapter.SettingItemModel("Meditation", "3 day streak", "habit_3"));

            adapter = new GenericSettingAdapter(habitList, item -> {
                // Handle habit selection
            });
            rvHabits.setAdapter(adapter);
        }
    }
}
