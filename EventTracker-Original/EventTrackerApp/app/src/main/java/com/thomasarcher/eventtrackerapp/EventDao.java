package com.thomasarcher.eventtrackerapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object (DAO) for performing database operations
 * on Event objects.
 */
@Dao
public interface EventDao {

    /**
     * Inserts a new event into the database.
     *
     * @param event Event to insert
     * @return Row ID of the inserted event
     */
    @Insert
    long insertEvent(Event event);

    /**
     * Updates an existing event.
     *
     * @param event Event to update
     */
    @Update
    void updateEvent(Event event);

    /**
     * Deletes an event from the database.
     *
     * @param event Event to delete
     */
    @Delete
    void deleteEvent(Event event);

    /**
     * Returns all events for the specified user,
     * ordered by date and time.
     *
     * @param userId User ID
     * @return LiveData containing the user's event list
     */
    @Query("SELECT * FROM events " +
            "WHERE userId = :userId " +
            "ORDER BY date, time")
    LiveData<List<Event>> getAllEvents(int userId);

    /**
     * Returns the event with the specified ID.
     *
     * @param eventId Event ID
     * @return Matching event
     */
    @Query("SELECT * FROM events WHERE id = :eventId")
    Event getEventById(int eventId);
}