package com.timed.activities;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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

        bottomNav.setSelectedItemId(getNavigationMenuItemId());
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
