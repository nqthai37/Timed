package com.timed.Features.ConflictResolver;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// Import đúng Model đã định nghĩa bên trong Adapter
import com.timed.R;
import com.timed.Features.ConflictResolver.ConflictEventAdapter;
import com.timed.Setting.SyncStorage.SyncStorageActivity;
import com.timed.repositories.EventsRepository;

public class ConflictResolverActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ConflictEventAdapter adapter; 
    private List<ConflictEvent> conflictEventList;

    private EventsRepository eventsRepository;

    private long newEventStartMillis;
    private long newEventEndMillis;
    private String newEventTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conflict_resolver);

        eventsRepository = new EventsRepository();

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rvConflictEvent);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        conflictEventList = new ArrayList<>();
        adapter = new ConflictEventAdapter(this, conflictEventList);
        recyclerView.setAdapter(adapter);

        newEventStartMillis = getIntent().getLongExtra("NEW_EVENT_START", -1);
        newEventEndMillis = getIntent().getLongExtra("NEW_EVENT_END", -1);
        newEventTitle = getIntent().getStringExtra("NEW_EVENT_TITLE");

        if (newEventStartMillis == -1) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 10);
            cal.set(Calendar.MINUTE, 0);
            newEventStartMillis = cal.getTimeInMillis(); // 10:00 AM Today

            cal.set(Calendar.HOUR_OF_DAY, 11);
            newEventEndMillis = cal.getTimeInMillis(); // 11:00 AM Today
            newEventTitle = "Dev Test Event";
        }

        detectConflictsOnDay(newEventStartMillis, newEventEndMillis);
    }

    private void detectConflictsOnDay(long newStart, long newEnd) {
        eventsRepository.checkConflictsOnDay(newStart, newEnd, newEventTitle, new EventsRepository.OnConflictCheckListener() {
            @Override
            public void onConflictsFound(List<ConflictEvent> conflicts) {
                conflictEventList.clear();
                conflictEventList.addAll(conflicts);
                adapter.notifyDataSetChanged();

                if (conflictEventList.isEmpty()) {
                    Toast.makeText(ConflictResolverActivity.this, "No conflicts! Safe to save.", Toast.LENGTH_SHORT).show();
                    // In production: Auto-save event here and finish();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("ConflictResolver", "Error fetching events", e);
                Toast.makeText(ConflictResolverActivity.this, "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }
}