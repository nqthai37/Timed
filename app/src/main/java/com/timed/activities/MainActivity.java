package com.timed.activities;

import android.content.Intent;
import android.widget.ImageView;
import android.view.Menu;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;


import com.timed.R;
import com.timed.managers.MainCalendarDrawerController;
import com.timed.managers.MainFabController;
import com.timed.managers.MainInvitationController;
import com.timed.managers.MainBottomNavController;
import com.timed.managers.MainCalendarTabController;
import com.timed.managers.MainProfileController;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.navigation.NavigationView;
import com.timed.activities.SearchFilterActivity;
import com.timed.managers.InvitationManager;
import com.timed.repositories.CalendarOwnerRepository;
import com.timed.repositories.UserRepository;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.utils.CalendarIntegrationService;
import com.timed.utils.InvitationService;
import com.google.firebase.auth.FirebaseAuth;

import java.time.LocalDate;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private MainCalendarTabController calendarTabController;

    private LocalDate selectedDate;
    private LocalDate startDate3Days;
    private LocalDate selectedWeekDate;

    private ImageView imgProfile;
    private UserRepository userRepository;
    private CalendarOwnerRepository calendarOwnerRepository;
    private MainCalendarDrawerController calendarDrawerController;
    private MainFabController fabController;
    private MainInvitationController invitationController;
    private MainProfileController profileController;

    private InvitationManager invitationManager;
    private InvitationService invitationService;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        CalendarIntegrationService calendarIntegrationService = new CalendarIntegrationService();
        
        // Khởi tạo Invitation Manager và Service
        invitationManager = InvitationManager.getInstance(this);
        invitationService = new InvitationService(this);
        firebaseAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        calendarOwnerRepository = new CalendarOwnerRepository(userRepository);
        calendarDrawerController = new MainCalendarDrawerController(this, calendarIntegrationService,
                calendarOwnerRepository, this::refreshCalendarViewsForVisibilityChange);
        invitationController = new MainInvitationController(this, firebaseAuth, invitationManager,
                invitationService, userRepository, new MainInvitationController.CalendarRefreshCallbacks() {
                    @Override
                    public String getCalendarId() {
                        return calendarDrawerController.getActiveCalendarId();
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
        
        // Tải số lượng lời mời khi app mở
        invitationController.loadInvitationCount();

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

        NavigationView navView = findViewById(R.id.navView);
        calendarDrawerController.setup(navView, drawerLayout);
        ensureDefaultCalendarReady(() -> {
        });

        ImageButton btnSearch = findViewById(R.id.btnSearch);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchFilterActivity.class)));
        }

        ImageButton btnInvitations = findViewById(R.id.btnInvitations);
        if (btnInvitations != null) {
            btnInvitations.setOnClickListener(v -> invitationController.showPendingInvitations());
        }

        ImageButton btnShareCalendar = findViewById(R.id.btnShareCalendar);
        if (btnShareCalendar != null) {
            btnShareCalendar.setOnClickListener(v -> invitationController.showShareCalendarDialog());
        }

        imgProfile = findViewById(R.id.imgProfile);
        profileController = new MainProfileController(this, imgProfile, userRepository,
                calendarOwnerRepository, user -> {
                    calendarDrawerController.updateDrawerHeader(user);
                });
        profileController.setup();
        profileController.refreshProfileAvatar();

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

        fabController = new MainFabController(this, this::getActiveCalendarId);
        fabController.setup();
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        MainBottomNavController.setup(this, findViewById(R.id.bottomNav));
    }

    @Override
    protected void onResume() {
        super.onResume();
        TimezoneHelper.invalidateCache();
        invitationController.loadInvitationCount();
        if (calendarTabController != null) {
            calendarTabController.refresh(findViewById(R.id.tabLayout));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        invitationController.addMenuItems(menu);
        return true;
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
