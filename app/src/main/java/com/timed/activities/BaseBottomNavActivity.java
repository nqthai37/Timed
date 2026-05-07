package com.timed.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;
import com.timed.R;
import com.timed.utils.ThemeManager;

public abstract class BaseBottomNavActivity extends AppCompatActivity {

    private String lastAppliedAppearance;
    private String lastAppliedPalette;

    @IdRes
    protected abstract int getNavigationMenuItemId();

    protected void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) {
            return;
        }

        applyBottomNavigationStyle(bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == getNavigationMenuItemId()) {
                return true;
            }

            Class<? extends Activity> targetActivity = getTargetActivity(itemId);
            if (targetActivity == null) {
                return false;
            }

            Intent intent = new Intent(this, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        });
        bottomNav.setSelectedItemId(getNavigationMenuItemId());
        if (bottomNav.getMenu().findItem(getNavigationMenuItemId()) != null) {
            bottomNav.getMenu().findItem(getNavigationMenuItemId()).setChecked(true);
        }
    }

    private void applyBottomNavigationStyle(BottomNavigationView bottomNav) {
        int primary = MaterialColors.getColor(bottomNav, androidx.appcompat.R.attr.colorPrimary);
        int onSurfaceVariant = MaterialColors.getColor(bottomNav,
                com.google.android.material.R.attr.colorOnSurfaceVariant);
        int surface = MaterialColors.getColor(bottomNav, com.google.android.material.R.attr.colorSurface);
        int transparent = android.graphics.Color.TRANSPARENT;
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
        };

        bottomNav.setBackgroundColor(surface);
        bottomNav.setItemIconTintList(new ColorStateList(states, new int[] { primary, onSurfaceVariant }));
        bottomNav.setItemTextColor(new ColorStateList(states, new int[] { primary, onSurfaceVariant }));
        bottomNav.setItemActiveIndicatorColor(new ColorStateList(states, new int[] { transparent, transparent }));
        bottomNav.setLabelVisibilityMode(com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED);
        applyBottomNavigationInsets(bottomNav);
    }

    private void applyBottomNavigationInsets(View bottomNav) {
        final int baseLeft = bottomNav.getPaddingLeft();
        final int baseTop = bottomNav.getPaddingTop();
        final int baseRight = bottomNav.getPaddingRight();
        final int baseBottom = bottomNav.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseLeft + bars.left, baseTop, baseRight + bars.right, baseBottom + bars.bottom);
            return insets;
        });
    }

    protected void anchorFabAboveBottomNavigation(@IdRes int... fabIds) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null || fabIds == null) {
            return;
        }

        for (int fabId : fabIds) {
            View fab = findViewById(fabId);
            if (fab != null) {
                anchorFabAboveBottomNavigation(bottomNav, fab);
            }
        }
    }

    private void anchorFabAboveBottomNavigation(BottomNavigationView bottomNav, View fab) {
        ViewGroup.LayoutParams layoutParams = fab.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
        final int baseLeft = marginParams.leftMargin;
        final int baseTop = marginParams.topMargin;
        final int baseRight = marginParams.rightMargin;
        final int spacing = bottomNavDpToPx(16);

        Runnable updateMargin = () -> {
            ViewGroup.LayoutParams currentParams = fab.getLayoutParams();
            if (!(currentParams instanceof ViewGroup.MarginLayoutParams)) {
                return;
            }
            ViewGroup.MarginLayoutParams updated = (ViewGroup.MarginLayoutParams) currentParams;
            updated.leftMargin = baseLeft;
            updated.topMargin = baseTop;
            updated.rightMargin = baseRight;
            updated.bottomMargin = bottomNav.getHeight() + spacing;
            fab.setLayoutParams(updated);
        };

        bottomNav.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                updateMargin.run());
        bottomNav.post(updateMargin);
    }

    private int bottomNavDpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private Class<? extends Activity> getTargetActivity(@IdRes int itemId) {
        if (itemId == R.id.nav_schedule) {
            return MainActivity.class;
        }
        if (itemId == R.id.nav_tasks) {
            return TasksActivity.class;
        }
        if (itemId == R.id.nav_features) {
            return FeaturesActivity.class;
        }
        if (itemId == R.id.nav_settings) {
            return SettingsActivity.class;
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeRefreshTheme();
    }

    private void maybeRefreshTheme() {
        String appearance = ThemeManager.getAppearance(this);
        String palette = ThemeManager.getPalette(this);
        if (lastAppliedAppearance == null || lastAppliedPalette == null) {
            lastAppliedAppearance = appearance;
            lastAppliedPalette = palette;
            return;
        }
        if (!appearance.equals(lastAppliedAppearance) || !palette.equals(lastAppliedPalette)) {
            lastAppliedAppearance = appearance;
            lastAppliedPalette = palette;
            if (!isFinishing() && !isDestroyed()) {
                ThemeManager.applyTheme(this);
                recreate();
            }
        }
    }
}
