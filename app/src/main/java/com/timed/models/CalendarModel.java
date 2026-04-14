package com.timed.models;

import com.google.firebase.firestore.PropertyName;
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
    private String id;

    private String name;

    private String description;

    private String ownerId;

    private List<String> memberIds; // For fast array-contains queries

    private Map<String, String> roles; // uid -> permission mapping (admin, editor, viewer)

    private String color; // Hex color code

    private boolean isPublic;

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date updatedAt;

    public CalendarModel() {
        this.memberIds = new ArrayList<>();
        this.roles = new HashMap<>();
    }

    @PropertyName("id")
    public String getId() {
        return id;
    }

    @PropertyName("id")
    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("name")
    public String getName() {
        return name;
    }

    @PropertyName("name")
    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("description")
    public String getDescription() {
        return description;
    }

    @PropertyName("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("owner_id")
    public String getOwnerId() {
        return ownerId;
    }

    @PropertyName("owner_id")
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @PropertyName("member_ids")
    public List<String> getMemberIds() {
        return memberIds;
    }

    @PropertyName("member_ids")
    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    @PropertyName("roles")
    public Map<String, String> getRoles() {
        return roles;
    }

    @PropertyName("roles")
    public void setRoles(Map<String, String> roles) {
        this.roles = roles;
    }

    @PropertyName("color")
    public String getColor() {
        return color;
    }

    @PropertyName("color")
    public void setColor(String color) {
        this.color = color;
    }

    @PropertyName("is_public")
    public boolean isPublic() {
        return isPublic;
    }

    @PropertyName("is_public")
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
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
