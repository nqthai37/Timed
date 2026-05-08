package com.timed.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model for requesting meeting room creation from backend
 */
public class CreateMeetingRoomRequest {
    @SerializedName("event_id")
    private String eventId;

    @SerializedName("event_title")
    private String eventTitle;

    @SerializedName("event_description")
    private String eventDescription;

    @SerializedName("start_time")
    private long startTime;

    @SerializedName("end_time")
    private long endTime;

    @SerializedName("organizer_id")
    private String organizerId;

    @SerializedName("organizer_email")
    private String organizerEmail;

    @SerializedName("organizer_name")
    private String organizerName;

    @SerializedName("meeting_type")
    private String meetingType; // "NONE", "GOOGLE_MEET", "ZOOM_MEETING"

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("participants")
    private String[] participants;

    public CreateMeetingRoomRequest() {}

    public CreateMeetingRoomRequest(String eventId, String eventTitle, String eventDescription,
                                  long startTime, long endTime, String organizerId,
                                  String organizerEmail, String organizerName, String meetingType, String refreshToken) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventDescription = eventDescription;
        this.startTime = startTime;
        this.endTime = endTime;
        this.organizerId = organizerId;
        this.organizerEmail = organizerEmail;
        this.organizerName = organizerName;
        this.meetingType = meetingType;
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getOrganizerEmail() { return organizerEmail; }
    public void setOrganizerEmail(String organizerEmail) { this.organizerEmail = organizerEmail; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String[] getParticipants() { return participants; }
    public void setParticipants(String[] participants) { this.participants = participants; }
}
