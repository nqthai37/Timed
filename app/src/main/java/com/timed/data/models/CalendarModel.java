package com.timed.data.models;

import com.google.firebase.firestore.ServerTimestamp;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Calendar (Work, Personal, Team, etc.)
 */
public class CalendarModel implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("owner_id")
    private String ownerId;

    @SerializedName("member_ids")
    private List<String> memberIds; // For fast array-contains queries

    @SerializedName("roles")
    private Map<String, String> roles; // uid -> permission mapping (admin, editor, viewer)

    @SerializedName("color")
    private String color; // Hex color code

    @SerializedName("is_public")
    private boolean isPublic;

    @SerializedName("created_at")
    @ServerTimestamp
    private Date createdAt;

    @SerializedName("updated_at")
    @ServerTimestamp
    private Date updatedAt;

    public CalendarModel() {
        this.memberIds = new ArrayList<>();
        this.roles = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public Map<String, String> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, String> roles) {
        this.roles = roles;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void addMember(String uid, String role) {
        if (!memberIds.contains(uid)) {
            memberIds.add(uid);
        }
        roles.put(uid, role);
    }

    public void removeMember(String uid) {
        memberIds.remove(uid);
        roles.remove(uid);
    }

    public String getMemberRole(String uid) {
        return roles.getOrDefault(uid, "viewer");
    }
}

