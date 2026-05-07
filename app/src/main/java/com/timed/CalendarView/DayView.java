package com.timed.CalendarView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.adapters.HorizontalCalendarAdapter;
import com.timed.managers.EventSyncManager;
import com.timed.models.Event;
import com.timed.utils.CalendarViewHelper;
import com.timed.utils.TimelineRenderer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class DayView extends Fragment {
    private RecyclerView rvHorizontalCalendar;
    private RelativeLayout timelineContainer;
    private HorizontalCalendarAdapter horizontalAdapter;
    private LocalDate selectedDate;
    private EventSyncManager eventSyncManager;
    private List<String> visibleCalendarIds;

    public DayView(LocalDate selectedDate, List<String> visibleCalendarIds) {
        this.selectedDate = selectedDate;
        this.visibleCalendarIds = visibleCalendarIds;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day, container, false);

        rvHorizontalCalendar = view.findViewById(R.id.rvHorizontalCalendar);
        timelineContainer = view.findViewById(R.id.timelineContainer);

        eventSyncManager = new EventSyncManager(requireContext());

        setupHorizontalCalendar();

        return view;
    }

    private void setupHorizontalCalendar() {
        rvHorizontalCalendar.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        List<LocalDate> horizontalDateList = new ArrayList<>();
        int currentDayOfWeek = selectedDate.getDayOfWeek().getValue();
        int daysToSubtract = (currentDayOfWeek == 7) ? 0 : currentDayOfWeek;
        LocalDate startOfWeek = selectedDate.minusDays(daysToSubtract);

        for (int i = 0; i < 7; i++) {
            horizontalDateList.add(startOfWeek.plusDays(i));
        }

        horizontalAdapter = new HorizontalCalendarAdapter(horizontalDateList, selectedDate, (date, position) -> {
            selectedDate = date;

            loadDayEvents(date);
        });

        rvHorizontalCalendar.setAdapter(horizontalAdapter);
        loadDayEvents(selectedDate);
    }

    private void loadDayEvents(LocalDate date) {
        if (visibleCalendarIds == null || visibleCalendarIds.isEmpty()) return;

        eventSyncManager.fetchEventsForCalendars(date, date, visibleCalendarIds, events -> {
            if (getContext() == null) return;

            timelineContainer.removeAllViews();
            int hourHeightPx = TimelineRenderer.dpToPx(requireContext(), 80);
            timelineContainer.setMinimumHeight(hourHeightPx * 24);

            for (int i = 0; i < 24; i++) {
                TextView tvTime = new TextView(requireContext());
                tvTime.setId(View.generateViewId());
                tvTime.setText(String.format("%02d:00", i));
                tvTime.setTextColor(android.graphics.Color.parseColor("#99741CE9"));
                tvTime.setTextSize(12f);

                RelativeLayout.LayoutParams timeParams = new RelativeLayout.LayoutParams(
                        TimelineRenderer.dpToPx(requireContext(), 56), RelativeLayout.LayoutParams.WRAP_CONTENT);
                timeParams.topMargin = i * hourHeightPx;
                timelineContainer.addView(tvTime, timeParams);

                View line = new View(requireContext());
                line.setBackgroundColor(android.graphics.Color.parseColor("#1A741CE9"));
                RelativeLayout.LayoutParams lineParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, TimelineRenderer.dpToPx(requireContext(), 1));
                lineParams.addRule(RelativeLayout.RIGHT_OF, tvTime.getId());
                lineParams.topMargin = i * hourHeightPx + TimelineRenderer.dpToPx(requireContext(), 8);
                timelineContainer.addView(line, lineParams);
            }

            if (events == null || events.isEmpty()) return;

            int colorIndex = 0;
            for (Event event : events) {
                CalendarViewHelper.EventTimeParts parts = CalendarViewHelper.toTimeParts(requireContext(), event);
                if (parts == null) continue;

                String title = event.getTitle() != null ? event.getTitle() : "(Untitled)";
                String details = CalendarViewHelper.buildEventDetails(requireContext(), event);

                int bgRes = CalendarViewHelper.pickEventBackground(colorIndex++);
                Integer eventTint = CalendarViewHelper.resolveEventTintColor(event);

                boolean useDarkText = eventTint != null && !TimelineRenderer.isDarkColor(eventTint);
                String titleColor = useDarkText ? "#334155" : "#FFFFFF";
                String detailsColor = useDarkText ? "#64748b" : "#E6FFFFFF";

                TimelineRenderer.addEventCardToTimeline(
                        requireContext(),
                        timelineContainer,
                        hourHeightPx,
                        title,
                        details,
                        parts.startHour,
                        parts.startMinute,
                        parts.durationMinutes,
                        bgRes,
                        titleColor,
                        detailsColor,
                        eventTint
                );
            }
        });
    }
}
