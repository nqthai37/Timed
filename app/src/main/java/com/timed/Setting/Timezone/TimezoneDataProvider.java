package com.timed.Setting.Timezone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * Provides a curated, comprehensive list of UTC-standard timezones
 * covering all major world regions. Timezones are sorted by UTC offset.
 */
public class TimezoneDataProvider {

    /**
     * Returns a complete list of UTC timezones with representative cities,
     * sorted from UTC-12:00 to UTC+14:00.
     */
    public static List<TimezoneItem> getAllTimezones() {
        List<TimezoneItem> list = new ArrayList<>();

        // UTC-12:00
        list.add(createItem("Etc/GMT+12", "International Date Line West",
                "Baker Island, Howland Island"));

        // UTC-11:00
        list.add(createItem("Pacific/Midway", "Samoa Standard Time",
                "Midway Island, American Samoa"));

        // UTC-10:00
        list.add(createItem("Pacific/Honolulu", "Hawaii-Aleutian Standard Time",
                "Honolulu, Hilo, Kahului"));

        // UTC-09:30
        list.add(createItem("Pacific/Marquesas", "Marquesas Time",
                "Marquesas Islands"));

        // UTC-09:00
        list.add(createItem("America/Anchorage", "Alaska Standard Time",
                "Anchorage, Juneau, Fairbanks"));

        // UTC-08:00
        list.add(createItem("America/Los_Angeles", "Pacific Standard Time",
                "Los Angeles, San Francisco, Vancouver"));

        // UTC-07:00
        list.add(createItem("America/Denver", "Mountain Standard Time",
                "Denver, Phoenix, Salt Lake City"));

        // UTC-06:00
        list.add(createItem("America/Chicago", "Central Standard Time",
                "Chicago, Houston, Mexico City"));

        // UTC-05:00
        list.add(createItem("America/New_York", "Eastern Standard Time",
                "New York, Toronto, Bogotá"));

        // UTC-04:00
        list.add(createItem("America/Caracas", "Atlantic Standard Time",
                "Caracas, Santiago, La Paz"));

        // UTC-03:30
        list.add(createItem("America/St_Johns", "Newfoundland Standard Time",
                "St. John's, Labrador"));

        // UTC-03:00
        list.add(createItem("America/Sao_Paulo", "Brasilia Time",
                "São Paulo, Buenos Aires, Montevideo"));

        // UTC-02:00
        list.add(createItem("Atlantic/South_Georgia", "South Georgia Time",
                "South Georgia, Fernando de Noronha"));

        // UTC-01:00
        list.add(createItem("Atlantic/Azores", "Azores Standard Time",
                "Azores, Cape Verde"));

        // UTC+00:00
        list.add(createItem("UTC", "Coordinated Universal Time",
                "London, Dublin, Lisbon"));

        // UTC+01:00
        list.add(createItem("Europe/Paris", "Central European Time",
                "Paris, Berlin, Rome, Madrid"));

        // UTC+02:00
        list.add(createItem("Europe/Bucharest", "Eastern European Time",
                "Bucharest, Athens, Cairo, Helsinki"));

        // UTC+03:00
        list.add(createItem("Europe/Moscow", "Moscow Standard Time",
                "Moscow, Istanbul, Riyadh, Nairobi"));

        // UTC+03:30
        list.add(createItem("Asia/Tehran", "Iran Standard Time",
                "Tehran, Isfahan"));

        // UTC+04:00
        list.add(createItem("Asia/Dubai", "Gulf Standard Time",
                "Dubai, Abu Dhabi, Baku, Muscat"));

        // UTC+04:30
        list.add(createItem("Asia/Kabul", "Afghanistan Time",
                "Kabul"));

        // UTC+05:00
        list.add(createItem("Asia/Karachi", "Pakistan Standard Time",
                "Karachi, Islamabad, Tashkent"));

        // UTC+05:30
        list.add(createItem("Asia/Kolkata", "India Standard Time",
                "Mumbai, Delhi, Colombo, Kolkata"));

        // UTC+05:45
        list.add(createItem("Asia/Kathmandu", "Nepal Time",
                "Kathmandu, Pokhara"));

        // UTC+06:00
        list.add(createItem("Asia/Dhaka", "Bangladesh Standard Time",
                "Dhaka, Almaty, Bishkek"));

        // UTC+06:30
        list.add(createItem("Asia/Yangon", "Myanmar Time",
                "Yangon, Mandalay, Cocos Islands"));

        // UTC+07:00
        list.add(createItem("Asia/Ho_Chi_Minh", "Indochina Time",
                "Ho Chi Minh City, Bangkok, Jakarta"));

        // UTC+08:00
        list.add(createItem("Asia/Shanghai", "China Standard Time",
                "Beijing, Shanghai, Singapore, Taipei"));

        // UTC+08:45
        list.add(createItem("Australia/Eucla", "Australian Central Western Time",
                "Eucla, Border Village"));

        // UTC+09:00
        list.add(createItem("Asia/Tokyo", "Japan Standard Time",
                "Tokyo, Seoul, Osaka"));

        // UTC+09:30
        list.add(createItem("Australia/Adelaide", "Australian Central Time",
                "Adelaide, Darwin"));

        // UTC+10:00
        list.add(createItem("Australia/Sydney", "Australian Eastern Time",
                "Sydney, Melbourne, Brisbane"));

        // UTC+10:30
        list.add(createItem("Australia/Lord_Howe", "Lord Howe Standard Time",
                "Lord Howe Island"));

        // UTC+11:00
        list.add(createItem("Pacific/Guadalcanal", "Solomon Islands Time",
                "Honiara, Noumea, Vladivostok"));

        // UTC+12:00
        list.add(createItem("Pacific/Auckland", "New Zealand Standard Time",
                "Auckland, Wellington, Fiji"));

        // UTC+12:45
        list.add(createItem("Pacific/Chatham", "Chatham Island Time",
                "Chatham Islands"));

        // UTC+13:00
        list.add(createItem("Pacific/Apia", "West Samoa Time",
                "Apia, Nuku'alofa, Tokelau"));

        // UTC+14:00
        list.add(createItem("Pacific/Kiritimati", "Line Islands Time",
                "Kiritimati, Line Islands"));

        // Sort by raw offset
        Collections.sort(list, (a, b) -> Integer.compare(a.getRawOffset(), b.getRawOffset()));

        return list;
    }

