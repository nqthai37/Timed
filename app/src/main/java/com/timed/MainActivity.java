package com.timed;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Chỉ import đúng cái Repository vừa tạo và Adapter
import com.timed.Setting.Data.SettingsRepository;
import com.timed.Setting.Main.SettingItem;
import com.timed.Setting.Main.SettingsAdapter;
import com.timed.demo.NotificationDemo;
import com.timed.managers.UserManager;
public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<SettingItem> settingList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Test notification ngay lập tức
        findViewById(R.id.btn_test_notification).setOnClickListener(v -> {
            NotificationDemo.showSampleNotification(MainActivity.this);
        });

        // Test reminder (5 phút sau)
        findViewById(R.id.btn_test_reminder).setOnClickListener(v -> {
            String userId = UserManager.getInstance().getCurrentUserId();
            if (userId != null) {
                NotificationDemo.createSampleReminder(MainActivity.this, userId);
            }
        });

        // Test urgent notification
        findViewById(R.id.btn_test_urgent).setOnClickListener(v -> {
            NotificationDemo.showSampleUrgentNotification(MainActivity.this);
        });

        // Initialize RecyclerView for Settings
        recyclerView = findViewById(R.id.recyclerViewSettings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get settings data from repository
        settingList = SettingsRepository.getSettingsData();

        // Create and set adapter
        adapter = new SettingsAdapter(this, settingList);
        recyclerView.setAdapter(adapter);
    }
}