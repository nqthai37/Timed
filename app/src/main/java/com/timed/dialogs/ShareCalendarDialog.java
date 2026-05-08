package com.timed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.timed.models.CalendarModel;

import java.util.List;

/**
 * Dialog for sharing calendars and sending invitations
 * Allows selection of recipient, role/permission, and custom message
 */
public class ShareCalendarDialog {

    public interface OnShareListener {
        void onShare(CalendarModel calendar, String email, String role, String message);
        void onCancel();
    }

    public static Dialog createShareCalendarDialog(Context context, List<CalendarModel> calendars,
                                                   OnShareListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Share Calendar");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 20);
        layout.setPadding(padding, padding / 2, padding, padding / 2);

        TextView calendarLabel = label(context, "Calendar");
        layout.addView(calendarLabel);

        Spinner calendarSpinner = new Spinner(context);
        ArrayAdapter<String> calendarAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, buildCalendarNames(calendars));
        calendarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        calendarSpinner.setAdapter(calendarAdapter);
        layout.addView(calendarSpinner);

        layout.addView(spacer(context));
        layout.addView(label(context, "Invitee"));
        EditText emailInput = new EditText(context);
        emailInput.setHint("Recipient email");
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        layout.addView(spacer(context));
        layout.addView(label(context, "Permission"));
        Spinner roleSpinner = new Spinner(context);
        String[] roles = {"Editor", "Viewer"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);
        layout.addView(roleSpinner);

        layout.addView(spacer(context));
        layout.addView(label(context, "Message"));
        EditText messageInput = new EditText(context);
        messageInput.setHint("Optional message");
        messageInput.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        messageInput.setMinLines(3);
        layout.addView(messageInput);

        builder.setView(layout);

        builder.setPositiveButton("Share", null);

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            listener.onCancel();
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String role = roles[roleSpinner.getSelectedItemPosition()].toLowerCase();
            String message = messageInput.getText().toString().trim();

            if (email.isEmpty()) {
                emailInput.setError("Email is required");
                emailInput.requestFocus();
                return;
            }

            CalendarModel selectedCalendar = calendars.get(calendarSpinner.getSelectedItemPosition());
            listener.onShare(selectedCalendar, email, role, message);
            dialog.dismiss();
        }));

        return dialog;
    }

    private static String[] buildCalendarNames(List<CalendarModel> calendars) {
        String[] names = new String[calendars.size()];
        for (int i = 0; i < calendars.size(); i++) {
            CalendarModel calendar = calendars.get(i);
            names[i] = calendar != null && calendar.getName() != null
                    ? calendar.getName()
                    : "Calendar";
        }
        return names;
    }

    private static TextView label(Context context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(12);
        label.setAllCaps(true);
        return label;
    }

    private static View spacer(Context context) {
        View view = new View(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 12)));
        return view;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