    /**
     * Returns the device's current timezone as a TimezoneItem.
     */
    public static TimezoneItem getDeviceTimezone() {
        TimeZone tz = TimeZone.getDefault();
        String id = tz.getID();
        String displayName = tz.getDisplayName(false, TimeZone.LONG);
        String abbreviation = tz.getDisplayName(false, TimeZone.SHORT);
        String offset = formatOffset(tz.getRawOffset());
        return new TimezoneItem(id, displayName, offset, "", abbreviation, tz.getRawOffset());
    }

    /**
     * Creates a TimezoneItem from a timezone ID string.
     */
    private static TimezoneItem createItem(String timezoneId, String displayName, String cities) {
        TimeZone tz = TimeZone.getTimeZone(timezoneId);
        int rawOffset = tz.getRawOffset();
        String offset = formatOffset(rawOffset);
        String abbreviation = tz.getDisplayName(false, TimeZone.SHORT);

        return new TimezoneItem(timezoneId, displayName, offset, cities, abbreviation, rawOffset);
    }

    /**
     * Formats a raw offset in milliseconds to a UTC offset string like "UTC+07:00".
     */
    public static String formatOffset(int rawOffsetMillis) {
        int totalMinutes = rawOffsetMillis / (60 * 1000);
        int hours = Math.abs(totalMinutes) / 60;
        int minutes = Math.abs(totalMinutes) % 60;
        String sign = totalMinutes >= 0 ? "+" : "-";

        if (totalMinutes == 0) {
            return "UTC±00:00";
        }
        return String.format("UTC%s%02d:%02d", sign, hours, minutes);
    }

    /**
     * Filters the timezone list based on a search query.
     * Matches against display name, cities, UTC offset, and timezone ID.
     */
    public static List<TimezoneItem> filterTimezones(List<TimezoneItem> all, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(all);
        }

        String lowerQuery = query.trim().toLowerCase();
        List<TimezoneItem> filtered = new ArrayList<>();

        for (TimezoneItem item : all) {
            if (item.getDisplayName().toLowerCase().contains(lowerQuery)
                    || item.getCities().toLowerCase().contains(lowerQuery)
                    || item.getUtcOffset().toLowerCase().contains(lowerQuery)
                    || item.getTimezoneId().toLowerCase().contains(lowerQuery)
                    || item.getAbbreviation().toLowerCase().contains(lowerQuery)) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}
