package com.thomasarcher.eventtrackerapp;

import androidx.lifecycle.ViewModel;

/**
 * Stores information about the currently logged-in user.
 */
public class SessionViewModel extends ViewModel {

    // ID of the currently logged-in user.
    private int userId = -1;

    /**
     * Returns the logged-in user's ID.
     *
     * @return User ID, or -1 if no user is logged in
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Sets the logged-in user's ID.
     *
     * @param userId User ID
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }
}