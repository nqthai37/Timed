package com.timed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.timed.R;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.managers.InvitationManager;
import com.timed.managers.MainCalendarDrawerController;
import com.timed.managers.MainCalendarTabController;
import com.timed.managers.MainFabController;
import com.timed.managers.MainInvitationController;
import com.timed.managers.MainProfileController;
import com.timed.repositories.CalendarOwnerRepository;
import com.timed.repositories.UserRepository;
import com.timed.utils.CalendarIntegrationService;
import com.timed.utils.InvitationService;

import java.time.LocalDate;
import java.util.List;

public class MainActivity extends BaseBottomNavActivity {

    private static final String TAG = "MainActivity";

    private MainCalendarTabController calendarTabController;
    private MainCalendarDrawerController calendarDrawerController;
    private MainFabController fabController;
    private MainInvitationController invitationController;
    private MainProfileController profileController;

    private LocalDate selectedDate;
    private LocalDate startDate3Days;
    private LocalDate selectedWeekDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        CalendarIntegrationService calendarIntegrationService = new CalendarIntegrationService();
        UserRepository userRepository = new UserRepository();
        CalendarOwnerRepository calendarOwnerRepository = new CalendarOwnerRepository(userRepository);
        InvitationManager invitationManager = InvitationManager.getInstance(this);
        InvitationService invitationService = new InvitationService(this);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        calendarDrawerController = new MainCalendarDrawerController(this, calendarIntegrationService,
                calendarOwnerRepository, this::refreshCalendarViewsForVisibilityChange);
        invitationController = new MainInvitationController(this, firebaseAuth, invitationManager,
                invitationService, userRepository, new MainInvitationController.CalendarRefreshCallbacks() {
                    @Override
                    public String getCalendarId() {
                        return getActiveCalendarId();
                    }

                    @Override
                    public void onCalendarInvitationAccepted() {
                        ensureDefaultCalendarReady(() -> Log.d(TAG,
                                "Calendar drawer refreshed after accepting invitation"));
                    }

                    @Override
                    public void onEventInvitationAccepted() {
                        refreshCalendarViewsForVisibilityChange();
                    }
                });

        setupInsets();
        setupDrawer();
        setupTopBar();
        setupProfile(userRepository, calendarOwnerRepository);
        setupCalendarTabs();
        setupFabMenu();
        setupBottomNavigation();
        anchorFabAboveBottomNavigation(R.id.fabAddEvent, R.id.fabCloseMenu);

        invitationController.loadInvitationCount();
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.nav_schedule;
    }

    @Override
    protected void onResume() {
        super.onResume();
        TimezoneHelper.invalidateCache();
        if (invitationController != null) {
            invitationController.loadInvitationCount();
        }
        refreshCalendarViewsForVisibilityChange();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (invitationController != null) {
            invitationController.addMenuItems(menu);
        }
        return true;
    }

    private void setupInsets() {
        View mainContent = findViewById(R.id.mainContent);
        if (mainContent == null) {
            return;
        }
        final int baseLeft = mainContent.getPaddingLeft();
        final int baseTop = mainContent.getPaddingTop();
        final int baseRight = mainContent.getPaddingRight();
        final int baseBottom = mainContent.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseLeft + systemBars.left, baseTop + systemBars.top,
                    baseRight + systemBars.right, baseBottom);
            return insets;
        });
    }

    private void setupDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null && drawerLayout != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        NavigationView navView = findViewById(R.id.navView);
        calendarDrawerController.setup(navView, drawerLayout);
        ensureDefaultCalendarReady(() -> {
        });
    }

    private void setupTopBar() {
        ImageButton btnSearch = findViewById(R.id.btnSearch);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));
        }

        ImageButton btnInvitations = findViewById(R.id.btnInvitations);
        if (btnInvitations != null) {
            btnInvitations.setOnClickListener(v -> invitationController.showPendingInvitations());
        }
        TextView tvInvitationCount = findViewById(R.id.tvInvitationCount);
        if (tvInvitationCount != null) {
            invitationController.attachCountView(tvInvitationCount);
        }

        ImageButton btnShareCalendar = findViewById(R.id.btnShareCalendar);
        if (btnShareCalendar != null) {
            btnShareCalendar.setOnClickListener(v -> invitationController.showShareCalendarDialog());
        }
    }

    private void setupProfile(UserRepository userRepository, CalendarOwnerRepository calendarOwnerRepository) {
        ImageView imgProfile = findViewById(R.id.imgProfile);
        profileController = new MainProfileController(this, imgProfile, userRepository,
                calendarOwnerRepository, user -> calendarDrawerController.updateDrawerHeader(user));
        profileController.setup();
        profileController.refreshProfileAvatar();
    }

    private void setupCalendarTabs() {
        selectedDate = LocalDate.now();
        startDate3Days = LocalDate.now();
        selectedWeekDate = LocalDate.now();

        calendarTabController = new MainCalendarTabController(this,
                new MainCalendarTabController.CalendarStateProvider() {
                    @Override
                    public LocalDate getSelectedDate() {
                        return selectedDate;
                    }

                    @Override
                    public LocalDate getStartDate3Days() {
                        return startDate3Days;
                    }

                    @Override
                    public LocalDate getSelectedWeekDate() {
                        return selectedWeekDate;
                    }

                    @Override
                    public List<String> getVisibleCalendarIds() {
                        return MainActivity.this.getVisibleCalendarIds();
                    }
                });

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        calendarTabController.setup(tabLayout);
    }

    private void setupFabMenu() {
        fabController = new MainFabController(this, this::getActiveCalendarId);
        fabController.setup();
    }

    private String getActiveCalendarId() {
        return calendarDrawerController != null ? calendarDrawerController.getActiveCalendarId() : null;
    }

    private void ensureDefaultCalendarReady(Runnable onReady) {
        if (calendarDrawerController != null) {
            calendarDrawerController.ensureDefaultCalendarReady(onReady);
        }
    }

    private List<String> getVisibleCalendarIds() {
        return calendarDrawerController != null ? calendarDrawerController.getVisibleCalendarIds()
                : java.util.Collections.emptyList();
    }

    private void refreshCalendarViewsForVisibilityChange() {
        if (calendarTabController != null) {
            calendarTabController.refresh(findViewById(R.id.tabLayout));
        }
    }
}
