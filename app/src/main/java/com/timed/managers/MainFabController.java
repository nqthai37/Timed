package com.timed.managers;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.timed.R;
import com.timed.activities.CreateEventActivity;
import com.timed.activities.CreateTaskActivity;

public class MainFabController {
    public interface CalendarIdProvider {
        String getActiveCalendarId();
    }

    private final Activity activity;
    private final CalendarIdProvider calendarIdProvider;
    private View overlay;
    private View optionEvent;
    private View optionTask;
    private boolean menuOpen;

    public MainFabController(Activity activity, CalendarIdProvider calendarIdProvider) {
        this.activity = activity;
        this.calendarIdProvider = calendarIdProvider;
    }

    public void setup() {
        overlay = activity.findViewById(R.id.layoutFabMenuOverlay);
        optionEvent = activity.findViewById(R.id.fabOptionEvent);
        optionTask = activity.findViewById(R.id.fabOptionTask);

        FloatingActionButton fabMain = activity.findViewById(R.id.fabAddEvent);
        FloatingActionButton fabClose = activity.findViewById(R.id.fabCloseMenu);

        if (fabMain != null) {
            fabMain.setOnClickListener(v -> toggleMenu());
        }
        if (fabClose != null) {
            fabClose.setOnClickListener(v -> toggleMenu());
        }
        if (overlay != null) {
            overlay.setOnClickListener(v -> {
                if (menuOpen) {
                    toggleMenu();
                }
            });
        }

        View btnFabEvent = activity.findViewById(R.id.btnFabEvent);
        if (btnFabEvent != null) {
            btnFabEvent.setOnClickListener(v -> {
                toggleMenu();
                Intent intent = new Intent(activity, CreateEventActivity.class);
                intent.putExtra("calendarId", calendarIdProvider.getActiveCalendarId());
                activity.startActivity(intent);
            });
        }

        View btnFabTask = activity.findViewById(R.id.btnFabTask);
        if (btnFabTask != null) {
            btnFabTask.setOnClickListener(v -> {
                toggleMenu();
                Intent intent = new Intent(activity, CreateTaskActivity.class);
                intent.putExtra("calendarId", calendarIdProvider.getActiveCalendarId());
                activity.startActivity(intent);
            });
        }

    }

    private void toggleMenu() {
        if (overlay == null || optionEvent == null || optionTask == null) {
            return;
        }

        menuOpen = !menuOpen;
        if (menuOpen) {
            overlay.setVisibility(View.VISIBLE);
            overlay.setAlpha(0f);
            overlay.animate().alpha(1f).setDuration(200).start();

            showOption(optionEvent, 0);
            showOption(optionTask, 50);
        } else {
            hideOption(optionTask, 0);
            hideOption(optionEvent, 50);

            overlay.animate().alpha(0f).setDuration(200).setStartDelay(100).withEndAction(() -> {
                overlay.setVisibility(View.GONE);
                optionEvent.setVisibility(View.INVISIBLE);
                optionTask.setVisibility(View.INVISIBLE);
            }).start();
        }
    }

    private void showOption(View option, long delay) {
        option.setTranslationY(100f);
        option.setAlpha(0f);
        option.setVisibility(View.VISIBLE);
        option.animate().translationY(0f).alpha(1f).setDuration(200).setStartDelay(delay).start();
    }

    private void hideOption(View option, long delay) {
        option.animate().translationY(100f).alpha(0f).setDuration(150).setStartDelay(delay).start();
    }

}
