package com.timed.managers;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.timed.models.CalendarModel;
import com.timed.models.Event;
import com.timed.models.Invitation;
import com.timed.models.User;
import com.timed.repositories.CalendarRepository;
import com.timed.repositories.EventsRepository;
import com.timed.repositories.InvitationRepository;
import com.timed.repositories.RepositoryCallback;
import com.timed.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Manager for handling invitation operations
 * Supports both calendar sharing invitations and event invitations with RSVP
 */
public class InvitationManager {
    private static final String TAG = "InvitationManager";
    private static InvitationManager instance;

    private InvitationRepository invitationRepository;
    private CalendarRepository calendarRepository;
    private EventsRepository eventsRepository;
    private UserRepository userRepository;
    private CalendarManager calendarManager;
    private EventsManager eventsManager;

    private InvitationManager(Context context) {
        this.invitationRepository = new InvitationRepository();
        this.calendarRepository = new CalendarRepository();
        this.eventsRepository = new EventsRepository();
        this.userRepository = new UserRepository();
        this.calendarManager = new CalendarManager();
        this.eventsManager = EventsManager.getInstance(context);
    }

    public static synchronized InvitationManager getInstance(Context context) {
        if (instance == null) {
            instance = new InvitationManager(context);
        }
        return instance;
    }

    /**
     * Send a calendar sharing invitation
     */
    public void inviteToCalendar(String calendarId, String toUserEmail, String toUserId, 
                                      String role, String message, String fromUserId, 
                                      String fromUserName, String fromUserEmail,
                                      RepositoryCallback<String> callback) {
        // Check if already invited
        invitationRepository.checkExistingCalendarInvitation(calendarId, fromUserId, toUserId)
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.size() > 0) {
                        callback.onFailure("User already invited to this calendar");
                        return;
                    }

                    // Get calendar details
                    calendarRepository.getCalendarById(calendarId, new RepositoryCallback<CalendarModel>() {
                        @Override
                        public void onSuccess(CalendarModel calendar) {
                            if (calendar == null || calendar.getId() == null) {
                                callback.onFailure("Calendar not found");
                                return;
                            }
                            if (fromUserId == null || !fromUserId.equals(calendar.getOwnerId())) {
                                callback.onFailure("Only the calendar owner can share this calendar");
                                return;
                            }
                            if (!"editor".equals(role) && !"viewer".equals(role)) {
                                callback.onFailure("Invalid calendar permission");
                                return;
                            }
                            if (calendar.getMemberIds() != null && calendar.getMemberIds().contains(toUserId)) {
                                callback.onFailure("User already has access to this calendar");
                                return;
                            }

                            Invitation invitation = new Invitation(
                                    calendarId,
                                    fromUserId,
                                    fromUserName,
                                    fromUserEmail,
                                    toUserId,
                                    toUserEmail,
                                    role,
                                    calendar.getName(),
                                    null
                            );
                            if (message != null && !message.isEmpty()) {
                                invitation.setMessage(message);
                            }

                            calendarManager.addMember(calendarId, toUserId, role,
                                    new RepositoryCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            invitationRepository.createInvitation(invitation,
                                                    new RepositoryCallback<String>() {
                                                        @Override
                                                        public void onSuccess(String result) {
                                                            callback.onSuccess("Invitation sent");
                                                        }

                                                        @Override
                                                        public void onFailure(String errorMessage) {
                                                            callback.onFailure(errorMessage);
                                                        }
                                                    });
                                        }

                                        @Override
                                        public void onFailure(String errorMessage) {
                                            callback.onFailure(errorMessage);
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            callback.onFailure("Calendar not found");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending calendar invitation", e);
                    callback.onFailure("Error: " + e.getMessage());
                });
    }

    /**
     * Send an event invitation
     */
    public void inviteToEvent(String eventId, String calendarId, String toUserEmail, 
                                   String toUserId, String fromUserId, String fromUserName, 
                                   String fromUserEmail, String message,
                                   RepositoryCallback<String> callback) {
        // Check if already invited
        invitationRepository.checkExistingEventInvitation(eventId, fromUserId, toUserId)
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.size() > 0) {
                        callback.onFailure("User already invited to this event");
                        return;
                    }

                    // Get event details
                    eventsRepository.getEventById(eventId).addOnSuccessListener(eventDoc -> {
                        if (!eventDoc.exists()) {
                            callback.onFailure("Event not found");
                            return;
                        }

                        Event event = eventDoc.toObject(Event.class);
                        Invitation invitation = new Invitation(
                                eventId,
                                calendarId,
                                fromUserId,
                                fromUserName,
                                fromUserEmail,
                                toUserId,
                                toUserEmail,
                                event != null ? event.getTitle() : "Event Invitation",
                                1
                        );
                        if (message != null && !message.isEmpty()) {
                            invitation.setMessage(message);
                        }

                        invitationRepository.createInvitation(invitation,
                                new RepositoryCallback<String>() {
                                    @Override
                                    public void onSuccess(String result) {
                                        // Also add as participant with pending status
                                        eventsManager.addParticipant(eventId, toUserId, "pending");
                                        callback.onSuccess("Invitation sent");
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        callback.onFailure(errorMessage);
                                    }
                                });
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting event", e);
                        callback.onFailure("Event not found");
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending event invitation", e);
                    callback.onFailure("Error: " + e.getMessage());
                });
    }

