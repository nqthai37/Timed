package com.timed.Setting.FocusMode;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.timed.R;

public class FocusModeActivity extends AppCompatActivity {

    private RecyclerView rvPresets;
    private FocusPresetAdapter adapter;
    private List<FocusPreset> presetList;
    private TextView tvMainTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_mode);

        tvMainTimer = findViewById(R.id.tv_main_timer);
        rvPresets = findViewById(R.id.rv_focus_presets);
        
        // Nút Back
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        if (rvPresets != null) {
            rvPresets.setLayoutManager(new LinearLayoutManager(this));

            // Tạo dữ liệu cho các Preset (Nhớ thay bằng ID icon thật của bạn)
            presetList = new ArrayList<>();
            presetList.add(new FocusPreset("Pomodoro", "25 min focus, 5 min break", R.drawable.ic_clock ));
            presetList.add(new FocusPreset("Deep Work", "90 min focus, 15 min break", R.drawable.ic_focus));
            presetList.add(new FocusPreset("Quick Break", "5 min rest", R.drawable.ic_theme ));

            // Setup Adapter và bắt sự kiện khi click vào 1 preset
            adapter = new FocusPresetAdapter(presetList, preset -> {
                // Ví dụ: Đổi text của đồng hồ chính khi bấm vào Preset
                if (preset.getTitle().equals("Pomodoro")) {
                    tvMainTimer.setText("25:00");
                } else if (preset.getTitle().equals("Deep Work")) {
                    tvMainTimer.setText("90:00");
                } else if (preset.getTitle().equals("Quick Break")) {
                    tvMainTimer.setText("05:00");
                }
                Toast.makeText(this, "Selected: " + preset.getTitle(), Toast.LENGTH_SHORT).show();
            });
            rvPresets.setAdapter(adapter);
        }
    }
}