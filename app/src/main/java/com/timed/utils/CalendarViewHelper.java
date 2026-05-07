package com.timed.utils;

import android.content.Context;

import com.timed.R;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.models.Event;

import java.util.Calendar;
import java.util.Date;

public class CalendarViewHelper {
    public static class EventTimeParts {
        public final int startHour;
        public final int startMinute;
        public final int durationMinutes;

        public EventTimeParts(int startHour, int startMinute, int durationMinutes) {
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.durationMinutes = durationMinutes;
        }
    }

    public static EventTimeParts toTimeParts(Context context, Event event) {
        if (event == null || event.getStartTime() == null) {
            return null;
        }

        Date start = event.getStartTime().toDate();
        Date end = event.getEndTime() != null ? event.getEndTime().toDate() : start;

        Calendar startCal = TimezoneHelper.getCalendarInSelectedTz(context, start);
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

    public static String buildEventDetails(Context context, Event event) {
        String timeRange = buildTimeRange(context, event);
        String location = buildEventLocation(event);

        if (!timeRange.isEmpty() && !location.isEmpty()) {
            return timeRange + " • " + location;
        }
        if (!timeRange.isEmpty()) {
            return timeRange;
        }
        return location;
    }

    public static String buildTimeRange(Context context, Event event) {
        if (event == null || event.getStartTime() == null) {
            return "";
        }

        if (event.getAllDay() != null && event.getAllDay()) {
            return "All day";
        }

        String startText = TimezoneHelper.formatTime24h(context, event.getStartTime().toDate());

        if (event.getEndTime() != null) {
            String endText = TimezoneHelper.formatTime24h(context, event.getEndTime().toDate());
            return startText + " - " + endText;
        }

        return startText;
    }

    public static String buildEventLocation(Event event) {
        if (event == null || event.getLocation() == null) {
            return "";
        }
        return event.getLocation().trim();
    }

    public static Integer resolveEventTintColor(Event event) {
        if (event == null || event.getColor() == null || event.getColor().trim().isEmpty()) {
            return null;
        }
        try {
            return android.graphics.Color.parseColor(event.getColor().trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static int pickEventBackground(int index) {
        int mod = index % 3;
        if (mod == 1) {
            return R.drawable.bg_day_event_emerald;
        }
        if (mod == 2) {
            return R.drawable.bg_day_event_light;
        }
        return R.drawable.bg_day_event_primary;
    }
}
