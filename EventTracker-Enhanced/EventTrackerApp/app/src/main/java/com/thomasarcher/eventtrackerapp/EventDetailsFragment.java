package com.thomasarcher.eventtrackerapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Displays the screen used to create, edit, or delete an event.
 */
public class EventDetailsFragment extends Fragment {

    // Tracks whether the user has changed any event information.
    private boolean isDirty = false;

    /**
     * Required empty public constructor.
     */
    public EventDetailsFragment() {
    }

    /**
     * Creates and initializes the event details screen.
     *
     * @param inflater LayoutInflater used to inflate the fragment layout
     * @param container Parent view that contains the fragment
     * @param savedInstanceState Previously saved fragment state
     * @return Inflated fragment view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_event_details,
                container,
                false
        );

        // Retrieve references to the event input fields.
        TextInputEditText editTitle =
                view.findViewById(R.id.editEventName);

        TextInputEditText editDate =
                view.findViewById(R.id.editDate);

        TextInputEditText editTime =
                view.findViewById(R.id.editTime);

        TextInputEditText editDescription =
                view.findViewById(R.id.editDescription);

        MaterialButton buttonSave =
                view.findViewById(R.id.buttonSave);

        // Display a date picker when the date field is selected.
        Calendar selectedDate = Calendar.getInstance();

        editDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat dateFormat =
                                new SimpleDateFormat("MM/dd/yyyy", Locale.US);

                        editDate.setText(dateFormat.format(selectedDate.getTime()));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.show();
        });

        // Display a time picker when the time field is selected.
        Calendar selectedTime = Calendar.getInstance();

        editTime.setOnClickListener(v -> {

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view1, hourOfDay, minute) -> {

                        selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedTime.set(Calendar.MINUTE, minute);

                        SimpleDateFormat timeFormat =
                                new SimpleDateFormat("h:mm a", Locale.US);

                        editTime.setText(timeFormat.format(selectedTime.getTime()));
                    },
                    selectedTime.get(Calendar.HOUR_OF_DAY),
                    selectedTime.get(Calendar.MINUTE),
                    false
            );

            timePickerDialog.show();
        });

        // Monitor the input fields and update the Save button state.
        TextWatcher saveTextWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // Mark the form as changed whenever the user edits a field.
                isDirty = true;
                updateSaveButton(buttonSave, editTitle, editDate, editTime);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        MaterialButton buttonCancel =
                view.findViewById(R.id.buttonCancel);

        MaterialButton buttonDelete =
                view.findViewById(R.id.buttonDelete);

        EventRepository repository =
                EventRepository.getInstance(requireContext());

        // Get the ViewModel containing the current user session.
        SessionViewModel sessionViewModel =
                new ViewModelProvider(requireActivity())
                        .get(SessionViewModel.class);

        // Get the logged-in user's ID.
        int userId = sessionViewModel.getUserId();

        // Retrieve the event ID when editing an existing event.
        int eventId = -1;

        if (getArguments() != null) {
            eventId = getArguments().getInt("eventId", -1);
        }

        Event existingEvent = null;

        // Load the selected event and populate the form.
        if (eventId != -1) {
            existingEvent = repository.getEventById(eventId);

            if (existingEvent != null) {
                editTitle.setText(existingEvent.getTitle());
                editDate.setText(existingEvent.getDate());

                // Initialize the selected date from the existing event.
                try {
                    SimpleDateFormat dateFormat =
                            new SimpleDateFormat("MM/dd/yyyy", Locale.US);

                    selectedDate.setTime(
                            dateFormat.parse(existingEvent.getDate())
                    );

                } catch (Exception e) {
                    // Retain today's date if the stored date cannot be parsed.
                }

                editTime.setText(existingEvent.getTime());

                // Initialize the selected time from the existing event.
                try {
                    SimpleDateFormat timeFormat =
                            new SimpleDateFormat("h:mm a", Locale.US);

                    selectedTime.setTime(
                            timeFormat.parse(existingEvent.getTime())
                    );

                } catch (Exception e) {
                    // Retain the current time if the stored time cannot be parsed.
                }

                editDescription.setText(existingEvent.getDescription());

                buttonDelete.setVisibility(View.VISIBLE);
            }
        }

        // Begin monitoring all editable fields.
        editTitle.addTextChangedListener(saveTextWatcher);
        editDate.addTextChangedListener(saveTextWatcher);
        editTime.addTextChangedListener(saveTextWatcher);
        editDescription.addTextChangedListener(saveTextWatcher);

        // Prevent the initial field population from marking the form as changed.
        isDirty = false;
        buttonSave.setEnabled(false);

        Event eventToEdit = existingEvent;

        // Confirm before permanently deleting an existing event.
        buttonDelete.setOnClickListener(v -> {
            if (eventToEdit == null) {
                return;
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_event_title)
                    .setMessage(R.string.delete_event_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        repository.deleteEvent(eventToEdit);

                        NavHostFragment.findNavController(
                                EventDetailsFragment.this
                        ).navigateUp();
                    })
                    .show();
        });

        // Leave immediately when unchanged, or confirm before discarding edits.
        buttonCancel.setOnClickListener(v -> {
            if (!isDirty) {
                NavHostFragment.findNavController(EventDetailsFragment.this)
                        .navigateUp();
                return;
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.cancel_event_title)
                    .setMessage(R.string.cancel_event_message)
                    .setNegativeButton(R.string.keep_editing, null)
                    .setPositiveButton(R.string.discard, (dialog, which) ->
                            NavHostFragment.findNavController(
                                    EventDetailsFragment.this
                            ).navigateUp()
                    )
                    .show();
        });

        // Insert a new event or update the existing event.
        buttonSave.setOnClickListener(v -> {

            if (eventToEdit == null) {

                Event event = new Event(
                        userId,
                        editTitle.getText().toString(),
                        editDate.getText().toString(),
                        editTime.getText().toString(),
                        editDescription.getText().toString()
                );

                repository.insertEvent(event);

            } else {

                eventToEdit.setTitle(editTitle.getText().toString());
                eventToEdit.setDate(editDate.getText().toString());
                eventToEdit.setTime(editTime.getText().toString());
                eventToEdit.setDescription(editDescription.getText().toString());

                repository.updateEvent(eventToEdit);
            }

            NavHostFragment.findNavController(EventDetailsFragment.this)
                    .navigateUp();
        });

        // Set the initial focus to the event name field.
        editTitle.requestFocus();

        return view;
    }

    /**
     * Enables the Save button when required fields contain values and the
     * user has changed the form.
     *
     * @param buttonSave Save button
     * @param editTitle Event title field
     * @param editDate Event date field
     * @param editTime Event time field
     */
    private void updateSaveButton(
            MaterialButton buttonSave,
            TextInputEditText editTitle,
            TextInputEditText editDate,
            TextInputEditText editTime) {

        String title = editTitle.getText() == null
                ? ""
                : editTitle.getText().toString().trim();

        String date = editDate.getText() == null
                ? ""
                : editDate.getText().toString().trim();

        String time = editTime.getText() == null
                ? ""
                : editTime.getText().toString().trim();

        buttonSave.setEnabled(
                isDirty
                        && !title.isEmpty()
                        && !date.isEmpty()
                        && !time.isEmpty()
        );
    }
}