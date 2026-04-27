package com.timed.Features.FreeSlotFinder;

public class Date {
    private String displayTitle; // e.g., "Today", "Tomorrow", "Wed 29"
    private long startOfDayMillis; // To pass to your algorithm later

    public Date(String displayTitle, long startOfDayMillis) {
        this.displayTitle = displayTitle;
        this.startOfDayMillis = startOfDayMillis;
    }

    public String getDisplayTitle() { return displayTitle; }
    public long getStartOfDayMillis() { return startOfDayMillis; }
}
