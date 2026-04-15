package com.timed.models;

public class Feature {
    private String name;
    private String description;
    private int iconDrawableId;
    private Class<?> activityClass;
    private String headerName;

    public Feature(String name, String description, int iconDrawableId, Class<?> activityClass, String headerName) {
        this.name = name;
        this.description = description;
        this.iconDrawableId = iconDrawableId;
        this.activityClass = activityClass;
        this.headerName = headerName;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getIconDrawableId() { return iconDrawableId; }
    public Class<?> getActivityClass() { return activityClass; }
    public String getHeaderName() { return headerName; }
}