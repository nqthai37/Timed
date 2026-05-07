package com.timed.activities;

import android.os.Bundle;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;
import com.timed.Setting.Main.SettingItem;
import com.timed.Setting.Main.SettingsAdapter;
import com.timed.Setting.Profile.ProfileActivity;
import com.timed.Setting.Themes.ThemeActivity;
import com.timed.Setting.Timezone.TimezoneSettingActivity;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseBottomNavActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_setting);
        setupInsets();
        setupRecyclerView();
        setupBottomNavigation();
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.nav_settings;
    }

    private void setupInsets() {
        View root = findViewById(R.id.settingsRoot);
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
                    baseRight + bars.right, baseBottom);
            return insets;
        });
    }

    private void setupRecyclerView() {
        RecyclerView rvSettings = findViewById(R.id.rv_settings);
        if (rvSettings != null) {
            rvSettings.setLayoutManager(new LinearLayoutManager(this));
            rvSettings.setAdapter(new SettingsAdapter(this, getSettings()));
        }
    }

    private List<SettingItem> getSettings() {
        List<SettingItem> settingList = new ArrayList<>();
        settingList.add(new SettingItem(R.drawable.ic_time_zone, "Timezone Setting", TimezoneSettingActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_security, "Profile", ProfileActivity.class));
        settingList.add(
                new SettingItem(R.drawable.ic_send, "Import ICS/CSV and Export Calendar", ImportExportActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_theme, "Theme & Appearance", ThemeActivity.class));

        return settingList;
    }

}
