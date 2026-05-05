package com.timed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.timed.models.Invitation;

import java.util.List;

/**
 * Dialog for displaying and managing pending invitations
 */
public class InvitationsDialog {

    public interface OnInvitationActionListener {
        void onAccept(Invitation invitation);
        void onDecline(Invitation invitation);
    }

    public static Dialog createInvitationsListDialog(Context context, List<Invitation> invitations,
                                                     OnInvitationActionListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Invitations (" + invitations.size() + ")");

        if (invitations.isEmpty()) {
            builder.setMessage("No pending invitations");
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            return builder.create();
        }

        // Create adapter for invitations
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1) {
            @Override
            public int getCount() {
                return invitations.size();
            }
            
            @Override
            public String getItem(int position) {
                Invitation inv = invitations.get(position);
                return inv.getFromUserName() + " invited you to: " + inv.getTitle();
            }
        };

        // Create list view
        ListView listView = new ListView(context);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Invitation invitation = invitations.get(position);
            showInvitationDetailDialog(context, invitation, listener);
        });

        builder.setView(listView);
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        return builder.create();
    }

    private static void showInvitationDetailDialog(Context context, Invitation invitation,
                                                   OnInvitationActionListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        String type = "calendar".equals(invitation.getInvitationType()) ? "Calendar" : "Event";
        String roleOrStatus = "calendar".equals(invitation.getInvitationType()) 
                ? "Role: " + capitalizeRole(invitation.getRole())
                : "Event Invitation";

        StringBuilder message = new StringBuilder();
        message.append(invitation.getFromUserName()).append(" invited you to:\n\n");
        message.append("Title: ").append(invitation.getTitle()).append("\n");
        message.append("Type: ").append(type).append("\n");
        if ("calendar".equals(invitation.getInvitationType())) {
            message.append("Role: ").append(capitalizeRole(invitation.getRole())).append("\n");
        }
        if (invitation.getMessage() != null && !invitation.getMessage().isEmpty()) {
            message.append("\nMessage: ").append(invitation.getMessage());
        }

        builder.setTitle(type + " Invitation");
        builder.setMessage(message.toString());

        builder.setPositiveButton("Accept", (dialog, which) -> {
            listener.onAccept(invitation);
            dialog.dismiss();
        });

        builder.setNegativeButton("Decline", (dialog, which) -> {
            listener.onDecline(invitation);
            dialog.dismiss();
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.create().show();
    }

    private static String capitalizeRole(String role) {
        if (role == null) return "";
        return role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
    }
}
