package com.timed.activities;

import android.content.Intent;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.util.Patterns;
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
import com.timed.models.CalendarModel;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.timed.activities.CreateEventActivity;
import com.timed.activities.FeaturesActivity;
import com.timed.activities.SearchFilterActivity;
import com.timed.activities.SettingsActivity;
import com.timed.Auth.LoginActivity;
import com.timed.managers.EventsManager;
import com.timed.models.Event;
import com.timed.models.User;
import com.timed.managers.UserManager;
import com.timed.repositories.AuthRepository;
import com.timed.repositories.UserRepository;
import com.timed.utils.CalendarIntegrationService;
import com.timed.utils.FirebaseAuthManager;
import com.timed.utils.FirebaseHelper;
import com.timed.utils.FirebaseInitializer;
import com.bumptech.glide.Glide;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

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
    private CalendarIntegrationService calendarIntegrationService;
    private String defaultCalendarId;
    private final List<String> visibleCalendarIds = new ArrayList<>();

    private ImageView imgProfile;
    private UserRepository userRepository;
    private NavigationView navView;
    private TextView tvDrawerName;
    private final Map<Integer, String> drawerCalendarIdMap = new HashMap<>();

    private EventsManager eventsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase
        initializeFirebase();
        eventsManager = EventsManager.getInstance(this);
        calendarIntegrationService = new CalendarIntegrationService();
        defaultCalendarId = calendarIntegrationService.getCachedDefaultCalendarId(this);

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

        navView = findViewById(R.id.navView);
        setupDrawerHeader();
        setupDrawerSelection(drawerLayout);
        ensureDefaultCalendarReady(() -> {
        });

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

        imgProfile = findViewById(R.id.imgProfile);
        if (imgProfile != null) {
            imgProfile.setOnClickListener(v -> showProfileMenu(v));
        }
        userRepository = new UserRepository();
        refreshProfileAvatar();

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
                    intent.putExtra("calendarId", getActiveCalendarId());
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
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_schedule) {
                return true;
            }

            if (itemId == R.id.nav_settings) {
                // Lưu ý: Bạn đang dùng 2 class Settings khác nhau trong code (SettingsActivity và SettingActivity)
                // Hãy kiểm tra chính xác class bạn muốn mở
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }

            if (itemId == R.id.nav_features) {
                Intent intent = new Intent(this, FeaturesActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateEventsForDate(selectedDate);

        // Chỉ cập nhật trạng thái lựa chọn của BottomNav, không set lại Listener
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_schedule);
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
        loadMonthEventIndicators(selectedDate, daysInMonth, adapter);
    }

    private void loadMonthEventIndicators(LocalDate month, List<CalendarDay> days, CalendarAdapter adapter) {
        List<String> calendarIds = getVisibleCalendarIds();
        if (calendarIds.isEmpty()) {
            ensureDefaultCalendarReady(() -> loadMonthEventIndicators(month, days, adapter));
            return;
        }

        LocalDate startOfMonth = month.withDayOfMonth(1);
        LocalDate endOfMonth = month.withDayOfMonth(month.lengthOfMonth());

        fetchEventsForCalendars(startOfMonth, endOfMonth, calendarIds, events -> {
            Map<LocalDate, Integer> counts = new HashMap<>();
            for (Event event : events) {
                if (event == null || event.getStartTime() == null) {
                    continue;
                }
                LocalDate eventDate = toLocalDate(event.getStartTime());
                if (eventDate == null) {
                    continue;
                }
                counts.put(eventDate, counts.getOrDefault(eventDate, 0) + 1);
            }

            for (CalendarDay day : days) {
                if (day != null && day.date != null) {
                    day.eventCount = counts.getOrDefault(day.date, 0);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showProfileMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Change avatar");
        menu.getMenu().add("Sign out");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if ("Change avatar".equals(title)) {
                showChangeAvatarDialog();
                return true;
            }
            if ("Sign out".equals(title)) {
                showSignOutDialog();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showChangeAvatarDialog() {
        EditText input = new EditText(this);
        input.setHint("https://...");
        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getAvatar() != null) {
            input.setText(currentUser.getAvatar());
        }

        new AlertDialog.Builder(this)
                .setTitle("Update avatar")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!isValidAvatarUrl(url)) {
                        Toast.makeText(this, "Invalid image URL", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateAvatarUrl(url);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateAvatarUrl(String url) {
        String userId = firebaseInitializer.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("avatar", url);
        userRepository.updateUser(userId, updates)
                .addOnSuccessListener(unused -> {
                    User currentUser = UserManager.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        currentUser.setAvatar(url);
                    }
                    loadAvatar(url);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to update avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void refreshProfileAvatar() {
        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            updateDrawerHeader(currentUser);
            if (isValidAvatarUrl(currentUser.getAvatar())) {
                loadAvatar(currentUser.getAvatar());
                return;
            }
        }

        String userId = firebaseInitializer.getCurrentUserId();
        if (userId == null) {
            return;
        }

        userRepository.getUser(userId)
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        UserManager.getInstance().setCurrentUser(user);
                        if (isValidAvatarUrl(user.getAvatar())) {
                            loadAvatar(user.getAvatar());
                        }
                        updateDrawerHeader(user);
                    }
                });
    }

    private void loadAvatar(String url) {
        if (imgProfile == null) {
            return;
        }
        Glide.with(this)
                .load(url)
                .circleCrop()
                .error(R.drawable.ic_account_circle)
                .into(imgProfile);
    }

    private boolean isValidAvatarUrl(String url) {
        return url != null && !url.isEmpty() && Patterns.WEB_URL.matcher(url).matches();
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign out")
                .setMessage("Do you want to sign out?")
                .setPositiveButton("Sign out", (dialog, which) -> handleSignOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleSignOut() {
        new AuthRepository().logout();
        getSharedPreferences("TimedAppPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("REMEMBER_ME", false)
                .apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

        List<String> calendarIds = getVisibleCalendarIds();
        if (calendarIds.isEmpty()) {
            ensureDefaultCalendarReady(() -> updateEventsForDate(date));
            return;
        }
        fetchEventsForCalendars(date, date, calendarIds, events -> {
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
        String fallbackCalendarId = event.getCalendarId();
        if (fallbackCalendarId == null || fallbackCalendarId.isEmpty()) {
            fallbackCalendarId = getActiveCalendarId();
        }
        intent.putExtra("calendarId", fallbackCalendarId);
        intent.putExtra("title", event.getTitle());
        intent.putExtra("description", event.getDescription());
        intent.putExtra("location", event.getLocation());
        intent.putExtra("startTime", event.getStartTime() != null ? event.getStartTime().toDate().getTime() : 0L);
        intent.putExtra("endTime", event.getEndTime() != null ? event.getEndTime().toDate().getTime() : 0L);
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
            Toast.makeText(this, "Lỗi khởi tạo Firebase", Toast.LENGTH_SHORT).show();
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
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Firebase connection failed: " + errorMessage);
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

    private String getActiveCalendarId() {
        if (defaultCalendarId != null && !defaultCalendarId.isEmpty()) {
            return defaultCalendarId;
        }
        if (calendarIntegrationService != null) {
            defaultCalendarId = calendarIntegrationService.getCachedDefaultCalendarId(this);
        }
        return defaultCalendarId;
    }

    private void ensureDefaultCalendarReady(Runnable onReady) {
        if (calendarIntegrationService == null) {
            calendarIntegrationService = new CalendarIntegrationService();
        }
        calendarIntegrationService.ensureDefaultCalendar(this,
                new CalendarIntegrationService.DefaultCalendarListener() {
                    @Override
                    public void onReady(String calendarId, java.util.List<com.timed.models.CalendarModel> calendars) {
                        defaultCalendarId = calendarId;
                        updateVisibleCalendarIds(calendars);
                        updateDrawerCalendars(calendars);
                        if (onReady != null) {
                            onReady.run();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Failed to ensure default calendar: " + errorMessage);
                    }
                });
    }

    private List<String> getVisibleCalendarIds() {
        if (!visibleCalendarIds.isEmpty()) {
            return new ArrayList<>(visibleCalendarIds);
        }
        String calendarId = getActiveCalendarId();
        if (calendarId != null && !calendarId.isEmpty()) {
            List<String> ids = new ArrayList<>();
            ids.add(calendarId);
            return ids;
        }
        return new ArrayList<>();
    }

    private void updateVisibleCalendarIds(List<CalendarModel> calendars) {
        visibleCalendarIds.clear();
        if (defaultCalendarId != null && !defaultCalendarId.isEmpty()) {
            visibleCalendarIds.add(defaultCalendarId);
        }
    }

    private void setupDrawerHeader() {
        if (navView == null) {
            return;
        }
        View header = navView.getHeaderView(0);
        if (header == null) {
            return;
        }
        tvDrawerName = header.findViewById(R.id.tvDrawerName);
        updateDrawerHeader(UserManager.getInstance().getCurrentUser());
    }

    private void updateDrawerHeader(User user) {
        if (user == null) {
            return;
        }
        if (tvDrawerName != null && user.getName() != null) {
            tvDrawerName.setText(user.getName());
        }
        // Drawer header now shows only the username.
    }

    private void setupDrawerSelection(DrawerLayout drawerLayout) {
        if (navView == null) {
            return;
        }
        navView.setNavigationItemSelectedListener(item -> {
            String calendarId = drawerCalendarIdMap.get(item.getItemId());
            if (calendarId == null) {
                return false;
            }
            setSelectedCalendar(calendarId);
            item.setChecked(true);
            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });
    }

    private void updateDrawerCalendars(List<CalendarModel> calendars) {
        if (navView == null || calendars == null) {
            return;
        }
        Menu menu = navView.getMenu();
        menu.clear();
        drawerCalendarIdMap.clear();

        String userId = firebaseInitializer.getCurrentUserId();
        List<CalendarModel> owned = new ArrayList<>();
        List<CalendarModel> others = new ArrayList<>();
        for (CalendarModel calendar : calendars) {
            if (calendar == null || calendar.getId() == null) {
                continue;
            }
            if (userId != null && userId.equals(calendar.getOwnerId())) {
                owned.add(calendar);
            } else {
                others.add(calendar);
            }
        }

        if (!owned.isEmpty()) {
            SubMenu myMenu = menu.addSubMenu("MY CALENDARS");
            addCalendarMenuItems(myMenu, owned);
        }
        if (!others.isEmpty()) {
            SubMenu otherMenu = menu.addSubMenu("OTHER CALENDARS");
            addCalendarMenuItems(otherMenu, others);
        }
    }

    private void addCalendarMenuItems(SubMenu menu, List<CalendarModel> calendars) {
        for (CalendarModel calendar : calendars) {
            String name = calendar.getName() != null ? calendar.getName() : "Calendar";
            int itemId = View.generateViewId();
            MenuItem item = menu.add(Menu.NONE, itemId, Menu.NONE, name);
            item.setCheckable(true);
            if (calendar.getId().equals(defaultCalendarId)) {
                item.setChecked(true);
            }
            drawerCalendarIdMap.put(itemId, calendar.getId());
        }
    }

    private void setSelectedCalendar(String calendarId) {
        if (calendarId == null || calendarId.isEmpty()) {
            return;
        }
        defaultCalendarId = calendarId;
        calendarIntegrationService.setCachedDefaultCalendarId(this, calendarId);
        visibleCalendarIds.clear();
        visibleCalendarIds.add(calendarId);
        setMonthView();
        updateEventsForDate(selectedDate);
    }

    private void fetchEventsForCalendars(LocalDate startDate, LocalDate endDate, List<String> calendarIds,
            EventsLoadCallback callback) {
        if (calendarIds == null || calendarIds.isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return;
        }
        List<Event> merged = new ArrayList<>();
        fetchEventsForCalendarsSequential(startDate, endDate, calendarIds, 0, merged, callback);
    }

    private void fetchEventsForCalendarsSequential(LocalDate startDate, LocalDate endDate, List<String> calendarIds,
            int index, List<Event> merged, EventsLoadCallback callback) {
        if (index >= calendarIds.size()) {
            callback.onLoaded(sortEvents(merged));
            return;
        }

        String calendarId = calendarIds.get(index);
        fetchEventsForRange(startDate, endDate, calendarId, events -> {
            mergeEvents(merged, events);
            fetchEventsForCalendarsSequential(startDate, endDate, calendarIds, index + 1, merged, callback);
        });
    }

    private void mergeEvents(List<Event> target, List<Event> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }

        Map<String, Event> map = new HashMap<>();
        for (Event event : target) {
            if (event != null && event.getId() != null) {
                map.put(event.getId(), event);
            }
        }

        for (Event event : incoming) {
            if (event == null) {
                continue;
            }
            String id = event.getId();
            if (id == null || !map.containsKey(id)) {
                target.add(event);
                if (id != null) {
                    map.put(id, event);
                }
            }
        }
    }

    private List<Event> sortEvents(List<Event> events) {
        if (events == null || events.size() <= 1) {
            return events == null ? new ArrayList<>() : events;
        }

        Collections.sort(events, (a, b) -> {
            if (a == null || a.getStartTime() == null) {
                return -1;
            }
            if (b == null || b.getStartTime() == null) {
                return 1;
            }
            return a.getStartTime().toDate().compareTo(b.getStartTime().toDate());
        });
        return events;
    }

    private void loadDayEvents(LocalDate date) {
        List<String> calendarIds = getVisibleCalendarIds();
        if (calendarIds.isEmpty()) {
            ensureDefaultCalendarReady(() -> loadDayEvents(date));
            return;
        }
        fetchEventsForCalendars(date, date, calendarIds, events -> renderDayViewTimeline(date, events));
    }

    private void loadThreeDaysEvents(LocalDate startDate) {
        List<String> calendarIds = getVisibleCalendarIds();
        if (calendarIds.isEmpty()) {
            ensureDefaultCalendarReady(() -> loadThreeDaysEvents(startDate));
            return;
        }
        fetchEventsForCalendars(startDate, startDate.plusDays(2), calendarIds,
                events -> render3DaysTimeline(startDate, events));
    }

    private void loadWeekEvents(LocalDate startOfWeek) {
        List<String> calendarIds = getVisibleCalendarIds();
        if (calendarIds.isEmpty()) {
            ensureDefaultCalendarReady(() -> loadWeekEvents(startOfWeek));
            return;
        }
        fetchEventsForCalendars(startOfWeek, startOfWeek.plusDays(6), calendarIds,
                events -> renderWeekGridTimeline(startOfWeek, events));
    }

    private void fetchEventsForRange(LocalDate startDate, LocalDate endDate, String calendarId,
            EventsLoadCallback callback) {
        if (eventsManager == null) {
            callback.onLoaded(new ArrayList<>());
            return;
        }

        Timestamp startTimestamp = toTimestamp(
                startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        long endMillis = endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                - 1;
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
