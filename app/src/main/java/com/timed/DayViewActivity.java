package com.timed;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DayViewActivity extends AppCompatActivity {

    private LinearLayout viewSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        viewSwitcher = findViewById(R.id.viewSwitcher);
        TextView tvTopTitle = findViewById(R.id.tvTopTitle);
        RecyclerView rvHorizontalCalendar = findViewById(R.id.rvHorizontalCalendar);

        if (rvHorizontalCalendar != null) {
            rvHorizontalCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        }

        if (viewSwitcher != null) {
            viewSwitcher.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(DayViewActivity.this, viewSwitcher);
                popupMenu.getMenu().add("Day View");
                popupMenu.getMenu().add("Month View");

                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().toString().equals("Month View")) {
                        Intent intent = new Intent(DayViewActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }
                    return true;
                });
                popupMenu.show();
            });
        }
    }
}