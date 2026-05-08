package com.timed.Features.HabitTracker.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.timed.Features.HabitTracker.data.HabitRepository;
import com.timed.Features.HabitTracker.models.Habit;

import java.util.List;

/**
 * ViewModel for Habit Tracker.
 * Manages UI-related data and handles communication with Repository.
 * Survives configuration changes like device rotation.
 */
public class HabitTrackerViewModel extends AndroidViewModel {
    
    private final HabitRepository repository;
    private final LiveData<List<Habit>> allHabits;
    private final MutableLiveData<Integer> selectedDateIndex = new MutableLiveData<>(0); // 0 = today
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public HabitTrackerViewModel(@NonNull Application application) {
        super(application);
        repository = HabitRepository.getInstance(application);
        allHabits = repository.getAllActiveHabits();
    }
    
    // ==================== HABIT OPERATIONS ====================
    
    /**
     * Gets all active habits as LiveData.
     */
    public LiveData<List<Habit>> getAllHabits() {
        return allHabits;
    }
    
    /**
     * Gets a specific habit by ID.
     */
    public LiveData<Habit> getHabitById(int habitId) {
        return repository.getHabitById(habitId);
    }
    
    /**
     * Creates a new habit.
     */
    public void createHabit(Habit habit, HabitRepository.InsertCallback callback) {
        if (habit == null || habit.getTitle().trim().isEmpty()) {
            errorMessage.setValue("Habit title cannot be empty");
            return;
        }
        
        if (habit.getTitle().length() > 100) {
            errorMessage.setValue("Habit title too long (max 100 characters)");
            return;
        }
        
        repository.insertHabit(habit, callback);
    }
    
    /**
     * Updates an existing habit.
     */
    public void updateHabit(Habit habit) {
        if (habit == null) {
            errorMessage.setValue("Habit cannot be null");
            return;
        }
        
        repository.updateHabit(habit);
    }
    
    /**
     * Deletes a habit (soft delete).
     */
    public void deleteHabit(int habitId) {
        repository.deleteHabit(habitId);
    }
    
    // ==================== COMPLETION OPERATIONS ====================
    
    /**
     * Marks a habit as completed for today.
     */
    public void markHabitComplete(int habitId, HabitRepository.CompletionCallback callback) {
        repository.markHabitComplete(habitId, new HabitRepository.CompletionCallback() {
            @Override
            public void onComplete(boolean success) {
                if (!success) {
                    errorMessage.setValue("Habit already completed today");
                }
                if (callback != null) {
                    callback.onComplete(success);
                }
            }
        });
    }
    
    /**
     * Unmarks a habit as completed.
     */
    public void unmarkHabitComplete(int habitId, long date) {
        repository.unmarkHabitComplete(habitId, date);
    }
    
    /**
     * Checks if a habit was completed on a specific date.
     */
    public boolean isCompletedOnDate(int habitId, long date) {
        return repository.isCompletedOnDate(habitId, date);
    }
    
    // ==================== DATE NAVIGATION ====================
    
    /**
     * Gets the currently selected date index (0 = today, -1 = yesterday, etc.).
     */
    public LiveData<Integer> getSelectedDateIndex() {
        return selectedDateIndex;
    }
    
    /**
     * Sets the selected date index.
     */
    public void setSelectedDateIndex(int index) {
        selectedDateIndex.setValue(index);
    }
    
    /**
     * Gets the date in milliseconds for a given index.
     * Index 0 = today, -1 = yesterday, 1 = tomorrow, etc.
     */
    public long getDateForIndex(int index) {
        long today = repository.getTodayStartTime();
        long oneDay = 24 * 60 * 60 * 1000;
        return today + (index * oneDay);
    }
    
    /**
     * Gets array of dates for the past 7 days starting from today.
     * Used for week view.
     */
    public long[] getWeekDates() {
        long[] dates = new long[7];
        long today = repository.getTodayStartTime();
        long oneDay = 24 * 60 * 60 * 1000;
        
        // Fill from 6 days ago to today
        for (int i = 0; i < 7; i++) {
            dates[i] = today - ((6 - i) * oneDay);
        }
        
        return dates;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Gets error message from operations.
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Clears error message.
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }
    
    /**
     * Gets the current day of the week (0-6).
     */
    public int getCurrentDayOfWeek() {
        return repository.getDayOfWeek(repository.getTodayStartTime());
    }
}
