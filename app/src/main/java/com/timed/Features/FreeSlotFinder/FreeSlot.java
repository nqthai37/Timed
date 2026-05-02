package com.timed.Features.FreeSlotFinder;

public class FreeSlot {
    private String timeRange;
    private String durationType;
    private String id;
    private long startMillis;
    private long endMillis;

    public FreeSlot(String timeRange, String durationType, String id, long startMillis, long endMillis) {
        this.timeRange = timeRange;
        this.durationType = durationType;
        this.id = id;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    public String getTimeRange() { return timeRange; }
    public String getDurationType() { return durationType; }
    public String getId() { return id; }
    public long getStartMillis() { return startMillis; }
    public long getEndMillis() { return endMillis; }
}
