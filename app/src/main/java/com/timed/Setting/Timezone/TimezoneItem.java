package com.timed.Setting.Timezone;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Model class representing a timezone entry for display in the timezone list.
 * Each item contains the timezone ID (IANA format), display name, UTC offset,
 * representative cities, and the abbreviation.
 */
public class TimezoneItem {
    private final String timezoneId;   // e.g., "America/New_York"
    private final String displayName;  // e.g., "Eastern Time"
    private final String utcOffset;    // e.g., "UTC-05:00"
    private final String cities;       // e.g., "New York, Toronto, Bogotá"
    private final String abbreviation; // e.g., "EST"
    private final int rawOffset;       // Raw offset in milliseconds for sorting

    public TimezoneItem(String timezoneId, String displayName, String utcOffset,
                        String cities, String abbreviation, int rawOffset) {
        this.timezoneId = timezoneId;
        this.displayName = displayName;
        this.utcOffset = utcOffset;
        this.cities = cities;
        this.abbreviation = abbreviation;
        this.rawOffset = rawOffset;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUtcOffset() {
        return utcOffset;
    }

    public String getCities() {
        return cities;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public int getRawOffset() {
        return rawOffset;
    }

    /**
     * Returns the current time in this timezone formatted as "HH:mm" (24-hour).
     */
    public String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone(timezoneId));
        return sdf.format(new Date());
    }

    /**
     * Returns the current date in this timezone formatted as "EEE, MMM d".
     */
    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone(timezoneId));
        return sdf.format(new Date());
    }

    /**
     * Returns the offset difference from local timezone as a human-readable string.
     * e.g., "3 hours ahead", "2 hours behind", "Same time"
     */
    public String getOffsetFromLocal() {
        TimeZone local = TimeZone.getDefault();
        TimeZone target = TimeZone.getTimeZone(timezoneId);
        long now = System.currentTimeMillis();
        int localOffset = local.getOffset(now);
        int targetOffset = target.getOffset(now);
        int diffMinutes = (targetOffset - localOffset) / (60 * 1000);

        if (diffMinutes == 0) {
            return "Same time";
        }

        int hours = Math.abs(diffMinutes) / 60;
        int minutes = Math.abs(diffMinutes) % 60;
        String direction = diffMinutes > 0 ? "ahead" : "behind";

        if (hours == 0) {
            return minutes + " min " + direction;
        } else if (minutes == 0) {
            return hours + (hours == 1 ? " hour " : " hours ") + direction;
        } else {
            return hours + "h " + minutes + "m " + direction;
        }
    }
}
