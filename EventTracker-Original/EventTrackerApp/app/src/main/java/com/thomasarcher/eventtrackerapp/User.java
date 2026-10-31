package com.thomasarcher.eventtrackerapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a user account stored in the Room database.
 */
@Entity(tableName = "users")
public class User {

    // Unique identifier for the user.
    @PrimaryKey(autoGenerate = true)
    private int id;

    // User login information.
    private String username;
    private String password;

    /**
     * Creates a new user account.
     *
     * @param username User's login name
     * @param password User's password
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Sets the database-generated user ID.
     *
     * @param id User ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the user ID.
     *
     * @return User ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the username.
     *
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the password.
     *
     * @return Password
     */
    public String getPassword() {
        return password;
    }
}