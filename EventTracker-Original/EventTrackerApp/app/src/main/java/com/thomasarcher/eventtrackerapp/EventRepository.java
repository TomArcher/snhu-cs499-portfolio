package com.thomasarcher.eventtrackerapp;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;

import java.util.List;

/**
 * Provides a single access point for event database operations.
 */
public class EventRepository {

    // Shared repository instance used throughout the application.
    private static EventRepository eventRepository;

    // Data Access Object used to perform event database operations.
    private final EventDao eventDao;
    private final UserDao userDao;

    /**
     * Returns the shared EventRepository instance.
     *
     * @param context Application or activity context
     * @return Shared EventRepository instance
     */
    public static EventRepository getInstance(Context context) {
        if (eventRepository == null) {
            eventRepository = new EventRepository(context);
        }

        return eventRepository;
    }

    /**
     * Creates the Room database and retrieves the EventDao.
     *
     * @param context Application or activity context
     */
    private EventRepository(Context context) {
        EventDatabase database = Room.databaseBuilder(
                        context.getApplicationContext(),
                        EventDatabase.class,
                        "event_tracker.db"
                )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        eventDao = database.eventDao();
        userDao = database.userDao();
    }

    /**
     * Returns all events for the specified user.
     *
     * @param userId User ID
     * @return LiveData containing the user's event list
     */
    public LiveData<List<Event>> getAllEvents(int userId) {
        return eventDao.getAllEvents(userId);
    }

    /**
     * Returns the event with the specified ID.
     *
     * @param eventId Event ID
     * @return Matching event
     */
    public Event getEventById(int eventId) {
        return eventDao.getEventById(eventId);
    }

    /**
     * Inserts a new event into the database.
     *
     * @param event Event to insert
     * @return Row ID of the inserted event
     */
    public long insertEvent(Event event) {
        return eventDao.insertEvent(event);
    }

    /**
     * Updates an existing event in the database.
     *
     * @param event Event to update
     */
    public void updateEvent(Event event) {
        eventDao.updateEvent(event);
    }

    /**
     * Deletes an event from the database.
     *
     * @param event Event to delete
     */
    public void deleteEvent(Event event) {
        eventDao.deleteEvent(event);
    }

    /**
     * Inserts a new user into the database.
     *
     * @param user User to insert
     * @return Row ID of the inserted user
     */
    public long insertUser(User user) {
        return userDao.insertUser(user);
    }

    /**
     * Returns the user matching the specified username and password.
     *
     * @param username Username to match
     * @param password Password to match
     * @return Matching user, or null if no match exists
     */
    public User getUser(String username, String password) {
        return userDao.getUser(username, password);
    }

    /**
     * Returns the user with the specified username.
     *
     * @param username Username to match
     * @return Matching user, or null if no match exists
     */
    public User getUserByUsername(String username) {
        return userDao.getUserByUsername(username);
    }
}