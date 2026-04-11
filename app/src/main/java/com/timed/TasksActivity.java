package com.timed;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    private LinearLayout tasksContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_tasks);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNav), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        tasksContainer = findViewById(R.id.tasksContainer);

        setupBottomNavigation();
        populateDummyTasks();

        findViewById(R.id.fabAddTask).setOnClickListener(v -> {
            // Logic to add task
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_tasks);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_schedule) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_tasks) {
                return true;
            }
            return false;
        });
    }

    private void populateDummyTasks() {
        // Today
        addTaskGroup("TODAY", "24", true);
        addTaskItem("Review Q3 Marketing Strategy", "09:00 AM", "WORK", true);
        addTaskItem("Finalize design handoff for Flow project", "11:30 AM", "WORK", false);
        addTaskItem("Weekly grocery run", "05:00 PM", "PERSONAL", false);

        // Tomorrow
        addTaskGroup("TOMORROW", "25", false);
        addTaskItem("Client feedback session", "10:00 AM", "WORK", false);
        addTaskItem("Renew gym membership", "02:00 PM", "PERSONAL", false);

        // Wed
        addTaskGroup("WED", "26", false);
        addTaskItem("Project brainstorming", "09:00 AM", "WORK", false);
    }

    private void addTaskGroup(String label, String day, boolean isPrimary) {
        View groupView = LayoutInflater.from(this).inflate(R.layout.item_task_group_header, tasksContainer, false);
        TextView tvLabel = groupView.findViewById(R.id.tvGroupLabel);
        TextView tvDay = groupView.findViewById(R.id.tvGroupDay);
        
        tvLabel.setText(label);
        tvDay.setText(day);
        
        if (!isPrimary) {
            tvLabel.setTextColor(Color.parseColor("#94a3b8"));
        }
        
        tasksContainer.addView(groupView);
    }

    private void addTaskItem(String title, String time, String category, boolean isCompleted) {
        View taskView = LayoutInflater.from(this).inflate(R.layout.item_task, tasksContainer, false);
        
        TextView tvTitle = taskView.findViewById(R.id.tvTaskTitle);
        TextView tvTime = taskView.findViewById(R.id.tvTaskTime);
        TextView tvCategory = taskView.findViewById(R.id.tvTaskCategory);
        View checkbox = taskView.findViewById(R.id.viewCheckbox);
        ImageView ivCheck = taskView.findViewById(R.id.ivCheck);
        
        tvTitle.setText(title);
        tvTime.setText(time);
        tvCategory.setText(category);
        
        if (isCompleted) {
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setAlpha(0.5f);
            tvTime.setAlpha(0.6f);
            checkbox.setBackgroundResource(R.drawable.bg_checkbox_checked);
            ivCheck.setVisibility(View.VISIBLE);
        } else {
            checkbox.setBackgroundResource(R.drawable.bg_checkbox_unchecked);
            ivCheck.setVisibility(View.GONE);
        }

        if ("PERSONAL".equals(category)) {
            tvCategory.setBackgroundResource(R.drawable.bg_tag_personal);
            tvCategory.setTextColor(Color.parseColor("#4f2e88"));
        }
        
        tasksContainer.addView(taskView);
    }
}