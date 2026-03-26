package com.timed.Setting.SyncStorage;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.timed.R;
import com.timed.Setting.Main.GenericSettingAdapter;

import java.util.ArrayList;
import java.util.List;

public class SyncStorageActivity extends AppCompatActivity {

    private RecyclerView rvSyncStorage;
    private GenericSettingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_storage);

        rvSyncStorage = findViewById(R.id.rv_sync_storage);
        if (rvSyncStorage != null) {
            rvSyncStorage.setLayoutManager(new LinearLayoutManager(this));

            List<GenericSettingAdapter.SettingItemModel> storageList = new ArrayList<>();
            storageList.add(new GenericSettingAdapter.SettingItemModel("Cloud Sync", "Auto-sync enabled", "storage_1"));
            storageList.add(new GenericSettingAdapter.SettingItemModel("Storage", "1.2 GB / 5 GB used", "storage_2"));
            storageList.add(new GenericSettingAdapter.SettingItemModel("Backup", "Last backup 2 hours ago", "storage_3"));

            adapter = new GenericSettingAdapter(storageList, item -> {
                // Handle storage option selection
            });
            rvSyncStorage.setAdapter(adapter);
        }
    }
}
