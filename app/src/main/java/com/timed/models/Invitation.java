package com.timed.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * Model for Calendar and Event Invitations
 * Tracks pending invitations, RSVP status (Yes/No/Maybe), and permissions
 */
public class Invitation implements Serializable {
    
    @DocumentId
    private String id;
    // Getter và Setter cho id
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("invitation_type")
    private String invitationType; // "calendar" or "event"

    @PropertyName("calendar_id")
    private String calendarId; // Nullable if invitation_type is "event"

    @PropertyName("event_id")
    private String eventId; // Nullable if invitation_type is "calendar"

    @PropertyName("from_user_id")
    private String fromUserId; // Who sent the invitation

    @PropertyName("from_user_name")
    private String fromUserName;

    @PropertyName("from_user_email")
    private String fromUserEmail;

    @PropertyName("to_user_id")
    private String toUserId; // Who received the invitation

    @PropertyName("to_user_email")
    private String toUserEmail;

    @PropertyName("role")
    private String role; // "admin", "editor", "viewer" (for calendar sharing)

    @PropertyName("status")
    private String status; // "pending", "accepted", "declined", "tentative"

    @PropertyName("title")
    private String title; // Calendar name or Event title

    @PropertyName("message")
    private String message; // Custom invitation message

    @PropertyName("created_at")
    @ServerTimestamp
    private Date createdAt;

    @PropertyName("updated_at")
    @ServerTimestamp
    private Date updatedAt;

    @PropertyName("responded_at")
    private Date respondedAt;

    public Invitation() {
        this.status = "pending";
    }

    // Constructor for Calendar Invitations
    public Invitation(String calendarId, String fromUserId, String fromUserName, 
                     String fromUserEmail, String toUserId, String toUserEmail, 
                     String role, String title, String eventId) {
        this.invitationType = "calendar";
        this.calendarId = calendarId;
        this.eventId = eventId;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserEmail = fromUserEmail;
        this.toUserId = toUserId;
        this.toUserEmail = toUserEmail;
        this.role = role;
        this.status = "pending";
        this.title = title;
    }

    // Constructor for Event Invitations (with calendar context)
    public Invitation(String eventId, String calendarId, String fromUserId, 
                     String fromUserName, String fromUserEmail, String toUserId, 
                     String toUserEmail, String title, int type) {
        this.invitationType = "event";
        this.eventId = eventId;
        this.calendarId = calendarId;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserEmail = fromUserEmail;
        this.toUserId = toUserId;
        this.toUserEmail = toUserEmail;
        this.role = "member";
        this.status = "pending";
        this.title = title;
    }



    @PropertyName("invitation_type")
    public String getInvitationType() {
        return invitationType;
    }

    @PropertyName("invitation_type")
    public void setInvitationType(String invitationType) {
        this.invitationType = invitationType;
    }

    @PropertyName("calendar_id")
    public String getCalendarId() {
        return calendarId;
    }

    @PropertyName("calendar_id")
    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    @PropertyName("event_id")
    public String getEventId() {
        return eventId;
    }

    @PropertyName("event_id")
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @PropertyName("from_user_id")
    public String getFromUserId() {
        return fromUserId;
    }

    @PropertyName("from_user_id")
    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    @PropertyName("from_user_name")
    public String getFromUserName() {
        return fromUserName;
    }

    @PropertyName("from_user_name")
    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    @PropertyName("from_user_email")
    public String getFromUserEmail() {
        return fromUserEmail;
    }

    @PropertyName("from_user_email")
    public void setFromUserEmail(String fromUserEmail) {
        this.fromUserEmail = fromUserEmail;
    }

    @PropertyName("to_user_id")
    public String getToUserId() {
        return toUserId;
    }

    @PropertyName("to_user_id")
    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    @PropertyName("to_user_email")
    public String getToUserEmail() {
        return toUserEmail;
    }

    @PropertyName("to_user_email")
    public void setToUserEmail(String toUserEmail) {
        this.toUserEmail = toUserEmail;
    }

    @PropertyName("role")
    public String getRole() {
        return role;
    }

    @PropertyName("role")
    public void setRole(String role) {
        this.role = role;
    }

    @PropertyName("status")
    public String getStatus() {
        return status;
    }

    @PropertyName("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("title")
    public String getTitle() {
        return title;
    }

    @PropertyName("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @PropertyName("message")
    public String getMessage() {
        return message;
    }

    @PropertyName("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @PropertyName("created_at")
    public Date getCreatedAt() {
        return createdAt;
    }

    @PropertyName("created_at")
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("updated_at")
    public Date getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updated_at")
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PropertyName("responded_at")
    public Date getRespondedAt() {
        return respondedAt;
    }

    @PropertyName("responded_at")
    public void setRespondedAt(Date respondedAt) {
        this.respondedAt = respondedAt;
    }

    @Override
    public String toString() {
        return "Invitation{" +
                "id='" + id + '\'' +
                ", invitationType='" + invitationType + '\'' +
                ", fromUserName='" + fromUserName + '\'' +
                ", toUserEmail='" + toUserEmail + '\'' +
                ", status='" + status + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
