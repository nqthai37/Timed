package com.timed.activities;

import android.accounts.Account;
import android.content.Intent;
import android.app.AlertDialog;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.LayoutInflater;
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


import com.timed.CalendarView.DayView;
import com.timed.CalendarView.MonthView;
import com.timed.CalendarView.ThreeDaysView;
import com.timed.CalendarView.WeekView;
import com.timed.R;
import com.timed.adapters.CalendarAdapter;
import com.timed.adapters.CalendarDrawerAdapter;
import com.timed.adapters.ColorPickerAdapter;
import com.timed.adapters.EventAdapter;
import com.timed.adapters.HorizontalCalendarAdapter;
import com.timed.adapters.WeekEventAdapter;
import com.timed.managers.AccountActionManager;
import com.timed.managers.CalendarDialogManager;
import com.timed.managers.EventSyncManager;
import com.timed.models.CalendarDay;
import com.timed.models.CalendarModel;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.timed.activities.CreateEventActivity;
import com.timed.activities.FeaturesActivity;
import com.timed.activities.SearchFilterActivity;
import com.timed.activities.SettingsActivity;
import com.timed.Auth.LoginActivity;
import com.timed.managers.CalendarColorManager;
import com.timed.managers.EventsManager;
import com.timed.managers.InvitationManager;
import com.timed.models.Event;
import com.timed.models.User;
import com.timed.models.Invitation;
import com.timed.managers.UserManager;
import com.timed.repositories.AuthRepository;
import com.timed.repositories.UserRepository;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.repositories.RepositoryCallback;
import com.timed.utils.CalendarIntegrationService;
import com.timed.utils.FirebaseAuthManager;
import com.timed.utils.FirebaseHelper;
import com.timed.utils.FirebaseInitializer;
import com.timed.utils.InvitationService;
import com.timed.dialogs.InvitationsDialog;
import com.timed.dialogs.ShareCalendarDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.bumptech.glide.Glide;
import com.timed.utils.TimelineRenderer;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int MENU_ID_ADD_CALENDAR = 0x70001;

    private EventSyncManager eventSyncManager;

    private LocalDate selectedDate;
    private LocalDate startDate3Days;
    private LocalDate selectedWeekDate;

    private TextView tvTopTitle;

    private View layoutFabMenuOverlay;
    private View fabOptionEvent, fabOptionTask, fabOptionReminder;
    private boolean isFabMenuOpen = false;

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
    private final Map<String, CalendarModel> calendarsById = new HashMap<>();
    private final Map<String, String> ownerNameCache = new HashMap<>();
    private CalendarDrawerAdapter calendarDrawerAdapter;

    private EventsManager eventsManager;
    private InvitationManager invitationManager;
    private InvitationService invitationService;
    private android.app.Dialog currentInvitationsDialog;
    private FirebaseAuth firebaseAuth;

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
        
        // Khởi tạo Invitation Manager và Service
        invitationManager = InvitationManager.getInstance(this);
        invitationService = new InvitationService(this);
        firebaseAuth = FirebaseAuth.getInstance();
        eventSyncManager = new EventSyncManager(this);
        
        // Tải số lượng lời mời khi app mở
        loadInvitationCount();

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

        tvTopTitle = findViewById(R.id.tvTopTitle);

        ImageButton btnSearch = findViewById(R.id.btnSearch);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));
        }

        ImageButton btnInvitations = findViewById(R.id.btnInvitations);
        if (btnInvitations != null) {
            btnInvitations.setOnClickListener(v -> showPendingInvitations());
        }

        ImageButton btnShareCalendar = findViewById(R.id.btnShareCalendar);
        if (btnShareCalendar != null) {
            btnShareCalendar.setOnClickListener(v -> showShareCalendarDialog());
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

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            TabLayout.Tab monthTab = tabLayout.getTabAt(3);
            if (monthTab != null)
                monthTab.select();

            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {

                    if (tab.getPosition() == 0) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new DayView(selectedDate, getVisibleCalendarIds()))
                                .commit();
                    } else if (tab.getPosition() == 1) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new ThreeDaysView(startDate3Days, getVisibleCalendarIds()))
                                .commit();
                    } else if (tab.getPosition() == 2) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new WeekView(selectedWeekDate, getVisibleCalendarIds()))
                                .commit();
                    } else if (tab.getPosition() == 3) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new MonthView(selectedDate, getVisibleCalendarIds()))
                                .commit();
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
                Intent intent = new Intent(this, CreateTaskActivity.class);
                intent.putExtra("calendarId", getActiveCalendarId());
                startActivity(intent);
            });

            findViewById(R.id.btnFabReminder).setOnClickListener(v -> {
                toggleFabMenu();
                AccountActionManager.showCreateReminderDialog(this, title -> createQuickReminder(title));
            });
        }
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
        // Invalidate timezone cache so we pick up any changes from TimezoneSettingActivity
        TimezoneHelper.invalidateCache();

        // 2. Cập nhật số lượng lời mời
        loadInvitationCount();

        // 3. ÉP LỊCH CHÍNH (BÊN TRÊN) TẢI LẠI NGAY LẬP TỨC TỪ BỘ NHỚ OFFLINE
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            int selectedTab = tabLayout.getSelectedTabPosition();
            if (selectedTab == 0) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new DayView(selectedDate, getVisibleCalendarIds()))
                        .commit();
            } else if (selectedTab == 1) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ThreeDaysView(startDate3Days, getVisibleCalendarIds()))
                        .commit();         // Đang ở Tab 3 Ngày
            } else if (selectedTab == 2) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new WeekView(selectedWeekDate, getVisibleCalendarIds()))
                        .commit();           // Đang ở Tab Tuần
            } else if (selectedTab == 3) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new MonthView(selectedDate, getVisibleCalendarIds()))
                        .commit();            // Đang ở Tab Tháng
            }
        }
    }

    private void showProfileMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Change avatar");
        menu.getMenu().add("Sign out");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if ("Change avatar".equals(title)) {
                String currentUrl = "";
                User currentUser = UserManager.getInstance().getCurrentUser();

                if (currentUser != null && currentUser.getAvatar() != null) {
                    currentUrl = currentUser.getAvatar();
                }

                AccountActionManager.showChangeAvatarDialog(this, currentUrl, url -> updateAvatarUrl(url));
                return true;
            }
            if ("Sign out".equals(title)) {
                AccountActionManager.showSignOutDialog(this, () -> handleSignOut());
                return true;
            }
            return false;
        });
        menu.show();
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
            cacheOwnerName(currentUser);
            updateDrawerHeader(currentUser);
            if (currentUser.getAvatar() != null && Patterns.WEB_URL.matcher(currentUser.getAvatar()).matches()) {
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
                        cacheOwnerName(user);
                        if (user.getAvatar() != null && Patterns.WEB_URL.matcher(user.getAvatar()).matches()) {
                            loadAvatar(user.getAvatar());
                        }
                        updateDrawerHeader(user);
                        if (!calendarsById.isEmpty()) {
                            updateDrawerCalendars(new ArrayList<>(calendarsById.values()));
                        }
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

    private void createQuickReminder(String title) {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1); // Reminder for 1 hour from now

        // Create as a Task with reminder
        ArrayList<com.timed.models.Task.TaskReminder> taskReminders = new ArrayList<>();
        taskReminders.add(new com.timed.models.Task.TaskReminder("popup", 0));

        com.timed.models.Task task = new com.timed.models.Task(
                title,
                "",
                new com.google.firebase.Timestamp(calendar.getTime()),
                false,
                "High",
                userId,
                "default_list",
                taskReminders
        );

        com.timed.managers.TasksManager tasksManager = com.timed.managers.TasksManager.getInstance(this);
        tasksManager.createTask(task)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Reminder created!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
        if (!visibleCalendarIds.isEmpty()) {
            defaultCalendarId = visibleCalendarIds.get(0);
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

        List<String> resolvedVisibleIds = calendarIntegrationService.resolveVisibleCalendarIds(this, calendars,
                defaultCalendarId);
        if (resolvedVisibleIds != null) {
            visibleCalendarIds.addAll(resolvedVisibleIds);
        }

        if (visibleCalendarIds.isEmpty() && defaultCalendarId != null && !defaultCalendarId.isEmpty()) {
            visibleCalendarIds.add(defaultCalendarId);
        }

        if (!visibleCalendarIds.isEmpty()) {
            if (defaultCalendarId == null || defaultCalendarId.isEmpty() || !visibleCalendarIds.contains(defaultCalendarId)) {
                defaultCalendarId = visibleCalendarIds.get(0);
                calendarIntegrationService.setCachedDefaultCalendarId(this, defaultCalendarId);
            }
            calendarIntegrationService.saveVisibleCalendarIds(this, visibleCalendarIds);
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
        
        // Set up calendar drawer RecyclerView
        RecyclerView rvCalendarDrawer = header.findViewById(R.id.rvDrawerCalendars);
        if (rvCalendarDrawer != null) {
            rvCalendarDrawer.setLayoutManager(new LinearLayoutManager(this));
            calendarDrawerAdapter = new CalendarDrawerAdapter(
                    new ArrayList<>(calendarsById.values()),
                    visibleCalendarIds,
                    new CalendarDrawerAdapter.OnCalendarActionListener() {
                        @Override
                        public void onCalendarToggle(CalendarModel calendar, boolean isVisible) {
                            toggleCalendarVisibility(calendar.getId());
                        }
                        
                        @Override
                        public void onEditCalendar(CalendarModel calendar) {
                            showEditCalendarDialog(calendar);
                        }
                    });
            rvCalendarDrawer.setAdapter(calendarDrawerAdapter);
        }
        
        // Set up Add Calendar button
        com.google.android.material.button.MaterialButton btnAddCalendar = header.findViewById(R.id.btnAddCalendar);
        if (btnAddCalendar != null) {
            btnAddCalendar.setOnClickListener(v -> {
                showCreateCalendarDialog();
                DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }
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
        // This method is now partially handled by the RecyclerView adapter
        // The adapter manages calendar toggling and editing directly
    }

    private void updateDrawerCalendars(List<CalendarModel> calendars) {
        if (calendars == null) {
            return;
        }

        drawerCalendarIdMap.clear();
        calendarsById.clear();

        List<CalendarModel> sortedCalendars = new ArrayList<>();
        for (CalendarModel calendar : calendars) {
            if (calendar != null && calendar.getId() != null && !calendar.getId().isEmpty()) {
                sortedCalendars.add(calendar);
                calendarsById.put(calendar.getId(), calendar);
            }
        }

        Collections.sort(sortedCalendars, (c1, c2) -> {
            int byOrder = Integer.compare(c1.getSortOrder(), c2.getSortOrder());
            if (byOrder != 0) {
                return byOrder;
            }
            String name1 = c1.getName() != null ? c1.getName() : "";
            String name2 = c2.getName() != null ? c2.getName() : "";
            return name1.compareToIgnoreCase(name2);
        });

        // Update the drawer adapter with new calendars
        if (calendarDrawerAdapter != null) {
            calendarDrawerAdapter.updateData(sortedCalendars, visibleCalendarIds);
        }

        loadCalendarOwnerNames(sortedCalendars, () -> {
            if (calendarDrawerAdapter != null) {
                calendarDrawerAdapter.updateData(sortedCalendars, visibleCalendarIds);
            }
        });
    }

    private void cacheOwnerName(User user) {
        if (user == null) {
            return;
        }
        String userId = user.getUid();
        String name = user.getName();
        if (userId == null || name == null) {
            return;
        }
        String normalized = name.trim();
        if (!normalized.isEmpty()) {
            ownerNameCache.put(userId, normalized);
        }
    }

    private void applyOwnerNameToCalendars(List<CalendarModel> calendars, String ownerId, String ownerName) {
        if (calendars == null || ownerId == null || ownerName == null) {
            return;
        }
        for (CalendarModel calendar : calendars) {
            if (calendar != null && ownerId.equals(calendar.getOwnerId())) {
                calendar.setOwnerName(ownerName);
            }
        }
    }

    private void loadCalendarOwnerNames(List<CalendarModel> calendars, Runnable onComplete) {
        if (calendars == null || calendars.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        cacheOwnerName(UserManager.getInstance().getCurrentUser());

        if (userRepository == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        Set<String> ownerIdsToFetch = new HashSet<>();
        for (CalendarModel calendar : calendars) {
            if (calendar == null) {
                continue;
            }
            String ownerId = calendar.getOwnerId();
            if (ownerId == null || ownerId.isEmpty()) {
                continue;
            }
            String cachedName = ownerNameCache.get(ownerId);
            if (cachedName != null) {
                calendar.setOwnerName(cachedName);
            } else {
                ownerIdsToFetch.add(ownerId);
            }
        }

        if (ownerIdsToFetch.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        AtomicInteger remaining = new AtomicInteger(ownerIdsToFetch.size());
        for (String ownerId : ownerIdsToFetch) {
            userRepository.getUser(ownerId)
                    .addOnSuccessListener(snapshot -> {
                        User owner = snapshot.toObject(User.class);
                        String ownerName = owner != null ? owner.getName() : null;
                        if (ownerName != null) {
                            ownerName = ownerName.trim();
                        }
                        if (ownerName != null && !ownerName.isEmpty()) {
                            ownerNameCache.put(ownerId, ownerName);
                            applyOwnerNameToCalendars(calendars, ownerId, ownerName);
                        }
                        if (remaining.decrementAndGet() == 0 && onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0 && onComplete != null) {
                            onComplete.run();
                        }
                    });
        }
    }

    private void toggleCalendarVisibility(String calendarId) {
        if (calendarId == null || calendarId.isEmpty()) {
            return;
        }

        boolean currentlyVisible = visibleCalendarIds.contains(calendarId);
        if (currentlyVisible && visibleCalendarIds.size() == 1) {
            Toast.makeText(this, "At least one calendar must remain visible", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentlyVisible) {
            visibleCalendarIds.remove(calendarId);
        } else {
            visibleCalendarIds.add(calendarId);
            defaultCalendarId = calendarId;
        }

        if (!visibleCalendarIds.isEmpty() && (defaultCalendarId == null || !visibleCalendarIds.contains(defaultCalendarId))) {
            defaultCalendarId = visibleCalendarIds.get(0);
        }

        if (defaultCalendarId != null && !defaultCalendarId.isEmpty()) {
            calendarIntegrationService.setCachedDefaultCalendarId(this, defaultCalendarId);
        }
        calendarIntegrationService.saveVisibleCalendarIds(this, visibleCalendarIds);

        boolean newVisibility = !currentlyVisible;
        calendarIntegrationService.updateCalendarVisibility(calendarId, newVisibility,
                new CalendarIntegrationService.CalendarSaveListener() {
                    @Override
                    public void onSuccess(String ignoredCalendarId) {
                        Log.d(TAG, "Calendar visibility synced: " + calendarId + " -> " + newVisibility);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.w(TAG, "Failed to sync calendar visibility: " + errorMessage);
                    }
                });

        refreshCalendarViewsForVisibilityChange();
    }

    private void refreshCalendarViewsForVisibilityChange() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            int selectedTab = tabLayout.getSelectedTabPosition();

            if (selectedTab == 0) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new DayView(selectedDate, getVisibleCalendarIds()))
                        .commit();
            } else if (selectedTab == 1) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ThreeDaysView(startDate3Days, getVisibleCalendarIds()))
                        .commit();
            } else if (selectedTab == 2) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new WeekView(selectedWeekDate, getVisibleCalendarIds()))
                        .commit();
            } else if (selectedTab == 3) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new MonthView(selectedDate, getVisibleCalendarIds()))
                        .commit();
            }
        }
    }

    private void setSelectedCalendar(String calendarId) {
        if (calendarId == null || calendarId.isEmpty()) {
            return;
        }
        defaultCalendarId = calendarId;
        calendarIntegrationService.setCachedDefaultCalendarId(this, calendarId);
        if (!visibleCalendarIds.contains(calendarId)) {
            visibleCalendarIds.add(calendarId);
        }
        calendarIntegrationService.saveVisibleCalendarIds(this, visibleCalendarIds);
        refreshCalendarViewsForVisibilityChange();
    }

    private void showCreateCalendarDialog() {
        CalendarDialogManager.showCreateCalendarDialog(this, (name, description) -> {
            if (isDuplicateCalendarName(name)) {
                Toast.makeText(this, "Calendar name already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            showColorPickerForNewCalendar(name, description);
        });
    }

    private void showColorPickerForNewCalendar(String name, String description) {
        List<CalendarColorManager.CalendarColor> colors = calendarIntegrationService.getPresetColors();
        if (colors == null || colors.isEmpty()) {
            createCalendarFromDrawer(name, description, "#2B78E4");
            return;
        }

        String[] labels = new String[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            CalendarColorManager.CalendarColor color = colors.get(i);
            labels[i] = "● " + color.getName() + " (" + color.getHex() + ")";
        }

        final int[] selectedIndex = { 0 };

        new AlertDialog.Builder(this)
                .setTitle("Choose color")
                .setSingleChoiceItems(labels, selectedIndex[0], (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("Create", (dialog, which) -> {
                    CalendarColorManager.CalendarColor selected = colors.get(selectedIndex[0]);
                    createCalendarFromDrawer(name, description, selected.getHex());
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private void createCalendarFromDrawer(String name, String description, String colorHex) {
        calendarIntegrationService.createCalendar(name, description, colorHex, false,
                new CalendarIntegrationService.CalendarSaveListener() {
                    @Override
                    public void onSuccess(String calendarId) {
                        if (calendarId == null || calendarId.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Calendar created", Toast.LENGTH_SHORT).show();
                            ensureDefaultCalendarReady(() -> refreshCalendarViewsForVisibilityChange());
                            return;
                        }

                        defaultCalendarId = calendarId;
                        calendarIntegrationService.setCachedDefaultCalendarId(MainActivity.this, calendarId);

                        if (!visibleCalendarIds.contains(calendarId)) {
                            visibleCalendarIds.add(calendarId);
                        }
                        calendarIntegrationService.saveVisibleCalendarIds(MainActivity.this, visibleCalendarIds);

                        ensureDefaultCalendarReady(() -> {
                            Toast.makeText(MainActivity.this, "Calendar created", Toast.LENGTH_SHORT).show();
                            refreshCalendarViewsForVisibilityChange();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this,
                                "Failed to create calendar: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isDuplicateCalendarName(String candidateName) {
        if (candidateName == null || candidateName.trim().isEmpty()) {
            return false;
        }
        String normalized = candidateName.trim();
        for (CalendarModel calendar : calendarsById.values()) {
            if (calendar == null || calendar.getName() == null) {
                continue;
            }
            if (normalized.equalsIgnoreCase(calendar.getName().trim())) {
                return true;
            }
        }
        return false;
    }

    private void showEditCalendarDialog(CalendarModel calendar) {
        if (calendar == null) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_calendar, null);
        EditText etName = dialogView.findViewById(R.id.etCalendarName);
        EditText etDescription = dialogView.findViewById(R.id.etCalendarDescription);
        RecyclerView rvColorPicker = dialogView.findViewById(R.id.rvColorPicker);

        etName.setText(calendar.getName());
        etDescription.setText(calendar.getDescription());

        List<CalendarColorManager.CalendarColor> colors = calendarIntegrationService.getPresetColors();
        if (colors != null && !colors.isEmpty()) {
            rvColorPicker.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            ColorPickerAdapter colorAdapter = new ColorPickerAdapter(
                    colors,
                    calendar.getColor(),
                    colorHex -> {
                        // Color selection handled internally
                    });
            rvColorPicker.setAdapter(colorAdapter);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Calendar")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", null)
                .create();

        dialog.setOnShowListener(d -> {
            // Save button
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                String newDescription = etDescription.getText().toString().trim();

                if (newName.isEmpty()) {
                    etName.setError("Calendar name is required");
                    etName.requestFocus();
                    return;
                }

                // Check for duplicate only if name changed
                if (!newName.equalsIgnoreCase(calendar.getName())) {
                    if (isDuplicateCalendarName(newName)) {
                        etName.setError("Calendar name already exists");
                        etName.requestFocus();
                        return;
                    }
                }

                // Get selected color from adapter (keep current for now)
                String selectedColor = calendar.getColor();

                // Update in Firestore
                calendarIntegrationService.updateCalendar(calendar.getId(), newName, newDescription, selectedColor, false, new CalendarIntegrationService.CalendarSaveListener() {
                    @Override
                    public void onSuccess(String calendarId) {
                        Toast.makeText(MainActivity.this, "Calendar updated", Toast.LENGTH_SHORT).show();
                        updateDrawerCalendars(new ArrayList<>(calendarsById.values()));
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this,
                                "Failed to update calendar: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            });

            // Delete button
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Calendar")
                        .setMessage("Are you sure you want to delete this calendar? This cannot be undone.")
                        .setPositiveButton("Delete", (deleteDialog, which) -> {
                            calendarIntegrationService.deleteCalendar(calendar.getId(),
                                    new CalendarIntegrationService.CalendarSaveListener() {
                                        @Override
                                        public void onSuccess(String calendarId) {
                                            Toast.makeText(MainActivity.this, "Calendar deleted", Toast.LENGTH_SHORT).show();
                                            visibleCalendarIds.remove(calendarId);
                                            calendarIntegrationService.saveVisibleCalendarIds(MainActivity.this, visibleCalendarIds);
                                            updateDrawerCalendars(new ArrayList<>(calendarsById.values()));
                                            dialog.dismiss();
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            Toast.makeText(MainActivity.this,
                                                    "Failed to delete calendar: " + errorMessage,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });

        dialog.show();
    }

    private LocalDate toLocalDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        java.time.ZoneId userZone = TimezoneHelper.getSelectedZoneId(this);
        return timestamp.toDate().toInstant().atZone(userZone).toLocalDate();
    }

    // ======================== INVITATION FEATURES ========================
    
    private MenuItem invitationsMenuItem;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        
        // Thêm menu item cho lời mời
        invitationsMenuItem = menu.add("Lời mời");
        invitationsMenuItem.setIcon(android.R.drawable.ic_dialog_email);
        invitationsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        invitationsMenuItem.setOnMenuItemClickListener(item -> {
            showPendingInvitations();
            return true;
        });
        
        MenuItem shareCalendarItem = menu.add("Chia sẻ lịch");
        shareCalendarItem.setIcon(android.R.drawable.ic_menu_share);
        shareCalendarItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        shareCalendarItem.setOnMenuItemClickListener(item -> {
            showShareCalendarDialog();
            return true;
        });
        
        // Load invitation count to show badge
        loadInvitationCount();
        
        return true;
    }

    private void loadInvitationCount() {
        if (firebaseAuth.getCurrentUser() == null) return;
        
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        invitationService.getPendingInvitationCount(currentUserId,
            new InvitationService.InvitationCountListener() {
                @Override
                public void onCountLoaded(int count) {
                    // Display badge on menu item by updating title
                    if (invitationsMenuItem != null) {
                        if (count > 0) {
                            // Show count indicator in menu item title
                            invitationsMenuItem.setTitle("Lời mời (" + count + ")");
                            Log.d(TAG, "Badge set: " + count + " invitations");
                        } else {
                            invitationsMenuItem.setTitle("Lời mời");
                        }
                    }
                    Log.d(TAG, "Bạn có " + count + " lời mời");
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Lỗi tải số lời mời: " + error);
                }
            });
    }

    private void showShareCalendarDialog() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        ShareCalendarDialog.createShareCalendarDialog(this,
            new ShareCalendarDialog.OnShareListener() {
                @Override
                public void onShare(String email, String role, String message) {
                    shareCalendarWithUser(email, role, message);
                }

                @Override
                public void onCancel() {
                    Toast.makeText(MainActivity.this, "Đã hủy chia sẻ", 
                        Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    private void shareCalendarWithUser(String toUserEmail, String role, String message) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String calendarId = defaultCalendarId;
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String currentUserEmail = firebaseAuth.getCurrentUser().getEmail();
        String currentUserName = firebaseAuth.getCurrentUser().getDisplayName();

        // Cần convert email sang userId
        convertEmailToUserId(toUserEmail, userId -> {
            if (userId == null) {
                Toast.makeText(MainActivity.this, 
                    "Không tìm thấy người dùng với email này", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            invitationManager.inviteToCalendar(
                calendarId, toUserEmail, userId, role, message,
                currentUserId, currentUserName, currentUserEmail,
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(MainActivity.this, 
                            "Lời mời đã được gửi!", 
                            Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(MainActivity.this, 
                            "Lỗi: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
        });
    }

    private void showPendingInvitations() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        
        invitationService.loadPendingInvitations(currentUserId,
            new InvitationService.InvitationLoadListener() {
                @Override
                public void onInvitationsLoaded(List<Invitation> invitations) {
                    if (invitations.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Không có lời mời nào", 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentInvitationsDialog = InvitationsDialog.createInvitationsListDialog(MainActivity.this, 
                        invitations,
                        new InvitationsDialog.OnInvitationActionListener() {
                            @Override
                            public void onAccept(Invitation invitation) {
                                handleAcceptInvitation(invitation);
                            }

                            @Override
                            public void onDecline(Invitation invitation) {
                                handleDeclineInvitation(invitation);
                            }
                        });
                    currentInvitationsDialog.show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, 
                        "Lỗi: " + error, 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void handleAcceptInvitation(Invitation invitation) {
        String invitationType = invitation.getInvitationType();
        
        if ("calendar".equals(invitationType)) {
            acceptCalendarInvitation(invitation);
        } else if ("event".equals(invitationType)) {
            acceptEventInvitation(invitation);
        }
    }

    private void acceptCalendarInvitation(Invitation invitation) {
        if (firebaseAuth.getCurrentUser() == null) return;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        
        invitationManager.acceptCalendarInvitation(
            invitation.getId(), currentUserId,
            new RepositoryCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Toast.makeText(MainActivity.this, 
                        "Đã chấp nhận lời mời!", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Close the invitations dialog
                    if (currentInvitationsDialog != null && currentInvitationsDialog.isShowing()) {
                        currentInvitationsDialog.dismiss();
                    }
                    
                    // Refresh calendar list to show the new joined calendar
                    ensureDefaultCalendarReady(() -> {
                        Log.d(TAG, "Calendar drawer refreshed after accepting invitation");
                    });
                    
                    // Refresh invitation count
                    loadInvitationCount();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(MainActivity.this, 
                        "Lỗi: " + errorMessage, 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void acceptEventInvitation(Invitation invitation) {
        if (firebaseAuth.getCurrentUser() == null) return;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        
        invitationManager.respondToEventInvitation(
            invitation.getId(), currentUserId, "accepted",
            new RepositoryCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Toast.makeText(MainActivity.this, 
                        "Bạn sẽ tham dự sự kiện!", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Close the invitations dialog
                    if (currentInvitationsDialog != null && currentInvitationsDialog.isShowing()) {
                        currentInvitationsDialog.dismiss();
                    }
                    
                    // Refresh events to show the accepted event
                    refreshCalendarViewsForVisibilityChange();
                    
                    // Refresh invitation count
                    loadInvitationCount();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(MainActivity.this, 
                        "Lỗi: " + errorMessage, 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void handleDeclineInvitation(Invitation invitation) {
        String invitationType = invitation.getInvitationType();
        
        if ("calendar".equals(invitationType)) {
            invitationManager.declineCalendarInvitation(
                invitation.getId(),
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(MainActivity.this, 
                            "Đã từ chối lời mời", 
                            Toast.LENGTH_SHORT).show();
                        
                        // Close the invitations dialog
                        if (currentInvitationsDialog != null && currentInvitationsDialog.isShowing()) {
                            currentInvitationsDialog.dismiss();
                        }
                        
                        // Refresh invitation count
                        loadInvitationCount();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(MainActivity.this, 
                            "Lỗi: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
        } else if ("event".equals(invitationType)) {
            invitationManager.declineEventInvitation(
                invitation.getId(),
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(MainActivity.this, 
                            "Đã từ chối lời mời", 
                            Toast.LENGTH_SHORT).show();
                        
                        // Close the invitations dialog
                        if (currentInvitationsDialog != null && currentInvitationsDialog.isShowing()) {
                            currentInvitationsDialog.dismiss();
                        }
                        
                        // Refresh invitation count
                        loadInvitationCount();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(MainActivity.this, 
                            "Lỗi: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void convertEmailToUserId(String email, EmailToUserIdCallback callback) {
        if (email == null || email.isEmpty()) {
            Log.e(TAG, "Email trống");
            Toast.makeText(this, "Email không được để trống", Toast.LENGTH_SHORT).show();
            callback.onResult(null);
            return;
        }
        
        // Normalize email: convert to lowercase and trim whitespace
        String normalizedEmail = email.toLowerCase().trim();
        Log.d(TAG, "Tìm kiếm email: " + normalizedEmail);
        
        UserRepository userRepository = new UserRepository();
        
        userRepository.getUserByEmail(normalizedEmail)
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Query success. Số kết quả: " + querySnapshot.size());
                    
                    if (querySnapshot.isEmpty()) {
                        Log.w(TAG, "Không tìm thấy người dùng với email: " + normalizedEmail);
                        
                        // Try alternative: search with different case variations
                        tryAlternativeEmailSearch(normalizedEmail, callback);
                        return;
                    }
                    
                    String userId = querySnapshot.getDocuments().get(0).getId();
                    Log.d(TAG, "Tìm thấy userId: " + userId);
                    callback.onResult(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi query Firestore: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "Lỗi tìm kiếm: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    callback.onResult(null);
                });
    }
    
    private void tryAlternativeEmailSearch(String email, EmailToUserIdCallback callback) {
        // If exact match fails, try searching all users and comparing emails
        Log.d(TAG, "Cố gắng tìm kiếm thay thế...");
        
        UserRepository userRepository = new UserRepository();
        // This is a workaround - get all users and check manually
        // In production, consider using Cloud Functions for better security
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Tổng số người dùng: " + querySnapshot.size());
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        com.timed.models.User user = doc.toObject(com.timed.models.User.class);
                        if (user != null && user.getEmail() != null) {
                            String userEmail = user.getEmail().toLowerCase().trim();
                            Log.d(TAG, "So sánh: " + userEmail + " với " + email);
                            if (userEmail.equals(email)) {
                                Log.d(TAG, "Tìm thấy người dùng: " + doc.getId());
                                callback.onResult(doc.getId());
                                return;
                            }
                        }
                    }
                    
                    Log.w(TAG, "Không tìm thấy người dùng trong danh sách");
                    callback.onResult(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tìm kiếm thay thế: " + e.getMessage(), e);
                    callback.onResult(null);
                });
    }

    /**
     * Test function để debug email lookup - gọi từ Console hoặc button
     */
    public void debugEmailLookup(String email) {
        Log.d(TAG, "=== DEBUG EMAIL LOOKUP ===");
        Log.d(TAG, "Email cần tìm: " + email);
        
        convertEmailToUserId(email, userId -> {
            if (userId != null) {
                Log.d(TAG, "✓ Tìm thấy userId: " + userId);
                Toast.makeText(this, "✓ Tìm thấy: " + userId, Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "✗ Không tìm thấy userId");
                Toast.makeText(this, "✗ Không tìm thấy email này", Toast.LENGTH_SHORT).show();
            }
        });
    }

    interface EmailToUserIdCallback {
        void onResult(String userId);
    }
}
