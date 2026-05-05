package com.timed.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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

        LocalDate lastDate = toLocalDateTime(Math.max(rangeEnd, startTime)).toLocalDate();
        if (rule.until != null) {
            LocalDate untilDate = toLocalDateTime(rule.until.getTime()).toLocalDate();
            if (untilDate.isBefore(lastDate)) {
                lastDate = untilDate;
            }
        }

        String frequency = rule.frequency.toUpperCase(Locale.US);
        switch (frequency) {
            case FREQ_DAILY:
                return generateDaily(startDate, startClockTime, rule, startTime,
                        rangeStart, rangeEnd, exceptions, safeMax, lastDate);
            case FREQ_WEEKLY:
                return generateWeekly(startDate, startClockTime, rule, startTime,
                        rangeStart, rangeEnd, exceptions, safeMax, lastDate);
            case FREQ_MONTHLY:
                return generateMonthly(startDate, startClockTime, rule, startTime,
                        rangeStart, rangeEnd, exceptions, safeMax, lastDate);
            case FREQ_YEARLY:
                return generateYearly(startDate, startClockTime, rule, startTime,
                        rangeStart, rangeEnd, exceptions, safeMax, lastDate);
            default:
                return occurrences;
        }
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

    // ── DAILY generator ─────────────────────────────────────────────────

    private static List<Long> generateDaily(LocalDate startDate, LocalTime clockTime,
                                             RecurrenceRule rule, long startTimeMillis,
                                             long rangeStart, long rangeEnd,
                                             Set<String> exceptions, int maxCount,
                                             LocalDate lastDate) {
        List<Long> result = new ArrayList<>();
        int matched = 0;
        LocalDate cursor = startDate;

        while (!cursor.isAfter(lastDate) && matched < maxCount) {
            long millis = toMillis(LocalDateTime.of(cursor, clockTime));

            if (millis >= startTimeMillis) {
                if (rule.until != null && millis > rule.until.getTime()) break;

                matched++;
                if (rule.count > 0 && matched > rule.count) break;

                if (millis >= rangeStart && millis <= rangeEnd
                        && !isExceptionDate(millis, exceptions)) {
                    result.add(millis);
                }
            }

            cursor = cursor.plusDays(rule.interval);
        }
        return result;
    }

    // ── WEEKLY generator ────────────────────────────────────────────────

    private static List<Long> generateWeekly(LocalDate startDate, LocalTime clockTime,
                                              RecurrenceRule rule, long startTimeMillis,
                                              long rangeStart, long rangeEnd,
                                              Set<String> exceptions, int maxCount,
                                              LocalDate lastDate) {
        List<Long> result = new ArrayList<>();
        int matched = 0;

        // Resolve target weekdays (sorted Monday→Sunday)
        List<DayOfWeek> targetDays = new ArrayList<>();
        if (rule.byDayEntries != null) {
            for (ByDayEntry entry : rule.byDayEntries) {
                if (entry != null) {
                    DayOfWeek dow = codeToDayOfWeek(entry.dayCode);
                    if (dow != null) targetDays.add(dow);
                }
            }
        }
        if (targetDays.isEmpty()) {
            targetDays.add(startDate.getDayOfWeek());
        }
        Collections.sort(targetDays);

        // Anchor: Monday of the week containing startDate
        LocalDate weekMonday = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1);

        while (!weekMonday.isAfter(lastDate) && matched < maxCount) {
            for (DayOfWeek day : targetDays) {
                LocalDate candidate = weekMonday.plusDays(day.getValue() - 1);

                if (candidate.isBefore(startDate)) continue;
                if (candidate.isAfter(lastDate)) return result;

                long millis = toMillis(LocalDateTime.of(candidate, clockTime));
                if (millis < startTimeMillis) continue;
                if (rule.until != null && millis > rule.until.getTime()) return result;

                matched++;
                if (rule.count > 0 && matched > rule.count) return result;

                if (millis >= rangeStart && millis <= rangeEnd
                        && !isExceptionDate(millis, exceptions)) {
                    result.add(millis);
                }
            }
            weekMonday = weekMonday.plusWeeks(rule.interval);
        }
        return result;
    }

    // ── MONTHLY generator ───────────────────────────────────────────────

    private static List<Long> generateMonthly(LocalDate startDate, LocalTime clockTime,
                                               RecurrenceRule rule, long startTimeMillis,
                                               long rangeStart, long rangeEnd,
                                               Set<String> exceptions, int maxCount,
                                               LocalDate lastDate) {
        List<Long> result = new ArrayList<>();
        int matched = 0;
        YearMonth cursorMonth = YearMonth.from(startDate);
        YearMonth lastMonth = YearMonth.from(lastDate);

        while (!cursorMonth.isAfter(lastMonth) && matched < maxCount) {
            List<LocalDate> candidates = expandMonthCandidates(cursorMonth, startDate, rule);

            for (LocalDate candidate : candidates) {
                if (candidate.isBefore(startDate)) continue;
                if (candidate.isAfter(lastDate)) return result;

                long millis = toMillis(LocalDateTime.of(candidate, clockTime));
                if (millis < startTimeMillis) continue;
                if (rule.until != null && millis > rule.until.getTime()) return result;

                matched++;
                if (rule.count > 0 && matched > rule.count) return result;

                if (millis >= rangeStart && millis <= rangeEnd
                        && !isExceptionDate(millis, exceptions)) {
                    result.add(millis);
                }
            }
            cursorMonth = cursorMonth.plusMonths(rule.interval);
        }
        return result;
    }

    // ── YEARLY generator ────────────────────────────────────────────────

    private static List<Long> generateYearly(LocalDate startDate, LocalTime clockTime,
                                              RecurrenceRule rule, long startTimeMillis,
                                              long rangeStart, long rangeEnd,
                                              Set<String> exceptions, int maxCount,
                                              LocalDate lastDate) {
        List<Long> result = new ArrayList<>();
        int matched = 0;
        int cursorYear = startDate.getYear();
        int lastYear = lastDate.getYear();

        while (cursorYear <= lastYear && matched < maxCount) {
            List<LocalDate> candidates = expandYearCandidates(cursorYear, startDate, rule);

            for (LocalDate candidate : candidates) {
                if (candidate.isBefore(startDate)) continue;
                if (candidate.isAfter(lastDate)) return result;

                long millis = toMillis(LocalDateTime.of(candidate, clockTime));
                if (millis < startTimeMillis) continue;
                if (rule.until != null && millis > rule.until.getTime()) return result;

                matched++;
                if (rule.count > 0 && matched > rule.count) return result;

                if (millis >= rangeStart && millis <= rangeEnd
                        && !isExceptionDate(millis, exceptions)) {
                    result.add(millis);
                }
            }
            cursorYear += rule.interval;
        }
        return result;
    }

    // ── Expansion helpers ───────────────────────────────────────────────

    private static List<LocalDate> expandMonthCandidates(YearMonth yearMonth,
                                                          LocalDate startDate,
                                                          RecurrenceRule rule) {
        // BYMONTHDAY: resolve each day value (supports negative = from end)
        if (rule.byMonthDay != null && !rule.byMonthDay.isEmpty()) {
            List<LocalDate> dates = new ArrayList<>();
            int length = yearMonth.lengthOfMonth();
            for (Integer value : rule.byMonthDay) {
                if (value == null || value == 0) continue;
                int day = value > 0 ? value : length + value + 1;
                if (day > 0 && day <= length) {
                    dates.add(yearMonth.atDay(day));
                }
            }
            Collections.sort(dates);
            return dates;
        }

        // BYDAY (with optional ordinal / BYSETPOS)
        if (rule.byDayEntries != null && !rule.byDayEntries.isEmpty()) {
            return expandByDayInMonth(yearMonth, rule);
        }

        // Default: same day of month as startDate
        int day = Math.min(startDate.getDayOfMonth(), yearMonth.lengthOfMonth());
        List<LocalDate> dates = new ArrayList<>();
        dates.add(yearMonth.atDay(day));
        return dates;
    }

    private static List<LocalDate> expandByDayInMonth(YearMonth yearMonth, RecurrenceRule rule) {
        List<LocalDate> result = new ArrayList<>();
        List<ByDayEntry> entries = rule.byDayEntries;
        if (entries == null || entries.isEmpty()) return result;

        // Separate ordinal entries (e.g. 1MO, -1FR) from plain entries (e.g. MO)
        List<ByDayEntry> ordinalEntries = new ArrayList<>();
        List<ByDayEntry> plainEntries = new ArrayList<>();
        for (ByDayEntry entry : entries) {
            if (entry == null) continue;
            if (entry.ordinal != null) {
                ordinalEntries.add(entry);
            } else {
                plainEntries.add(entry);
            }
        }

        // Resolve ordinal entries directly
        for (ByDayEntry entry : ordinalEntries) {
            LocalDate resolved = resolveOrdinalDayInMonth(yearMonth, entry);
            if (resolved != null) result.add(resolved);
        }

        if (plainEntries.isEmpty()) {
            Collections.sort(result);
            return result;
        }

        // Collect all matching weekdays in month for plain entries
        List<LocalDate> allMatching = new ArrayList<>();
        LocalDate cursor = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        while (!cursor.isAfter(monthEnd)) {
            for (ByDayEntry entry : plainEntries) {
                if (entry.matchesWeekday(cursor.getDayOfWeek())) {
                    allMatching.add(cursor);
                    break;
                }
            }
            cursor = cursor.plusDays(1);
        }

        if (allMatching.isEmpty()) {
            Collections.sort(result);
            return result;
        }

        // Without BYSETPOS, return all matching days + ordinal results
        if (rule.bySetPos == null || rule.bySetPos.isEmpty()) {
            result.addAll(allMatching);
            Collections.sort(result);
            return result;
        }

        // Apply BYSETPOS filter
        for (Integer position : rule.bySetPos) {
            if (position == null || position == 0) continue;
            int index = position > 0 ? position - 1 : allMatching.size() + position;
            if (index >= 0 && index < allMatching.size()) {
                result.add(allMatching.get(index));
            }
        }
        Collections.sort(result);
        return result;
    }

    private static LocalDate resolveOrdinalDayInMonth(YearMonth yearMonth, ByDayEntry entry) {
        if (entry.ordinal == null) return null;

        DayOfWeek targetDay = codeToDayOfWeek(entry.dayCode);
        if (targetDay == null) return null;

        if (entry.ordinal > 0) {
            // e.g. 1MO = first Monday of the month
            LocalDate first = yearMonth.atDay(1);
            int daysUntil = targetDay.getValue() - first.getDayOfWeek().getValue();
            if (daysUntil < 0) daysUntil += 7;
            LocalDate resolved = first.plusDays(daysUntil).plusWeeks(entry.ordinal - 1);
            return resolved.getMonth() == yearMonth.getMonth() ? resolved : null;
        } else {
            // e.g. -1FR = last Friday of the month
            LocalDate last = yearMonth.atEndOfMonth();
            int daysBack = last.getDayOfWeek().getValue() - targetDay.getValue();
            if (daysBack < 0) daysBack += 7;
            LocalDate resolved = last.minusDays(daysBack).plusWeeks(entry.ordinal + 1);
            return resolved.getMonth() == yearMonth.getMonth() ? resolved : null;
        }
    }

    private static List<LocalDate> expandYearCandidates(int year, LocalDate startDate,
                                                         RecurrenceRule rule) {
        List<LocalDate> result = new ArrayList<>();
        int month = startDate.getMonthValue();

        if (rule.byMonthDay != null && !rule.byMonthDay.isEmpty()) {
            YearMonth ym = YearMonth.of(year, month);
            int length = ym.lengthOfMonth();
            for (Integer value : rule.byMonthDay) {
                if (value == null || value == 0) continue;
                int day = value > 0 ? value : length + value + 1;
                if (day > 0 && day <= length) {
                    result.add(ym.atDay(day));
                }
            }
            Collections.sort(result);
            return result;
        }

        // Default: same month and day, skip if invalid (e.g. Feb 29 in non-leap year)
        try {
            result.add(LocalDate.of(year, month, startDate.getDayOfMonth()));
        } catch (java.time.DateTimeException ignored) {
            // Skip: e.g. Feb 29 in non-leap year
        }
        return result;
    }

    private static DayOfWeek codeToDayOfWeek(String code) {
        if (code == null) return null;
        switch (code.toUpperCase(Locale.US)) {
            case "MO": return DayOfWeek.MONDAY;
            case "TU": return DayOfWeek.TUESDAY;
            case "WE": return DayOfWeek.WEDNESDAY;
            case "TH": return DayOfWeek.THURSDAY;
            case "FR": return DayOfWeek.FRIDAY;
            case "SA": return DayOfWeek.SATURDAY;
            case "SU": return DayOfWeek.SUNDAY;
            default: return null;
        }
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