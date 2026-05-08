package com.timed.Features.HabitTracker.data;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.timed.Features.HabitTracker.models.Habit;
import com.timed.Features.HabitTracker.models.HabitCompletion;

import java.util.Calendar;
import java.util.List;

/**
 * Repository for Habit Tracker.
 * Abstracts data sources and provides clean API for ViewModels.
 * Handles business logic for streak calculation and completion tracking.
 */
public class HabitRepository {
    
    private static HabitRepository instance;
    private final HabitTrackerDatabase database;
    private final HabitDao habitDao;
    private final HabitCompletionDao completionDao;
    
    /**
     * Private constructor for singleton pattern.
     */
    private HabitRepository(Context context) {
        database = HabitTrackerDatabase.getInstance(context);
        habitDao = database.habitDao();
        completionDao = database.habitCompletionDao();
    }
    
    /**
     * Gets singleton instance of repository.
     */
    public static synchronized HabitRepository getInstance(Context context) {
        if (instance == null) {
            instance = new HabitRepository(context);
        }
        return instance;
    }
    
    // ==================== HABIT OPERATIONS ====================
    
    /**
     * Gets all active habits as LiveData.
     */
    public LiveData<List<Habit>> getAllActiveHabits() {
        return habitDao.getAllActiveHabits();
    }
    
    /**
     * Gets habit by ID.
     */
    public LiveData<Habit> getHabitById(int habitId) {
        return habitDao.getHabitById(habitId);
    }
    
    /**
     * Gets habit by ID synchronously.
     */
    public Habit getHabitByIdSync(int habitId) {
        return habitDao.getHabitByIdSync(habitId);
    }
    
    /**
     * Inserts a new habit.
     */
    public void insertHabit(Habit habit, InsertCallback callback) {
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... voids) {
                return habitDao.insertHabit(habit);
            }
            
