package com.mobile.timed.utils;

import android.util.Log;

import com.mobile.timed.data.models.CalendarModel;
import com.mobile.timed.managers.CalendarManager;
import com.mobile.timed.data.repository.CalendarRepository;
import com.mobile.timed.data.repository.EventRepository;

import java.util.List;

/**
 * Calendar Integration Service - Tích hợp backend CalendarManager vào UI
 */
public class CalendarIntegrationService {
    private static final String TAG = "CalendarIntegration";
    private final CalendarManager calendarManager;
    private final FirebaseInitializer firebaseInitializer;

    public interface CalendarLoadListener {
        void onCalendarsLoaded(List<CalendarModel> calendars);
        void onError(String errorMessage);
    }

    public interface CalendarSaveListener {
        void onSuccess(String calendarId);
        void onError(String errorMessage);
    }

    public CalendarIntegrationService() {
        this.calendarManager = new CalendarManager();
        this.firebaseInitializer = FirebaseInitializer.getInstance();
    }

    /**
     * Lấy tất cả lịch của user hiện tại
     */
    public void getUserCalendars(CalendarLoadListener listener) {
        String userId = firebaseInitializer.getCurrentUserId();

        if (userId == null) {
            listener.onError("User not logged in");
            return;
        }

        calendarManager.getUserCalendars(userId, new EventRepository.OnEventListener<List<CalendarModel>>() {
            @Override
            public void onSuccess(List<CalendarModel> calendars) {
                Log.d(TAG, "User calendars loaded: " + calendars.size());
                listener.onCalendarsLoaded(calendars);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error loading calendars: " + errorMessage);
                listener.onError(errorMessage);
            }
        });
    }

    /**
     * Lấy lịch cụ thể
     */
    public interface CalendarLoadDetailListener {
        void onCalendarLoaded(CalendarModel calendar);
        void onError(String errorMessage);
    }

    public void getCalendar(String calendarId, CalendarLoadDetailListener listener) {
        calendarManager.getCalendar(calendarId, new EventRepository.OnEventListener<CalendarModel>() {
            @Override
            public void onSuccess(CalendarModel calendar) {
                Log.d(TAG, "Calendar loaded: " + calendarId);
                listener.onCalendarLoaded(calendar);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error loading calendar: " + errorMessage);
                listener.onError(errorMessage);
            }
        });
    }

    /**
     * Tạo lịch mới
     */
    public void createCalendar(String name, String description, String color, boolean isPublic, CalendarSaveListener listener) {
        String userId = firebaseInitializer.getCurrentUserId();

        if (userId == null) {
            listener.onError("User not logged in");
            return;
        }

        calendarManager.createCalendar(name, description, userId, color, isPublic,
            new EventRepository.OnEventListener<String>() {
                @Override
                public void onSuccess(String calendarId) {
                    Log.d(TAG, "Calendar created: " + calendarId);
                    listener.onSuccess(calendarId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error creating calendar: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    public void updateCalendar(String calendarId, String name, String description, String color, boolean isPublic, CalendarSaveListener listener) {
        calendarManager.updateCalendar(calendarId, name, description, color, isPublic,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Calendar updated: " + calendarId);
                    listener.onSuccess(calendarId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error updating calendar: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Xóa lịch
     */
    public void deleteCalendar(String calendarId, CalendarSaveListener listener) {
        calendarManager.deleteCalendar(calendarId,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Calendar deleted: " + calendarId);
                    listener.onSuccess(calendarId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error deleting calendar: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Chia sẻ lịch với user khác
     */
    public void shareCalendar(String calendarId, String userId, String role, CalendarSaveListener listener) {
        calendarManager.addMember(calendarId, userId, role,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Calendar shared: " + calendarId);
                    listener.onSuccess(calendarId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error sharing calendar: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Xóa thành viên khỏi lịch
     */
    public void removeMember(String calendarId, String userId, CalendarSaveListener listener) {
        calendarManager.removeMember(calendarId, userId,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Member removed: " + calendarId);
                    listener.onSuccess(calendarId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error removing member: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Cập nhật vai trò của thành viên
     */
    public void updateMemberRole(String calendarId, String userId, String newRole, CalendarSaveListener listener) {
        calendarManager.updateMemberRole(calendarId, userId, newRole,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Member role updated: " + calendarId);
                    listener.onSuccess(calendarId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error updating member role: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Kiểm tra quyền chỉnh sửa lịch
     */
    public void canEditCalendar(String calendarId, String userId, EventRepository.OnEventListener<Boolean> listener) {
        calendarManager.canEditCalendar(calendarId, userId, listener);
    }
}


