package com.timed.utils;

import android.content.Context;

import com.timed.managers.RemindersManager;
import com.timed.models.Reminder;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ReminderUtil {

    /**
     * Create a reminder for a specific time today
     */
    public static Task<DocumentReference> remindMeToday(
            Context context, 
            String userId,
            String title, 
            String description,
            int hour, 
            int minute) {
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        RemindersManager manager = RemindersManager.getInstance(context);
        return manager.createReminder(userId, title, description, calendar.getTime(), "medium", "personal");
    }

    /**
     * Create a reminder for tomorrow at a specific time
     */
    public static Task<DocumentReference> remindMeTomorrow(
            Context context,
            String userId,
            String title,
            String description,
            int hour,
            int minute) {
        
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        RemindersManager manager = RemindersManager.getInstance(context);
        return manager.createReminder(userId, title, description, calendar.getTime(), "medium", "personal");
    }

    /**
     * Create a reminder for a specific number of days from now
     */
    public static Task<DocumentReference> remindMeInDays(
            Context context,
            String userId,
            String title,
            String description,
            int days,
            int hour,
            int minute) {
        
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        RemindersManager manager = RemindersManager.getInstance(context);
        return manager.createReminder(userId, title, description, calendar.getTime(), "medium", "personal");
    }

    /**
     * Create a reminder with custom priority
     */
    public static Task<DocumentReference> createReminderWithPriority(
            Context context,
            String userId,
            String title,
            String description,
            Date dueDate,
            String priority) {
        
        RemindersManager manager = RemindersManager.getInstance(context);
        return manager.createReminder(userId, title, description, dueDate, priority, "personal");
    }

    /**
     * Create a reminder with custom category
     */
    public static Task<DocumentReference> createReminderWithCategory(
            Context context,
            String userId,
            String title,
            String description,
            Date dueDate,
            String category) {
        
        RemindersManager manager = RemindersManager.getInstance(context);
        return manager.createReminder(userId, title, description, dueDate, "medium", category);
    }

    /**
     * Get all pending reminders
     */
    public static Task<List<Reminder>> getPendingReminders(
            Context context,
            String userId) {
        
        RemindersManager manager = RemindersManager.getInstance(context);
        return manager.getPendingReminders(userId);
    }

    /**
     * Check if a reminder is due soon (within 1 hour)
     */
    public static boolean isReminderDueSoon(Reminder reminder) {
        if (reminder == null || reminder.getDueDate() == null) {
            return false;
        }
        
        long dueTime = reminder.getDueDate().toDate().getTime();
        long currentTime = System.currentTimeMillis();
        long oneHourInMillis = 60 * 60 * 1000;
        
        return (dueTime - currentTime) <= oneHourInMillis && (dueTime - currentTime) > 0;
    }

    /**
     * Check if a reminder is overdue
     */
    public static boolean isReminderOverdue(Reminder reminder) {
        if (reminder == null || reminder.getDueDate() == null) {
            return false;
        }
        
        long dueTime = reminder.getDueDate().toDate().getTime();
        long currentTime = System.currentTimeMillis();
        
        return dueTime < currentTime && !reminder.getIsCompleted();
    }

    /**
     * Get time remaining until reminder is due
     */
    public static long getTimeRemainingMillis(Reminder reminder) {
        if (reminder == null || reminder.getDueDate() == null) {
            return -1;
        }
        
        long dueTime = reminder.getDueDate().toDate().getTime();
        long currentTime = System.currentTimeMillis();
        
        return dueTime - currentTime;
    }
}
