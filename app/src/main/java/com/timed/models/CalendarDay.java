package com.timed.models;

import java.time.LocalDate;

public class CalendarDay {
    public final LocalDate date;
    public final boolean isCurrentMonth;
    public boolean isSelected;
    public boolean isToday;
    public int eventCount;

    public CalendarDay(LocalDate date, boolean isCurrentMonth) {
        this.date = date;
        this.isCurrentMonth = isCurrentMonth;
        this.isSelected = false;
        this.isToday = false;
        this.eventCount = 0;
    }
}
