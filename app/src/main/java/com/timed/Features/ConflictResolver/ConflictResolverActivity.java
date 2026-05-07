package com.timed.Features.ConflictResolver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    private ActivityResultLauncher<Intent> freeSlotLauncher;

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

        freeSlotLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        setResult(RESULT_OK, result.getData());
                        finish();
                    }
                }
        );

        newEventStartMillis = getIntent().getLongExtra("NEW_EVENT_START", -1);
        newEventEndMillis = getIntent().getLongExtra("NEW_EVENT_END", -1);
        newEventTitle = getIntent().getStringExtra("NEW_EVENT_TITLE");

        if (newEventStartMillis == -1 || newEventEndMillis == -1) {
            Toast.makeText(this, "Error loading event details.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        detectConflictsOnDay(newEventStartMillis, newEventEndMillis);
        setupClickListeners();
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_ignore_conflict).setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        findViewById(R.id.btn_edit_manually).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        findViewById(R.id.btn_auto_reschedule).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.timed.Features.FreeSlotFinder.FreeSlotFinderActivity.class);
            intent.putExtra("MODE", "RESOLVER");
            intent.putExtra("EVENT_DURATION", newEventEndMillis - newEventStartMillis);
            intent.putExtra("ORIGINAL_START", newEventStartMillis);
            intent.putExtra("ORIGINAL_END", newEventEndMillis);
            freeSlotLauncher.launch(intent);
        });
    }

    private void detectConflictsOnDay(long newStart, long newEnd) {
        eventsRepository.checkConflictsOnDay(newStart, newEnd, newEventTitle, new EventsRepository.OnConflictCheckListener() {
            @Override
            public void onConflictsFound(List<ConflictEvent> conflicts) {
                conflictEventList.clear();
                conflictEventList.addAll(conflicts);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Log.e("ConflictResolver", "Error fetching events", e);
                Toast.makeText(ConflictResolverActivity.this, "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }
}