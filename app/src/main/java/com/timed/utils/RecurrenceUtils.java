package com.timed.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling recurring events based on iCal RRULE format
 * Examples:
 * - FREQ=DAILY;COUNT=5 (daily for 5 times)
 * - FREQ=WEEKLY;BYDAY=MO,WE,FR (every Monday, Wednesday, Friday)
 * - FREQ=MONTHLY;BYMONTHDAY=15 (15th of every month)
 */
public class RecurrenceUtils {
    public static final String FREQ_DAILY = "DAILY";
    public static final String FREQ_WEEKLY = "WEEKLY";
    public static final String FREQ_MONTHLY = "MONTHLY";
    public static final String FREQ_YEARLY = "YEARLY";

    /**
     * Parse iCal RRULE string to RecurrenceRule object
     */
    public static RecurrenceRule parseRRule(String rruleString) {
        RecurrenceRule rule = new RecurrenceRule();

        if (rruleString == null || rruleString.isEmpty()) {
            return rule;
        }

        Map<String, String> params = new HashMap<>();
        String[] parts = rruleString.split(";");

        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }

        rule.frequency = params.getOrDefault("FREQ", FREQ_DAILY);
        rule.count = parseInt(params.get("COUNT"), -1);
        rule.interval = parseInt(params.get("INTERVAL"), 1);

        if (params.containsKey("UNTIL")) {
            rule.until = parseDate(params.get("UNTIL"));
        }

        String byDay = params.get("BYDAY");
        if (byDay != null) {
            rule.byDay = new ArrayList<>();
            for (String day : byDay.split(",")) {
                rule.byDay.add(day.trim());
            }
        }

        String byMonthDay = params.get("BYMONTHDAY");
        if (byMonthDay != null) {
            rule.byMonthDay = new ArrayList<>();
            for (String day : byMonthDay.split(",")) {
                rule.byMonthDay.add(Integer.parseInt(day.trim()));
            }
        }

        if (params.containsKey("BYSETPOS")) {
            rule.setPos = parseInt(params.get("BYSETPOS"), 0);
        }