    /**
     * Accept a calendar invitation
     */
    public void acceptCalendarInvitation(String invitationId, String userId,
                                              RepositoryCallback<String> callback) {
        invitationRepository.getInvitationById(invitationId)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("Invitation not found");
                        return;
                    }

                    Invitation invitation = doc.toObject(Invitation.class);
                    if (invitation == null) {
                        callback.onFailure("Invitation not found");
                        return;
                    }

                    String calendarId = invitation.getCalendarId();
                    String role = invitation.getRole();

                    calendarRepository.getCalendarById(calendarId, new RepositoryCallback<CalendarModel>() {
                        @Override
                        public void onSuccess(CalendarModel calendar) {
                            if (calendar == null || calendar.getMemberIds() == null
                                    || !calendar.getMemberIds().contains(userId)) {
                                callback.onFailure("Calendar access was not granted by the owner");
                                return;
                            }

                            String grantedRole = calendar.getMemberRole(userId);
                            if (role != null && grantedRole != null && !role.equals(grantedRole)) {
                                callback.onFailure("Calendar permission does not match this invitation");
                                return;
                            }

                            invitationRepository.updateInvitationStatus(invitationId, "accepted",
                                    new RepositoryCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void result2) {
                                            callback.onSuccess("Calendar invitation accepted");
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            callback.onFailure(error);
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            callback.onFailure(errorMessage);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error accepting calendar invitation", e);
                    callback.onFailure("Error: " + e.getMessage());
                });
    }

    /**
     * Accept an event invitation with RSVP status (accepted/tentative/declined)
     */
    public void respondToEventInvitation(String invitationId, String userId, String rsvpStatus,
                                              RepositoryCallback<String> callback) {
        invitationRepository.getInvitationById(invitationId)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("Invitation not found");
                        return;
                    }

                    Invitation invitation = doc.toObject(Invitation.class);
                    if (invitation == null) {
                        callback.onFailure("Invitation not found");
                        return;
                    }

                    String eventId = invitation.getEventId();

                    // Update participant status in event
                    eventsManager.updateParticipantStatus(eventId, userId, rsvpStatus)
                            .addOnSuccessListener(aVoid -> {
                                // Update invitation status
                                invitationRepository.updateInvitationStatus(invitationId, rsvpStatus,
                                        new RepositoryCallback<Void>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                                String message = "RSVP: " + formatRsvpStatus(rsvpStatus);
                                                callback.onSuccess(message);
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                                callback.onFailure(error);
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating participant status", e);
                                callback.onFailure("Failed to update participant status");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error responding to event invitation", e);
                    callback.onFailure("Error: " + e.getMessage());
                });
    }

    /**
     * Decline a calendar invitation
     */
    public void declineCalendarInvitation(String invitationId,
                                               RepositoryCallback<String> callback) {
        invitationRepository.updateInvitationStatus(invitationId, "declined",
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        callback.onSuccess("Invitation declined");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        callback.onFailure(errorMessage);
                    }
                });
    }

    /**
     * Decline an event invitation
     */
    public void declineEventInvitation(String invitationId,
                                            RepositoryCallback<String> callback) {
        invitationRepository.updateInvitationStatus(invitationId, "declined",
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        callback.onSuccess("Invitation declined");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        callback.onFailure(errorMessage);
                    }
                });
    }

    /**
     * Get pending invitations for a user
     */
    public Task<List<Invitation>> getPendingInvitations(String userId) {
        return invitationRepository.getPendingInvitationsForUser(userId)
                .continueWith(task -> {
                    List<Invitation> invitations = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            Invitation invitation = document.toObject(Invitation.class);
                            if (invitation != null) {
                                invitations.add(invitation);
                            }
                        }
                    }
                    return invitations;
                });
    }

    /**
     * Get all invitations for a user
     */
    public Task<List<Invitation>> getAllInvitations(String userId) {
        return invitationRepository.getAllInvitationsForUser(userId)
                .continueWith(task -> {
                    List<Invitation> invitations = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            Invitation invitation = document.toObject(Invitation.class);
                            if (invitation != null) {
                                invitations.add(invitation);
                            }
                        }
                    }
                    return invitations;
                });
    }

    /**
     * Get calendar invitations
     */
    public Task<List<Invitation>> getCalendarInvitations(String calendarId) {
        return invitationRepository.getCalendarInvitations(calendarId)
                .continueWith(task -> {
                    List<Invitation> invitations = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            Invitation invitation = document.toObject(Invitation.class);
                            if (invitation != null) {
                                invitations.add(invitation);
                            }
                        }
                    }
                    return invitations;
                });
    }

    /**
     * Get event invitations
     */
    public Task<List<Invitation>> getEventInvitations(String eventId) {
        return invitationRepository.getEventInvitations(eventId)
                .continueWith(task -> {
                    List<Invitation> invitations = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            Invitation invitation = document.toObject(Invitation.class);
                            if (invitation != null) {
                                invitations.add(invitation);
                            }
                        }
                    }
                    return invitations;
                });
    }

    /**
     * Get pending invitation count for a user
     */
    public Task<Integer> getPendingInvitationCount(String userId) {
        return invitationRepository.countPendingInvitations(userId)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return (int) task.getResult().size();
                    }
                    return 0;
                });
    }

    private String formatRsvpStatus(String status) {
        switch (status) {
            case "accepted":
                return "Yes, I'll be there";
            case "declined":
                return "No, I can't make it";
            case "tentative":
                return "Maybe, still thinking";
            default:
                return "Pending";
        }
    }
}
