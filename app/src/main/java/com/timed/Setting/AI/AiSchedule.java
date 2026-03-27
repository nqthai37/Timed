package com.timed.Setting.AI;

public class AiSchedule {
    private String title;
    private String status;
    private boolean isSuccess;

    public AiSchedule(String title, String status, boolean isSuccess) {
        this.title = title;
        this.status = status;
        this.isSuccess = isSuccess;
    }

    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public boolean isSuccess() { return isSuccess; }
}