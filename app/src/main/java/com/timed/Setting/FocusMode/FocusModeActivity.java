package com.timed.Setting.FocusMode;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;

import com.timed.R;
import com.timed.Setting.Main.GenericSettingAdapter;

public class FocusModeActivity extends AppCompatActivity {

    private RecyclerView rvPresets;
    private GenericSettingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.focus_mode);

        rvPresets = findViewById(R.id.rv_focus_presets);
        if (rvPresets != null) {
            rvPresets.setLayoutManager(new LinearLayoutManager(this));

            List<GenericSettingAdapter.SettingItemModel> presetList = new ArrayList<>();
            presetList.add(new GenericSettingAdapter.SettingItemModel("Pomodoro", "25 min focus", "preset_1"));
            presetList.add(new GenericSettingAdapter.SettingItemModel("Deep Work", "90 min focus", "preset_2"));
            presetList.add(new GenericSettingAdapter.SettingItemModel("Quick Break", "5 min rest", "preset_3"));

            adapter = new GenericSettingAdapter(presetList, item -> {
                // Handle preset selection
            });
            rvPresets.setAdapter(adapter);
        }
    }
}
