package com.timed.Setting.Timezone;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Central utility for timezone-aware time operations throughout the app.
 * All time display/conversion in the app should go through this class
 * so that switching timezone in Settings immediately affects the entire app.
 *
 * Usage:
 *   ZoneId zone = TimezoneHelper.getSelectedZoneId(context);
 *   TimeZone tz  = TimezoneHelper.getSelectedTimeZone(context);
 */
public class TimezoneHelper {

    private static final String PREFS_NAME = "TimedAppPrefs";
    private static final String PREF_KEY_TIMEZONE = "selected_timezone";

    // In-memory cache to avoid reading SharedPreferences on every call
    private static String cachedTimezoneId = null;

    /**
     * Returns the user-selected timezone ID (e.g. "Asia/Ho_Chi_Minh").
     * Falls back to the system default if no timezone has been selected.
     */
    public static String getSelectedTimezoneId(Context context) {
        if (cachedTimezoneId != null) {
            return cachedTimezoneId;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cachedTimezoneId = prefs.getString(PREF_KEY_TIMEZONE, TimeZone.getDefault().getID());
        return cachedTimezoneId;
    }

    /**
     * Returns a java.util.TimeZone for the user-selected timezone.
     * Use this with SimpleDateFormat and Calendar.
     */
    public static TimeZone getSelectedTimeZone(Context context) {
        return TimeZone.getTimeZone(getSelectedTimezoneId(context));
    }

    /**
     * Returns a java.time.ZoneId for the user-selected timezone.
     * Use this with java.time APIs (LocalDate, ZonedDateTime, etc.).
     */
    public static ZoneId getSelectedZoneId(Context context) {
        return ZoneId.of(getSelectedTimezoneId(context));
    }

    /**
     * Saves the selected timezone and updates the in-memory cache.
     * Called from TimezoneSettingActivity when the user picks a timezone.
     */
    public static void setSelectedTimezoneId(Context context, String timezoneId) {
        cachedTimezoneId = timezoneId;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_TIMEZONE, timezoneId)
                .apply();
    }

    /**
     * Clears the in-memory cache. Call this when the timezone may have changed
     * externally (e.g., after returning from TimezoneSettingActivity).
     */
    public static void invalidateCache() {
        cachedTimezoneId = null;
    }

    /**
     * Formats a Date to "HH:mm" using the user-selected timezone.
     */
    public static String formatTime24h(Context context, Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setTimeZone(getSelectedTimeZone(context));
        return sdf.format(date);
    }

    /**
     * Formats a Date to "hh:mm a" using the user-selected timezone.
     */
    public static String formatTime12h(Context context, Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        sdf.setTimeZone(getSelectedTimeZone(context));
        return sdf.format(date);
    }

    /**
     * Formats a Date to a custom pattern using the user-selected timezone.
     */
    public static String formatDate(Context context, Date date, String pattern) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        sdf.setTimeZone(getSelectedTimeZone(context));
        return sdf.format(date);
    }

    /**
     * Returns a Calendar instance set to the user-selected timezone.
     */
    public static java.util.Calendar getCalendarInSelectedTz(Context context) {
        return java.util.Calendar.getInstance(getSelectedTimeZone(context));
    }

    /**
     * Returns a Calendar set to the given Date in the user-selected timezone.
     */
    public static java.util.Calendar getCalendarInSelectedTz(Context context, Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance(getSelectedTimeZone(context));
        cal.setTime(date);
        return cal;
    }
}
