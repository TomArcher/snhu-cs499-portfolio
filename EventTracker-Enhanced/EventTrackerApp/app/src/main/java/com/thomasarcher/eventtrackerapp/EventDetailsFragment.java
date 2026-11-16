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

import com.google.android.material.snackbar.Snackbar;

/**
 * Displays the screen used to create, edit, or delete an event.
 */
public class EventDetailsFragment extends Fragment {

    // Retain the event being edited for use by asynchronous
    // callbacks and button listeners.
    private Event eventToEdit;

    // Track whether an existing event is being retrieved from the database.
    private boolean isLoadingEvent = false;

    // Track whether a database operation is awaiting completion.
    private boolean isModifyingEvent = false;

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

        // Load the selected event and populate the form.
        if (eventId != -1) {

            // Disable all input-capable controls to prevent changes until
            // the existing event finishes loading.
            isLoadingEvent = true;
            editTitle.setEnabled(false);
            editDate.setEnabled(false);
            editTime.setEnabled(false);
            editDescription.setEnabled(false);
            buttonSave.setEnabled(false);
            buttonDelete.setEnabled(false);

            // Retrieve the existing event without blocking the UI thread.
            repository.getEventById(eventId, new EventRepository.RepositoryCallback<Event>() {
                @Override
                public void onSuccess(Event event) {

                    // Ignore results intended for a view that is no longer active.
                    if (getView() != view) {
                        return;
                    }

                    // Prevent editing when the requested event no longer exists.
                    if (event == null) {
                        isLoadingEvent = false;

                        // Keep the editor disabled to avoid creating an unintended event.
                        Snackbar.make(
                                view,
                                "The requested event could not be found.",
                                Snackbar.LENGTH_LONG
                        ).show();

                        return;
                    }

                    // Retain the event for subsequent Save and Delete operations.
                    eventToEdit = event;

                    editTitle.setText(event.getTitle());
                    editDate.setText(event.getDate());

                    // Initialize the selected date from the existing event.
                    try {
                        SimpleDateFormat dateFormat =
                                new SimpleDateFormat("MM/dd/yyyy", Locale.US);

                        selectedDate.setTime(
                                dateFormat.parse(event.getDate())
                        );

                    } catch (Exception e) {
                        // Retain today's date if the stored date cannot be parsed.
                    }

                    editTime.setText(event.getTime());

                    // Initialize the selected time from the existing event.
                    try {
                        SimpleDateFormat timeFormat =
                                new SimpleDateFormat("h:mm a", Locale.US);

                        selectedTime.setTime(
                                timeFormat.parse(event.getTime())
                        );

                    } catch (Exception e) {
                        // Retain the current time if the stored time cannot be parsed.
                    }

                    editDescription.setText(event.getDescription());

                    buttonDelete.setVisibility(View.VISIBLE);

                    // Now that the loading has completed, allow the user to edit the event .
                    isLoadingEvent = false;
                    editTitle.setEnabled(true);
                    editDate.setEnabled(true);
                    editTime.setEnabled(true);
                    editDescription.setEnabled(true);

                    // Enable deletion after successfully loading the existing event.
                    buttonDelete.setEnabled(true);

                    // Reset the dirty flag to treat the loaded values as unchanged until
                    // the user edits them.
                    isDirty = false;
                    buttonSave.setEnabled(false);
                }

                @Override
                public void onError(Exception error) {

                    // Ignore results intended for a view that is no longer active.
                    if (getView() != view) {
                        return;
                    }

                    // Mark the database loading operation as complete.
                    isLoadingEvent = false;

                    // Keep the form disabled to prevent editing an event that could not be loaded.
                    editTitle.setEnabled(false);
                    editDate.setEnabled(false);
                    editTime.setEnabled(false);
                    editDescription.setEnabled(false);
                    buttonSave.setEnabled(false);
                    buttonDelete.setEnabled(false);

                    // Report the database error without terminating the application.
                    Snackbar.make(
                            view,
                            "Unable to load the event.",
                            Snackbar.LENGTH_LONG
                    ).show();
                }
            });
        }

        // Begin monitoring all editable fields.
        editTitle.addTextChangedListener(saveTextWatcher);
        editDate.addTextChangedListener(saveTextWatcher);
        editTime.addTextChangedListener(saveTextWatcher);
        editDescription.addTextChangedListener(saveTextWatcher);

        // Prevent the initial field population from marking the form as changed.
        isDirty = false;
        buttonSave.setEnabled(false);

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

                        // Ignore deletion requests while an operation is pending.
                        if (isModifyingEvent) {
                            return;
                        }

                        // Prevent changes while the database processes the deletion.
                        isModifyingEvent = true;
                        editTitle.setEnabled(false);
                        editDate.setEnabled(false);
                        editTime.setEnabled(false);
                        editDescription.setEnabled(false);
                        buttonSave.setEnabled(false);
                        buttonDelete.setEnabled(false);
                        buttonCancel.setEnabled(false);

