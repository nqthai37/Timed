package com.timed.Features.ConflictResolver;

public class ConflictEvent {
    private String title;
    private String timeRange;
    private String description;
    private String id;
    private boolean isConflicting;
    private Class<?> activity;

    public ConflictEvent(String title, String timeRange, String description, String id, boolean isConflicting, Class<?> activity) {
        this.title = title;
        this.timeRange = timeRange;
        this.description = description;
        this.id = id;
        this.isConflicting = isConflicting;
        this.activity = activity;
    }

    public String getTitle() { return title; }
    public String getTimeRange() { return timeRange; }
    public String getDescription() { return description; }
    public String getId() { return id; }
    public boolean isConflicting() { return isConflicting; }
    public Class<?> getActivity() { return activity; }
}
