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

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<SettingItem> settingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        recyclerView = findViewById(R.id.rv_settings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // CHỈ CẦN GỌI 1 DÒNG NÀY LÀ CÓ TOÀN BỘ DỮ LIỆU!
        settingList = SettingsRepository.getSettingsData();

        adapter = new SettingsAdapter(this, settingList);
        recyclerView.setAdapter(adapter);
    }
}