                        // Delete the event without blocking the UI thread.
                        repository.deleteEvent(eventToEdit, new EventRepository.RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {

                                // Ignore results intended for a view that is no longer active.
                                if (getView() != view) {
                                    return;
                                }

                                // Return to the event list only after deletion succeeds.
                                NavHostFragment.findNavController(EventDetailsFragment.this)
                                        .navigateUp();
                            }

                            @Override
                            public void onError(Exception error) {

                                // Ignore results intended for a view that is no longer active.
                                if (getView() != view) {
                                    return;
                                }

                                // Restore editing controls without discarding the user's equity.
                                restoreEditor(view);

                                // Notify the user that the event could not be deleted.
                                Snackbar.make(
                                        view,
                                        "Unable to delete the event.",
                                        Snackbar.LENGTH_LONG
                                ).show();
                            }
                        });
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

            // Ignore additional Save requests while a database operation is pending.
            if (isModifyingEvent) {
                return;
            }

            // Prevent another Save request until the current operation completes.
            isModifyingEvent = true;
            buttonSave.setEnabled(false);

            // Prevent changes to the event while the save operation is pending.
            editTitle.setEnabled(false);
            editDate.setEnabled(false);
            editTime.setEnabled(false);
            editDescription.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonCancel.setEnabled(false);

            if (eventToEdit == null) {

                Event event = new Event(
                        userId,
                        editTitle.getText().toString(),
                        editDate.getText().toString(),
                        editTime.getText().toString(),
                        editDescription.getText().toString()
                );

                // Insert the new event without blocking the UI thread.
                repository.insertEvent(event, new EventRepository.RepositoryCallback<Long>() {
                    @Override
                    public void onSuccess(Long eventId) {

                        // Ignore results intended for a view that is no longer active.
                        if (getView() != view) {
                            return;
                        }

                        // Return to the event list after the insert succeeds.
                        NavHostFragment.findNavController(EventDetailsFragment.this)
                                .navigateUp();
                    }

                    @Override
                    public void onError(Exception error) {

                        // Ignore results intended for a view that is no longer active.
                        if (getView() != view) {
                            return;
                        }

                        // Restore the editor while preserving the user's changes.
                        restoreEditor(view);

                        // Notify the user that the event could not be saved.
                        Snackbar.make(
                                view,
                                "Unable to save the event.",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                });

            } else {

                eventToEdit.setTitle(editTitle.getText().toString());
                eventToEdit.setDate(editDate.getText().toString());
                eventToEdit.setTime(editTime.getText().toString());
                eventToEdit.setDescription(editDescription.getText().toString());

                // Update the existing event without blocking the UI thread.
                repository.updateEvent(eventToEdit, new EventRepository.RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {

                        // Ignore results intended for a view that is no longer active.
                        if (getView() != view) {
                            return;
                        }

                        // Return to the event list only after the update succeeds.
                        NavHostFragment.findNavController(EventDetailsFragment.this)
                                .navigateUp();
                    }

                    @Override
                    public void onError(Exception error) {

                        // Ignore results intended for a view that is no longer active.
                        if (getView() != view) {
                            return;
                        }

                        // Restore the editor while preserving the user's changes.
                        restoreEditor(view);

                        // Notify the user when the update fails.
                        Snackbar.make(
                                view,
                                "Unable to update the event.",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                });

            }
        });

        // Set the initial focus to the event name field.
        editTitle.requestFocus();

        return view;
    }

    /**
     * Restore the event editor after an unsuccessful database modification.
     *
     * @param view Event details screen containing the editing controls
     */
    private void restoreEditor(View view) {

        // Allow another database operation while preserving the user's changes.
        isModifyingEvent = false;

        // Restore access to the event fields and buttons.
        view.findViewById(R.id.editEventName).setEnabled(true);
        view.findViewById(R.id.editDate).setEnabled(true);
        view.findViewById(R.id.editTime).setEnabled(true);
        view.findViewById(R.id.editDescription).setEnabled(true);
        view.findViewById(R.id.buttonCancel).setEnabled(true);

        // Allow deletion only when editing an existing event.
        view.findViewById(R.id.buttonDelete).setEnabled(eventToEdit != null);

        // Restore Save when valid changes are still present.
        MaterialButton buttonSave = view.findViewById(R.id.buttonSave);
        updateSaveButton(
                buttonSave,
                view.findViewById(R.id.editEventName),
                view.findViewById(R.id.editDate),
                view.findViewById(R.id.editTime)
        );
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

        // Enable Save only when loading is complete and there is user equity.
        buttonSave.setEnabled(
                !isLoadingEvent
                        && !isModifyingEvent
                        && isDirty
                        && !title.isEmpty()
                        && !date.isEmpty()
                        && !time.isEmpty()
        );
    }
}