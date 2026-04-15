package com.timed.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling recurring events based on iCal RRULE format.
 *
 * Supported examples:
 * - FREQ=DAILY;COUNT=5
 * - FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE,FR
 * - FREQ=MONTHLY;BYMONTHDAY=15
 * - FREQ=MONTHLY;BYDAY=MO;BYSETPOS=1 (first Monday in month)
 */
public class RecurrenceUtils {
    public static final String FREQ_DAILY = "DAILY";
    public static final String FREQ_WEEKLY = "WEEKLY";
    public static final String FREQ_MONTHLY = "MONTHLY";
    public static final String FREQ_YEARLY = "YEARLY";

    private static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    private static final Pattern BY_DAY_PATTERN = Pattern.compile("^([+-]?\\d{1,2})?(SU|MO|TU|WE|TH|FR|SA)$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[+-]?\\d+$");
    private static final String RRULE_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";

    /**
     * Parse iCal RRULE string to RecurrenceRule object.
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
                params.put(keyValue[0].trim().toUpperCase(Locale.US), keyValue[1].trim());
            }
        }

        rule.frequency = params.getOrDefault("FREQ", FREQ_DAILY).toUpperCase(Locale.US);
        rule.count = parseInt(params.get("COUNT"), -1);
        rule.interval = Math.max(1, parseInt(params.get("INTERVAL"), 1));

        if (params.containsKey("UNTIL")) {
            rule.until = parseDate(params.get("UNTIL"));
        }

        String byDay = params.get("BYDAY");
        if (byDay != null) {
            rule.byDay = new ArrayList<>();
            rule.byDayEntries = new ArrayList<>();
            for (String day : byDay.split(",")) {
                String token = day.trim().toUpperCase(Locale.US);
                ByDayEntry entry = parseByDayEntry(token);
                if (entry != null) {
                    rule.byDayEntries.add(entry);
                    rule.byDay.add(entry.toToken());
                }
            }
        }

        String byMonthDay = params.get("BYMONTHDAY");
        if (byMonthDay != null) {
            rule.byMonthDay = new ArrayList<>();
            for (String day : byMonthDay.split(",")) {
                int parsed = parseInt(day.trim(), 0);
                if (parsed != 0) {
                    rule.byMonthDay.add(parsed);
                }
            }
        }

        String bySetPos = params.get("BYSETPOS");
        if (bySetPos != null) {
            rule.bySetPos = new ArrayList<>();
            for (String position : bySetPos.split(",")) {
                int parsed = parseInt(position.trim(), 0);
                if (parsed != 0) {
                    rule.bySetPos.add(parsed);
                }
            }
        }

        return rule;
    }

    /**
     * Generate occurrences from startTime forward.
     */
    public static List<Long> generateOccurrences(long startTime, RecurrenceRule rule, int maxOccurrences) {
        int safeMax = maxOccurrences <= 0 ? 1 : maxOccurrences;
        long rangeEnd = startTime + (370L * safeMax * ONE_DAY_MS);
        if (rule != null && rule.until != null) {
            rangeEnd = Math.min(rangeEnd, rule.until.getTime());
        }
        return generateOccurrencesInRange(startTime, rule, startTime, rangeEnd, null, safeMax);
    }

