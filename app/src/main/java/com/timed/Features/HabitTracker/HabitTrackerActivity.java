package com.timed.Features.HabitTracker;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.Features.HabitTracker.adapters.HabitAdapter;
import com.timed.Features.HabitTracker.viewmodels.HabitTrackerViewModel;
import com.timed.R;
import com.timed.utils.SystemBarInsets;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Main activity for Habit Tracker feature.
 * Displays today's habits with completion tracking and streak counter.
 */
public class HabitTrackerActivity extends AppCompatActivity {

    private RecyclerView rvTodayHabits;
    private HabitAdapter adapter;
    private HabitTrackerViewModel viewModel;
    private Button btnNewHabit;
    private ImageView ivBack;
    private LinearLayout llStreakBadge;
    private TextView tvStreakCount;
    private TextView tvDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_tracker);
        SystemBarInsets.applyTo(this, findViewById(R.id.habitTrackerRoot));
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(HabitTrackerViewModel.class);
        
        // Initialize views
        initializeViews();
        
        // Setup observers
        setupObservers();
        
        // Setup listeners
        setupListeners();

        // Refresh the date shown in the header from the device clock
        updateCurrentDateDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentDateDisplay();
    }

    /**
     * Initializes all UI views.
     */
    private void initializeViews() {
        rvTodayHabits = findViewById(R.id.rvTodayHabits);
        btnNewHabit = findViewById(R.id.btn_new_habit);
        ivBack = findViewById(R.id.iv_back);
        llStreakBadge = findViewById(R.id.llStreakBadge);
        tvStreakCount = findViewById(R.id.tv_streak_count);
        tvDate = findViewById(R.id.tv_date);
        
        rvTodayHabits.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Updates the date label to the device's current date.
     */
    private void updateCurrentDateDisplay() {
        if (tvDate == null) {
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault());
        tvDate.setText(formatter.format(new Date()));
    }

    /**
     * Sets up LiveData observers.
     */
    private void setupObservers() {
        // Observe all habits and update adapter
        viewModel.getAllHabits().observe(this, habits -> {
            if (habits != null) {
                // Update streak badge: show only when there's at least one streak > 0
                int maxStreak = 0;
                for (com.timed.Features.HabitTracker.models.Habit h : habits) {
                    if (h.getCurrentStreak() > maxStreak) maxStreak = h.getCurrentStreak();
                }
                if (maxStreak > 0) {
                    llStreakBadge.setVisibility(View.VISIBLE);
                    tvStreakCount.setText(String.valueOf(maxStreak));
                } else {
                    llStreakBadge.setVisibility(View.GONE);
                }

                if (adapter == null) {
                    adapter = new HabitAdapter(this, habits);
                    rvTodayHabits.setAdapter(adapter);
                    setupHabitClickListeners();
                } else {
                    adapter.updateList(habits);
                }

                // Compute completed ids for today and pass to adapter
                long today = viewModel.getDateForIndex(0);
                java.util.HashSet<Integer> completedSet = new java.util.HashSet<>();
                for (com.timed.Features.HabitTracker.models.Habit h : habits) {
                    if (viewModel.isCompletedOnDate(h.getId(), today)) {
                        completedSet.add(h.getId());
                    }
                }
                adapter.setCompletedIds(completedSet);
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                // Show error message (can use Snackbar or Toast)
                showErrorMessage(error);
                viewModel.clearErrorMessage();
            }
        });
    }

    /**
     * Sets up click listeners for buttons.
     */
    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        btnNewHabit.setOnClickListener(v -> showCreateHabitDialog());
    }

    /**
     * Shows a simple dialog to create a new habit quickly.
     */
    private void showCreateHabitDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Tạo thói quen mới");

        // Simple vertical layout with two EditTexts
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText etName = new EditText(this);
        etName.setHint("Tên thói quen");
        etName.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(etName);

        final EditText etDesc = new EditText(this);
        etDesc.setHint("Mô tả (tuỳ chọn)");
        etDesc.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(etDesc);

        builder.setView(layout);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            if (name.isEmpty()) {
                android.widget.Toast.makeText(this, "Tên thói quen không được để trống", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // Create habit with defaults for icon and color
            com.timed.Features.HabitTracker.models.Habit habit = new com.timed.Features.HabitTracker.models.Habit(
                    name,
                    desc,
                    R.drawable.ic_habit_tracker, // default icon
                    "#3B82F6" // default color (blue)
            );

            viewModel.createHabit(habit, habitId -> {
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, "Đã tạo thói quen", android.widget.Toast.LENGTH_SHORT).show();
                });
            });
        });

        builder.setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Sets up habit item click listeners.
     */
    private void setupHabitClickListeners() {
        adapter.setOnHabitClickListener(new HabitAdapter.OnHabitClickListener() {
            @Override
            public void onCheckClick(int habitId, int position) {
                // Mark habit as completed
                viewModel.markHabitComplete(habitId, success -> {
                    if (success) {
                        // Show celebratory animation
                        showCheckAnimation();
                        // Ensure UI refresh for this item (streak/count may have changed)
                        if (adapter != null) {
                            runOnUiThread(() -> adapter.notifyItemChanged(position));
                        }
                    }
                });
            }

            @Override
            public void onHabitLongClick(int habitId, com.timed.Features.HabitTracker.models.Habit habit) {
                showDeleteHabitDialog(habitId, habit);
            }
        });
    }

    /**
     * Shows a confirmation dialog before deleting a habit.
     */
    private void showDeleteHabitDialog(int habitId, com.timed.Features.HabitTracker.models.Habit habit) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa thói quen")
                .setMessage("Xóa \"" + habit.getTitle() + "\"? Hành động này sẽ ẩn thói quen khỏi danh sách.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteHabit(habitId);
                    android.widget.Toast.makeText(this, "Đã xóa thói quen", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Shows celebratory animation when habit is completed.
     */
    private void showCheckAnimation() {
        // TODO: Implement celebratory micro-interaction
        // This can be a scale animation, particle effect, or haptic feedback
    }

    /**
     * Shows error message to user.
     */
    private void showErrorMessage(String message) {
        // TODO: Implement using Snackbar or Toast
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}
