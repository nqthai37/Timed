package com.timed.Features.AI;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class AiPromptTemplate {
    private String id;
    private String title;
    private String promptText;

    @ServerTimestamp
    private Date createdAt;

    public AiPromptTemplate() {}

    public AiPromptTemplate(String title, String promptText) {
        this.title = title;
        this.promptText = promptText;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