    /**
     * Generate occurrences for a specific range, honoring COUNT/UNTIL and exception dates.
     */
    public static List<Long> generateOccurrencesInRange(long startTime,
                                                        RecurrenceRule rule,
                                                        long rangeStart,
                                                        long rangeEnd,
                                                        List<String> exceptionDates,
                                                        int maxOccurrences) {
        List<Long> occurrences = new ArrayList<>();

        if (rangeEnd < rangeStart) {
            return occurrences;
        }

        if (rule == null || rule.frequency == null || rule.frequency.isEmpty()) {
            if (startTime >= rangeStart && startTime <= rangeEnd) {
                Set<String> exceptions = normalizeExceptionDates(exceptionDates);
                if (!isExceptionDate(startTime, exceptions)) {
                    occurrences.add(startTime);
                }
            }
            return occurrences;
        }

        int safeMax = maxOccurrences > 0 ? maxOccurrences : Integer.MAX_VALUE;
        Set<String> exceptions = normalizeExceptionDates(exceptionDates);

        LocalDateTime startDateTime = toLocalDateTime(startTime);
        LocalDate startDate = startDateTime.toLocalDate();
        LocalTime startClockTime = startDateTime.toLocalTime();

        LocalDate cursorDate = startDate;
        LocalDate lastDate = toLocalDateTime(Math.max(rangeEnd, startTime)).toLocalDate();
        if (rule.until != null) {
            LocalDate untilDate = toLocalDateTime(rule.until.getTime()).toLocalDate();
            if (untilDate.isBefore(lastDate)) {
                lastDate = untilDate;
            }
        }

        int matchedCount = 0;
        while (!cursorDate.isAfter(lastDate) && matchedCount < safeMax) {
            long candidateMillis = toMillis(LocalDateTime.of(cursorDate, startClockTime));

            if (candidateMillis < startTime) {
                cursorDate = cursorDate.plusDays(1);
                continue;
            }

            if (rule.until != null && candidateMillis > rule.until.getTime()) {
                break;
            }

            if (matchesRuleOnDate(startDate, cursorDate, rule)) {
                matchedCount++;

                if (rule.count > 0 && matchedCount > rule.count) {
                    break;
                }

                if (!isExceptionDate(candidateMillis, exceptions)
                        && candidateMillis >= rangeStart
                        && candidateMillis <= rangeEnd) {
                    occurrences.add(candidateMillis);
                }
            }

            cursorDate = cursorDate.plusDays(1);
        }

        return occurrences;
    }

    /**
     * Check if a timestamp is an occurrence of a recurring event.
     */
    public static boolean isOccurrence(long startTime, RecurrenceRule rule, long timestamp, List<Long> exceptions) {
        if (exceptions != null && exceptions.contains(timestamp)) {
            return false;
        }

        List<String> exceptionDates = new ArrayList<>();
        if (exceptions != null) {
            for (Long value : exceptions) {
                if (value != null) {
                    exceptionDates.add(toLocalDateTime(value).toLocalDate().toString());
                }
            }
        }

        List<Long> result = generateOccurrencesInRange(startTime, rule, timestamp, timestamp, exceptionDates, 1);
        return !result.isEmpty() && result.get(0) == timestamp;
    }

    private static boolean isOccurrenceDaily(LocalDate start, LocalDate target, RecurrenceRule rule) {
        long diff = ChronoUnit.DAYS.between(start, target);
        return diff >= 0 && diff % rule.interval == 0;
    }

