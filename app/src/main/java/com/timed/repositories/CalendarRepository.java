package com.timed.repositories;

import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.timed.models.CalendarModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for managing Calendar operations with Firebase Firestore
 */
public class CalendarRepository {
    private static final String TAG = "CalendarRepository";
    private static final String CALENDARS_COLLECTION = "calendars";

    private final FirebaseFirestore db;

    public CalendarRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new calendar
     */
    public void createCalendar(CalendarModel calendar, RepositoryCallback<String> callback) {
        com.google.firebase.firestore.DocumentReference docRef = db.collection(CALENDARS_COLLECTION).document();
        String calendarId = docRef.getId();
        Map<String, Object> payload = buildCalendarPayload(calendar, calendarId, true);

        docRef.set(payload)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(calendarId);
                    Log.d(TAG, "Calendar created with ID: " + calendarId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to create calendar: " + e.getMessage());
                });
    }

    /**
     * Create or overwrite a calendar with a fixed ID
     */
    public void createCalendarWithId(String calendarId, CalendarModel calendar, RepositoryCallback<String> callback) {
        if (calendarId == null || calendarId.isEmpty()) {
            callback.onFailure("Calendar ID is required");
            return;
        }
        Map<String, Object> payload = buildCalendarPayload(calendar, calendarId, true);
        db.collection(CALENDARS_COLLECTION)
                .document(calendarId)
                .set(payload)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(calendarId);
                    Log.d(TAG, "Calendar created with fixed ID: " + calendarId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to create calendar with fixed ID: " + e.getMessage());
                });
    }

    /**
     * Get calendar by ID
     */
    public void getCalendarById(String calendarId, RepositoryCallback<CalendarModel> callback) {
        db.collection(CALENDARS_COLLECTION)
                .document(calendarId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        CalendarModel calendar = documentSnapshot.toObject(CalendarModel.class);
                        if (calendar != null && calendar.getId() == null) {
                            calendar.setId(documentSnapshot.getId());
                        }
                        callback.onSuccess(calendar);
                    } else {
                        callback.onFailure("Calendar not found");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to fetch calendar: " + e.getMessage());
                });
    }

    /**
     * Get all calendars for a user (owner or member)
     * This queries both:
     * 1. Calendars owned by the user
     * 2. Calendars where the user is a member
     */
    public void getCalendarsByUser(String userId, RepositoryCallback<List<CalendarModel>> callback) {
        // First get calendars where user is a member
        db.collection(CALENDARS_COLLECTION)
                .whereArrayContains("member_ids", userId)
                .get()
                .addOnSuccessListener(memberQuerySnapshot -> {
                    List<CalendarModel> calendars = new ArrayList<>();
                    
                    // Add all member calendars
                    for (int i = 0; i < memberQuerySnapshot.getDocuments().size(); i++) {
                        CalendarModel calendar = memberQuerySnapshot.getDocuments().get(i).toObject(CalendarModel.class);
                        if (calendar != null) {
                            if (calendar.getId() == null) {
                                calendar.setId(memberQuerySnapshot.getDocuments().get(i).getId());
                            }
                            calendars.add(calendar);
                        }
                    }
                    
                    // Now also get owned calendars
                    db.collection(CALENDARS_COLLECTION)
                            .whereEqualTo("owner_id", userId)
                            .get()
                            .addOnSuccessListener(ownedQuerySnapshot -> {
                                // Add owned calendars that aren't already in the list
                                for (int i = 0; i < ownedQuerySnapshot.getDocuments().size(); i++) {
                                    CalendarModel calendar = ownedQuerySnapshot.getDocuments().get(i).toObject(CalendarModel.class);
                                    if (calendar != null) {
                                        if (calendar.getId() == null) {
                                            calendar.setId(ownedQuerySnapshot.getDocuments().get(i).getId());
                                        }
                                        // Check if this calendar is already in our list
                                        boolean exists = false;
                                        for (CalendarModel existing : calendars) {
                                            if (existing.getId() != null && existing.getId().equals(calendar.getId())) {
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            calendars.add(calendar);
                                        }
                                    }
                                }
                                
                                Log.d(TAG, "All calendars for user: " + calendars.size() + " (owned + member)");
                                callback.onSuccess(calendars);
                            })
                            .addOnFailureListener(e -> {
                                // If owned calendars query fails, still return member calendars
                                Log.w(TAG, "Failed to fetch owned calendars: " + e.getMessage());
                                callback.onSuccess(calendars);
                            });
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    callback.onFailure(errorMsg);
                    Log.e(TAG, "Failed to fetch member calendars: " + errorMsg + " | Exception: " + e.toString(), e);
                });
    }

    /**
     * Get calendars owned by a user
     */
    public void getOwnedCalendars(String userId, RepositoryCallback<List<CalendarModel>> callback) {
        db.collection(CALENDARS_COLLECTION)
                .whereEqualTo("owner_id", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<CalendarModel> calendars = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        CalendarModel calendar = querySnapshot.getDocuments().get(i).toObject(CalendarModel.class);
                        if (calendar != null && calendar.getId() == null) {
                            calendar.setId(querySnapshot.getDocuments().get(i).getId());
                        }
                        calendars.add(calendar);
                    }
                    callback.onSuccess(calendars);
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    callback.onFailure(errorMsg);
                    Log.e(TAG, "Failed to fetch owned calendars: " + errorMsg + " | Exception: " + e.toString(), e);
                });
    }

    /**
     * Update a calendar
     */
    public void updateCalendar(String calendarId, CalendarModel calendar, RepositoryCallback<Void> callback) {
        Map<String, Object> payload = buildCalendarPayload(calendar, calendarId, false);
        db.collection(CALENDARS_COLLECTION)
                .document(calendarId)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(null);
                    Log.d(TAG, "Calendar updated: " + calendarId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to update calendar: " + e.getMessage());
                });
    }

    private Map<String, Object> buildCalendarPayload(CalendarModel calendar, String calendarId,
            boolean includeCreateTimestamps) {
        Map<String, Object> payload = new HashMap<>();
        if (calendarId != null && !calendarId.isEmpty()) {
            payload.put("id", calendarId);
        }
        if (calendar != null) {
            payload.put("name", calendar.getName());
            payload.put("description", calendar.getDescription());
            payload.put("owner_id", calendar.getOwnerId());
            payload.put("member_ids", calendar.getMemberIds());
            payload.put("roles", calendar.getRoles());
            payload.put("color", calendar.getColor());
            payload.put("color_name", calendar.getColorName());
            payload.put("type", calendar.getType());
            payload.put("icon", calendar.getIcon());
            payload.put("is_visible", calendar.isVisible());
            payload.put("sort_order", calendar.getSortOrder());
            payload.put("is_archived", calendar.isArchived());
            payload.put("settings", calendar.getSettings());
            payload.put("stats", calendar.getStats());
            payload.put("is_public", calendar.isPublic());
        }
        if (includeCreateTimestamps) {
            payload.put("created_at", FieldValue.serverTimestamp());
        }
        payload.put("updated_at", FieldValue.serverTimestamp());
        return payload;
    }

    /**
     * Delete a calendar
     */
    public void deleteCalendar(String calendarId, RepositoryCallback<Void> callback) {
        db.collection(CALENDARS_COLLECTION)
                .document(calendarId)
                .delete()
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(null);
                    Log.d(TAG, "Calendar deleted: " + calendarId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to delete calendar: " + e.getMessage());
                });
    }

    /**
     * Add a member to a calendar
     */
    public void addMember(String calendarId, String userId, String role, RepositoryCallback<Void> callback) {
        db.collection(CALENDARS_COLLECTION)
                .document(calendarId)
                .update(
                        "member_ids", com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                        "roles." + userId, role)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(null);
                    Log.d(TAG, "Member added to calendar: " + calendarId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to add member: " + e.getMessage());
                });
    }

    /**
     * Remove a member from a calendar
     */
    public void removeMember(String calendarId, String userId, RepositoryCallback<Void> callback) {
        db.collection(CALENDARS_COLLECTION)
                .document(calendarId)
                .update(
                        "member_ids", com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "roles." + userId, com.google.firebase.firestore.FieldValue.delete())
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(null);
                    Log.d(TAG, "Member removed from calendar: " + calendarId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                    Log.e(TAG, "Failed to remove member: " + e.getMessage());
                });
    }
}
