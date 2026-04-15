package com.timed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.timed.Setting.Main.SettingActivity;

import com.timed.R;
import com.timed.adapters.CalendarAdapter;
import com.timed.adapters.EventAdapter;
import com.timed.adapters.HorizontalCalendarAdapter;
import com.timed.adapters.WeekEventAdapter;
import com.timed.models.CalendarDay;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.timed.activities.CreateEventActivity;
import com.timed.activities.SearchFilterActivity;
import com.timed.activities.SettingsActivity;
import com.timed.managers.EventsManager;
import com.timed.models.Event;
import com.timed.utils.FirebaseAuthManager;
import com.timed.utils.FirebaseHelper;
import com.timed.utils.FirebaseInitializer;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener {

    private static final String TAG = "MainActivity";

    private RecyclerView rvCalendar;
    private RecyclerView rvHorizontalCalendar;

    private LocalDate selectedDate;
    private LocalDate startDate3Days;
    private LocalDate selectedWeekDate;

    private TextView tvTopTitle;
    private TextView tvCurrentMonth;
    private TextView tvUpcomingTitle;
    private TextView tvCurrentDayFull;

    private EventAdapter eventAdapter;
    private List<Event> currentEvents;

    private View layoutMonthView;
    private View layoutDayView;
    private View layout3DaysView;
    private View layoutWeekView;
    private View layoutFabMenuOverlay;
    private View fabOptionEvent, fabOptionTask, fabOptionReminder;
    private boolean isFabMenuOpen = false;

    private HorizontalCalendarAdapter horizontalAdapter;
    private List<LocalDate> horizontalDateList;

    // Firebase instances
    private FirebaseInitializer firebaseInitializer;
    private FirebaseHelper firebaseHelper;
    private FirebaseAuthManager firebaseAuthManager;

    private EventsManager eventsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase
        initializeFirebase();
        eventsManager = EventsManager.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContent), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null && drawerLayout != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        layoutMonthView = findViewById(R.id.layoutMonthView);
        layoutDayView = findViewById(R.id.layoutDayView);
        layout3DaysView = findViewById(R.id.layout3DaysView);
        layoutWeekView = findViewById(R.id.layoutWeekView);

        tvCurrentDayFull = findViewById(R.id.tvCurrentDayFull);
        rvHorizontalCalendar = findViewById(R.id.rvHorizontalCalendar);
        tvTopTitle = findViewById(R.id.tvTopTitle);

        ImageButton btnSearch = findViewById(R.id.btnSearch);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));
        }

        selectedDate = LocalDate.now();
        startDate3Days = LocalDate.now();
        selectedWeekDate = LocalDate.now();

        ImageButton btnPrevWeek = findViewById(R.id.btnPrevWeek);
        ImageButton btnNextWeek = findViewById(R.id.btnNextWeek);
        if (btnPrevWeek != null)
            btnPrevWeek.setOnClickListener(v -> {
                selectedDate = selectedDate.minusWeeks(1);
                setupHorizontalCalendar();
            });
        if (btnNextWeek != null)
            btnNextWeek.setOnClickListener(v -> {
                selectedDate = selectedDate.plusWeeks(1);
                setupHorizontalCalendar();
            });

        ImageButton btnPrev3Days = findViewById(R.id.btnPrev3Days);
        ImageButton btnNext3Days = findViewById(R.id.btnNext3Days);
        if (btnPrev3Days != null)
            btnPrev3Days.setOnClickListener(v -> {
                startDate3Days = startDate3Days.minusDays(3);
                setup3DaysView();
            });
        if (btnNext3Days != null)
            btnNext3Days.setOnClickListener(v -> {
                startDate3Days = startDate3Days.plusDays(3);
                setup3DaysView();
            });

        ImageButton btnPrevWeekView = findViewById(R.id.btnPrevWeekView);
        ImageButton btnNextWeekView = findViewById(R.id.btnNextWeekView);
        if (btnPrevWeekView != null)
            btnPrevWeekView.setOnClickListener(v -> {
                selectedWeekDate = selectedWeekDate.minusWeeks(1);
                setupWeekView();
            });
        if (btnNextWeekView != null)
            btnNextWeekView.setOnClickListener(v -> {
                selectedWeekDate = selectedWeekDate.plusWeeks(1);
                setupWeekView();
            });

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            TabLayout.Tab monthTab = tabLayout.getTabAt(3);
            if (monthTab != null)
                monthTab.select();

            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    layoutMonthView.setVisibility(View.GONE);
                    layoutDayView.setVisibility(View.GONE);
                    layout3DaysView.setVisibility(View.GONE);
                    layoutWeekView.setVisibility(View.GONE);

                    if (tab.getPosition() == 0) {
                        layoutDayView.setVisibility(View.VISIBLE);
                        setupHorizontalCalendar();
                    } else if (tab.getPosition() == 1) {
                        layout3DaysView.setVisibility(View.VISIBLE);
                        setup3DaysView();
                    } else if (tab.getPosition() == 2) {
                        layoutWeekView.setVisibility(View.VISIBLE);
                        setupWeekView();
                    } else if (tab.getPosition() == 3) {
                        layoutMonthView.setVisibility(View.VISIBLE);
                        setMonthView();
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });

            layoutFabMenuOverlay = findViewById(R.id.layoutFabMenuOverlay);
            fabOptionEvent = findViewById(R.id.fabOptionEvent);
            fabOptionTask = findViewById(R.id.fabOptionTask);
            fabOptionReminder = findViewById(R.id.fabOptionReminder);

            com.google.android.material.floatingactionbutton.FloatingActionButton fabMain = findViewById(
                    R.id.fabAddEvent);
            com.google.android.material.floatingactionbutton.FloatingActionButton fabClose = findViewById(
                    R.id.fabCloseMenu);

            if (fabMain != null) {
                fabMain.setOnClickListener(v -> toggleFabMenu());
            }

            if (fabClose != null) {
                fabClose.setOnClickListener(v -> toggleFabMenu());
            }

            if (layoutFabMenuOverlay != null) {
                layoutFabMenuOverlay.setOnClickListener(v -> {
                    if (isFabMenuOpen)
                        toggleFabMenu();
                });
            }

            View btnFabEvent = findViewById(R.id.btnFabEvent);
            if (btnFabEvent != null) {
                btnFabEvent.setOnClickListener(v -> {
                    toggleFabMenu();
                    Intent intent = new Intent(this, CreateEventActivity.class);
                    intent.putExtra("calendarId", "default_calendar");
                    startActivity(intent);
                });
            }

            findViewById(R.id.btnFabTask).setOnClickListener(v -> {
                toggleFabMenu();
                android.widget.Toast.makeText(this, "Create Task Clicked", android.widget.Toast.LENGTH_SHORT).show();
            });

            findViewById(R.id.btnFabReminder).setOnClickListener(v -> {
                toggleFabMenu();
                android.widget.Toast.makeText(this, "Create Reminder Clicked", android.widget.Toast.LENGTH_SHORT)
                        .show();
            });
        }

        rvCalendar = findViewById(R.id.rvCalendar);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        ImageButton btnPrevMonth = findViewById(R.id.btnPrevMonth);
        ImageButton btnNextMonth = findViewById(R.id.btnNextMonth);

        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));

        updateMonthYearText();
        setMonthView();

        if (btnPrevMonth != null)
            btnPrevMonth.setOnClickListener(v -> {
                selectedDate = selectedDate.minusMonths(1);
                updateMonthYearText();
                setMonthView();
            });
        if (btnNextMonth != null)
            btnNextMonth.setOnClickListener(v -> {
                selectedDate = selectedDate.plusMonths(1);
                updateMonthYearText();
                setMonthView();
            });

        RecyclerView rvEvents = findViewById(R.id.rvEvents);
        tvUpcomingTitle = findViewById(R.id.tvUpcomingTitle);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        currentEvents = new ArrayList<>();
        eventAdapter = new EventAdapter(currentEvents, this::openEditEvent);
        rvEvents.setAdapter(eventAdapter);
        setupBottomNavigation();
        updateEventsForDate(selectedDate);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) {
            return;
        }

        bottomNav.setSelectedItemId(R.id.nav_schedule);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_schedule) {
                return true;
            }

            if (itemId == R.id.nav_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }

            Toast.makeText(this, "This feature is under development", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateEventsForDate(selectedDate);

        // Setup BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_settings) {
                    Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void setupHorizontalCalendar() {
        if (rvHorizontalCalendar.getLayoutManager() == null) {
            rvHorizontalCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        }

        horizontalDateList = new ArrayList<>();
        int currentDayOfWeek = selectedDate.getDayOfWeek().getValue();
        int daysToSubtract = (currentDayOfWeek == 7) ? 0 : currentDayOfWeek;
        LocalDate startOfWeek = selectedDate.minusDays(daysToSubtract);

        for (int i = 0; i < 7; i++) {
            horizontalDateList.add(startOfWeek.plusDays(i));
        }

        horizontalAdapter = new HorizontalCalendarAdapter(horizontalDateList, selectedDate, (date, position) -> {
            selectedDate = date;
            if (tvCurrentDayFull != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);
                tvCurrentDayFull.setText(selectedDate.format(formatter));
            }
            loadDayEvents(date);
        });

        rvHorizontalCalendar.setAdapter(horizontalAdapter);
        if (tvCurrentDayFull != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);
            tvCurrentDayFull.setText(selectedDate.format(formatter));
        }
        loadDayEvents(selectedDate);
    }

    private void renderDayViewTimeline(LocalDate date, List<Event> events) {
        android.widget.RelativeLayout timelineContainer = findViewById(R.id.timelineContainer);
        if (timelineContainer == null)
            return;

        timelineContainer.removeAllViews();
        int hourHeightDp = 80;
        int hourHeightPx = dpToPx(hourHeightDp);
        timelineContainer.setMinimumHeight(hourHeightPx * 24);

        for (int i = 0; i < 24; i++) {
            TextView tvTime = new TextView(this);
            tvTime.setId(View.generateViewId());
            tvTime.setText(String.format(Locale.getDefault(), "%02d:00", i));
            tvTime.setTextColor(android.graphics.Color.parseColor("#99741CE9"));
            tvTime.setTextSize(12f);
            tvTime.setTypeface(null, android.graphics.Typeface.BOLD);

            android.widget.RelativeLayout.LayoutParams timeParams = new android.widget.RelativeLayout.LayoutParams(
                    dpToPx(56), android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
            timeParams.topMargin = i * hourHeightPx;
            timelineContainer.addView(tvTime, timeParams);

            View line = new View(this);
            line.setBackgroundColor(android.graphics.Color.parseColor("#1A741CE9"));
            android.widget.RelativeLayout.LayoutParams lineParams = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
            lineParams.addRule(android.widget.RelativeLayout.RIGHT_OF, tvTime.getId());
            lineParams.topMargin = i * hourHeightPx + dpToPx(8);
            timelineContainer.addView(line, lineParams);
        }

        if (events == null || events.isEmpty()) {
            return;
        }

        int colorIndex = 0;
        for (Event event : events) {
            EventTimeParts parts = toTimeParts(event);
            if (parts == null) {
            continue;
            }

            String title = event.getTitle() != null ? event.getTitle() : "(Untitled)";
            String details = buildEventDetails(event);
            int bgRes = pickEventBackground(colorIndex++);
            String titleColor = bgRes == R.drawable.bg_day_event_light ? "#741ce9" : "#FFFFFF";
            String detailsColor = bgRes == R.drawable.bg_day_event_light ? "#64748b" : "#E6FFFFFF";

            addEventCardToTimeline(timelineContainer, hourHeightPx, title, details, parts.startHour,
                parts.startMinute, parts.durationMinutes, bgRes, titleColor, detailsColor);
        }
    }

    private void addEventCardToTimeline(android.widget.RelativeLayout container, int hourHeightPx, String title,
            String details, int startHour, int startMinute, int durationMinutes, int backgroundResId,
            String titleColorHex, String detailsColorHex) {
        android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        card.setBackgroundResource(backgroundResId);
        card.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        card.setElevation(dpToPx(4));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(android.graphics.Color.parseColor(titleColorHex));
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvDetails = new TextView(this);
        tvDetails.setText(details);
        tvDetails.setTextColor(android.graphics.Color.parseColor(detailsColorHex));
        tvDetails.setTextSize(12f);
        tvDetails.setPadding(0, dpToPx(4), 0, 0);
        card.addView(tvDetails);

        int topMargin = (startHour * hourHeightPx) + (startMinute * hourHeightPx / 60);
        int cardHeight = (durationMinutes * hourHeightPx / 60);

        android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT, cardHeight);
        params.topMargin = topMargin;
        params.leftMargin = dpToPx(60);
        params.rightMargin = dpToPx(16);

        container.addView(card, params);
    }

    private void setup3DaysView() {
        DateTimeFormatter dowFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

        TextView[] dowTvs = { findViewById(R.id.tv3DaysDow1), findViewById(R.id.tv3DaysDow2),
                findViewById(R.id.tv3DaysDow3) };
        TextView[] dateTvs = { findViewById(R.id.tv3DaysDate1), findViewById(R.id.tv3DaysDate2),
                findViewById(R.id.tv3DaysDate3) };

        for (int i = 0; i < 3; i++) {
            LocalDate day = startDate3Days.plusDays(i);
            if (dowTvs[i] != null && dateTvs[i] != null) {
                dowTvs[i].setText(day.format(dowFormatter).toUpperCase());
                dateTvs[i].setText(String.valueOf(day.getDayOfMonth()));

                if (day.equals(LocalDate.now())) {
                    dateTvs[i].setTextColor(android.graphics.Color.parseColor("#741ce9"));
                } else {
                    dateTvs[i].setTextColor(android.graphics.Color.parseColor("#0f172a"));
                }
            }
        }

        if (tvTopTitle != null) {
            DateTimeFormatter titleFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
            tvTopTitle.setText(
                    startDate3Days.format(titleFormatter) + " - " + startDate3Days.plusDays(2).getDayOfMonth());
        }

        loadThreeDaysEvents(startDate3Days);
    }

    private void render3DaysTimeline(LocalDate startDate, List<Event> events) {
        android.widget.RelativeLayout container = findViewById(R.id.timeline3DaysContainer);
        if (container == null)
            return;

        container.post(() -> {
            container.removeAllViews();
            int hourHeightPx = dpToPx(80);
            container.setMinimumHeight(hourHeightPx * 24);

            int timeColumnWidth = dpToPx(50);
            int totalGridWidth = container.getWidth() - timeColumnWidth;
            int colWidth = totalGridWidth / 3;

            for (int i = 0; i < 24; i++) {
                TextView tvTime = new TextView(this);
                tvTime.setId(View.generateViewId());
                tvTime.setText(String.format(Locale.getDefault(), "%02d:00", i));
                tvTime.setTextColor(android.graphics.Color.parseColor("#94a3b8"));
                tvTime.setTextSize(10f);
                tvTime.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

                android.widget.RelativeLayout.LayoutParams timeParams = new android.widget.RelativeLayout.LayoutParams(
                        timeColumnWidth, android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
                timeParams.topMargin = i * hourHeightPx + dpToPx(8);
                container.addView(tvTime, timeParams);

                View line = new View(this);
                line.setBackgroundColor(android.graphics.Color.parseColor("#0D741CE9"));
                android.widget.RelativeLayout.LayoutParams lineParams = new android.widget.RelativeLayout.LayoutParams(
                        android.widget.RelativeLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
                lineParams.leftMargin = timeColumnWidth;
                lineParams.topMargin = i * hourHeightPx + dpToPx(16);
                container.addView(line, lineParams);
            }

            for (int i = 1; i < 3; i++) {
                View verticalLine = new View(this);
                verticalLine.setBackgroundColor(android.graphics.Color.parseColor("#0D741CE9"));
                android.widget.RelativeLayout.LayoutParams vParams = new android.widget.RelativeLayout.LayoutParams(
                        dpToPx(1), android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
                vParams.leftMargin = timeColumnWidth + (i * colWidth);
                container.addView(verticalLine, vParams);
            }

            if (events == null || events.isEmpty()) {
                return;
            }

            int colorIndex = 0;
            for (Event event : events) {
                LocalDate eventDate = toLocalDate(event.getStartTime());
                if (eventDate == null) {
                    continue;
                }
                int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, eventDate);
                if (dayIndex < 0 || dayIndex > 2) {
                    continue;
                }

                EventTimeParts parts = toTimeParts(event);
                if (parts == null) {
                    continue;
                }

                int bgRes = pickEventBackground(colorIndex++);
                String titleColor = bgRes == R.drawable.bg_day_event_light ? "#334155" : "#FFFFFF";
                String detailColor = bgRes == R.drawable.bg_day_event_light ? "#64748b" : "#E6FFFFFF";

                addEventTo3Days(container, hourHeightPx, timeColumnWidth, colWidth, dayIndex,
                        event.getTitle() != null ? event.getTitle() : "(Untitled)",
                        buildEventLocation(event), parts.startHour, parts.startMinute,
                        parts.durationMinutes, bgRes, titleColor, detailColor);
            }
        });
    }

    private void addEventTo3Days(android.widget.RelativeLayout container, int hourHeightPx, int timeOffset,
            int colWidth, int dayIndex, String title, String details, int startHour, int startMinute, int durationMins,
            int bgRes, String titleHex, String detailHex) {
        android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        card.setBackgroundResource(bgRes);
        card.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        card.setElevation(dpToPx(2));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(android.graphics.Color.parseColor(titleHex));
        tvTitle.setTextSize(11f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvTitle);

        if (!details.isEmpty()) {
            TextView tvDetails = new TextView(this);
            tvDetails.setText(details);
            tvDetails.setTextColor(android.graphics.Color.parseColor(detailHex));
            tvDetails.setTextSize(9f);
            card.addView(tvDetails);
        }

        int topMargin = (startHour * hourHeightPx) + (startMinute * hourHeightPx / 60) + dpToPx(16);
        int cardHeight = (durationMins * hourHeightPx / 60);

        android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(
                colWidth - dpToPx(4), cardHeight);
        params.topMargin = topMargin;
        params.leftMargin = timeOffset + (dayIndex * colWidth) + dpToPx(2);

        container.addView(card, params);
    }

    private void setupWeekView() {
        if (tvTopTitle != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
            tvTopTitle.setText(selectedWeekDate.format(formatter));
        }

        int currentDayOfWeek = selectedWeekDate.getDayOfWeek().getValue();
        int daysToSubtract = (currentDayOfWeek == 7) ? 0 : currentDayOfWeek;
        LocalDate startOfWeek = selectedWeekDate.minusDays(daysToSubtract);

        android.widget.LinearLayout headerContainer = findViewById(R.id.layoutWeekDaysHeader);
        if (headerContainer != null) {
            headerContainer.removeAllViews();
            DateTimeFormatter dowFormatter = DateTimeFormatter.ofPattern("E", Locale.ENGLISH);

            for (int i = 0; i < 7; i++) {
                LocalDate day = startOfWeek.plusDays(i);

                android.widget.LinearLayout dayCol = new android.widget.LinearLayout(this);
                dayCol.setOrientation(android.widget.LinearLayout.VERTICAL);
                dayCol.setGravity(android.view.Gravity.CENTER);
                dayCol.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                dayCol.setPadding(0, dpToPx(8), 0, dpToPx(8));

                TextView tvDow = new TextView(this);
                tvDow.setText(day.format(dowFormatter).toUpperCase().substring(0, 1));
                tvDow.setTextSize(10f);
                tvDow.setTextColor(android.graphics.Color.parseColor("#94a3b8"));

                TextView tvDate = new TextView(this);
                tvDate.setText(String.valueOf(day.getDayOfMonth()));
                tvDate.setTextSize(14f);
                tvDate.setTypeface(null, android.graphics.Typeface.BOLD);

                if (day.equals(LocalDate.now())) {
                    tvDate.setTextColor(android.graphics.Color.parseColor("#741ce9"));
                    tvDow.setTextColor(android.graphics.Color.parseColor("#741ce9"));
                } else {
                    tvDate.setTextColor(android.graphics.Color.parseColor("#0f172a"));
                }

                dayCol.addView(tvDow);
                dayCol.addView(tvDate);
                headerContainer.addView(dayCol);
            }
        }

        loadWeekEvents(startOfWeek);
    }

    private void renderWeekGridTimeline(LocalDate startOfWeek, List<Event> events) {
        android.widget.RelativeLayout container = findViewById(R.id.timelineWeekContainer);
        if (container == null)
            return;

        container.post(() -> {
            container.removeAllViews();
            int hourHeightPx = dpToPx(60);
            container.setMinimumHeight(hourHeightPx * 24);

            int timeColumnWidth = dpToPx(40);
            int totalGridWidth = container.getWidth() - timeColumnWidth;
            int colWidth = totalGridWidth / 7;

            for (int i = 0; i < 24; i++) {
                TextView tvTime = new TextView(this);
                tvTime.setId(View.generateViewId());
                tvTime.setText(String.format(Locale.getDefault(), "%02d", i));
                tvTime.setTextColor(android.graphics.Color.parseColor("#94a3b8"));
                tvTime.setTextSize(10f);
                tvTime.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

                android.widget.RelativeLayout.LayoutParams timeParams = new android.widget.RelativeLayout.LayoutParams(
                        timeColumnWidth, android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
                timeParams.topMargin = i * hourHeightPx + dpToPx(4);
                container.addView(tvTime, timeParams);

                View line = new View(this);
                line.setBackgroundColor(android.graphics.Color.parseColor("#0D741CE9"));
                android.widget.RelativeLayout.LayoutParams lineParams = new android.widget.RelativeLayout.LayoutParams(
                        android.widget.RelativeLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
                lineParams.leftMargin = timeColumnWidth;
                lineParams.topMargin = i * hourHeightPx + dpToPx(10);
                container.addView(line, lineParams);
            }

            for (int i = 1; i < 7; i++) {
                View verticalLine = new View(this);
                verticalLine.setBackgroundColor(android.graphics.Color.parseColor("#0D741CE9"));
                android.widget.RelativeLayout.LayoutParams vParams = new android.widget.RelativeLayout.LayoutParams(
                        dpToPx(1), android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
                vParams.leftMargin = timeColumnWidth + (i * colWidth);
                container.addView(verticalLine, vParams);
            }

            if (events == null || events.isEmpty()) {
                return;
            }

            int colorIndex = 0;
            for (Event event : events) {
                LocalDate eventDate = toLocalDate(event.getStartTime());
                if (eventDate == null) {
                    continue;
                }
                int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, eventDate);
                if (dayIndex < 0 || dayIndex > 6) {
                    continue;
                }

                EventTimeParts parts = toTimeParts(event);
                if (parts == null) {
                    continue;
                }

                int bgRes = pickEventBackground(colorIndex++);
                String title = event.getTitle() != null ? event.getTitle() : "(Untitled)";
                addEventToWeekGrid(container, hourHeightPx, timeColumnWidth, colWidth, dayIndex,
                        title, parts.startHour, parts.startMinute, parts.durationMinutes, bgRes);
            }
        });
    }

    private void addEventToWeekGrid(android.widget.RelativeLayout container, int hourHeightPx, int timeOffset,
            int colWidth, int dayIndex, String shortTitle, int startHour, int startMinute, int durationMins,
            int bgRes) {
        android.widget.TextView card = new android.widget.TextView(this);
        card.setBackgroundResource(bgRes);
        card.setText(shortTitle);
        card.setTextColor(android.graphics.Color.WHITE);
        card.setTextSize(9f);
        card.setTypeface(null, android.graphics.Typeface.BOLD);
        card.setPadding(dpToPx(4), dpToPx(4), dpToPx(2), dpToPx(2));
        card.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.setMaxLines(2);

        if (bgRes == R.drawable.bg_day_event_light) {
            card.setTextColor(android.graphics.Color.parseColor("#741ce9"));
        }

        int topMargin = (startHour * hourHeightPx) + (startMinute * hourHeightPx / 60) + dpToPx(10);
        int cardHeight = (durationMins * hourHeightPx / 60);

        android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(
                colWidth - dpToPx(2), cardHeight);
        params.topMargin = topMargin;
        params.leftMargin = timeOffset + (dayIndex * colWidth) + dpToPx(1);

        container.addView(card, params);
    }

    private void setMonthView() {
        List<CalendarDay> daysInMonth = daysInMonthArray(selectedDate);
        CalendarAdapter adapter = new CalendarAdapter(daysInMonth, this);
        rvCalendar.setAdapter(adapter);
    }

    private List<CalendarDay> daysInMonthArray(LocalDate date) {
        List<CalendarDay> daysInMonthArray = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate firstOfMonth = date.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        int offset = (dayOfWeek == 7) ? 0 : dayOfWeek;

        for (int i = 1; i <= 42; i++) {
            if (i <= offset) {
                LocalDate prevMonthDate = firstOfMonth.minusDays(offset - i + 1);
                daysInMonthArray.add(new CalendarDay(prevMonthDate, false));
            } else if (i > daysInMonth + offset) {
                LocalDate nextMonthDate = firstOfMonth.plusDays(i - offset - 1);
                daysInMonthArray.add(new CalendarDay(nextMonthDate, false));
            } else {
                LocalDate currentMonthDate = firstOfMonth.plusDays(i - offset - 1);
                CalendarDay day = new CalendarDay(currentMonthDate, true);
                if (currentMonthDate != null) {
                    if (selectedDate != null && currentMonthDate.equals(selectedDate))
                        day.isSelected = true;
                    if (currentMonthDate.equals(LocalDate.now()))
                        day.isToday = true;
                }
                daysInMonthArray.add(day);
            }
        }
        return daysInMonthArray;
    }

    private void updateMonthYearText() {
        if (tvCurrentMonth != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            tvCurrentMonth.setText(selectedDate.format(formatter));
        }
        if (tvTopTitle != null) {
            DateTimeFormatter titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            tvTopTitle.setText(selectedDate.format(titleFormatter));
        }
    }

    @Override
    public void onItemClick(int position, LocalDate date) {
        if (date != null) {
            selectedDate = date;
            setMonthView();
            updateEventsForDate(date);
        }
    }

    private void updateEventsForDate(LocalDate date) {
        if (tvUpcomingTitle == null || eventAdapter == null)
            return;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");
        String formattedDate = date.format(formatter).toUpperCase();
        tvUpcomingTitle.setText("UPCOMING FOR " + formattedDate);

        String calendarId = "default_calendar";
        fetchEventsForRange(date, date, calendarId, events -> {
            currentEvents.clear();
            currentEvents.addAll(events);
            eventAdapter.notifyDataSetChanged();
            renderDayViewTimeline(date, events);
        });
    }

    private void openEditEvent(Event event) {
        Intent intent = new Intent(this, CreateEventActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("eventId", event.getId());
        intent.putExtra("calendarId", event.getCalendarId() != null ? event.getCalendarId() : "default_calendar");
        intent.putExtra("title", event.getTitle());
        intent.putExtra("description", event.getDescription());
        intent.putExtra("location", event.getLocation());
        boolean recurring = event.getRecurrenceRule() != null && !event.getRecurrenceRule().trim().isEmpty();
        intent.putExtra("startTime", recurring ? 0L : (event.getStartTime() != null ? event.getStartTime().toDate().getTime() : 0L));
        intent.putExtra("endTime", recurring ? 0L : (event.getEndTime() != null ? event.getEndTime().toDate().getTime() : 0L));
        intent.putExtra("allDay", event.getAllDay() != null && event.getAllDay());
        startActivity(intent);
    }

    private void toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen;

        if (isFabMenuOpen) {
            layoutFabMenuOverlay.setVisibility(View.VISIBLE);
            layoutFabMenuOverlay.setAlpha(0f);
            layoutFabMenuOverlay.animate().alpha(1f).setDuration(200).start();

            fabOptionEvent.setTranslationY(100f);
            fabOptionEvent.setAlpha(0f);
            fabOptionEvent.setVisibility(View.VISIBLE);

            fabOptionTask.setTranslationY(100f);
            fabOptionTask.setAlpha(0f);
            fabOptionTask.setVisibility(View.VISIBLE);

            fabOptionReminder.setTranslationY(100f);
            fabOptionReminder.setAlpha(0f);
            fabOptionReminder.setVisibility(View.VISIBLE);

            fabOptionEvent.animate().translationY(0f).alpha(1f).setDuration(200).start();
            fabOptionTask.animate().translationY(0f).alpha(1f).setDuration(200).setStartDelay(50).start();
            fabOptionReminder.animate().translationY(0f).alpha(1f).setDuration(200).setStartDelay(100).start();

        } else {
            fabOptionReminder.animate().translationY(100f).alpha(0f).setDuration(150).start();
            fabOptionTask.animate().translationY(100f).alpha(0f).setDuration(150).setStartDelay(50).start();
            fabOptionEvent.animate().translationY(100f).alpha(0f).setDuration(150).setStartDelay(100).start();

            layoutFabMenuOverlay.animate().alpha(0f).setDuration(200).setStartDelay(150).withEndAction(() -> {
                layoutFabMenuOverlay.setVisibility(View.GONE);
                fabOptionEvent.setVisibility(View.INVISIBLE);
                fabOptionTask.setVisibility(View.INVISIBLE);
                fabOptionReminder.setVisibility(View.INVISIBLE);
            }).start();
        }
    }
    
    /**
     * Khởi tạo Firebase
     */
    private void initializeFirebase() {
        try {
            // Khởi tạo Firebase Initializer
            firebaseInitializer = FirebaseInitializer.getInstance();
            firebaseInitializer.initialize(this);
            
            // Tạo Firebase Helper
            firebaseHelper = new FirebaseHelper();
            
            // Tạo Firebase Auth Manager
            firebaseAuthManager = new FirebaseAuthManager();
            
            Log.d(TAG, "Firebase initialized successfully");
            
            // Kiểm tra kết nối Firebase
            checkFirebaseConnection();
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to initialize Firebase", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Kiểm tra kết nối Firebase
     */
    private void checkFirebaseConnection() {
        firebaseHelper.checkConnection(new FirebaseHelper.FirebaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.d(TAG, "Firebase connection verified");
                Toast.makeText(MainActivity.this, "Firebase connected successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Firebase connection failed: " + errorMessage);
                Toast.makeText(MainActivity.this, "Firebase connection error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Lấy Firebase Auth Manager
     */
    public FirebaseAuthManager getFirebaseAuthManager() {
        return firebaseAuthManager;
    }
    
    /**
     * Lấy Firebase Helper
     */
    public FirebaseHelper getFirebaseHelper() {
        return firebaseHelper;
    }
    
    /**
     * Lấy Firebase Initializer
     */
    public FirebaseInitializer getFirebaseInitializer() {
        return firebaseInitializer;
    }

    private void loadDayEvents(LocalDate date) {
        fetchEventsForRange(date, date, "default_calendar", events -> renderDayViewTimeline(date, events));
    }

    private void loadThreeDaysEvents(LocalDate startDate) {
        fetchEventsForRange(startDate, startDate.plusDays(2), "default_calendar",
                events -> render3DaysTimeline(startDate, events));
    }

    private void loadWeekEvents(LocalDate startOfWeek) {
        fetchEventsForRange(startOfWeek, startOfWeek.plusDays(6), "default_calendar",
                events -> renderWeekGridTimeline(startOfWeek, events));
    }

    private void fetchEventsForRange(LocalDate startDate, LocalDate endDate, String calendarId,
            EventsLoadCallback callback) {
        if (eventsManager == null) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        Timestamp startTimestamp = toTimestamp(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        long endMillis = endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
        Timestamp endTimestamp = toTimestamp(endMillis);

        eventsManager.getEventsByDateRange(calendarId, startTimestamp, endTimestamp)
                .addOnSuccessListener(callback::onLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events: " + e.getMessage(), e);
                    callback.onLoaded(new ArrayList<>());
                });
    }

    private Timestamp toTimestamp(long millis) {
        return new Timestamp(new Date(millis));
    }

    private LocalDate toLocalDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private EventTimeParts toTimeParts(Event event) {
        if (event == null || event.getStartTime() == null) {
            return null;
        }

        Date start = event.getStartTime().toDate();
        Date end = event.getEndTime() != null ? event.getEndTime().toDate() : start;

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        int startHour = startCal.get(Calendar.HOUR_OF_DAY);
        int startMinute = startCal.get(Calendar.MINUTE);

        long durationMillis = Math.max(30 * 60 * 1000L, end.getTime() - start.getTime());
        int durationMinutes = (int) Math.max(15, durationMillis / 60000L);

        if (event.getAllDay() != null && event.getAllDay()) {
            startHour = 0;
            startMinute = 0;
            durationMinutes = 60;
        }

        return new EventTimeParts(startHour, startMinute, durationMinutes);
    }

    private String buildEventDetails(Event event) {
        String timeRange = buildTimeRange(event);
        String location = buildEventLocation(event);

        if (!timeRange.isEmpty() && !location.isEmpty()) {
            return timeRange + " • " + location;
        }
        if (!timeRange.isEmpty()) {
            return timeRange;
        }
        return location;
    }

    private String buildEventLocation(Event event) {
        if (event == null || event.getLocation() == null) {
            return "";
        }
        return event.getLocation().trim();
    }

    private String buildTimeRange(Event event) {
        if (event == null || event.getStartTime() == null) {
            return "";
        }

        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        Date start = event.getStartTime().toDate();
        String startText = formatter.format(start);

        if (event.getAllDay() != null && event.getAllDay()) {
            return "All day";
        }

        if (event.getEndTime() != null) {
            String endText = formatter.format(event.getEndTime().toDate());
            return startText + " - " + endText;
        }

        return startText;
    }

    private int pickEventBackground(int index) {
        int mod = index % 3;
        if (mod == 1) {
            return R.drawable.bg_day_event_emerald;
        }
        if (mod == 2) {
            return R.drawable.bg_day_event_light;
        }
        return R.drawable.bg_day_event_primary;
    }

    private interface EventsLoadCallback {
        void onLoaded(List<Event> events);
    }

    private static class EventTimeParts {
        final int startHour;
        final int startMinute;
        final int durationMinutes;

        EventTimeParts(int startHour, int startMinute, int durationMinutes) {
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.durationMinutes = durationMinutes;
        }
    }
}
