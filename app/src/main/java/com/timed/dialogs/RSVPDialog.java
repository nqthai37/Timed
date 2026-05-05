package com.timed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.widget.Toast;

/**
 * Dialog for RSVP (Yes/No/Maybe) responses to event invitations
 */
public class RSVPDialog {

    public interface OnRSVPListener {
        void onYes();      // accepted
        void onMaybe();    // tentative
        void onNo();       // declined
        void onCancel();
    }

    public static Dialog createRSVPDialog(Context context, String eventTitle, OnRSVPListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Your Response");
        builder.setMessage("Will you attend \"" + eventTitle + "\"?");

        builder.setPositiveButton("Yes, I'll be there", (dialog, which) -> {
            listener.onYes();
        });

        builder.setNeutralButton("Maybe", (dialog, which) -> {
            listener.onMaybe();
        });

        builder.setNegativeButton("No, I can't make it", (dialog, which) -> {
            listener.onNo();
        });

        builder.setOnCancelListener(dialog -> {
            listener.onCancel();
        });

        return builder.create();
    }

    /**
     * Alternative dialog with more friendly presentation
     */
    public static Dialog createRSVPChoiceDialog(Context context, String eventTitle, OnRSVPListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Event Invitation");
        builder.setMessage(eventTitle);

        CharSequence[] options = {
                "✓ Yes, I'll be there",
                "? Maybe, still thinking",
                "✗ No, I can't make it"
        };

        builder.setSingleChoiceItems(options, -1, (dialog, which) -> {
            switch (which) {
                case 0:
                    listener.onYes();
                    dialog.dismiss();
                    break;
                case 1:
                    listener.onMaybe();
                    dialog.dismiss();
                    break;
                case 2:
                    listener.onNo();
                    dialog.dismiss();
                    break;
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            listener.onCancel();
        });

        return builder.create();
    }
}
