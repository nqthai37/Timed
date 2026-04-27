package com.timed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Dialog for inviting participants to an event
 * Collects email addresses and optional message
 */
public class InviteParticipantDialog {

    public interface OnInviteListener {
        void onInvite(String email, String message);
        void onCancel();
    }

    public static Dialog createInviteParticipantDialog(Context context, String eventTitle, 
                                                       OnInviteListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Invite to \"" + eventTitle + "\"");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Email input
        EditText emailInput = new EditText(context);
        emailInput.setHint("Participant email");
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        // Message input
        EditText messageInput = new EditText(context);
        messageInput.setHint("Optional message");
        messageInput.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        messageInput.setMinLines(3);
        layout.addView(messageInput);

        builder.setView(layout);

        builder.setPositiveButton("Invite", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            String message = messageInput.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(context, "Please enter email address", Toast.LENGTH_SHORT).show();
                return;
            }

            listener.onInvite(email, message);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            listener.onCancel();
        });

        return builder.create();
    }
}
