package com.timed;

public class Event {
    private String id;
    private String calendarId;
    private String time;
    private String title;
    private String details;
    private String location;
    private long startTime;
    private long endTime;
    private boolean allDay;

    public Event(String time, String title, String details) {
        this.time = time;
        this.title = title;
        this.details = details;
        this.id = null;
        this.calendarId = null;
        this.location = "";
        this.startTime = 0L;
        this.endTime = 0L;
        this.allDay = false;
    }

    public Event(String id, String calendarId, String time, String title, String details,
                 String location, long startTime, long endTime, boolean allDay) {
        this.id = id;
        this.calendarId = calendarId;
        this.time = time;
        this.title = title;
        this.details = details;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.allDay = allDay;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }
}
