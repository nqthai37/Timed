package com.timed.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;

public class CalendarDialogManager {
    public interface OnCalendarCreatedListener {
        void onNext(String name, String description);
    }

    public static void showCreateCalendarDialog(Context context, OnCalendarCreatedListener listener) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 20, 50, 0);

        EditText nameInput = new EditText(context);
        nameInput.setHint("Calendar name");
        nameInput.setSingleLine(true);
        container.addView(nameInput);

        EditText descriptionInput = new EditText(context);
        descriptionInput.setHint("Description (optional)");
        descriptionInput.setMinLines(2);
        descriptionInput.setMaxLines(3);
        container.addView(descriptionInput);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Create Calendar")
                .setView(container)
                .setPositiveButton("Next", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String description = descriptionInput.getText() != null
                    ? descriptionInput.getText().toString().trim()
                    : "";

            if (name.isEmpty()) {
                nameInput.setError("Calendar name is required");
                nameInput.requestFocus();
                return;
            }

            dialog.dismiss();
            if (listener != null) listener.onNext(name, description);
        }));

        dialog.show();
    }

}
