package com.timed.utils;

import android.content.Context;
import android.util.Log;

import com.timed.managers.InvitationManager;
import com.timed.models.Invitation;
import com.timed.repositories.RepositoryCallback;

import java.util.List;

/**
 * Service for integrating InvitationManager with UI components
 * Handles calendar sharing and event invitation operations for the UI layer
 */
public class InvitationService {
    private static final String TAG = "InvitationService";

    private InvitationManager invitationManager;
    private Context context;

    public interface InvitationLoadListener {
        void onInvitationsLoaded(List<Invitation> invitations);
        void onError(String errorMessage);
    }

    public interface InvitationSaveListener {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface InvitationCountListener {
        void onCountLoaded(int count);
        void onError(String errorMessage);
    }

    public InvitationService(Context context) {
        this.context = context;
        this.invitationManager = InvitationManager.getInstance(context);
    }

    /**
     * Send a calendar sharing invitation
     * @param calendarId Calendar ID to share
     * @param toUserEmail Email of the user to invite
     * @param toUserId UID of the user to invite
     * @param role Permission role (admin, editor, viewer)
     * @param message Optional custom message
     * @param listener Callback listener
     */
    public void inviteToCalendar(String calendarId, String toUserEmail, String toUserId, 
                                String role, String message, String fromUserId, 
                                String fromUserName, String fromUserEmail,
                                InvitationSaveListener listener) {
        invitationManager.inviteToCalendar(calendarId, toUserEmail, toUserId, role, message, 
                fromUserId, fromUserName, fromUserEmail,
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "Calendar invitation sent");
                        listener.onSuccess(result);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error sending calendar invitation: " + errorMessage);
                        listener.onError(errorMessage);
                    }
                });
    }

    /**
     * Send an event invitation
     * @param eventId Event ID to invite to
     * @param calendarId Calendar ID containing the event
     * @param toUserEmail Email of the user to invite
     * @param toUserId UID of the user to invite
     * @param message Optional custom message
     * @param listener Callback listener
     */
    public void inviteToEvent(String eventId, String calendarId, String toUserEmail, 
                             String toUserId, String fromUserId, String fromUserName, 
                             String fromUserEmail, String message,
                             InvitationSaveListener listener) {
        invitationManager.inviteToEvent(eventId, calendarId, toUserEmail, toUserId, 
                fromUserId, fromUserName, fromUserEmail, message,
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "Event invitation sent");
                        listener.onSuccess(result);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error sending event invitation: " + errorMessage);
                        listener.onError(errorMessage);
                    }
                });
    }

    /**
     * Accept a calendar invitation
     */
    public void acceptCalendarInvitation(String invitationId, String userId,
                                        InvitationSaveListener listener) {
        invitationManager.acceptCalendarInvitation(invitationId, userId,
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "Calendar invitation accepted");
                        listener.onSuccess("Calendar added successfully!");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error accepting calendar invitation: " + errorMessage);
                        listener.onError(errorMessage);
                    }
                });
    }

    /**
     * Respond to an event invitation (Yes/No/Maybe)
     * @param rsvpStatus "accepted", "declined", or "tentative"
     */
    public void respondToEventInvitation(String invitationId, String userId, String rsvpStatus,
                                        InvitationSaveListener listener) {
        invitationManager.respondToEventInvitation(invitationId, userId, rsvpStatus,
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "Event invitation responded");
                        listener.onSuccess("Response recorded: " + result);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error responding to event invitation: " + errorMessage);
                        listener.onError(errorMessage);
                    }
                });
    }

    /**
     * Decline a calendar invitation
     */
    public void declineCalendarInvitation(String invitationId,
                                         InvitationSaveListener listener) {
        invitationManager.declineCalendarInvitation(invitationId,
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "Calendar invitation declined");
                        listener.onSuccess("Invitation declined");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error declining calendar invitation: " + errorMessage);
                        listener.onError(errorMessage);
                    }
                });
    }

    /**
     * Decline an event invitation
     */
    public void declineEventInvitation(String invitationId,
                                      InvitationSaveListener listener) {
        invitationManager.declineEventInvitation(invitationId,
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "Event invitation declined");
                        listener.onSuccess("Invitation declined");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error declining event invitation: " + errorMessage);
                        listener.onError(errorMessage);
                    }
                });
    }

    /**
     * Load pending invitations for the current user
     */
    public void loadPendingInvitations(String userId, InvitationLoadListener listener) {
        invitationManager.getPendingInvitations(userId)
                .addOnSuccessListener(invitations -> {
                    Log.d(TAG, "Pending invitations loaded: " + invitations.size());
                    listener.onInvitationsLoaded(invitations);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading pending invitations", e);
                    listener.onError("Failed to load invitations: " + e.getMessage());
                });
    }

    /**
     * Load all invitations for the current user
     */
    public void loadAllInvitations(String userId, InvitationLoadListener listener) {
        invitationManager.getAllInvitations(userId)
                .addOnSuccessListener(invitations -> {
                    Log.d(TAG, "All invitations loaded: " + invitations.size());
                    listener.onInvitationsLoaded(invitations);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading all invitations", e);
                    listener.onError("Failed to load invitations: " + e.getMessage());
                });
    }

    /**
     * Load calendar invitations
     */
    public void loadCalendarInvitations(String calendarId, InvitationLoadListener listener) {
        invitationManager.getCalendarInvitations(calendarId)
                .addOnSuccessListener(invitations -> {
                    Log.d(TAG, "Calendar invitations loaded: " + invitations.size());
                    listener.onInvitationsLoaded(invitations);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading calendar invitations", e);
                    listener.onError("Failed to load invitations: " + e.getMessage());
                });
    }

    /**
     * Load event invitations
     */
    public void loadEventInvitations(String eventId, InvitationLoadListener listener) {
        invitationManager.getEventInvitations(eventId)
                .addOnSuccessListener(invitations -> {
                    Log.d(TAG, "Event invitations loaded: " + invitations.size());
                    listener.onInvitationsLoaded(invitations);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event invitations", e);
                    listener.onError("Failed to load invitations: " + e.getMessage());
                });
    }

    /**
     * Get count of pending invitations
     */
    public void getPendingInvitationCount(String userId, InvitationCountListener listener) {
        invitationManager.getPendingInvitationCount(userId)
                .addOnSuccessListener(count -> {
                    Log.d(TAG, "Pending invitations count: " + count);
                    listener.onCountLoaded(count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting invitation count", e);
                    listener.onError("Failed to get count: " + e.getMessage());
                });
    }
}
