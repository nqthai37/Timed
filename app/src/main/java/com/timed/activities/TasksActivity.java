package com.timed.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.timed.R;
import com.timed.managers.TasksManager;
import com.timed.models.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TasksActivity extends BaseBottomNavActivity {

    private LinearLayout tasksContainer;
    private TasksManager tasksManager;
    private FirebaseAuth firebaseAuth;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_tasks);

        tasksManager = TasksManager.getInstance(this);
        firebaseAuth = FirebaseAuth.getInstance();
        tasksContainer = findViewById(R.id.tasksContainer);

        setupInsets();
        setupTopBar();
        setupBottomNavigation();
        setupActions();
        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void setupInsets() {
        View root = findViewById(R.id.tasksRoot);
        if (root == null) {
            return;
        }
        final int baseTopPadding = root.getPaddingTop();
        final int baseBottomPadding = root.getPaddingBottom();
        final int baseLeftPadding = root.getPaddingLeft();
        final int baseRightPadding = root.getPaddingRight();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            root.setPadding(baseLeftPadding + bars.left, baseTopPadding + bars.top,
                    baseRightPadding + bars.right, baseBottomPadding);
            return insets;
        });
    }

    private void setupTopBar() {
        TextView title = findViewById(R.id.tvTopTitle);
        if (title != null) {
            title.setText("");
        }

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> openSchedule());
        }

        ImageButton btnSearch = findViewById(R.id.btnSearch);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));
        }
    }

    private void setupActions() {
        View fabAddTask = findViewById(R.id.fabAddTask);
        if (fabAddTask != null) {
            fabAddTask.setOnClickListener(v -> startActivity(new Intent(this, CreateTaskActivity.class)));
        }
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.nav_tasks;
    }

    private void openSchedule() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void loadTasks() {
        if (tasksContainer == null) {
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            renderEmptyState("Sign in to view tasks");
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        tasksManager.getAllTasks(userId)
                .addOnSuccessListener(snapshot -> {
                    List<Task> tasks = new ArrayList<>();
                    snapshot.getDocuments().forEach(document -> {
                        Task task = document.toObject(Task.class);
                        if (task != null) {
                            task.setId(document.getId());
                            tasks.add(task);
                        }
                    });
                    renderTasks(tasks);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    renderEmptyState("Unable to load tasks");
                });
    }

    private void renderTasks(List<Task> tasks) {
        tasksContainer.removeAllViews();
        if (tasks == null || tasks.isEmpty()) {
            renderEmptyState("No tasks yet");
            return;
        }

        Collections.sort(tasks, (a, b) -> {
            Date aDate = a.getDue_date() != null ? a.getDue_date().toDate() : null;
            Date bDate = b.getDue_date() != null ? b.getDue_date().toDate() : null;
            if (aDate == null && bDate == null) {
                return 0;
            }
            if (aDate == null) {
                return 1;
            }
            if (bDate == null) {
                return -1;
            }
            return aDate.compareTo(bDate);
        });

        Map<String, List<Task>> grouped = new LinkedHashMap<>();
        for (Task task : tasks) {
            String groupKey = buildGroupKey(task);
            if (!grouped.containsKey(groupKey)) {
                grouped.put(groupKey, new ArrayList<>());
            }
            grouped.get(groupKey).add(task);
        }

        boolean firstGroup = true;
        for (Map.Entry<String, List<Task>> entry : grouped.entrySet()) {
            Task firstTask = entry.getValue().get(0);
            addTaskGroup(entry.getKey(), buildGroupDay(firstTask), firstGroup);
            for (Task task : entry.getValue()) {
                addTaskItem(task);
            }
            firstGroup = false;
        }
    }

    private void renderEmptyState(String message) {
        tasksContainer.removeAllViews();
        TextView emptyView = new TextView(this);
        emptyView.setText(message);
        emptyView.setTextColor(Color.parseColor("#64748b"));
        emptyView.setTextSize(14f);
        emptyView.setPadding(0, dpToPx(32), 0, 0);
        tasksContainer.addView(emptyView);
    }

    private void addTaskGroup(String label, String day, boolean isPrimary) {
        View groupView = LayoutInflater.from(this)
                .inflate(R.layout.item_task_group_header, tasksContainer, false);
        TextView tvLabel = groupView.findViewById(R.id.tvGroupLabel);
        TextView tvDay = groupView.findViewById(R.id.tvGroupDay);

        tvLabel.setText(label);
        tvDay.setText(day);
        if (!isPrimary) {
            tvLabel.setTextColor(Color.parseColor("#94a3b8"));
        }
        tasksContainer.addView(groupView);
    }

    private void addTaskItem(Task task) {
        View taskView = LayoutInflater.from(this).inflate(R.layout.item_task_timeline, tasksContainer, false);

        TextView tvTitle = taskView.findViewById(R.id.tvTaskTitle);
        TextView tvTime = taskView.findViewById(R.id.tvTaskTime);
        TextView tvCategory = taskView.findViewById(R.id.tvTaskCategory);
        View checkbox = taskView.findViewById(R.id.viewCheckbox);
        ImageView ivCheck = taskView.findViewById(R.id.ivCheck);

        tvTitle.setText(task.getTitle() != null ? task.getTitle() : "Untitled task");
        tvTime.setText(task.getDue_date() != null ? timeFormat.format(task.getDue_date().toDate()) : "No time");

        String tag = task.getPriority() != null && !task.getPriority().trim().isEmpty()
                ? task.getPriority().trim().toUpperCase(Locale.getDefault())
                : "TASK";
        tvCategory.setText(tag);

        if (task.isIs_completed()) {
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setAlpha(0.5f);
            tvTime.setAlpha(0.6f);
            checkbox.setBackgroundResource(R.drawable.bg_checkbox_checked);
            ivCheck.setVisibility(View.VISIBLE);
        } else {
            checkbox.setBackgroundResource(R.drawable.bg_checkbox_unchecked);
            ivCheck.setVisibility(View.GONE);
        }

        if ("PERSONAL".equals(tag)) {
            tvCategory.setBackgroundResource(R.drawable.bg_tag_personal);
            tvCategory.setTextColor(Color.parseColor("#4f2e88"));
        } else {
            tvCategory.setBackgroundResource(R.drawable.bg_tag_work);
            tvCategory.setTextColor(getColor(R.color.primary));
        }

        taskView.setOnClickListener(v -> startActivity(new Intent(this, CreateTaskActivity.class)));
        checkbox.setOnClickListener(v -> markDone(task));
        tasksContainer.addView(taskView);
    }

    private void markDone(Task task) {
        if (task == null || task.getId() == null || task.isIs_completed()) {
            return;
        }
        tasksManager.markTaskAsDone(task.getId())
                .addOnSuccessListener(unused -> loadTasks())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String buildGroupKey(Task task) {
        if (task.getDue_date() == null) {
            return "NO DATE";
        }
        Date dueDate = task.getDue_date().toDate();
        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar due = java.util.Calendar.getInstance();
        due.setTime(dueDate);

        if (isSameDay(today, due)) {
            return "TODAY";
        }
        today.add(java.util.Calendar.DAY_OF_YEAR, 1);
        if (isSameDay(today, due)) {
            return "TOMORROW";
        }
        return new SimpleDateFormat("EEE", Locale.getDefault()).format(dueDate).toUpperCase(Locale.getDefault());
    }

    private String buildGroupDay(Task task) {
        if (task.getDue_date() == null) {
            return "--";
        }
        return new SimpleDateFormat("dd", Locale.getDefault()).format(task.getDue_date().toDate());
    }

    private boolean isSameDay(java.util.Calendar first, java.util.Calendar second) {
        return first.get(java.util.Calendar.YEAR) == second.get(java.util.Calendar.YEAR)
                && first.get(java.util.Calendar.DAY_OF_YEAR) == second.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
