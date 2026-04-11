package com.timed.Setting.FreeSlotFinder;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.timed.R;
import com.timed.Setting.Main.GenericSettingAdapter;

import java.util.ArrayList;
import java.util.List;

public class FreeSlotFinderActivity extends AppCompatActivity {

    private RecyclerView rvSlots;
    private GenericSettingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_slot_finder);
        // Nút quay lại
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        rvSlots = findViewById(R.id.rv_free_slots);
        if (rvSlots != null) {
            rvSlots.setLayoutManager(new LinearLayoutManager(this));

            List<GenericSettingAdapter.SettingItemModel> slotList = new ArrayList<>();
            slotList.add(new GenericSettingAdapter.SettingItemModel("Today", "2:00 PM - 3:30 PM", "slot_1"));
            slotList.add(new GenericSettingAdapter.SettingItemModel("Tomorrow", "10:00 AM - 11:00 AM", "slot_2"));
            slotList.add(new GenericSettingAdapter.SettingItemModel("Next Friday", "4:00 PM - 5:00 PM", "slot_3"));

            adapter = new GenericSettingAdapter(slotList, item -> {
                // Handle slot selection
            });
            rvSlots.setAdapter(adapter);
        }
    }
}
