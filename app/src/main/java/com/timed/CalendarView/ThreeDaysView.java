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

import com.google.firebase.Timestamp;
import com.timed.R;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.managers.EventSyncManager;
import com.timed.models.Event;
import com.timed.utils.CalendarViewHelper;
import com.timed.utils.ThemeManager;
import com.timed.utils.TimelineRenderer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ThreeDaysView extends Fragment {

    private LocalDate startDate3Days;
    private List<String> visibleCalendarIds;
    private EventSyncManager eventSyncManager;
    private View rootView;

    public ThreeDaysView(LocalDate startDate3Days, List<String> visibleCalendarIds) {
        this.startDate3Days = startDate3Days;
        this.visibleCalendarIds = visibleCalendarIds;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_three_days, container, false);
        eventSyncManager = new EventSyncManager(requireContext());

        setup3DaysView();
        return rootView;
    }

    private void setup3DaysView() {
        DateTimeFormatter dowFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

        TextView[] dowTvs = { rootView.findViewById(R.id.tv3DaysDow1), rootView.findViewById(R.id.tv3DaysDow2), rootView.findViewById(R.id.tv3DaysDow3) };
        TextView[] dateTvs = { rootView.findViewById(R.id.tv3DaysDate1), rootView.findViewById(R.id.tv3DaysDate2), rootView.findViewById(R.id.tv3DaysDate3) };

        for (int i = 0; i < 3; i++) {
            LocalDate day = startDate3Days.plusDays(i);
            if (dowTvs[i] != null && dateTvs[i] != null) {
                dowTvs[i].setText(day.format(dowFormatter).toUpperCase());
                dateTvs[i].setText(String.valueOf(day.getDayOfMonth()));

                int primaryColor = resolveThemePrimaryColor();
                if (day.equals(LocalDate.now())) {
                    dateTvs[i].setTextColor(primaryColor);
                } else {
                    dateTvs[i].setTextColor(android.graphics.Color.parseColor("#0f172a"));
                }
            }
        }

        loadThreeDaysEvents(startDate3Days);
    }

    private void loadThreeDaysEvents(LocalDate startDate) {
        if (visibleCalendarIds == null || visibleCalendarIds.isEmpty()) return;

        eventSyncManager.fetchEventsForCalendars(startDate, startDate.plusDays(2), visibleCalendarIds, events -> {
            if (getContext() == null) return;
            render3DaysTimeline(startDate, events);
        });
    }

    private void render3DaysTimeline(LocalDate startDate, List<Event> events) {
        RelativeLayout container = rootView.findViewById(R.id.timeline3DaysContainer);
        if (container == null) return;

        container.post(() -> {
            if (getContext() == null) return;

            container.removeAllViews();
            int hourHeightPx = TimelineRenderer.dpToPx(requireContext(), 80);
            container.setMinimumHeight(hourHeightPx * 24);

            int timeColumnWidth = TimelineRenderer.dpToPx(requireContext(), 50);
            int totalGridWidth = container.getWidth() - timeColumnWidth;
            int colWidth = totalGridWidth / 3;

            for (int i = 0; i < 24; i++) {
                TextView tvTime = new TextView(requireContext());
                tvTime.setId(View.generateViewId());
                tvTime.setText(String.format(Locale.getDefault(), "%02d:00", i));
                tvTime.setTextColor(android.graphics.Color.parseColor("#94a3b8"));
                tvTime.setTextSize(10f);
                tvTime.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

                RelativeLayout.LayoutParams timeParams = new RelativeLayout.LayoutParams(timeColumnWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);
                timeParams.topMargin = i * hourHeightPx + TimelineRenderer.dpToPx(requireContext(), 8);
                container.addView(tvTime, timeParams);

                View line = new View(requireContext());
                line.setBackgroundColor(android.graphics.Color.parseColor("#0D741CE9"));
                RelativeLayout.LayoutParams lineParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, TimelineRenderer.dpToPx(requireContext(), 1));
                lineParams.leftMargin = timeColumnWidth;
                lineParams.topMargin = i * hourHeightPx + TimelineRenderer.dpToPx(requireContext(), 16);
                container.addView(line, lineParams);
            }

            for (int i = 1; i < 3; i++) {
                View verticalLine = new View(requireContext());
                verticalLine.setBackgroundColor(android.graphics.Color.parseColor("#0D741CE9"));
                RelativeLayout.LayoutParams vParams = new RelativeLayout.LayoutParams(TimelineRenderer.dpToPx(requireContext(), 1), RelativeLayout.LayoutParams.MATCH_PARENT);
                vParams.leftMargin = timeColumnWidth + (i * colWidth);
                container.addView(verticalLine, vParams);
            }

            if (events == null || events.isEmpty()) return;

            // Draw events
            int colorIndex = 0;
            for (Event event : events) {
                LocalDate eventDate = toLocalDate(event.getStartTime());
                if (eventDate == null) continue;

                int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, eventDate);
                if (dayIndex < 0 || dayIndex > 2) continue;

                CalendarViewHelper.EventTimeParts parts = CalendarViewHelper.toTimeParts(requireContext(), event);
                if (parts == null) continue;

                int bgRes = CalendarViewHelper.pickEventBackground(colorIndex++);
                Integer eventTint = CalendarViewHelper.resolveEventTintColor(event);
                boolean useDarkText = eventTint != null && !TimelineRenderer.isDarkColor(eventTint);
                String titleColor = useDarkText ? "#334155" : "#FFFFFF";
                String detailColor = useDarkText ? "#64748b" : "#E6FFFFFF";

                TimelineRenderer.addEventTo3Days(requireContext(), container, hourHeightPx, timeColumnWidth, colWidth, dayIndex,
                        event.getTitle() != null ? event.getTitle() : "(Untitled)",
                        CalendarViewHelper.buildEventLocation(event), parts.startHour, parts.startMinute,
                        parts.durationMinutes, bgRes, titleColor, detailColor, eventTint);
            }
        });
    }

    private LocalDate toLocalDate(Timestamp timestamp) {
        if (timestamp == null) return null;
        ZoneId userZone = TimezoneHelper.getSelectedZoneId(requireContext());
        return timestamp.toDate().toInstant().atZone(userZone).toLocalDate();
    }

    private int resolveThemePrimaryColor() {
        String palette = ThemeManager.getPalette(requireContext());
        if (ThemeManager.PALETTE_EMERALD.equals(palette)) {
            return android.graphics.Color.parseColor("#10B981");
        }
        if (ThemeManager.PALETTE_SUNSET.equals(palette)) {
            return android.graphics.Color.parseColor("#F97316");
        }
        return android.graphics.Color.parseColor("#2563EB");
    }
}
