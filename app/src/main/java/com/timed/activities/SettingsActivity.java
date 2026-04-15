package com.timed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.timed.R;
import com.timed.utils.CalendarIntegrationService;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_settings);

        setupInsets();
        setupListeners();
    }

    private void setupListeners() {
        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View rowImportExport = findViewById(R.id.rowImportExport);
        if (rowImportExport != null) {
            rowImportExport.setOnClickListener(v -> {
                Intent intent = new Intent(this, ImportExportActivity.class);
                CalendarIntegrationService calendarService = new CalendarIntegrationService();
                String calendarId = calendarService.getCachedDefaultCalendarId(this);
                if (calendarId != null && !calendarId.isEmpty()) {
                    intent.putExtra("calendarId", calendarId);
                }
                startActivity(intent);
            });
        }
    }

    private void setupInsets() {
        View root = findViewById(R.id.rootSettings);
        View topBar = findViewById(R.id.topBarSettings);
        if (root == null || topBar == null) {
            return;
        }

        final int baseTopBarHeight = dpToPx(56);
        final int baseTopPadding = topBar.getPaddingTop();
        final int baseBottomPadding = topBar.getPaddingBottom();
        final int baseLeftPadding = topBar.getPaddingLeft();
        final int baseRightPadding = topBar.getPaddingRight();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            topBar.setPadding(baseLeftPadding, baseTopPadding + bars.top, baseRightPadding, baseBottomPadding);
            ViewGroup.LayoutParams lp = topBar.getLayoutParams();
            lp.height = baseTopBarHeight + bars.top;
            topBar.setLayoutParams(lp);

            v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
