package com.timed.models;

public class CalendarEventResponse {
    public boolean success;
    public String eventId;
    public String htmlLink;
    public String hangoutLink;
    public String summary;
    public String startTime;
    public String endTime;
    public String description;
    public ConferenceData conferenceData;

    public boolean isSuccess() {
        return success;
    }

    public String getHangoutLink() {
        return hangoutLink;
    }

    public String getHtmlLink() {
        return htmlLink;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSummary() {
        return summary;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public ConferenceData getConferenceData() {
        return conferenceData;
    }
}
