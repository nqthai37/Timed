package com.timed.Features.FreeSlotFinder;

public class FreeSlot {
    private String timeRange;
    private String durationType;
    private String id;

    public FreeSlot(String timeRange, String durationType, String id) {
        this.timeRange = timeRange;
        this.durationType = durationType;
        this.id = id;
    }

    public String getTimeRange() { return timeRange; }
    public String getDurationType() { return durationType; }
    public String getId() { return id; }
}
