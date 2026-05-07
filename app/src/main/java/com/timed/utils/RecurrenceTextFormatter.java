package com.timed.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecurrenceTextFormatter {
    private static final SimpleDateFormat DATE_DISPLAY = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static String formatSummary(RecurrenceConfig config, long startTimeMillis, boolean includeEnd) {
        if (config == null || !config.enabled) {
            return "Không lặp lại";
        }

        StringBuilder builder = new StringBuilder();
        String freq = config.frequency != null ? config.frequency : RecurrenceUtils.FREQ_WEEKLY;

        if (RecurrenceUtils.FREQ_DAILY.equals(freq)) {
            builder.append(buildIntervalPrefix("ngày", config.interval, true));
        } else if (RecurrenceUtils.FREQ_WEEKLY.equals(freq)) {
            builder.append(buildIntervalPrefix("tuần", config.interval, true));
            List<String> days = config.byDay != null && !config.byDay.isEmpty()
                    ? config.byDay
                    : defaultDayCodes(startTimeMillis);
            builder.append(" vào ").append(formatDayList(days));
        } else if (RecurrenceUtils.FREQ_MONTHLY.equals(freq)) {
            builder.append(buildIntervalPrefix("tháng", config.interval, true));
            if (config.setPos != null && config.setPos != 0 && config.byDay != null && !config.byDay.isEmpty()) {
                builder.append(" vào ").append(formatNthWeekday(config.setPos, config.byDay.get(0)));
            } else {
                int dayOfMonth = config.byMonthDay != null ? config.byMonthDay : getDayOfMonth(startTimeMillis);
                builder.append(" vào ngày ").append(dayOfMonth);
            }
        } else if (RecurrenceUtils.FREQ_YEARLY.equals(freq)) {
            builder.append(buildIntervalPrefix("năm", config.interval, true));
        } else {
            builder.append(buildIntervalPrefix("tuần", config.interval, true));
        }

        if (includeEnd) {
            String endText = buildEndText(config);
            if (!endText.isEmpty()) {
                builder.append(". ").append(endText);
            }
        }

        return builder.toString();
    }

    private static String buildIntervalPrefix(String unit, int interval, boolean includePrefix) {
        StringBuilder builder = new StringBuilder();
        if (includePrefix) {
            builder.append("Lặp ");
        }
        if (interval <= 1) {
            builder.append("hàng ").append(unit);
        } else {
            builder.append("mỗi ").append(interval).append(" ").append(unit);
        }
        return builder.toString();
    }

    private static String buildEndText(RecurrenceConfig config) {
        if (config.endType == RecurrenceConfig.EndType.COUNT && config.count > 0) {
            return "Kết thúc sau " + config.count + " lần lặp";
        }
        if (config.endType == RecurrenceConfig.EndType.UNTIL && config.until != null) {
            return "Kết thúc vào " + DATE_DISPLAY.format(config.until);
        }
        return "";
    }

    private static List<String> defaultDayCodes(long startTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTimeMillis);
        List<String> result = new ArrayList<>();
        result.add(dayCodeFromCalendar(calendar));
        return result;
    }

    private static int getDayOfMonth(long startTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTimeMillis);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    private static String formatDayList(List<String> dayCodes) {
        List<String> labels = new ArrayList<>();
        for (String code : dayCodes) {
            String label = dayCodeToLabel(code);
            if (!label.isEmpty()) {
                labels.add(label);
            }
        }
        return String.join(", ", labels);
    }

    private static String dayCodeToLabel(String code) {
        if (code == null) {
            return "";
        }
        switch (code) {
            case "MO":
                return "T2";
            case "TU":
                return "T3";
            case "WE":
                return "T4";
            case "TH":
                return "T5";
            case "FR":
                return "T6";
            case "SA":
                return "T7";
            case "SU":
                return "CN";
            default:
                return "";
        }
    }

    private static String formatNthWeekday(int setPos, String dayCode) {
        String dayName = dayCodeToFullDayName(dayCode);
        if (dayName.isEmpty()) {
            return "";
        }
        if (setPos == -1) {
            return dayName + " cuối cùng";
        }
        String ordinal;
        switch (setPos) {
            case 1:
                ordinal = "đầu tiên";
                break;
            case 2:
                ordinal = "thứ hai";
                break;
            case 3:
                ordinal = "thứ ba";
                break;
            case 4:
                ordinal = "thứ tư";
                break;
            default:
                ordinal = "đầu tiên";
                break;
        }
        return dayName + " " + ordinal;
    }

    private static String dayCodeToFullDayName(String code) {
        if (code == null) {
            return "";
        }
        switch (code) {
            case "MO":
                return "Thứ Hai";
            case "TU":
                return "Thứ Ba";
            case "WE":
                return "Thứ Tư";
            case "TH":
                return "Thứ Năm";
            case "FR":
                return "Thứ Sáu";
            case "SA":
                return "Thứ Bảy";
            case "SU":
                return "Chủ Nhật";
            default:
                return "";
        }
    }

    private static String dayCodeFromCalendar(Calendar calendar) {
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
                return "MO";
        }
    }
}
