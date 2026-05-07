package com.timed.managers;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.timed.dialogs.InvitationsDialog;
import com.timed.dialogs.ShareCalendarDialog;
import com.timed.models.Invitation;
import com.timed.models.User;
import com.timed.repositories.RepositoryCallback;
import com.timed.repositories.UserRepository;
import com.timed.utils.InvitationService;

import java.util.List;

public class MainInvitationController {
    private static final String TAG = "MainInvitationController";

    public interface CalendarRefreshCallbacks {
        String getCalendarId();

        void onCalendarInvitationAccepted();

        void onEventInvitationAccepted();
    }

    private final Activity activity;
    private final FirebaseAuth firebaseAuth;
    private final InvitationManager invitationManager;
    private final InvitationService invitationService;
    private final UserRepository userRepository;
    private final CalendarRefreshCallbacks callbacks;

    private Dialog currentInvitationsDialog;
    private MenuItem invitationsMenuItem;

    public MainInvitationController(Activity activity, FirebaseAuth firebaseAuth,
            InvitationManager invitationManager, InvitationService invitationService,
            UserRepository userRepository, CalendarRefreshCallbacks callbacks) {
        this.activity = activity;
        this.firebaseAuth = firebaseAuth;
        this.invitationManager = invitationManager;
        this.invitationService = invitationService;
        this.userRepository = userRepository;
        this.callbacks = callbacks;
    }

    public void addMenuItems(Menu menu) {
        invitationsMenuItem = menu.add("Invitations");
        invitationsMenuItem.setIcon(android.R.drawable.ic_dialog_email);
        invitationsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        invitationsMenuItem.setOnMenuItemClickListener(item -> {
            showPendingInvitations();
            return true;
        });

        MenuItem shareCalendarItem = menu.add("Share calendar");
        shareCalendarItem.setIcon(android.R.drawable.ic_menu_share);
        shareCalendarItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        shareCalendarItem.setOnMenuItemClickListener(item -> {
            showShareCalendarDialog();
            return true;
        });

        loadInvitationCount();
    }

    public void loadInvitationCount() {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        invitationService.getPendingInvitationCount(currentUserId,
                new InvitationService.InvitationCountListener() {
                    @Override
                    public void onCountLoaded(int count) {
                        if (invitationsMenuItem != null) {
                            invitationsMenuItem.setTitle(count > 0
                                    ? "Invitations (" + count + ")"
                                    : "Invitations");
                        }
                        Log.d(TAG, "Pending invitation count: " + count);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to load invitation count: " + error);
                    }
                });
    }

    public void showShareCalendarDialog() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(activity, "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        ShareCalendarDialog.createShareCalendarDialog(activity,
                new ShareCalendarDialog.OnShareListener() {
                    @Override
                    public void onShare(String email, String role, String message) {
                        shareCalendarWithUser(email, role, message);
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(activity, "Sharing canceled", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    public void showPendingInvitations() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(activity, "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        invitationService.loadPendingInvitations(currentUserId,
                new InvitationService.InvitationLoadListener() {
                    @Override
                    public void onInvitationsLoaded(List<Invitation> invitations) {
                        if (invitations.isEmpty()) {
                            Toast.makeText(activity, "No invitations", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        currentInvitationsDialog = InvitationsDialog.createInvitationsListDialog(activity,
                                invitations,
                                new InvitationsDialog.OnInvitationActionListener() {
                                    @Override
                                    public void onAccept(Invitation invitation) {
                                        handleAcceptInvitation(invitation);
                                    }

                                    @Override
                                    public void onDecline(Invitation invitation) {
                                        handleDeclineInvitation(invitation);
                                    }
                                });
                        currentInvitationsDialog.show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(activity, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void shareCalendarWithUser(String toUserEmail, String role, String message) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(activity, "Please sign in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String currentUserEmail = firebaseAuth.getCurrentUser().getEmail();
        String currentUserName = firebaseAuth.getCurrentUser().getDisplayName();
        String calendarId = callbacks.getCalendarId();

        convertEmailToUserId(toUserEmail, userId -> {
            if (userId == null) {
                Toast.makeText(activity, "No user found with this email", Toast.LENGTH_SHORT).show();
                return;
            }

            invitationManager.inviteToCalendar(calendarId, toUserEmail, userId, role, message,
                    currentUserId, currentUserName, currentUserEmail,
                    new RepositoryCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            Toast.makeText(activity, "Invitation sent", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(activity, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void handleAcceptInvitation(Invitation invitation) {
        String invitationType = invitation.getInvitationType();
        if ("calendar".equals(invitationType)) {
            acceptCalendarInvitation(invitation);
        } else if ("event".equals(invitationType)) {
            acceptEventInvitation(invitation);
        }
    }

    private void acceptCalendarInvitation(Invitation invitation) {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        invitationManager.acceptCalendarInvitation(invitation.getId(), currentUserId,
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(activity, "Invitation accepted", Toast.LENGTH_SHORT).show();
                        dismissInvitationsDialog();
                        callbacks.onCalendarInvitationAccepted();
                        loadInvitationCount();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(activity, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void acceptEventInvitation(Invitation invitation) {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        invitationManager.respondToEventInvitation(invitation.getId(), currentUserId, "accepted",
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(activity, "You will attend this event", Toast.LENGTH_SHORT).show();
                        dismissInvitationsDialog();
                        callbacks.onEventInvitationAccepted();
                        loadInvitationCount();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(activity, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleDeclineInvitation(Invitation invitation) {
        String invitationType = invitation.getInvitationType();
        if ("calendar".equals(invitationType)) {
            invitationManager.declineCalendarInvitation(invitation.getId(), declineCallback());
        } else if ("event".equals(invitationType)) {
            invitationManager.declineEventInvitation(invitation.getId(), declineCallback());
        }
    }

    private RepositoryCallback<String> declineCallback() {
        return new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Toast.makeText(activity, "Invitation declined", Toast.LENGTH_SHORT).show();
                dismissInvitationsDialog();
                loadInvitationCount();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(activity, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void convertEmailToUserId(String email, EmailToUserIdCallback callback) {
        if (email == null || email.isEmpty()) {
            Toast.makeText(activity, "Email is required", Toast.LENGTH_SHORT).show();
            callback.onResult(null);
            return;
        }

        String normalizedEmail = email.toLowerCase().trim();
        userRepository.getUserByEmail(normalizedEmail)
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        tryAlternativeEmailSearch(normalizedEmail, callback);
                        return;
                    }
                    callback.onResult(querySnapshot.getDocuments().get(0).getId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Lookup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onResult(null);
                });
    }

    private void tryAlternativeEmailSearch(String email, EmailToUserIdCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null && user.getEmail() != null
                                && user.getEmail().toLowerCase().trim().equals(email)) {
                            callback.onResult(doc.getId());
                            return;
                        }
                    }
                    callback.onResult(null);
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    private void dismissInvitationsDialog() {
        if (currentInvitationsDialog != null && currentInvitationsDialog.isShowing()) {
            currentInvitationsDialog.dismiss();
        }
    }

    private interface EmailToUserIdCallback {
        void onResult(String userId);
    }
}
