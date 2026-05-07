package com.timed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

/**
 * Dialog for sharing calendars and sending invitations
 * Allows selection of recipient, role/permission, and custom message
 */
public class ShareCalendarDialog {

    public interface OnShareListener {
        void onShare(String email, String role, String message);
        void onCancel();
    }

    public static Dialog createShareCalendarDialog(Context context, OnShareListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Share Calendar");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Email input
        EditText emailInput = new EditText(context);
        emailInput.setHint("Recipient email");
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        // Role spinner
        Spinner roleSpinner = new Spinner(context);
        String[] roles = {"Editor", "Viewer"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);
        layout.addView(roleSpinner);

        // Message input
        EditText messageInput = new EditText(context);
        messageInput.setHint("Optional message");
        messageInput.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        messageInput.setMinLines(3);
        layout.addView(messageInput);

        builder.setView(layout);

        builder.setPositiveButton("Share", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            String role = roles[roleSpinner.getSelectedItemPosition()].toLowerCase();
            String message = messageInput.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(context, "Please enter email address", Toast.LENGTH_SHORT).show();
                return;
            }

            listener.onShare(email, role, message);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            listener.onCancel();
        });

        return builder.create();
    }
}
