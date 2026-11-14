package com.thomasarcher.eventtrackerapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * Room database used to store application events and user accounts.
 */
@Database(
        entities = {Event.class, User.class},
        version = 3,
        exportSchema = false
)
public abstract class EventDatabase extends RoomDatabase {

    /**
     * Returns the Data Access Object (DAO) for event operations.
     *
     * @return EventDao instance
     */
    public abstract EventDao eventDao();

    /**
     * Returns the Data Access Object (DAO) for user operations.
     *
     * @return UserDao instance
     */
    public abstract UserDao userDao();
}