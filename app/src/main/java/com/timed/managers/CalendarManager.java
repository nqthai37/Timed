package com.timed.managers;

import android.util.Log;

import com.timed.data.models.CalendarModel;
import com.timed.data.repository.CalendarRepository;
import com.timed.data.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager class for handling calendar-related operations
 */
public class CalendarManager {
    private static final String TAG = "CalendarManager";
    private final CalendarRepository calendarRepository;

    public CalendarManager() {
        this.calendarRepository = new CalendarRepository();
    }

    /**
     * Create a new calendar
     */
    public void createCalendar(String name, String description, String ownerId,
                              String color, boolean isPublic,
                              EventRepository.OnEventListener<String> callback) {
        CalendarModel calendar = new CalendarModel();
        calendar.setName(name);
        calendar.setDescription(description);
        calendar.setOwnerId(ownerId);
        calendar.setColor(color);
        calendar.setPublic(isPublic);

        // Add owner as member with admin role
        calendar.addMember(ownerId, "admin");

        calendarRepository.createCalendar(calendar, callback);
    }

    /**
     * Get calendar details
     */
    public void getCalendar(String calendarId,
                           EventRepository.OnEventListener<CalendarModel> callback) {
        calendarRepository.getCalendarById(calendarId, callback);
    }

    /**
     * Get all calendars for current user
     */
    public void getUserCalendars(String userId,
                                EventRepository.OnEventListener<List<CalendarModel>> callback) {
        calendarRepository.getCalendarsByUser(userId, callback);
    }

    /**
     * Get calendars owned by user
     */
    public void getOwnedCalendars(String userId,
                                 EventRepository.OnEventListener<List<CalendarModel>> callback) {
        calendarRepository.getOwnedCalendars(userId, callback);
    }

    /**
     * Update calendar details
     */
    public void updateCalendar(String calendarId, String name, String description,
                              String color, boolean isPublic,
                              EventRepository.OnEventListener<Void> callback) {
        calendarRepository.getCalendarById(calendarId, new EventRepository.OnEventListener<CalendarModel>() {
            @Override
            public void onSuccess(CalendarModel calendar) {
                calendar.setName(name);
                calendar.setDescription(description);
                calendar.setColor(color);
                calendar.setPublic(isPublic);
                calendarRepository.updateCalendar(calendarId, calendar, callback);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure("Failed to update calendar: " + errorMessage);
            }
        });
    }

    /**
     * Delete a calendar
     */
    public void deleteCalendar(String calendarId,
                              EventRepository.OnEventListener<Void> callback) {
        calendarRepository.deleteCalendar(calendarId, callback);
    }

    /**
     * Add a member to calendar
     */
    public void addMember(String calendarId, String userId, String role,
                         EventRepository.OnEventListener<Void> callback) {
        calendarRepository.addMember(calendarId, userId, role, callback);
    }

    /**
     * Remove a member from calendar
     */
    public void removeMember(String calendarId, String userId,
                            EventRepository.OnEventListener<Void> callback) {
        calendarRepository.removeMember(calendarId, userId, callback);
    }

    /**
     * Update member's role
     */
    public void updateMemberRole(String calendarId, String userId, String newRole,
                                EventRepository.OnEventListener<Void> callback) {
        calendarRepository.getCalendarById(calendarId, new EventRepository.OnEventListener<CalendarModel>() {
            @Override
            public void onSuccess(CalendarModel calendar) {
                if (calendar.getMemberIds().contains(userId)) {
                    calendar.getRoles().put(userId, newRole);
                    calendarRepository.updateCalendar(calendarId, calendar, callback);
                } else {
                    callback.onFailure("User is not a member of this calendar");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure("Failed to update member role: " + errorMessage);
            }
        });
    }

    /**
     * Share calendar with another user
     */
    public void shareCalendar(String calendarId, String userEmail, String role,
                             EventRepository.OnEventListener<Void> callback) {
        // Note: In a real implementation, you would look up the user by email
        // and get their UID from your user database
        // For now, we'll just add them directly
        addMember(calendarId, userEmail, role, callback);
    }

    /**
     * Get members of a calendar
     */
    public void getCalendarMembers(String calendarId,
                                  EventRepository.OnEventListener<List<String>> callback) {
        calendarRepository.getCalendarById(calendarId, new EventRepository.OnEventListener<CalendarModel>() {
            @Override
            public void onSuccess(CalendarModel calendar) {
                callback.onSuccess(calendar.getMemberIds());
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure("Failed to get calendar members: " + errorMessage);
            }
        });
    }

    /**
     * Check if user has permission to edit calendar
     */
    public void canEditCalendar(String calendarId, String userId,
                               EventRepository.OnEventListener<Boolean> callback) {
        calendarRepository.getCalendarById(calendarId, new EventRepository.OnEventListener<CalendarModel>() {
            @Override
            public void onSuccess(CalendarModel calendar) {
                String role = calendar.getMemberRole(userId);
                boolean canEdit = role.equals("admin") || role.equals("editor");
                callback.onSuccess(canEdit);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure("Failed to check permission: " + errorMessage);
            }
        });
    }

    /**
     * Create a public shared calendar
     */
    public void createSharedCalendar(String name, String description, String ownerId,
                                    String color, List<String> memberIds,
                                    EventRepository.OnEventListener<String> callback) {
        CalendarModel calendar = new CalendarModel();
        calendar.setName(name);
        calendar.setDescription(description);
        calendar.setOwnerId(ownerId);
        calendar.setColor(color);
        calendar.setPublic(true);

        // Add owner as admin
        calendar.addMember(ownerId, "admin");

        // Add other members as editors
        for (String memberId : memberIds) {
            if (!memberId.equals(ownerId)) {
                calendar.addMember(memberId, "editor");
            }
        }

        calendarRepository.createCalendar(calendar, callback);
    }
}

