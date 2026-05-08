package com.timed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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

        ScrollView scrollView = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 16);
        list.setPadding(padding, padding / 2, padding, padding / 2);

        for (Invitation invitation : invitations) {
            list.addView(createInvitationRow(context, invitation, listener));
        }

        scrollView.addView(list);
        builder.setView(scrollView);
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        return builder.create();
    }

    private static View createInvitationRow(Context context, Invitation invitation,
                                            OnInvitationActionListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        row.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(context, 10));
        row.setLayoutParams(params);

        TextView type = new TextView(context);
        type.setText(formatType(invitation) + " invitation");
        type.setTextSize(12);
        type.setAllCaps(true);
        row.addView(type);

        TextView title = new TextView(context);
        title.setText(invitation.getTitle() != null ? invitation.getTitle() : "Untitled");
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(title);

        TextView detail = new TextView(context);
        detail.setText(buildSummary(invitation));
        detail.setTextSize(14);
        row.addView(detail);

        row.setOnClickListener(v -> showInvitationDetailDialog(context, invitation, listener));
        return row;
    }

    private static void showInvitationDetailDialog(Context context, Invitation invitation,
                                                   OnInvitationActionListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        String type = formatType(invitation);

        StringBuilder message = new StringBuilder();
        message.append("From: ").append(safe(invitation.getFromUserName(), "Unknown sender")).append("\n");
        if (invitation.getFromUserEmail() != null && !invitation.getFromUserEmail().isEmpty()) {
            message.append(invitation.getFromUserEmail()).append("\n");
        }
        message.append("\nTitle: ").append(safe(invitation.getTitle(), "Untitled")).append("\n");
        message.append("Type: ").append(type).append("\n");
        if ("calendar".equals(invitation.getInvitationType())) {
            message.append("Permission: ").append(capitalizeRole(invitation.getRole())).append("\n");
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

    private static String formatType(Invitation invitation) {
        return "calendar".equals(invitation.getInvitationType()) ? "Calendar" : "Event";
    }

    private static String buildSummary(Invitation invitation) {
        StringBuilder summary = new StringBuilder();
        summary.append("From ").append(safe(invitation.getFromUserName(), "Unknown sender"));
        if ("calendar".equals(invitation.getInvitationType())) {
            summary.append(" - ").append(capitalizeRole(invitation.getRole())).append(" access");
        }
        if (invitation.getMessage() != null && !invitation.getMessage().trim().isEmpty()) {
            summary.append("\n").append(invitation.getMessage().trim());
        }
        return summary.toString();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
