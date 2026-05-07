package com.timed.activities;

import android.os.Bundle;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.Features.AI.AiSchedulingActivity;
import com.timed.Features.Analytics.AnalyticsActivity;
import com.timed.Features.ConflictResolver.ConflictResolverActivity;
import com.timed.Features.FocusMode.FocusModeActivity;
import com.timed.Features.FreeSlotFinder.FreeSlotFinderActivity;
import com.timed.Features.HabitTracker.HabitTrackerActivity;
import com.timed.Features.FeatureAdapter;
import com.timed.models.Feature;
import com.timed.R;

import java.util.ArrayList;
import java.util.List;

public class FeaturesActivity extends BaseBottomNavActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_features);
        setupInsets();
        setupRecyclerView();
        setupBottomNavigation();
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.nav_features;
    }

    private void setupInsets() {
        View root = findViewById(R.id.featuresRoot);
        if (root == null) {
            return;
        }
        final int baseTop = root.getPaddingTop();
        final int baseBottom = root.getPaddingBottom();
        final int baseLeft = root.getPaddingLeft();
        final int baseRight = root.getPaddingRight();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseLeft + bars.left, baseTop + bars.top,
                    baseRight + bars.right, baseBottom + bars.bottom);
            return insets;
        });
    }

    private void setupRecyclerView() {
        RecyclerView rvFeatures = findViewById(R.id.rv_features);
        if (rvFeatures != null) {
            rvFeatures.setLayoutManager(new LinearLayoutManager(this));
            rvFeatures.setAdapter(new FeatureAdapter(this, getFeatures()));
        }
    }

    private List<Feature> getFeatures() {
        List<Feature> features = new ArrayList<>();

        // NHÓM 1: Tối ưu hóa (Hiện tiêu đề ở item đầu tiên)
        features.add(new Feature("AI Scheduling", "...", R.drawable.ic_ai_schedule, AiSchedulingActivity.class,
                "Optimizing Schedule"));
        features.add(new Feature("Conflict Resolver", "...", R.drawable.ic_conflict_resolver,
                ConflictResolverActivity.class, null));
        features.add(new Feature("Free Slot Finder", "...", R.drawable.ic_free_slot_finder,
                FreeSlotFinderActivity.class, null));

        // NHÓM 2: Năng suất (Hiện tiêu đề ở item đầu tiên của nhóm này)
        features.add(new Feature("Focus Mode", "...", R.drawable.ic_focus, FocusModeActivity.class, null));
        features.add(
                new Feature("Habit Tracker", "...", R.drawable.ic_habit_tracker, HabitTrackerActivity.class, null));

        return features;
    }

}
