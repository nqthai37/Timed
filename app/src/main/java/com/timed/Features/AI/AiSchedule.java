package com.timed.Features.AI;

public class AiSchedule {
    private String id;
    private String title;
    private String status;
    private boolean isSuccess;

    public AiSchedule(String id, String title, String status, boolean isSuccess) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.isSuccess = isSuccess;
    }

    public  String getId() { return id; }

    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public boolean isSuccess() { return isSuccess; }
}