            @Override
            protected void onPostExecute(Long habitId) {
                if (callback != null) {
                    callback.onSuccess((int) (long) habitId);
                }
            }
        }.execute();
    }
    
    /**
     * Updates an existing habit.
     */
    public void updateHabit(Habit habit) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                habitDao.updateHabit(habit);
                return null;
            }
        }.execute();
    }
    
    /**
     * Soft deletes a habit (sets isActive to false).
     */
    public void deleteHabit(int habitId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                habitDao.softDeleteHabit(habitId);
                return null;
            }
        }.execute();
    }
    
    // ==================== COMPLETION OPERATIONS ====================
    
    /**
     * Marks a habit as completed for today.
     * Updates streak calculation.
     */
    public void markHabitComplete(int habitId, CompletionCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                long today = getTodayStartTime();
                HabitCompletion existing = completionDao.getCompletionForDate(habitId, today);
                
                if (existing == null) {
                    // First time completing today
                    HabitCompletion completion = new HabitCompletion(
                        habitId,
                        today,
                        System.currentTimeMillis()
                    );
                    completionDao.insertCompletion(completion);
                    
                    // Update habit's streak
                    updateStreakAfterCompletion(habitId);
                    return true;
                }
                return false; // Already completed today
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    callback.onComplete(success);
                }
            }
        }.execute();
    }
    
    /**
     * Marks a habit as NOT completed for a specific date.
     * Used to undo a completion.
     */
    public void unmarkHabitComplete(int habitId, long date) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                HabitCompletion completion = completionDao.getCompletionForDate(habitId, date);
                if (completion != null) {
                    completionDao.deleteCompletion(completion);
                    updateStreakAfterUndoCompletion(habitId);
                }
                return null;
            }
        }.execute();
    }
    
    /**
     * Checks if a habit was completed on a specific date.
     */
    public boolean isCompletedOnDate(int habitId, long date) {
        return completionDao.getCompletionForDate(habitId, date) != null;
    }
    
    /**
     * Gets completions for a habit within date range.
     */
    public List<HabitCompletion> getCompletionsBetweenDates(
            int habitId, long startDate, long endDate) {
        return completionDao.getCompletionsBetweenDates(habitId, startDate, endDate);
    }
    
    /**
     * Gets last 7 days of completions for a habit.
     */
    public List<HabitCompletion> getLast7DaysCompletions(int habitId) {
        long sevenDaysAgo = getTodayStartTime() - (7 * 24 * 60 * 60 * 1000);
        return completionDao.getLast7DaysCompletions(habitId, sevenDaysAgo);
    }
    
    // ==================== STREAK CALCULATION ====================
    
    /**
     * Updates the streak after marking a habit complete.
     * Calculates consecutive days and handles grace day logic.
     */
    private void updateStreakAfterCompletion(int habitId) {
        Habit habit = habitDao.getHabitByIdSync(habitId);
        if (habit == null) return;
        
        long today = getTodayStartTime();
        long yesterday = today - (24 * 60 * 60 * 1000);
        
        // Check if completed yesterday
        HabitCompletion yesterdayCompletion = 
            completionDao.getCompletionForDate(habitId, yesterday);
        
        if (yesterdayCompletion != null) {
            // Continue the streak
            habit.setCurrentStreak(habit.getCurrentStreak() + 1);
        } else {
            // Check if grace day was used
            if (habit.isAllowGraceDay() && habit.getGraceDaySpecifiedAt() == 0) {
                // Grace day not used yet, apply it
                habit.setGraceDaySpecifiedAt(yesterday);
                habit.setCurrentStreak(habit.getCurrentStreak() + 1);
            } else {
                // Streak broken, reset
                habit.setCurrentStreak(1);
            }
        }
        
        habit.setLastCompletedAt(today);
        habit.setTotalCompletions(habit.getTotalCompletions() + 1);
        
        // Update longest streak if current exceeds it
        if (habit.getCurrentStreak() > habit.getLongestStreak()) {
            habit.setLongestStreak(habit.getCurrentStreak());
        }
        
        habitDao.updateHabit(habit);
    }
    
    /**
     * Updates the streak after undoing a completion.
     * Recalculates streak from historical data.
     */
    private void updateStreakAfterUndoCompletion(int habitId) {
        Habit habit = habitDao.getHabitByIdSync(habitId);
        if (habit == null) return;
        
        long today = getTodayStartTime();
        
        // Recalculate streak
        int streak = calculateStreakFromToday(habitId, today);
        habit.setCurrentStreak(streak);
        habit.setTotalCompletions(Math.max(0, habit.getTotalCompletions() - 1));
        
        habitDao.updateHabit(habit);
    }
    
    /**
     * Calculates current streak from today backwards.
     * Handles grace day logic.
     */
    private int calculateStreakFromToday(int habitId, long today) {
        int streak = 0;
        long currentDate = today;
        boolean graceUsed = false;
        
        while (true) {
            if (completionDao.getCompletionForDate(habitId, currentDate) != null) {
                streak++;
            } else if (!graceUsed) {
                // Try to use grace day
                graceUsed = true;
            } else {
                // Streak broken
                break;
            }
            
            currentDate -= (24 * 60 * 60 * 1000);
            if (currentDate < 0) break;
        }
        
        return streak;
    }
    
    /**
     * Resets grace day for a habit.
     */
    public void resetGraceDay(int habitId) {
        Habit habit = habitDao.getHabitByIdSync(habitId);
        if (habit != null && habit.isAllowGraceDay()) {
            habit.setGraceDaySpecifiedAt(0);
            habitDao.updateHabit(habit);
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Gets start of today in milliseconds since epoch.
     */
    public long getTodayStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
    
    /**
     * Gets start of a specific date.
     */
    public long getDateStartTime(long dateInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
    
    /**
     * Gets the day of week (0-6, where 0 is Sunday).
     */
    public int getDayOfWeek(long dateInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);
        return calendar.get(Calendar.DAY_OF_WEEK) - 1;
    }
    
    // ==================== CALLBACKS ====================
    
    public interface InsertCallback {
        void onSuccess(int habitId);
    }
    
    public interface CompletionCallback {
        void onComplete(boolean success);
    }
}