    private static boolean isOccurrenceWeekly(LocalDate start, LocalDate target, RecurrenceRule rule) {
        long daysDiff = ChronoUnit.DAYS.between(start, target);
        if (daysDiff < 0) {
            return false;
        }

        long weeksDiff = daysDiff / 7;
        if (weeksDiff % rule.interval != 0) {
            return false;
        }

        if (rule.byDayEntries == null || rule.byDayEntries.isEmpty()) {
            return target.getDayOfWeek() == start.getDayOfWeek();
        }

        for (ByDayEntry entry : rule.byDayEntries) {
            if (entry != null && entry.matchesWeekday(target.getDayOfWeek())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOccurrenceMonthly(LocalDate start, LocalDate target, RecurrenceRule rule) {
        long monthsDiff = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), target.withDayOfMonth(1));
        if (monthsDiff < 0 || monthsDiff % rule.interval != 0) {
            return false;
        }

        if (rule.byMonthDay != null && !rule.byMonthDay.isEmpty()) {
            return matchesByMonthDay(target, rule.byMonthDay);
        }

        if (rule.byDayEntries != null && !rule.byDayEntries.isEmpty()) {
            return matchesMonthlyByDay(target, rule);
        }

        return target.getDayOfMonth() == start.getDayOfMonth();
    }

    private static boolean isOccurrenceYearly(LocalDate start, LocalDate target, RecurrenceRule rule) {
        long yearsDiff = ChronoUnit.YEARS.between(start.withDayOfYear(1), target.withDayOfYear(1));
        if (yearsDiff < 0 || yearsDiff % rule.interval != 0) {
            return false;
        }

        if (rule.byMonthDay != null && !rule.byMonthDay.isEmpty()) {
            return target.getMonthValue() == start.getMonthValue()
                    && matchesByMonthDay(target, rule.byMonthDay);
        }

        return target.getMonthValue() == start.getMonthValue()
                && target.getDayOfMonth() == start.getDayOfMonth();
    }

    private static boolean matchesRuleOnDate(LocalDate startDate, LocalDate targetDate, RecurrenceRule rule) {
        String frequency = rule.frequency != null ? rule.frequency.toUpperCase(Locale.US) : "";

        switch (frequency) {
            case FREQ_DAILY:
                return isOccurrenceDaily(startDate, targetDate, rule);
            case FREQ_WEEKLY:
                return isOccurrenceWeekly(startDate, targetDate, rule);
            case FREQ_MONTHLY:
                return isOccurrenceMonthly(startDate, targetDate, rule);
            case FREQ_YEARLY:
                return isOccurrenceYearly(startDate, targetDate, rule);
            default:
                return false;
        }
    }

    private static boolean matchesMonthlyByDay(LocalDate candidateDate, RecurrenceRule rule) {
        List<ByDayEntry> entries = rule.byDayEntries;
        if (entries == null || entries.isEmpty()) {
            return false;
        }

        boolean containsPlainEntries = false;
        for (ByDayEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (entry.ordinal != null) {
                if (entry.matchesWeekday(candidateDate.getDayOfWeek())
                        && entry.matchesOrdinalInMonth(candidateDate)) {
                    return true;
                }
            } else {
                containsPlainEntries = true;
            }
        }

        if (!containsPlainEntries) {
            return false;
        }

        List<Integer> candidateDaysInMonth = new ArrayList<>();
        LocalDate monthCursor = candidateDate.withDayOfMonth(1);
        LocalDate monthEnd = candidateDate.withDayOfMonth(candidateDate.lengthOfMonth());

        while (!monthCursor.isAfter(monthEnd)) {
            for (ByDayEntry entry : entries) {
                if (entry != null && entry.ordinal == null && entry.matchesWeekday(monthCursor.getDayOfWeek())) {
                    candidateDaysInMonth.add(monthCursor.getDayOfMonth());
                    break;
                }
            }
            monthCursor = monthCursor.plusDays(1);
        }

        if (candidateDaysInMonth.isEmpty()) {
            return false;
        }

        if (rule.bySetPos == null || rule.bySetPos.isEmpty()) {
            return candidateDaysInMonth.contains(candidateDate.getDayOfMonth());
        }

        for (Integer position : rule.bySetPos) {
            if (position == null || position == 0) {
                continue;
            }
            int index = position > 0 ? position - 1 : candidateDaysInMonth.size() + position;
            if (index >= 0 && index < candidateDaysInMonth.size()) {
                if (candidateDaysInMonth.get(index) == candidateDate.getDayOfMonth()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean matchesByMonthDay(LocalDate date, List<Integer> byMonthDay) {
        int dayOfMonth = date.getDayOfMonth();
        int lengthOfMonth = date.lengthOfMonth();

        for (Integer value : byMonthDay) {
            if (value == null || value == 0) {
                continue;
            }

            if (value > 0 && dayOfMonth == value) {
                return true;
            }

            if (value < 0) {
                int resolvedDay = lengthOfMonth + value + 1;
                if (resolvedDay > 0 && dayOfMonth == resolvedDay) {
                    return true;
                }
            }
        }

        return false;
    }

    private static ByDayEntry parseByDayEntry(String token) {
        Matcher matcher = BY_DAY_PATTERN.matcher(token);
        if (!matcher.matches()) {
            return null;
        }

        Integer ordinal = null;
        if (matcher.group(1) != null) {
            int parsedOrdinal = parseInt(matcher.group(1), 0);
            if (parsedOrdinal != 0) {
                ordinal = parsedOrdinal;
            }
        }

        String dayCode = matcher.group(2);
        return new ByDayEntry(dayCode, ordinal);
    }

    private static Set<String> normalizeExceptionDates(List<String> exceptionDates) {
        Set<String> normalized = new HashSet<>();
        if (exceptionDates == null || exceptionDates.isEmpty()) {
            return normalized;
        }

        for (String raw : exceptionDates) {
            if (raw == null) {
                continue;
            }

            String value = raw.trim();
            if (value.isEmpty()) {
                continue;
            }

            if (NUMBER_PATTERN.matcher(value).matches()) {
                try {
                    long millis = Long.parseLong(value);
                    normalized.add(toLocalDateTime(millis).toLocalDate().toString());
                } catch (NumberFormatException ignored) {
                    // Ignore invalid numeric exception values.
                }
                continue;
            }

            if (value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                normalized.add(value);
                continue;
            }

            try {
                Instant parsed = Instant.parse(value);
                normalized.add(LocalDateTime.ofInstant(parsed, SYSTEM_ZONE).toLocalDate().toString());
            } catch (Exception ignored) {
                // Ignore unsupported exception format.
            }
        }

        return normalized;
    }

    private static boolean isExceptionDate(long millis, Set<String> normalizedExceptions) {
        if (normalizedExceptions == null || normalizedExceptions.isEmpty()) {
            return false;
        }
        String dateKey = toLocalDateTime(millis).toLocalDate().toString();
        return normalizedExceptions.contains(dateKey);
    }

    private static LocalDateTime toLocalDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), SYSTEM_ZONE);
    }

    private static long toMillis(LocalDateTime dateTime) {
        return dateTime.atZone(SYSTEM_ZONE).toInstant().toEpochMilli();
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Date parseDate(String dateString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(RRULE_DATE_FORMAT, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static class ByDayEntry {
        public final String dayCode;
        public final Integer ordinal;

        public ByDayEntry(String dayCode, Integer ordinal) {
            this.dayCode = dayCode;
            this.ordinal = ordinal;
        }

        public boolean matchesWeekday(DayOfWeek dayOfWeek) {
            return dayCode.equals(dayOfWeekToCode(dayOfWeek));
        }

        public boolean matchesOrdinalInMonth(LocalDate date) {
            if (ordinal == null) {
                return false;
            }

            int day = date.getDayOfMonth();
            int length = date.lengthOfMonth();
            int positiveOrdinal = ((day - 1) / 7) + 1;
            int negativeOrdinal = -(((length - day) / 7) + 1);

            return ordinal == positiveOrdinal || ordinal == negativeOrdinal;
        }

        public String toToken() {
            return ordinal == null ? dayCode : (ordinal + dayCode);
        }
    }

    private static String dayOfWeekToCode(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "MO";
            case TUESDAY:
                return "TU";
            case WEDNESDAY:
                return "WE";
            case THURSDAY:
                return "TH";
            case FRIDAY:
                return "FR";
            case SATURDAY:
                return "SA";
            case SUNDAY:
                return "SU";
            default:
                return "";
        }
    }

    /**
     * Inner class to represent parsed recurrence rule.
     */
    public static class RecurrenceRule {
        public String frequency;
        public int count = -1;
        public int interval = 1;
        public Date until;
        public List<String> byDay; // ["MO", "WE", "FR"]
        public List<ByDayEntry> byDayEntries;
        public List<Integer> byMonthDay; // [15, 20]
        public List<Integer> bySetPos; // [1] means first matching day in month

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
                SimpleDateFormat format = new SimpleDateFormat(RRULE_DATE_FORMAT, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                sb.append(";UNTIL=").append(format.format(until));
            }
            if (byDay != null && !byDay.isEmpty()) {
                sb.append(";BYDAY=").append(String.join(",", byDay));
            }
            if (byMonthDay != null && !byMonthDay.isEmpty()) {
                StringBuilder days = new StringBuilder();
                for (Integer day : byMonthDay) {
                    if (day == null || day == 0) {
                        continue;
                    }
                    if (days.length() > 0) {
                        days.append(",");
                    }
                    days.append(day);
                }
                if (days.length() > 0) {
                    sb.append(";BYMONTHDAY=").append(days);
                }
            }
            if (bySetPos != null && !bySetPos.isEmpty()) {
                StringBuilder positions = new StringBuilder();
                for (Integer position : bySetPos) {
                    if (position == null || position == 0) {
                        continue;
                    }
                    if (positions.length() > 0) {
                        positions.append(",");
                    }
                    positions.append(position);
                }
                if (positions.length() > 0) {
                    sb.append(";BYSETPOS=").append(positions);
                }
            }
            return sb.toString();
        }
    }
}