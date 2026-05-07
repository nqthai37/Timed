package com.timed.utils;

import com.timed.models.CalendarModel;

import java.util.Locale;

public final class CalendarPermissionUtils {
    private CalendarPermissionUtils() {
    }

    public static String roleFor(CalendarModel calendar, String userId) {
        if (calendar == null || userId == null || userId.isEmpty()) {
            return "viewer";
        }
        if (userId.equals(calendar.getOwnerId())) {
            return "editor";
        }
        String role = calendar.getMemberRole(userId);
        if (role == null) {
            return "viewer";
        }
        role = role.trim().toLowerCase(Locale.ROOT);
        return "viewer".equals(role) ? "viewer" : "editor";
    }

    public static boolean canWrite(CalendarModel calendar, String userId) {
        return "editor".equals(roleFor(calendar, userId));
    }
}
