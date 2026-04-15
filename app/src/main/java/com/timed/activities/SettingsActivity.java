package com.timed.activities;

import android.content.Intent; // Added missing import
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;
import com.timed.Setting.Main.SettingItem;
import com.timed.Setting.Main.SettingsAdapter;
import com.timed.Setting.Security.SecurityActivity;
import com.timed.Setting.SyncStorage.SyncStorageActivity;
import com.timed.Setting.Themes.ThemeActivity;
import com.timed.Setting.Timezone.TimezoneSettingActivity;
import com.timed.activities.ImportExportActivity;

import java.util.ArrayList;
import java.util.List;
import com.timed.utils.CalendarIntegrationService;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_setting);

        setupInsets();
        setupListeners();
        setupRecyclerView();
    }

    private void setupListeners() {
        ImageButton ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        // MOVED HERE: Logic for the Import/Export row
//        View rowImportExport = findViewById(R.id.rowImportExport);
//        if (rowImportExport != null) {
//            rowImportExport.setOnClickListener(v -> {
//                // Ensure ImportExportActivity is created/imported
//                Intent intent = new Intent(this, ImportExportActivity.class);
//                CalendarIntegrationService calendarService = new CalendarIntegrationService();
//                String calendarId = calendarService.getCachedDefaultCalendarId(this);
//                if (calendarId != null && !calendarId.isEmpty()) {
//                    intent.putExtra("calendarId", calendarId);
//                }
//                startActivity(intent);
//            });
//        }
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
        settingList.add(new SettingItem(R.drawable.ic_security, "Security", SecurityActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_cloud, "Sync & Storage", SyncStorageActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_send, "Import ICS/CSV and Export Calendar", ImportExportActivity.class));
        settingList.add(new SettingItem(R.drawable.ic_theme, "Theme & Appearance", ThemeActivity.class));

        return settingList;
    }

    private void setupInsets() {
        View root = findViewById(R.id.topBar);
        if (root == null) return;

        final int baseTopBarHeight = dpToPx(65);
        final int baseTopPadding = root.getPaddingTop();
        final int baseBottomPadding = root.getPaddingBottom();
        final int baseLeftPadding = root.getPaddingLeft();
        final int baseRightPadding = root.getPaddingRight();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            root.setPadding(baseLeftPadding, baseTopPadding + bars.top, baseRightPadding, baseBottomPadding);
            ViewGroup.LayoutParams lp = root.getLayoutParams();
            lp.height = baseTopBarHeight + bars.top;
            root.setLayoutParams(lp);
            return insets;
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}