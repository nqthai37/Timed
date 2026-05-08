package com.timed.CalendarView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.timed.R;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.activities.CreateEventActivity;
import com.timed.managers.EventSyncManager;
import com.timed.models.Event;
import com.timed.utils.CalendarViewHelper;
import com.timed.utils.ThemeManager;
import com.timed.utils.TimelineRenderer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class WeekView extends Fragment {

    private LocalDate selectedWeekDate;
    private List<String> visibleCalendarIds;
    private EventSyncManager eventSyncManager;
    private View rootView;

    public WeekView(LocalDate selectedWeekDate, List<String> visibleCalendarIds) {
        this.selectedWeekDate = selectedWeekDate;
        this.visibleCalendarIds = visibleCalendarIds;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_week, container, false);
        eventSyncManager = new EventSyncManager(requireContext());

        setupWeekView();
        return rootView;
    }

    private void setupWeekView() {
        int currentDayOfWeek = selectedWeekDate.getDayOfWeek().getValue();
        int daysToSubtract = (currentDayOfWeek == 7) ? 0 : currentDayOfWeek;
        LocalDate startOfWeek = selectedWeekDate.minusDays(daysToSubtract);

        LinearLayout headerContainer = rootView.findViewById(R.id.layoutWeekDaysHeader);
        if (headerContainer != null) {
            headerContainer.removeAllViews();
            DateTimeFormatter dowFormatter = DateTimeFormatter.ofPattern("E", Locale.ENGLISH);

            for (int i = 0; i < 7; i++) {
                LocalDate day = startOfWeek.plusDays(i);

                LinearLayout dayCol = new LinearLayout(requireContext());
                dayCol.setOrientation(LinearLayout.VERTICAL);
                dayCol.setGravity(android.view.Gravity.CENTER);
                dayCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                dayCol.setPadding(0, TimelineRenderer.dpToPx(requireContext(), 8), 0, TimelineRenderer.dpToPx(requireContext(), 8));

                TextView tvDow = new TextView(requireContext());
                tvDow.setText(day.format(dowFormatter).toUpperCase().substring(0, 1));
                tvDow.setTextSize(10f);
                tvDow.setTextColor(requireContext().getColor(R.color.slate_500));

                TextView tvDate = new TextView(requireContext());
                tvDate.setText(String.valueOf(day.getDayOfMonth()));
                tvDate.setTextSize(14f);
                tvDate.setTypeface(null, android.graphics.Typeface.BOLD);

                int primaryColor = ThemeManager.getPrimaryColor(requireContext());
                if (day.equals(LocalDate.now())) {
                    tvDate.setTextColor(primaryColor);
                    tvDow.setTextColor(primaryColor);
                } else {
                    tvDate.setTextColor(requireContext().getColor(R.color.slate_900));
                }

                dayCol.addView(tvDow);
                dayCol.addView(tvDate);
                headerContainer.addView(dayCol);
            }
        }

        loadWeekEvents(startOfWeek);
    }

    private void loadWeekEvents(LocalDate startOfWeek) {
        if (visibleCalendarIds == null || visibleCalendarIds.isEmpty()) return;

        eventSyncManager.fetchEventsForCalendars(startOfWeek, startOfWeek.plusDays(6), visibleCalendarIds, events -> {
            if (getContext() == null) return;
            renderWeekGridTimeline(startOfWeek, events);
        });
    }

    private void renderWeekGridTimeline(LocalDate startOfWeek, List<Event> events) {
        RelativeLayout container = rootView.findViewById(R.id.timelineWeekContainer);
        if (container == null) return;

        container.post(() -> {
            if (getContext() == null) return;

            container.removeAllViews();
            int hourHeightPx = TimelineRenderer.dpToPx(requireContext(), 60);
            container.setMinimumHeight(hourHeightPx * 24);

            int timeColumnWidth = TimelineRenderer.dpToPx(requireContext(), 40);
            int totalGridWidth = container.getWidth() - timeColumnWidth;
            int colWidth = totalGridWidth / 7;

            for (int i = 0; i < 24; i++) {
                TextView tvTime = new TextView(requireContext());
                tvTime.setId(View.generateViewId());
                tvTime.setText(String.format(Locale.getDefault(), "%02d", i));
                tvTime.setTextColor(requireContext().getColor(R.color.slate_500));
                tvTime.setTextSize(10f);
                tvTime.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

                RelativeLayout.LayoutParams timeParams = new RelativeLayout.LayoutParams(timeColumnWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);
                timeParams.topMargin = i * hourHeightPx + TimelineRenderer.dpToPx(requireContext(), 4);
                container.addView(tvTime, timeParams);

                View line = new View(requireContext());
                line.setBackgroundColor(android.graphics.Color.parseColor("#0D741CE9"));
                RelativeLayout.LayoutParams lineParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, TimelineRenderer.dpToPx(requireContext(), 1));
                lineParams.leftMargin = timeColumnWidth;
                lineParams.topMargin = i * hourHeightPx + TimelineRenderer.dpToPx(requireContext(), 10);
                container.addView(line, lineParams);
            }

            for (int i = 1; i < 7; i++) {
                View verticalLine = new View(requireContext());
                verticalLine.setBackgroundColor(android.graphics.Color.parseColor("#0D741CE9"));
                RelativeLayout.LayoutParams vParams = new RelativeLayout.LayoutParams(TimelineRenderer.dpToPx(requireContext(), 1), RelativeLayout.LayoutParams.MATCH_PARENT);
                vParams.leftMargin = timeColumnWidth + (i * colWidth);
                container.addView(verticalLine, vParams);
            }

            if (events == null || events.isEmpty()) return;

            int colorIndex = 0;
            for (Event event : events) {
                LocalDate eventDate = toLocalDate(event.getStartTime());
                if (eventDate == null) continue;

                int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, eventDate);
                if (dayIndex < 0 || dayIndex > 6) continue;

                CalendarViewHelper.EventTimeParts parts = CalendarViewHelper.toTimeParts(requireContext(), event);
                if (parts == null) continue;

                int bgRes = CalendarViewHelper.pickEventBackground(colorIndex++);
                String title = event.getTitle() != null ? event.getTitle() : "(Untitled)";
                Integer eventTint = CalendarViewHelper.resolveEventTintColor(event);

                TimelineRenderer.addEventToWeekGrid(requireContext(), container, hourHeightPx, timeColumnWidth, colWidth, dayIndex,
                        title, parts.startHour, parts.startMinute, parts.durationMinutes, bgRes, eventTint,
                        v -> openEditEvent(event));
            }
        });
    }

    private void openEditEvent(Event event) {
        String eventId = event == null ? null : event.getInstanceOf();
        if (eventId == null || eventId.isEmpty()) {
            eventId = event == null ? null : event.getId();
        }
        if (eventId == null || eventId.isEmpty()) {
            return;
        }

        Intent intent = new Intent(requireContext(), CreateEventActivity.class);
        intent.putExtra("eventId", eventId);
        startActivity(intent);
    }

    private LocalDate toLocalDate(Timestamp timestamp) {
        if (timestamp == null) return null;
        java.time.ZoneId userZone = TimezoneHelper.getSelectedZoneId(requireContext());
        return timestamp.toDate().toInstant().atZone(userZone).toLocalDate();
    }

}
