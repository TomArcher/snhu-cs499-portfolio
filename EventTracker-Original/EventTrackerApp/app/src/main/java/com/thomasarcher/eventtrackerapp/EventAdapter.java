package com.thomasarcher.eventtrackerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * RecyclerView adapter used to display the list of events.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

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

    // List of events displayed in the RecyclerView.
    private List<Event> eventList;

    // Callback interfaces for user interactions.
    private final OnDeleteClickListener deleteClickListener;
    private final OnEventClickListener eventClickListener;

    /**
     * Creates a new EventAdapter.
     *
     * @param eventList List of events to display
     * @param deleteClickListener Listener for delete button clicks
     * @param eventClickListener Listener for event item clicks
     */
    public EventAdapter(
            List<Event> eventList,
            OnDeleteClickListener deleteClickListener,
            OnEventClickListener eventClickListener) {

        this.eventList = eventList;
        this.deleteClickListener = deleteClickListener;
        this.eventClickListener = eventClickListener;
    }

    /**
     * Updates the displayed list of events.
     *
     * @param events Updated event list
     */
    public void setEvents(List<Event> events) {
        eventList = events;
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
     * Binds event data to a ViewHolder.
     *
     * @param holder ViewHolder to populate
     * @param position Position of the event in the list
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {

        Event event = eventList.get(position);

        holder.title.setText(event.getTitle());
        holder.date.setText(event.getDate());
        holder.time.setText(event.getTime());

        // Handle delete button selection.
        holder.deleteButton.setOnClickListener(v ->
                deleteClickListener.onDeleteClick(event)
        );

        // Open the selected event.
        holder.itemView.setOnClickListener(v ->
                eventClickListener.onEventClick(event)
        );
    }

    /**
     * Returns the number of events in the list.
     *
     * @return Event count
     */
    @Override
    public int getItemCount() {
        return eventList.size();
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