package com.timed.Setting.HabitTracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import com.timed.R;

public class HabitTrackerActivity extends AppCompatActivity {

    private RecyclerView rvHabits;
    private HabitAdapter adapter;
    private List<Habit> habitList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_tracker);

        // Ánh xạ RecyclerView
        rvHabits = findViewById(R.id.rvTodayHabits);
        
        if (rvHabits != null) {
            rvHabits.setLayoutManager(new LinearLayoutManager(this));

            // Khởi tạo dữ liệu mẫu giống y như trong ảnh
            habitList = new ArrayList<>();
            // (Lưu ý: Bạn hãy truyền đúng ID ảnh của bạn vào thay cho R.drawable.ic_...)
            habitList.add(new Habit("Drink 8 glasses of water", "7/8 glasses", R.drawable.ic_focus, false));
            habitList.add(new Habit("Read for 30 minutes", "Complete", R.drawable.ic_analytics, true));
            habitList.add(new Habit("Exercise", "No progress yet", R.drawable.ic_security, false));

            // Cài đặt Adapter
            adapter = new HabitAdapter(habitList);
            rvHabits.setAdapter(adapter);
        }
    }
}