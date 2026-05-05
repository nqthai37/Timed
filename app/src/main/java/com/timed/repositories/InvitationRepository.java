package com.timed.repositories;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.timed.models.Invitation;

/**
 * Repository for managing Invitation operations with Firebase Firestore
 * Handles calendar sharing and event invitations
 */
public class InvitationRepository {
    private static final String INVITATIONS_COLLECTION = "invitations";
    private static final String TAG = "InvitationRepository";

    private FirebaseFirestore db;

    public InvitationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new invitation
     */
    public Task<Void> createInvitation(Invitation invitation, RepositoryCallback<String> callback) {
        return db.collection(INVITATIONS_COLLECTION).document()
                .set(invitation)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation created successfully");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating invitation", e);
                    callback.onFailure("Failed to create invitation: " + e.getMessage());
                });
    }

    /**
     * Get pending invitations for a user
     */
    public Task<QuerySnapshot> getPendingInvitationsForUser(String userId) {
        return db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("to_user_id", userId)
                .whereEqualTo("status", "pending")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnFailureListener(e -> logRepoError("getPendingInvitationsForUser", e));
    }

    /**
     * Get all invitations for a user (pending, accepted, declined)
     */
    public Task<QuerySnapshot> getAllInvitationsForUser(String userId) {
        return db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("to_user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnFailureListener(e -> logRepoError("getAllInvitationsForUser", e));
    }

    /**
     * Get calendar invitations for a specific calendar
     */
    public Task<QuerySnapshot> getCalendarInvitations(String calendarId) {
        return db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("calendar_id", calendarId)
                .whereEqualTo("invitation_type", "calendar")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnFailureListener(e -> logRepoError("getCalendarInvitations", e));
    }

    /**
     * Get event invitations for a specific event
     */
    public Task<QuerySnapshot> getEventInvitations(String eventId) {
        return db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("invitation_type", "event")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnFailureListener(e -> logRepoError("getEventInvitations", e));
    }

    /**
     * Get a specific invitation by ID
     */
    public Task<com.google.firebase.firestore.DocumentSnapshot> getInvitationById(String invitationId) {
        return db.collection(INVITATIONS_COLLECTION)
                .document(invitationId)
                .get()
                .addOnFailureListener(e -> logRepoError("getInvitationById", e));
    }

    /**
     * Update invitation status (accept, decline, tentative)
     */
    public Task<Void> updateInvitationStatus(String invitationId, String status, 
                                            RepositoryCallback<Void> callback) {
        return db.collection(INVITATIONS_COLLECTION)
                .document(invitationId)
                .update(
                    "status", status,
                    "responded_at", com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation status updated to: " + status);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating invitation status", e);
                    callback.onFailure("Failed to update invitation: " + e.getMessage());
                });
    }

    /**
     * Delete an invitation (usually after accepting or declining)
     */
    public Task<Void> deleteInvitation(String invitationId) {
        return db.collection(INVITATIONS_COLLECTION)
                .document(invitationId)
                .delete()
                .addOnFailureListener(e -> logRepoError("deleteInvitation", e));
    }

    /**
     * Check if an invitation already exists between two users for a calendar
     */
    public Task<QuerySnapshot> checkExistingCalendarInvitation(String calendarId, String fromUserId, String toUserId) {
        return db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("calendar_id", calendarId)
                .whereEqualTo("from_user_id", fromUserId)
                .whereEqualTo("to_user_id", toUserId)
                .whereEqualTo("invitation_type", "calendar")
                .get()
                .addOnFailureListener(e -> logRepoError("checkExistingCalendarInvitation", e));
    }

    /**
     * Check if an invitation already exists for an event participant
     */
    public Task<QuerySnapshot> checkExistingEventInvitation(String eventId, String fromUserId, String toUserId) {
        return db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("from_user_id", fromUserId)
                .whereEqualTo("to_user_id", toUserId)
                .whereEqualTo("invitation_type", "event")
                .get()
                .addOnFailureListener(e -> logRepoError("checkExistingEventInvitation", e));
    }

    /**
     * Get sent invitations by a user
     */
    public Task<QuerySnapshot> getSentInvitations(String userId) {
        return db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("from_user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnFailureListener(e -> logRepoError("getSentInvitations", e));
    }

    /**
     * Count pending invitations for a user
     */
    public Task<QuerySnapshot> countPendingInvitations(String userId) {
        return db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("to_user_id", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnFailureListener(e -> logRepoError("countPendingInvitations", e));
    }

    private void logRepoError(String method, Exception e) {
        Log.e(TAG, "Error in " + method + ": " + e.getMessage());
    }
}
