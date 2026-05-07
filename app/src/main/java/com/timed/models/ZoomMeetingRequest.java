package com.timed.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model cho Zoom Meeting Request
 */
public class ZoomMeetingRequest {

    @SerializedName("topic")
    private String topic;

    @SerializedName("type")
    private int type; // 1: Instant Meeting, 2: Scheduled Meeting

    @SerializedName("start_time")
    private String startTime; // ISO 8601 format: yyyy-MM-ddTHH:mm:ssZ

    @SerializedName("duration")
    private int duration; // Duration in minutes

    @SerializedName("timezone")
    private String timezone; // Default: UTC

    @SerializedName("settings")
    private MeetingSettings settings;

    public ZoomMeetingRequest() {
    }

    public ZoomMeetingRequest(String topic, String startTime, int duration) {
        this.topic = topic;
        this.type = 2; // Scheduled meeting
        this.startTime = startTime;
        this.duration = duration;
        this.timezone = "UTC";
        this.settings = new MeetingSettings();
    }

    // Getters and Setters
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
        private boolean hostVideo = true;

        @SerializedName("participant_video")
        private boolean participantVideo = true;

        @SerializedName("join_before_host")
        private boolean joinBeforeHost = false;

        @SerializedName("mute_upon_entry")
        private boolean muteUponEntry = false;

        @SerializedName("waiting_room")
        private boolean waitingRoom = false;

        @SerializedName("auto_recording")
        private String autoRecording = "none"; // none, local, cloud

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
