package com.timed.Features.HabitTracker.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.timed.Features.HabitTracker.models.Habit;
import com.timed.Features.HabitTracker.models.HabitCompletion;

/**
 * Room Database for Habit Tracker feature.
 * Handles all database operations for habits and completions.
 * Implements singleton pattern to ensure only one database instance.
 */
@Database(
    entities = {Habit.class, HabitCompletion.class},
    version = 1,
    exportSchema = false
)
public abstract class HabitTrackerDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "habit_tracker.db";
    private static volatile HabitTrackerDatabase instance;
    
    /**
     * Gets the singleton instance of the database.
     * Creates the database on first call.
     */
    public static HabitTrackerDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (HabitTrackerDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        HabitTrackerDatabase.class,
                        DATABASE_NAME
                    )
                    .allowMainThreadQueries() // For simplicity; consider using LiveData/Coroutines for production
                    .build();
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets the DAO for Habit operations.
     */
    public abstract HabitDao habitDao();
    
    /**
     * Gets the DAO for HabitCompletion operations.
     */
    public abstract HabitCompletionDao habitCompletionDao();
}