        return rule;
    }

    /**
     * Generate occurrences of a recurring event
     */
    public static List<Long> generateOccurrences(long startTime, RecurrenceRule rule, int maxOccurrences) {
        List<Long> occurrences = new ArrayList<>();

        if (rule == null || rule.frequency == null) {
            return occurrences;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        int count = 0;
        int maxLimit = rule.count > 0 ? rule.count : maxOccurrences;

        while (count < maxLimit) {
            // Check if we've exceeded the UNTIL date
            if (rule.until != null && calendar.getTimeInMillis() > rule.until.getTime()) {
                break;
            }

            occurrences.add(calendar.getTimeInMillis());
            count++;

            // Add the interval based on frequency
            switch (rule.frequency) {
                case FREQ_DAILY:
                    calendar.add(Calendar.DAY_OF_MONTH, rule.interval);
                    break;
                case FREQ_WEEKLY:
                    calendar.add(Calendar.WEEK_OF_YEAR, rule.interval);
                    break;
                case FREQ_MONTHLY:
                    calendar.add(Calendar.MONTH, rule.interval);
                    break;
                case FREQ_YEARLY:
                    calendar.add(Calendar.YEAR, rule.interval);
                    break;
            }
        }

        return occurrences;
    }

    /**
     * Check if a timestamp is an occurrence of a recurring event
     */
    public static boolean isOccurrence(long startTime, RecurrenceRule rule, long timestamp, List<Long> exceptions) {
        if (rule == null || rule.frequency == null) {
            return startTime == timestamp;
        }

        // Check if timestamp is in the exceptions list
        if (exceptions != null && exceptions.contains(timestamp)) {
            return false;
        }

        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(startTime);

        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);

        switch (rule.frequency) {
            case FREQ_DAILY:
                return isOccurrenceDaily(start, target, rule);
            case FREQ_WEEKLY:
                return isOccurrenceWeekly(start, target, rule);
            case FREQ_MONTHLY:  
                return isOccurrenceMonthly(start, target, rule);
            case FREQ_YEARLY:
                return isOccurrenceYearly(start, target, rule);
            default:
                return false;
        }
    }

    private static boolean isOccurrenceDaily(Calendar start, Calendar target, RecurrenceRule rule) {
        long diff = (target.getTimeInMillis() - start.getTimeInMillis()) / (1000 * 60 * 60 * 24);
        return diff >= 0 && diff % rule.interval == 0;
    }

    private static boolean isOccurrenceWeekly(Calendar start, Calendar target, RecurrenceRule rule) {
        long diffWeeks = (target.getTimeInMillis() - start.getTimeInMillis()) / (1000 * 60 * 60 * 24 * 7);
        if (diffWeeks < 0 || diffWeeks % rule.interval != 0) {
            return false;
        }

        String targetDay = getDayOfWeek(target);
        if (rule.byDay == null || rule.byDay.isEmpty()) {
            String startDay = getDayOfWeek(start);
            return startDay.equals(targetDay);
        }

        return rule.byDay.contains(targetDay);
    }

    private static boolean isOccurrenceMonthly(Calendar start, Calendar target, RecurrenceRule rule) {
        int diffMonths = (target.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12
                + (target.get(Calendar.MONTH) - start.get(Calendar.MONTH));
        if (diffMonths < 0 || diffMonths % rule.interval != 0) {
            return false;
        }

        if (rule.setPos != 0 && rule.byDay != null && !rule.byDay.isEmpty()) {
            String dayCode = rule.byDay.get(0);
            return isNthWeekdayOfMonth(target, rule.setPos, dayCode);
        }

        if (rule.byMonthDay == null || rule.byMonthDay.isEmpty()) {
            return target.get(Calendar.DAY_OF_MONTH) == start.get(Calendar.DAY_OF_MONTH);
        }

        return rule.byMonthDay.contains(target.get(Calendar.DAY_OF_MONTH));
    }

    private static boolean isOccurrenceYearly(Calendar start, Calendar target, RecurrenceRule rule) {
        int diffYears = target.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        if (diffYears < 0 || diffYears % rule.interval != 0) {
            return false;
        }

        return target.get(Calendar.MONTH) == start.get(Calendar.MONTH)
                && target.get(Calendar.DAY_OF_MONTH) == start.get(Calendar.DAY_OF_MONTH);
    }

    private static String getDayOfWeek(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.SUNDAY:
                return "SU";
            case Calendar.MONDAY:
                return "MO";
            case Calendar.TUESDAY:
                return "TU";
            case Calendar.WEDNESDAY:
                return "WE";
            case Calendar.THURSDAY:
                return "TH";
            case Calendar.FRIDAY:
                return "FR";
            case Calendar.SATURDAY:
                return "SA";
            default:
                return "";
        }
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int dayCodeToCalendar(String dayCode) {
        if (dayCode == null) {
            return -1;
        }
        switch (dayCode) {
            case "SU":
                return Calendar.SUNDAY;
            case "MO":
                return Calendar.MONDAY;
            case "TU":
                return Calendar.TUESDAY;
            case "WE":
                return Calendar.WEDNESDAY;
            case "TH":
                return Calendar.THURSDAY;
            case "FR":
                return Calendar.FRIDAY;
            case "SA":
                return Calendar.SATURDAY;
            default:
                return -1;
        }
    }

    private static boolean isNthWeekdayOfMonth(Calendar target, int setPos, String dayCode) {
        int targetDow = dayCodeToCalendar(dayCode);
        if (targetDow == -1) {
            return false;
        }

        int targetDay = target.get(Calendar.DAY_OF_MONTH);
        Calendar cursor = (Calendar) target.clone();
        cursor.set(Calendar.DAY_OF_MONTH, 1);

        int firstDow = cursor.get(Calendar.DAY_OF_WEEK);
        int offset = (targetDow - firstDow + 7) % 7;
        int firstOccurrence = 1 + offset;

        if (setPos > 0) {
            int day = firstOccurrence + (setPos - 1) * 7;
            return day == targetDay;
        }

        if (setPos < 0) {
            int daysInMonth = cursor.getActualMaximum(Calendar.DAY_OF_MONTH);
            cursor.set(Calendar.DAY_OF_MONTH, daysInMonth);
            int lastDow = cursor.get(Calendar.DAY_OF_WEEK);
            int backOffset = (lastDow - targetDow + 7) % 7;
            int lastOccurrence = daysInMonth - backOffset;
            return lastOccurrence == targetDay;
        }

        return false;
    }

    private static Date parseDate(String dateString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Inner class to represent parsed recurrence rule
     */
    public static class RecurrenceRule {
        public String frequency;
        public int count = -1;
        public int interval = 1;
        public Date until;
        public List<String> byDay; // ["MO", "WE", "FR"]
        public List<Integer> byMonthDay; // [15, 20]
        public int setPos = 0;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("FREQ=" + frequency);
            if (count > 0) {
                sb.append(";COUNT=").append(count);
            }
            if (interval > 1) {
                sb.append(";INTERVAL=").append(interval);
            }
            if (until != null) {
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
                sb.append(";UNTIL=").append(format.format(until));
            }
            if (byDay != null && !byDay.isEmpty()) {
                sb.append(";BYDAY=").append(String.join(",", byDay));
            }
            if (setPos != 0) {
                sb.append(";BYSETPOS=").append(setPos);
            }
            if (byMonthDay != null && !byMonthDay.isEmpty()) {
                StringBuilder days = new StringBuilder();
                for (Integer day : byMonthDay) {
                    if (days.length() > 0) days.append(",");
                    days.append(day);
                }
                sb.append(";BYMONTHDAY=").append(days);
            }
            return sb.toString();
        }
    }
}

