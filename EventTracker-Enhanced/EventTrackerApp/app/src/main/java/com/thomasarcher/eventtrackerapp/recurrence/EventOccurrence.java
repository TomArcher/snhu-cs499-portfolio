package com.thomasarcher.eventtrackerapp.recurrence;

import com.thomasarcher.eventtrackerapp.Event;

import java.time.LocalDate;

/**
 * Represent an individual occurrence of an event on a
 * calculated date without duplicating the persisted event.
 */
public final class EventOccurrence {

    private final Event event;
    private final LocalDate date;

    /**
     * Initialize an event occurrence.
     *
     * @param event Original event
     * @param date Calculated occurrence date
     * @throws IllegalArgumentException If either argument is null
     *
     * Time complexity: O(1)
     * Auxiliary space complexity: O(1)
     */
    public EventOccurrence(Event event, LocalDate date) {

        // Validate the supplied arguments.
        if (event == null || date == null) {
            throw new IllegalArgumentException(
                    "Event and date cannot be null.");
        }

        this.event = event;
        this.date = date;
    }

    /**
     * Retrieve the original event.
     *
     * @return Original event
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Retrieve the calculated occurrence date.
     *
     * @return Occurrence date
     */
    public LocalDate getDate() {
        return date;
    }
}