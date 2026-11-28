package com.thomasarcher.eventtrackerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import com.thomasarcher.eventtrackerapp.recurrence.EventOccurrence;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Display dynamically generated event occurrences
 * in a RecyclerView.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    // Define the date format used to display event occurrences.
    private static final DateTimeFormatter EVENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US);
    /**
     * Listener for delete button clicks.
     */
    public interface OnDeleteClickListener {
        void onDeleteClick(Event event);
    }

    /**
     * Listener for event item clicks.
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    // List of event occurrences displayed in the RecyclerView.
    private List<EventOccurrence> occurrenceList;

    // Callback interfaces for user interactions.
    private final OnDeleteClickListener deleteClickListener;
    private final OnEventClickListener eventClickListener;

    /**
     * Initialize an EventAdapter.
     *
     * @param occurrenceList List of event occurrences to display
     * @param deleteClickListener Listener for delete button clicks
     * @param eventClickListener Listener for event item clicks
     */
    public EventAdapter(
            List<EventOccurrence> occurrenceList,
            OnDeleteClickListener deleteClickListener,
            OnEventClickListener eventClickListener) {

        this.occurrenceList = occurrenceList;
        this.deleteClickListener = deleteClickListener;
        this.eventClickListener = eventClickListener;
    }

    /**
     * Update the displayed list of event occurrences.
     *
     * @param occurrences Updated event occurrence list
     */
    public void setOccurrences(List<EventOccurrence> occurrences) {
        occurrenceList = occurrences;
        notifyDataSetChanged();
    }

    /**
     * Creates a new ViewHolder for an event item.
     *
     * @param parent Parent ViewGroup
     * @param viewType View type
     * @return EventViewHolder
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);

        return new EventViewHolder(view);
    }

    /**
     * Bind event occurrence data to a ViewHolder.
     *
     * @param holder ViewHolder to populate
     * @param position Position of the occurrence in the list
     */
    @Override
    public void onBindViewHolder(
            @NonNull EventViewHolder holder, int position) {

        // Retrieve the occurrence and its original event.
        EventOccurrence occurrence = occurrenceList.get(position);
        Event event = occurrence.getEvent();

        // Display the original event properties and calculated date.
        holder.title.setText(event.getTitle());
        holder.date.setText(
                occurrence.getDate().format(EVENT_DATE_FORMATTER));
        holder.time.setText(event.getTime());

        // Delete the original event.
        holder.deleteButton.setOnClickListener(v ->
                deleteClickListener.onDeleteClick(event)
        );

        // Open the original event for editing.
        holder.itemView.setOnClickListener(v ->
                eventClickListener.onEventClick(event)
        );
    }

    /**
     * Return the number of event occurrences in the list.
     *
     * @return Event occurrence count
     */
    @Override
    public int getItemCount() {
        return occurrenceList.size();
    }

    /**
     * Holds references to the views for a single event item.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView date;
        TextView time;
        MaterialButton deleteButton;

        /**
         * Initializes the views for an event item.
         *
         * @param itemView Event item layout
         */
        EventViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.textViewEventTitle);
            date = itemView.findViewById(R.id.textViewEventDate);
            time = itemView.findViewById(R.id.textViewEventTime);
            deleteButton = itemView.findViewById(R.id.buttonDeleteEvent);
        }
    }
}