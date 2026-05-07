package com.timed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.timed.R;
import com.timed.adapters.TaskAdapter;
import com.timed.managers.TasksManager;
import com.timed.models.Task;

import java.util.ArrayList;
import java.util.List;

public class TasksListActivity extends AppCompatActivity {

    private static final String TAG = "TasksListActivity";
    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private TasksManager tasksManager;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_list);

        firebaseAuth = FirebaseAuth.getInstance();
        tasksManager = TasksManager.getInstance(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        loadTasks();
    }

    private void initViews() {
        rvTasks = findViewById(R.id.rvTasks);
        View btnBack = findViewById(R.id.btnBackTasks);
        View btnAddTask = findViewById(R.id.btnAddTask);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnAddTask != null) {
            btnAddTask.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateTaskActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                // Open task editing
                Intent intent = new Intent(TasksListActivity.this, CreateTaskActivity.class);
                // You can pass task details if needed
                startActivity(intent);
            }

            @Override
            public void onTaskCheckChanged(Task task, boolean isChecked) {
                if (isChecked) {
                    // Mark as done
                    tasksManager.markTaskAsDone(task.getId())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(TasksListActivity.this, "Task completed!", Toast.LENGTH_SHORT).show();
                                loadTasks(); // Refresh list
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(TasksListActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupListeners() {
        // Additional listeners can be added here
    }

    private void loadTasks() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        tasksManager.getPendingTasks(userId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    for (int i = 0; i < queryDocumentSnapshots.getDocuments().size(); i++) {
                        Task task = queryDocumentSnapshots.getDocuments().get(i).toObject(Task.class);
                        if (task != null) {
                            task.setId(queryDocumentSnapshots.getDocuments().get(i).getId());
                            taskList.add(task);
                        }
                    }

                    taskAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded " + taskList.size() + " pending tasks");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading tasks: " + e.getMessage(), e);
                    Toast.makeText(this, "Error loading tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks(); // Reload tasks when returning to this activity
    }
}
