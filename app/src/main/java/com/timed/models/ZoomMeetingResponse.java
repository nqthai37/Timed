package com.timed.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model cho Zoom Meeting Response
 */
public class ZoomMeetingResponse {

    @SerializedName("id")
    private long id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("topic")
    private String topic;

    @SerializedName("type")
    private int type;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("duration")
    private int duration;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("join_url")
    private String joinUrl;

    @SerializedName("host_id")
    private String hostId;

    @SerializedName("host_email")
    private String hostEmail;

    @SerializedName("start_url")
    private String startUrl;

    @SerializedName("password")
    private String password;

    @SerializedName("settings")
    private MeetingSettings settings;

    public ZoomMeetingResponse() {
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public void setJoinUrl(String joinUrl) {
        this.joinUrl = joinUrl;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostEmail() {
        return hostEmail;
    }

    public void setHostEmail(String hostEmail) {
        this.hostEmail = hostEmail;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public void setStartUrl(String startUrl) {
        this.startUrl = startUrl;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public MeetingSettings getSettings() {
        return settings;
    }

    public void setSettings(MeetingSettings settings) {
        this.settings = settings;
    }

    /**
     * Nested class cho cài đặt cuộc họp
     */
    public static class MeetingSettings {
        @SerializedName("host_video")
        private boolean hostVideo;

        @SerializedName("participant_video")
        private boolean participantVideo;

        @SerializedName("join_before_host")
        private boolean joinBeforeHost;

        @SerializedName("mute_upon_entry")
        private boolean muteUponEntry;

        @SerializedName("waiting_room")
        private boolean waitingRoom;

        @SerializedName("auto_recording")
        private String autoRecording;

        public boolean isHostVideo() {
            return hostVideo;
        }

        public void setHostVideo(boolean hostVideo) {
            this.hostVideo = hostVideo;
        }

        public boolean isParticipantVideo() {
            return participantVideo;
        }

        public void setParticipantVideo(boolean participantVideo) {
            this.participantVideo = participantVideo;
        }

        public boolean isJoinBeforeHost() {
            return joinBeforeHost;
        }

        public void setJoinBeforeHost(boolean joinBeforeHost) {
            this.joinBeforeHost = joinBeforeHost;
        }

        public boolean isMuteUponEntry() {
            return muteUponEntry;
        }

        public void setMuteUponEntry(boolean muteUponEntry) {
            this.muteUponEntry = muteUponEntry;
        }

        public boolean isWaitingRoom() {
            return waitingRoom;
        }

        public void setWaitingRoom(boolean waitingRoom) {
            this.waitingRoom = waitingRoom;
        }

        public String getAutoRecording() {
            return autoRecording;
        }

        public void setAutoRecording(String autoRecording) {
            this.autoRecording = autoRecording;
        }
    }
}
