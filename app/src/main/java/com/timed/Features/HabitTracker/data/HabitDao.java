package com.timed.Features.HabitTracker.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.timed.Features.HabitTracker.models.Habit;

import java.util.List;

/**
 * Data Access Object (DAO) for Habit entity.
 * Provides database operations for habits.
 */
@Dao
public interface HabitDao {
    
    @Insert
    long insertHabit(Habit habit);
    
    @Update
    int updateHabit(Habit habit);
    
    @Delete
    int deleteHabit(Habit habit);
    
    // Soft delete by setting isActive to false
    @Query("UPDATE habits SET isActive = 0 WHERE id = :habitId")
    int softDeleteHabit(int habitId);
    
    // Get all active habits
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC")
    LiveData<List<Habit>> getAllActiveHabits();
    
    // Get all active habits as List (for one-time queries)
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC")
    List<Habit> getAllActiveHabitsSync();
    
    // Get habit by ID
    @Query("SELECT * FROM habits WHERE id = :habitId")
    LiveData<Habit> getHabitById(int habitId);
    
    // Get habit by ID (synchronous)
    @Query("SELECT * FROM habits WHERE id = :habitId")
    Habit getHabitByIdSync(int habitId);
    
    // Get habits for a specific recurrence pattern
    @Query("SELECT * FROM habits WHERE isActive = 1 AND recurrencePattern = :pattern ORDER BY createdAt DESC")
    LiveData<List<Habit>> getHabitsByRecurrence(String pattern);
    
    // Get total number of active habits
    @Query("SELECT COUNT(*) FROM habits WHERE isActive = 1")
    LiveData<Integer> getTotalHabitsCount();
    
    // Get habits with pending notifications
    @Query("SELECT * FROM habits WHERE isActive = 1 AND notificationHour >= 0")
    List<Habit> getHabitsWithNotifications();
}
