package com.timed.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import com.timed.models.User;

import java.util.regex.Pattern;

public class AccountActionManager {
    public interface OnAvatarAction {
        void onSave(String url);
    }

    public interface OnReminderAction {
        void onCreate(String title);
    }

    public static void showChangeAvatarDialog(Context context, String currentAvatar, OnAvatarAction callback) {
        EditText input = new EditText(context);
        input.setHint("https://...");

        if (currentAvatar != null && !currentAvatar.isEmpty()) {
            input.setText(currentAvatar);
        }

        new AlertDialog.Builder(context)
                .setTitle("Update avatar")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    if (url.isEmpty() || !Patterns.WEB_URL.matcher(url).matches()) {
                        Toast.makeText(context, "Invalid image URL", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    callback.onSave(url);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showSignOutDialog(Context context, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle("Sign out")
                .setMessage("Do you want to sign out?")
                .setPositiveButton("Sign out", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showCreateReminderDialog(Context context, OnReminderAction callback) {
        EditText etReminderTitle = new EditText(context);
        etReminderTitle.setHint("Reminder title");

        new AlertDialog.Builder(context)
                .setTitle("Create Reminder")
                .setView(etReminderTitle)
                .setPositiveButton("Create", (dialog, which) -> {
                    String title = etReminderTitle.getText().toString().trim();
                    if (!title.isEmpty()) {
                        callback.onCreate(title);
                    } else {
                        Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
