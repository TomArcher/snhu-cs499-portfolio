package com.thomasarcher.eventtrackerapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents an event stored in the Room database.
 */
@Entity(tableName = "events")
public class Event {

    // Unique identifier for the event.
    @PrimaryKey(autoGenerate = true)
    private int id;

    // ID of the user who created the event.
    private int userId;

    // Event detail information.
    private String title;
    private String date;
    private String time;
    private String description;

    /**
     * Creates a new event.
     *
     * @param title Event title
     * @param date Event date
     * @param time Event time
     * @param description Event description
     */
    public Event(
            int userId,
            String title,
            String date,
            String time,
            String description) {

        this.userId = userId;
        this.title = title;
        this.date = date;
        this.time = time;
        this.description = description;
    }

    /**
     * Sets the database-generated event ID.
     *
     * @param id Event ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the event ID.
     *
     * @return Event ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the ID of the user who created the event.
     *
     * @return User ID
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Updates the ID of the user who owns the event.
     *
     * @param userId User ID
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Returns the event title.
     *
     * @return Event title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Updates the event title.
     *
     * @param title Event title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the event date.
     *
     * @return Event date
     */
    public String getDate() {
        return date;
    }

    /**
     * Updates the event date.
     *
     * @param date Event date
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Returns the event time.
     *
     * @return Event time
     */
    public String getTime() {
        return time;
    }

    /**
     * Updates the event time.
     *
     * @param time Event time
     */
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * Returns the event description.
     *
     * @return Event description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Updates the event description.
     *
     * @param description Event description
     */
    public void setDescription(String description) {
        this.description = description;
    }
}