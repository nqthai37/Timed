package com.timed.utils;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RecurrenceUtilsTest {

    private long toMillis(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), hour, minute)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private LocalDate toLocalDate(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    @Test
    public void monthlyByDayAndBySetPos_generatesFirstMonday() {
        long start = toMillis(LocalDate.of(2026, 1, 5), 9, 0);
        long rangeStart = toMillis(LocalDate.of(2026, 1, 1), 0, 0);
        long rangeEnd = toMillis(LocalDate.of(2026, 3, 31), 23, 59);

        RecurrenceUtils.RecurrenceRule rule = RecurrenceUtils.parseRRule(
                "FREQ=MONTHLY;INTERVAL=1;BYDAY=MO;BYSETPOS=1"
        );

        List<Long> occurrences = RecurrenceUtils.generateOccurrencesInRange(
                start,
                rule,
                rangeStart,
                rangeEnd,
                null,
                32
        );

        assertEquals(3, occurrences.size());
        assertEquals(LocalDate.of(2026, 1, 5), toLocalDate(occurrences.get(0)));
        assertEquals(LocalDate.of(2026, 2, 2), toLocalDate(occurrences.get(1)));
        assertEquals(LocalDate.of(2026, 3, 2), toLocalDate(occurrences.get(2)));
    }

    @Test
    public void weeklyRule_skipsExceptionDate() {
        long start = toMillis(LocalDate.of(2026, 1, 5), 9, 0);
        long rangeStart = toMillis(LocalDate.of(2026, 1, 1), 0, 0);
        long rangeEnd = toMillis(LocalDate.of(2026, 1, 31), 23, 59);

        RecurrenceUtils.RecurrenceRule rule = RecurrenceUtils.parseRRule(
                "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO"
        );

        List<Long> occurrences = RecurrenceUtils.generateOccurrencesInRange(
                start,
                rule,
                rangeStart,
                rangeEnd,
                Arrays.asList("2026-01-12"),
                64
        );

        assertEquals(3, occurrences.size());
        assertEquals(LocalDate.of(2026, 1, 5), toLocalDate(occurrences.get(0)));
        assertEquals(LocalDate.of(2026, 1, 19), toLocalDate(occurrences.get(1)));
        assertEquals(LocalDate.of(2026, 1, 26), toLocalDate(occurrences.get(2)));
    }

    @Test
    public void parser_supportsOrdinalByDayToken() {
        RecurrenceUtils.RecurrenceRule rule = RecurrenceUtils.parseRRule(
                "FREQ=MONTHLY;INTERVAL=1;BYDAY=1MO"
        );

        assertNotNull(rule.byDayEntries);
        assertEquals(1, rule.byDayEntries.size());
        assertEquals(Integer.valueOf(1), rule.byDayEntries.get(0).ordinal);
        assertEquals("MO", rule.byDayEntries.get(0).dayCode);
    }
}
