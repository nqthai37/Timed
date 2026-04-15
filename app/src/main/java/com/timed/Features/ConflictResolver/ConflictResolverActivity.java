package com.timed.Features.ConflictResolver;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

// Import đúng Model đã định nghĩa bên trong Adapter
import com.timed.R;
import com.timed.Features.ConflictResolver.ConflictEventAdapter;
import com.timed.Setting.SyncStorage.SyncStorageActivity;

public class ConflictResolverActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    // Sửa tên class Adapter cho khớp với file ConflictEventAdapter.java
    private ConflictEventAdapter adapter; 
    private List<ConflictEventAdapter.ConflictEvent> conflictEventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conflict_resolver);
        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // Ánh xạ RecyclerView
        recyclerView = findViewById(R.id.rvConflictEvent);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo dữ liệu giả lập (Sửa Constructor cho khớp Model)
        conflictEventList = new ArrayList<>();
        // Định dạng Constructor: (Title, TimeRange, Description, ID, isConflicting, TargetActivity)
        conflictEventList.add(new ConflictEventAdapter.ConflictEvent(
                "Project Sync", "10:00 AM - 11:30 PM", "Team standup", 
                "evt1", true, SyncStorageActivity.class));
        
        conflictEventList.add(new ConflictEventAdapter.ConflictEvent(
                "Client Meeting", "02:00 PM - 03:30 PM", "Development sync", 
                "evt2", true, SyncStorageActivity.class));
        
        conflictEventList.add(new ConflictEventAdapter.ConflictEvent(
                "Design Review", "04:30 PM - 05:00 PM", "Marketing update", 
                "evt3", true, SyncStorageActivity.class));

        // Thiết lập Adapter (Sửa tên class Adapter)
        adapter = new ConflictEventAdapter(this, conflictEventList);
        recyclerView.setAdapter(adapter);
    }
}