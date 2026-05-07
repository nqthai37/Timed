package com.timed.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import com.timed.managers.UserManager;
import com.timed.models.CalendarModel;
import com.timed.models.User;
import com.timed.repositories.UserRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class quản lý logic chọn Calendar và load thông tin chủ sở hữu.
 * Tách biệt khỏi Activity để giảm tải code.
 */
public class CalendarSelectorHelper {
    private static final String TAG = "CalendarSelectorHelper";

    private final CalendarIntegrationService calendarIntegrationService;
    private final UserRepository userRepository;
    private final Map<String, String> ownerNameCache = new HashMap<>();

    public CalendarSelectorHelper(CalendarIntegrationService calendarIntegrationService, UserRepository userRepository) {
        this.calendarIntegrationService = calendarIntegrationService;
        this.userRepository = userRepository;
        cacheCurrentUserName();
    }

    /**
     * Callback khi người dùng chọn một Calendar
     */
    public interface OnCalendarSelectedListener {
        void onSelected(String calendarId);
    }

    /**
     * Callback khi danh sách Calendar đã load xong
     */
    public interface OnCalendarsLoadedListener {
        void onLoaded(String defaultCalendarId, List<CalendarModel> calendars);
        void onError(String errorMessage);
    }

    /**
     * Cache tên người dùng hiện tại vào bộ nhớ đệm
     */
    private void cacheCurrentUserName() {
        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        cacheOwnerName(currentUser);
    }

    /**
     * Cache tên của một User vào bộ nhớ đệm
     */
    public void cacheOwnerName(User user) {
        if (user == null) {
            return;
        }
        String userId = user.getUid();
        String name = user.getName();
        if (userId == null || name == null) {
            return;
        }
        String normalized = name.trim();
        if (!normalized.isEmpty()) {
            ownerNameCache.put(userId, normalized);
        }
    }

    /**
     * Load danh sách Calendars từ Firebase
     */
    public void loadCalendars(Context context, OnCalendarsLoadedListener listener) {
        calendarIntegrationService.ensureDefaultCalendar(context,
                new CalendarIntegrationService.DefaultCalendarListener() {
                    @Override
                    public void onReady(String defaultId, List<CalendarModel> calendars) {
                        listener.onLoaded(defaultId, calendars);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Failed to load calendars: " + errorMessage);
                        listener.onError(errorMessage);
                    }
                });
    }

    /**
     * Load tên chủ sở hữu cho danh sách Calendar
     */
    public void loadCalendarOwnerNames(List<CalendarModel> calendars, Runnable onComplete) {
        if (calendars == null || calendars.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        cacheCurrentUserName();

        if (userRepository == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        Set<String> ownerIdsToFetch = new HashSet<>();
        for (CalendarModel calendar : calendars) {
            if (calendar == null) {
                continue;
            }
            String ownerId = calendar.getOwnerId();
            if (ownerId == null || ownerId.isEmpty()) {
                continue;
            }
            String cachedName = ownerNameCache.get(ownerId);
            if (cachedName != null) {
                calendar.setOwnerName(cachedName);
            } else {
                ownerIdsToFetch.add(ownerId);
            }
        }

        if (ownerIdsToFetch.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        AtomicInteger remaining = new AtomicInteger(ownerIdsToFetch.size());
        for (String ownerId : ownerIdsToFetch) {
            userRepository.getUser(ownerId)
                    .addOnSuccessListener(snapshot -> {
                        User owner = snapshot.toObject(User.class);
                        String ownerName = owner != null ? owner.getName() : null;
                        if (ownerName != null) {
                            ownerName = ownerName.trim();
                        }
                        if (ownerName != null && !ownerName.isEmpty()) {
                            ownerNameCache.put(ownerId, ownerName);
                            applyOwnerNameToCalendars(calendars, ownerId, ownerName);
                        }
                        if (remaining.decrementAndGet() == 0 && onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0 && onComplete != null) {
                            onComplete.run();
                        }
                    });
        }
    }

    /**
     * Áp dụng ownerName vào danh sách Calendar
     */
    private void applyOwnerNameToCalendars(List<CalendarModel> calendars, String ownerId, String ownerName) {
        if (calendars == null || ownerId == null || ownerName == null) {
            return;
        }
        for (CalendarModel calendar : calendars) {
            if (calendar != null && ownerId.equals(calendar.getOwnerId())) {
                calendar.setOwnerName(ownerName);
            }
        }
    }

    /**
     * Format nhãn hiển thị cho Calendar (Tên Calendar - Tên chủ sở hữu)
     */
    public static String formatCalendarLabel(CalendarModel calendar) {
        if (calendar == null) {
            return "Calendar";
        }
        String name = calendar.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Calendar";
        } else {
            name = name.trim();
        }

        String ownerName = calendar.getOwnerName();
        if (ownerName != null) {
            ownerName = ownerName.trim();
        }
        if (ownerName != null && !ownerName.isEmpty()) {
            return name + " - " + ownerName;
        }
        return name;
    }

    /**
     * Hiển thị dialog để người dùng chọn Calendar
     */
    public void showCalendarPicker(Context context, List<CalendarModel> calendarOptions,
                                   String currentCalendarId, OnCalendarSelectedListener listener) {
        if (calendarOptions == null || calendarOptions.isEmpty()) {
            return;
        }

        String[] labels = new String[calendarOptions.size()];
        int selectedIndex = 0;
        for (int i = 0; i < calendarOptions.size(); i++) {
            CalendarModel calendar = calendarOptions.get(i);
            labels[i] = formatCalendarLabel(calendar);
            if (calendar != null && currentCalendarId != null && currentCalendarId.equals(calendar.getId())) {
                selectedIndex = i;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle("Choose Calendar")
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    CalendarModel selected = calendarOptions.get(which);
                    if (selected != null && listener != null) {
                        listener.onSelected(selected.getId());
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Kiểm tra danh sách có chứa calendar với ID cho trước không
     */
    public static boolean containsCalendar(List<CalendarModel> calendars, String id) {
        if (id == null || calendars == null) {
            return false;
        }
        for (CalendarModel calendar : calendars) {
            if (calendar != null && id.equals(calendar.getId())) {
                return true;
            }
        }
        return false;
    }
}
