package com.timed.Setting.SyncStorage;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.timed.R;

public class SyncStorageActivity extends AppCompatActivity {

    private RecyclerView rvSyncStorage;
    private SyncAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_storage);

        // Nút Back
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // Nút Xoá Cache (Clear Local Cache)
        LinearLayout btnClearCache = findViewById(R.id.btn_clear_cache);
        btnClearCache.setOnClickListener(v -> {
            Toast.makeText(this, "Local cache cleared successfully!", Toast.LENGTH_SHORT).show();
        });

        rvSyncStorage = findViewById(R.id.rv_sync_storage);
        if (rvSyncStorage != null) {
            rvSyncStorage.setLayoutManager(new LinearLayoutManager(this));

            // Dữ liệu giống bản thiết kế
            List<SyncOption> syncList = new ArrayList<>();
            syncList.add(new SyncOption("Auto-Sync", SyncOption.TYPE_SWITCH, true, null));
            syncList.add(new SyncOption("Sync over Wi-Fi only", SyncOption.TYPE_SWITCH, false, null));
            syncList.add(new SyncOption("Last Synced", SyncOption.TYPE_VALUE, false, "Today, 10:42 AM"));

            adapter = new SyncAdapter(syncList, (option, isChecked) -> {
                String status = isChecked ? "On" : "Off";
                Toast.makeText(this, option.getTitle() + " - " + status, Toast.LENGTH_SHORT).show();
            });
            rvSyncStorage.setAdapter(adapter);
        }
    }
}