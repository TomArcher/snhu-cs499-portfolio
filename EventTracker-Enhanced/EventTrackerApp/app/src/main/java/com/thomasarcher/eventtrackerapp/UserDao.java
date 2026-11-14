package com.thomasarcher.eventtrackerapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/**
 * Data Access Object (DAO) for performing database operations
 * on User objects.
 */
@Dao
public interface UserDao {

    /**
     * Inserts a new user into the database.
     *
     * @param user User to insert
     * @return Row ID of the inserted user
     */
    @Insert
    long insertUser(User user);

    /**
     * Returns the user with the specified username and password.
     *
     * @param username Username to match
     * @param password Password to match
     * @return Matching user, or null if no match exists
     */
    @Query("SELECT * FROM users " +
            "WHERE username = :username AND password = :password LIMIT 1")
    User getUser(String username, String password);

    /**
     * Returns the user with the specified username.
     *
     * @param username Username to match
     * @return Matching user, or null if no match exists
     */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);
}