package com.timed.managers;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.IdRes;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.timed.R;
import com.timed.activities.FeaturesActivity;
import com.timed.activities.SettingsActivity;

public final class MainBottomNavController {
    private MainBottomNavController() {
    }

    public static void setup(Activity activity, BottomNavigationView bottomNav) {
        if (activity == null || bottomNav == null) {
            return;
        }

        bottomNav.setSelectedItemId(R.id.nav_schedule);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_schedule) {
                return true;
            }

            Class<? extends Activity> target = resolveTarget(itemId);
            if (target == null) {
                return false;
            }

            Intent intent = new Intent(activity, target);
            activity.startActivity(intent);
            return true;
        });
    }

    private static Class<? extends Activity> resolveTarget(@IdRes int itemId) {
        if (itemId == R.id.nav_features) {
            return FeaturesActivity.class;
        }
        if (itemId == R.id.nav_settings) {
            return SettingsActivity.class;
        }
        return null;
    }
}
