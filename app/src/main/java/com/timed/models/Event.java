package com.timed.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Event {
    @DocumentId
    private String id;

    private String title;
    private String description;
    private String location;

    @PropertyName("start_time")
    private Timestamp startTime;

    @PropertyName("end_time")
    private Timestamp endTime;

    @PropertyName("all_day")
    private Boolean allDay;

    private String timezone;

    @PropertyName("created_by")
    private String createdBy;

    @PropertyName("created_at")
    private Timestamp createdAt;

    @PropertyName("updated_at")
    private Timestamp updatedAt;

    private List<EventReminder> reminders;

    @PropertyName("recurrence_rule")
    private String recurrenceRule;

    @PropertyName("recurrence_exceptions")
    private List<String> recurrenceExceptions;

    @PropertyName("participant_id")
    private List<String> participantId;

    @PropertyName("participant_status")
    private Map<String, String> participantStatus;

    private String visibility; // "public", "private", "confidential"

    @PropertyName("calendar_id")
    private String calendarId;

    @PropertyName("calendar_name")
    private String calendarName;

    private String color;

    private List<EventAttachment> attachments;

    @PropertyName("instance_of")
    private String instanceOf; // ID of recurring event

    public Event() {
        this.allDay = false;
        this.reminders = new ArrayList<>();
        this.participantId = new ArrayList<>();
        this.participantStatus = new HashMap<>();
        this.recurrenceExceptions = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.visibility = "public";
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @PropertyName("start_time")
    public Timestamp getStartTime() { return startTime; }
    @PropertyName("start_time")
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    @PropertyName("end_time")
    public Timestamp getEndTime() { return endTime; }
    @PropertyName("end_time")
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    @PropertyName("all_day")
    public Boolean getAllDay() { return allDay; }
    @PropertyName("all_day")
    public void setAllDay(Boolean allDay) { this.allDay = allDay; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    @PropertyName("created_by")
    public String getCreatedBy() { return createdBy; }
    @PropertyName("created_by")
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @PropertyName("created_at")
    public Timestamp getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @PropertyName("updated_at")
    public Timestamp getUpdatedAt() { return updatedAt; }
    @PropertyName("updated_at")
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public List<EventReminder> getReminders() { return reminders; }
    public void setReminders(List<EventReminder> reminders) { this.reminders = reminders; }

    @PropertyName("recurrence_rule")
    public String getRecurrenceRule() { return recurrenceRule; }
    @PropertyName("recurrence_rule")
    public void setRecurrenceRule(String recurrenceRule) { this.recurrenceRule = recurrenceRule; }

    @PropertyName("recurrence_exceptions")
    public List<String> getRecurrenceExceptions() { return recurrenceExceptions; }
    @PropertyName("recurrence_exceptions")
    public void setRecurrenceExceptions(List<String> recurrenceExceptions) { this.recurrenceExceptions = recurrenceExceptions; }

    @PropertyName("participant_id")
    public List<String> getParticipantId() { return participantId; }
    @PropertyName("participant_id")
    public void setParticipantId(List<String> participantId) { this.participantId = participantId; }

    @PropertyName("participant_status")
    public Map<String, String> getParticipantStatus() { return participantStatus; }
    @PropertyName("participant_status")
    public void setParticipantStatus(Map<String, String> participantStatus) { this.participantStatus = participantStatus; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    @PropertyName("calendar_id")
    public String getCalendarId() { return calendarId; }
    @PropertyName("calendar_id")
    public void setCalendarId(String calendarId) { this.calendarId = calendarId; }

    @PropertyName("calendar_name")
    public String getCalendarName() { return calendarName; }
    @PropertyName("calendar_name")
    public void setCalendarName(String calendarName) { this.calendarName = calendarName; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public List<EventAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<EventAttachment> attachments) { this.attachments = attachments; }

    @PropertyName("instance_of")
    public String getInstanceOf() { return instanceOf; }
    @PropertyName("instance_of")
    public void setInstanceOf(String instanceOf) { this.instanceOf = instanceOf; }

    // Nested class for Event Reminders
    public static class EventReminder {
        @PropertyName("minutes_before")
        private Long minutesBefore;

        private String type; // "push", "email", "sms"

        public EventReminder() {}

        public EventReminder(Long minutesBefore, String type) {
            this.minutesBefore = minutesBefore;
            this.type = type;
        }

        @PropertyName("minutes_before")
        public Long getMinutesBefore() { return minutesBefore; }
        @PropertyName("minutes_before")
        public void setMinutesBefore(Long minutesBefore) { this.minutesBefore = minutesBefore; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    // Nested class for Event Attachments
    public static class EventAttachment {
        private String name;
        private String url;

        public EventAttachment() {}

        public EventAttachment(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
