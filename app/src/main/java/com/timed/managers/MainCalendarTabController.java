package com.timed.managers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.timed.CalendarView.DayView;
import com.timed.CalendarView.MonthView;
import com.timed.CalendarView.ThreeDaysView;
import com.timed.CalendarView.WeekView;
import com.timed.R;

import java.time.LocalDate;
import java.util.List;

public class MainCalendarTabController {
    public interface CalendarStateProvider {
        LocalDate getSelectedDate();

        LocalDate getStartDate3Days();

        LocalDate getSelectedWeekDate();

        List<String> getVisibleCalendarIds();
    }

    private final AppCompatActivity activity;
    private final CalendarStateProvider stateProvider;

    public MainCalendarTabController(@NonNull AppCompatActivity activity,
            @NonNull CalendarStateProvider stateProvider) {
        this.activity = activity;
        this.stateProvider = stateProvider;
    }

    public void setup(TabLayout tabLayout) {
        if (tabLayout == null) {
            return;
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        TabLayout.Tab monthTab = tabLayout.getTabAt(3);
        if (monthTab != null) {
            monthTab.select();
        } else {
            showTab(tabLayout.getSelectedTabPosition());
        }
    }

    public void refresh(TabLayout tabLayout) {
        if (tabLayout == null) {
            return;
        }
        showTab(tabLayout.getSelectedTabPosition());
    }

    private void showTab(int position) {
        Fragment fragment = createFragment(position);
        if (fragment == null) {
            return;
        }
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private Fragment createFragment(int position) {
        List<String> visibleCalendarIds = stateProvider.getVisibleCalendarIds();
        if (position == 0) {
            return new DayView(stateProvider.getSelectedDate(), visibleCalendarIds);
        }
        if (position == 1) {
            return new ThreeDaysView(stateProvider.getStartDate3Days(), visibleCalendarIds);
        }
        if (position == 2) {
            return new WeekView(stateProvider.getSelectedWeekDate(), visibleCalendarIds);
        }
        if (position == 3) {
            return new MonthView(stateProvider.getSelectedDate(), visibleCalendarIds);
        }
        return null;
    }
}
