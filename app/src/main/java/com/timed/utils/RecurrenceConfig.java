package com.timed.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class RecurrenceConfig {
    public enum EndType {
        NEVER,
        UNTIL,
        COUNT
    }

    public boolean enabled = false;
    public String frequency = RecurrenceUtils.FREQ_WEEKLY;
    public int interval = 1;
    public List<String> byDay = new ArrayList<>();
    public Integer byMonthDay;
    public Integer setPos;
    public EndType endType = EndType.NEVER;
    public Date until;
    public int count = 10;
    public List<String> exceptions = new ArrayList<>();

    public static RecurrenceConfig disabled() {
        RecurrenceConfig config = new RecurrenceConfig();
        config.enabled = false;
        return config;
    }

    public static RecurrenceConfig fromRRule(String rrule) {
        RecurrenceConfig config = new RecurrenceConfig();
        if (rrule == null || rrule.trim().isEmpty()) {
            config.enabled = false;
            return config;
        }

        RecurrenceUtils.RecurrenceRule rule = RecurrenceUtils.parseRRule(rrule);
        config.enabled = true;
        config.frequency = rule.frequency;
        config.interval = rule.interval;
        config.byDay = rule.byDay != null ? new ArrayList<>(rule.byDay) : new ArrayList<>();
        if (rule.byMonthDay != null && !rule.byMonthDay.isEmpty()) {
            config.byMonthDay = rule.byMonthDay.get(0);
        }
        if (rule.setPos != 0) {
            config.setPos = rule.setPos;
            config.byMonthDay = null;
        }
        if (rule.count > 0) {
            config.endType = EndType.COUNT;
            config.count = rule.count;
        } else if (rule.until != null) {
            config.endType = EndType.UNTIL;
            config.until = rule.until;
        } else {
            config.endType = EndType.NEVER;
        }
        return config;
    }

    public void applyStartDefaults(long startTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTimeMillis);

        if (RecurrenceUtils.FREQ_WEEKLY.equals(frequency) && (byDay == null || byDay.isEmpty())) {
            byDay = new ArrayList<>();
            byDay.add(dayCodeFromCalendar(calendar));
        }

        if (RecurrenceUtils.FREQ_MONTHLY.equals(frequency) && (byMonthDay == null || byMonthDay == 0)
                && (setPos == null || setPos == 0)) {
            byMonthDay = calendar.get(Calendar.DAY_OF_MONTH);
        }
    }

    public RecurrenceUtils.RecurrenceRule toRule() {
        RecurrenceUtils.RecurrenceRule rule = new RecurrenceUtils.RecurrenceRule();
        rule.frequency = frequency;
        rule.interval = interval;
        rule.byDay = byDay != null && !byDay.isEmpty() ? new ArrayList<>(byDay) : null;
        if (byMonthDay != null && byMonthDay > 0) {
            rule.byMonthDay = new ArrayList<>();
            rule.byMonthDay.add(byMonthDay);
        }
        if (setPos != null && setPos != 0) {
            rule.setPos = setPos;
        }
        if (endType == EndType.COUNT && count > 0) {
            rule.count = count;
        }
        if (endType == EndType.UNTIL && until != null) {
            rule.until = until;
        }
        return rule;
    }

    public String toRRuleString() {
        if (!enabled) {
            return "";
        }
        return toRule().toString();
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
