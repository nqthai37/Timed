package com.timed.Features.HabitTracker.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.timed.Features.HabitTracker.models.HabitCompletion;

import java.util.List;

/**
 * Data Access Object (DAO) for HabitCompletion entity.
 * Provides database operations for tracking daily habit completions.
 */
@Dao
public interface HabitCompletionDao {
    
    @Insert
    long insertCompletion(HabitCompletion completion);
    
    @Delete
    int deleteCompletion(HabitCompletion completion);
    
    // Get all completions for a specific habit
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedDate DESC")
    LiveData<List<HabitCompletion>> getCompletionsForHabit(int habitId);
    
    // Get all completions for a specific habit (synchronous)
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedDate DESC")
    List<HabitCompletion> getCompletionsForHabitSync(int habitId);
    
    // Check if habit was completed on a specific date
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completedDate = :date LIMIT 1")
    HabitCompletion getCompletionForDate(int habitId, long date);
    
    // Get completions within a date range
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completedDate >= :startDate AND completedDate <= :endDate ORDER BY completedDate DESC")
    List<HabitCompletion> getCompletionsBetweenDates(int habitId, long startDate, long endDate);
    
    // Get last 7 days of completions for a habit
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completedDate >= :sevenDaysAgo ORDER BY completedDate DESC")
    List<HabitCompletion> getLast7DaysCompletions(int habitId, long sevenDaysAgo);
    
    // Get last completion record for a habit
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedDate DESC LIMIT 1")
    HabitCompletion getLastCompletion(int habitId);
    
    // Get completion count for a habit in the current month
    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND completedDate >= :monthStartDate AND completedDate <= :monthEndDate")
    int getMonthlyCompletionCount(int habitId, long monthStartDate, long monthEndDate);
    
    // Delete all completions for a habit
    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    int deleteAllCompletionsForHabit(int habitId);
}
