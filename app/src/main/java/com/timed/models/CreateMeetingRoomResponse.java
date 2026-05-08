package com.timed.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model for the response from meeting room creation request
 */
public class CreateMeetingRoomResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("event_id")
    private String eventId;

    @SerializedName("meeting_type")
    private String meetingType;

    @SerializedName("meeting_url")
    private String meetingUrl;

    @SerializedName("meeting_id")
    private String meetingId;

    @SerializedName("meeting_code")
    private String meetingCode;

    @SerializedName("created_at")
    private long createdAt;

    @SerializedName("error")
    private String error;

    @SerializedName("error_code")
    private int errorCode;

    public CreateMeetingRoomResponse() {}

    public CreateMeetingRoomResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }

    public String getMeetingId() { return meetingId; }
    public void setMeetingId(String meetingId) { this.meetingId = meetingId; }

    public String getMeetingCode() { return meetingCode; }
    public void setMeetingCode(String meetingCode) { this.meetingCode = meetingCode; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }
}
