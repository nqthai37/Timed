package com.timed.Setting.Timezone;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.timed.R;
import com.timed.Setting.Main.GenericSettingAdapter;

import java.util.ArrayList;
import java.util.List;

public class TimezoneSettingActivity extends AppCompatActivity {

    private RecyclerView rvTimezones;
    private GenericSettingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timezone);
        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        rvTimezones = findViewById(R.id.rv_timezone_settings);
        if (rvTimezones != null) {
            rvTimezones.setLayoutManager(new LinearLayoutManager(this));

            List<GenericSettingAdapter.SettingItemModel> tzList = new ArrayList<>();
            tzList.add(new GenericSettingAdapter.SettingItemModel("UTC", "Coordinated Universal Time", "tz_utc"));

            adapter = new GenericSettingAdapter(tzList, item -> {
                // Handle timezone selection
            });
            rvTimezones.setAdapter(adapter);
        }
    }
}
