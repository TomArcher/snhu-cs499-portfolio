package com.thomasarcher.eventtrackerapp;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;

import java.util.List;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.thomasarcher.eventtrackerapp.recurrence.EventOccurrence;
import com.thomasarcher.eventtrackerapp.recurrence.RecurrenceCalculator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import java.time.LocalTime;
import java.util.Comparator;

/**
 * Provides a single access point for event database operations.
 */
public class EventRepository {

    // Define the date format used by persisted events.
    private static final DateTimeFormatter EVENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);

    // Define the time format used by persisted events.
    private static final DateTimeFormatter EVENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    // Generic callback interface
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    // Shared repository instance used throughout the application.
    private static EventRepository eventRepository;

    // Data Access Object used to perform event database operations.
    private final EventDao eventDao;
    private final UserDao userDao;

    // Execute database operations sequentially on a background thread.
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // Deliver database results to the Android UI thread.
    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    /**
     * Returns the shared EventRepository instance.
     *
     * @param context Application or activity context
     * @return Shared EventRepository instance
     */
    public static synchronized EventRepository getInstance(Context context) {
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
     * Generate event occurrences asynchronously within the
     * specified inclusive date range.
     *
     * @param events Original persisted events
     * @param rangeStart First date of the requested range
     * @param rangeEnd Last date of the requested range
     * @param callback Callback to receive occurrences or an error
     *
     * Time complexity: O(n + r + k log k)
     * Auxiliary space complexity: O(m + k)
     *
     * n represents the number of original events.
     * r represents the total recurrence pattern repetitions examined.
     * k represents the total generated event occurrences.
     * m represents the maximum occurrences generated for one event.
     *
     * Output space complexity: O(k)
     */
    public void generateEventOccurrences(
            List<Event> events,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            RepositoryCallback<List<EventOccurrence>> callback) {

        // Generate occurrences on the background executor.
        executor.execute(() -> {
            try {
                List<EventOccurrence> occurrences = new ArrayList<>();

                // Generate occurrences for each original event.
                for (Event event : events) {

                    // Convert the persisted event date to a LocalDate.
                    LocalDate startDate = LocalDate.parse(
                            event.getDate(),
                            EVENT_DATE_FORMATTER);

                    // Calculate occurrence dates within the requested range.
                    List<LocalDate> dates =
                            RecurrenceCalculator.generateOccurrences(
                                    startDate,
                                    rangeStart,
                                    rangeEnd,
                                    event.getRecurrenceRule());

                    // Associate each calculated date with its original event.
                    for (LocalDate date : dates) {
                        occurrences.add(new EventOccurrence(event, date));
                    }
                }

                // Sort occurrences chronologically by date and time.
                occurrences.sort(
                        Comparator.comparing(EventOccurrence::getDate)
                                .thenComparing(occurrence ->
                                        LocalTime.parse(
                                                occurrence.getEvent().getTime(),
                                                EVENT_TIME_FORMATTER)));

                // Deliver the generated occurrences on the UI thread.
                mainHandler.post(() -> callback.onSuccess(occurrences));

            } catch (Exception e) {
                // Report occurrence generation errors on the UI thread.
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Retrieves an event asynchronously by its ID.
     *
     * @param eventId ID of the event to retrieve
     * @param callback Callback to receive the result or error
     */
    public void getEventById(
            int eventId,
            RepositoryCallback<Event> callback) {

        // Submit the database operation to the background executor.
        executor.execute(() -> {
            try {
                // Retrieve the requested event from the database.
                Event event = eventDao.getEventById(eventId);

                // Deliver the result on the main UI thread.
                mainHandler.post(() -> callback.onSuccess(event));

            } catch (Exception e) {
                // Report database errors on the main UI thread.
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Insert an event asynchronously and report its database ID (if successful)
     * or error (on failure).
     *
     * @param event Event to insert
     * @param callback Receive the inserted event ID or an error
     */
    public void insertEvent(
            Event event,
            RepositoryCallback<Long> callback) {

        // Submit the insert operation to the background executor.
        executor.execute(() -> {
            try {
                // Insert the event and retain its generated database ID.
                long eventId = eventDao.insertEvent(event);

                // Deliver the inserted event ID on the main UI thread.
                mainHandler.post(() -> callback.onSuccess(eventId));

            } catch (Exception e) {
                // Report database errors on the main UI thread.
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Update an existing event asynchronously and report the outcome.
     *
     * @param event Event to update
     * @param callback Receive the completion result or an error
     */
    public void updateEvent(
            Event event,
            RepositoryCallback<Void> callback) {

        // Submit the update operation to the background executor.
        executor.execute(() -> {
            try {
                // Update the existing event in the database.
                eventDao.updateEvent(event);

                // Report successful completion on the main UI thread.
                mainHandler.post(() -> callback.onSuccess(null));

            } catch (Exception e) {
                // Report database errors on the main UI thread.
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Delete an existing event asynchronously and report the outcome.
     *
     * @param event Event to delete
     * @param callback Receive the completion result or an error
     */
    public void deleteEvent(
            Event event,
            RepositoryCallback<Void> callback) {

        // Submit the delete operation to the background executor.
        executor.execute(() -> {
            try {
                // Delete the selected event from the database.
                eventDao.deleteEvent(event);

                // Report successful completion on the main UI thread.
                mainHandler.post(() -> callback.onSuccess(null));

            } catch (Exception e) {
                // Report database errors on the main UI thread.
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Insert a new user asynchronously.
     *
     * @param user User to insert into the database
     * @param callback Callback to receive the new user ID or a database exception
     */
    public void insertUser(User user, RepositoryCallback<Long> callback) {

        // Insert the user without blocking the UI thread.
        executor.execute(() -> {
            try {
                long userId = userDao.insertUser(user);

                // Deliver the new user ID on the UI thread.
                mainHandler.post(() -> callback.onSuccess(userId));

            } catch (Exception e) {

                // Deliver database errors on the UI thread.
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Retrieve a user matching the supplied credentials asynchronously.
     *
     * @param username Username to authenticate
     * @param password Password to authenticate
     * @param callback Callback to receive the matching user, null if no match
     *                 exists, or an exception if the database operation fails
     */
    public void getUser(
            String username,
            String password,
            RepositoryCallback<User> callback) {

        // Query the database without blocking the UI thread.
        executor.execute(() -> {
            try {
                User user = userDao.getUser(username, password);

                // Deliver the query result on the UI thread.
                mainHandler.post(() -> callback.onSuccess(user));

            } catch (Exception e) {

                // Deliver database errors on the UI thread.
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Returns the user with the specified username asynchronously.
     *
     * @param username Username to search for
     * @param callback Callback to receive the matching user, null if no match
     *                 exists, or an exception if the database operation fails
     */
    public void getUserByUsername(String username,
            RepositoryCallback<User> callback) {

        // Query the database without blocking the UI thread.
        executor.execute(() -> {
            try {
                User user = userDao.getUserByUsername(username);

                // Deliver the query result on the UI thread.
                mainHandler.post(() -> callback.onSuccess(user));

            } catch (Exception e) {

                // Deliver database errors on the UI thread.
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}