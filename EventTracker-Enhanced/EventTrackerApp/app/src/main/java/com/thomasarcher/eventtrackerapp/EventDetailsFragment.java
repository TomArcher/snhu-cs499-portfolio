package com.thomasarcher.eventtrackerapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;

import java.time.DayOfWeek;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import com.thomasarcher.eventtrackerapp.recurrence.RecurrenceRule;
import com.thomasarcher.eventtrackerapp.recurrence.RecurrenceFrequency;
import com.thomasarcher.eventtrackerapp.recurrence.RelativePosition;
import com.thomasarcher.eventtrackerapp.recurrence.RelativeDayType;
import com.thomasarcher.eventtrackerapp.recurrence.RecurrenceEndType;
import com.thomasarcher.eventtrackerapp.recurrence.MonthlyRecurrenceType;

/**
 * Displays the screen used to create, edit, or delete an event.
 */
public class EventDetailsFragment extends Fragment {

    // Retain the event being edited for use by asynchronous
    // callbacks and button listeners.
    private Event eventToEdit;

    // Retain the recurrence rule being configured.
    private RecurrenceRule recurrenceRule = RecurrenceRule.none();

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

        MaterialButton buttonRecurrence =
                view.findViewById(R.id.buttonRecurrence);

        // We don't want to enable this button until we
        // have enough information (date) to support a
        // recurrence record.
        buttonRecurrence.setEnabled(false);

        // Initialize the recurrence rule for the current editor.
        recurrenceRule = RecurrenceRule.none();

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

        // Initialize the date and time for a new event to the current
        // date and time.
        if (eventId == -1) {

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("MM/dd/yyyy", Locale.US);

            SimpleDateFormat timeFormat =
                    new SimpleDateFormat("h:mm a", Locale.US);

            editDate.setText(
                    dateFormat.format(selectedDate.getTime()));

            editTime.setText(
                    timeFormat.format(selectedTime.getTime()));

            buttonRecurrence.setEnabled(true);
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
            buttonRecurrence.setEnabled(false);
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

                    // Load the existing recurrence rule.
                    recurrenceRule = event.getRecurrenceRule();

                    buttonRecurrence.setText(
                            "Recurrence: " + recurrenceRule.getFrequency().name());

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
                    buttonRecurrence.setEnabled(true);

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
                    buttonRecurrence.setEnabled(false);
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

        // Open the recurrence configuration dialog.
        buttonRecurrence.setOnClickListener(v ->
                showRecurrenceDialog(view));

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
                        buttonRecurrence.setEnabled(false);
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

            // Prevent the recurrence end date from preceding the event start date.
            if (recurrenceRule.getFrequency() != RecurrenceFrequency.NONE
                    && recurrenceRule.getEndType() == RecurrenceEndType.END_DATE) {

                LocalDate startDate = LocalDate.parse(
                        editDate.getText().toString().trim(),
                        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US));

                if (recurrenceRule.getEndDate().isBefore(startDate)) {
                    Snackbar.make(
                            view,
                            "The recurrence end date cannot precede the event start date.",
                            Snackbar.LENGTH_LONG
                    ).show();

                    return;
                }
            }

            // Prevent another Save request until the current operation completes.
            isModifyingEvent = true;
            buttonSave.setEnabled(false);

            // Prevent changes to the event while the save operation is pending.
            editTitle.setEnabled(false);
            editDate.setEnabled(false);
            editTime.setEnabled(false);
            editDescription.setEnabled(false);
            buttonRecurrence.setEnabled(false);
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

                // Associate the configured recurrence rule with the new event.
                event.setRecurrenceRule(recurrenceRule);

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

                // Update the event's recurrence rule.
                eventToEdit.setRecurrenceRule(recurrenceRule);

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
     * Display the available recurrence frequencies.
     *
     * @param view The current event details view.
     */
    private void showRecurrenceDialog(View view) {

        // Define the available recurrence frequencies.
        String[] frequencies = {
                "None",
                "Daily",
                "Weekly",
                "Monthly",
                "Yearly"
        };

        // Identify the currently configured frequency.
        int selectedFrequency =
                recurrenceRule.getFrequency().ordinal();

        // Display the frequency selection dialog.
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Recurrence")
                .setSingleChoiceItems(
                        frequencies,
                        selectedFrequency,
                        (dialog, which) -> {
                            dialog.dismiss();
                            configureRecurrence(
                                    view,
                                    RecurrenceFrequency.values()[which]);
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Configure a default recurrence pattern for the selected frequency.
     *
     * @param view The current event details view.
     * @param frequency The selected recurrence frequency.
     */
    private void configureRecurrence(
            View view,
            RecurrenceFrequency frequency) {

        // Remove recurrence without requiring a start date.
        if (frequency == RecurrenceFrequency.NONE) {
            applyRecurrenceRule(view, RecurrenceRule.none());
            return;
        }

        // Retrieve the event start date.
        TextInputEditText editDate =
                view.findViewById(R.id.editDate);

        String date = editDate.getText().toString().trim();

        // Require a start date before configuring recurrence.
        if (date.isEmpty()) {
            Snackbar.make(
                    view,
                    "Select an event date first.",
                    Snackbar.LENGTH_LONG
            ).show();
            return;
        }

        LocalDate startDate = LocalDate.parse(
                date,
                DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US));

        showRecurrenceIntervalDialog(view, frequency, startDate);
    }

    /**
     * Prompt the user to specify the recurrence interval.
     *
     * @param view The current event details view.
     * @param frequency The selected recurrence frequency.
     * @param startDate The event start date.
     */
    private void showRecurrenceIntervalDialog(
            View view,
            RecurrenceFrequency frequency,
            LocalDate startDate) {

        // Create a Material input layout to display validation errors.
        TextInputLayout intervalLayout =
                new TextInputLayout(requireContext());

        intervalLayout.setHint("Interval");
        intervalLayout.setBoxBackgroundMode(
                TextInputLayout.BOX_BACKGROUND_OUTLINE);

        // Convert density-independent padding to pixels.
        float density = getResources().getDisplayMetrics().density;
        int horizontalPadding = Math.round(24 * density);
        int verticalPadding = Math.round(8 * density);

        // Apply consistent spacing around the input field.
        intervalLayout.setPadding(
                horizontalPadding,
                verticalPadding,
                horizontalPadding,
                verticalPadding);

        // Create the numeric input field.
        TextInputEditText intervalInput =
                new TextInputEditText(intervalLayout.getContext());

        // Add the input field to the Material layout.
        intervalLayout.addView(intervalInput);

        intervalInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Preserve the interval when editing the same frequency.
        int initialInterval =
                recurrenceRule.getFrequency() == frequency
                        ? recurrenceRule.getInterval()
                        : 1;

        intervalInput.setText(String.valueOf(initialInterval));
        intervalInput.setSelectAllOnFocus(true);

        // Identify the units associated with the frequency.
        String units;

        switch (frequency) {
            case DAILY:
                units = "days";
                break;
            case WEEKLY:
                units = "weeks";
                break;
            case MONTHLY:
                units = "months";
                break;
            case YEARLY:
                units = "years";
                break;
            default:
                return;
        }

        // Create the interval configuration dialog.
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Repeat every")
                        .setMessage("Enter the number of " + units + ".")
                        .setView(intervalLayout)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton("OK", null)
                        .create();

        // Validate the interval before dismissing the dialog.
        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {

                            int interval;

                            try {
                                interval = Integer.parseInt(
                                        intervalInput.getText().toString().trim());

                                if (interval < 1) {
                                    throw new NumberFormatException();
                                }
                            } catch (NumberFormatException e) {
                                intervalLayout.setError(
                                        "Enter a positive integer.");
                                return;
                            }

                            dialog.dismiss();

                            if (getView() != view) {
                                return;
                            }

                            configureRecurrencePattern(
                                    view,
                                    frequency,
                                    startDate,
                                    interval);
                        }));

        dialog.show();
    }

    /**
     * Construct a recurrence rule using the specified interval.
     *
     * @param view The current event details view.
     * @param frequency The selected recurrence frequency.
     * @param startDate The event start date.
     * @param interval The number of recurrence units between repetitions.
     */
    private void configureRecurrencePattern(
            View view,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            int interval) {

        RecurrenceRule rule;

        // Construct the recurrence pattern using the specified interval.
        switch (frequency) {
            case DAILY:
                rule = RecurrenceRule.daily(interval);
                break;

            case WEEKLY:
                showWeeklyPatternDialog(view, startDate, interval);
                return;

            case MONTHLY:
            case YEARLY:
                showCalendarPatternDialog(
                        view, frequency, startDate, interval);
                return;

            default:
                throw new IllegalArgumentException(
                        "Unsupported recurrence frequency.");
        }

        // Apply the configured recurrence rule.
        showRecurrenceEndDialog(view, rule);
    }

    /**
     * Allow the user to select the weekdays on which an event repeats.
     *
     * @param view The current event details view.
     * @param startDate The event start date.
     * @param interval The number of weeks between repetitions.
     */
    private void showWeeklyPatternDialog(
            View view,
            LocalDate startDate,
            int interval) {

        // Define the weekday labels in calendar order.
        String[] weekdays = {
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday",
                "Sunday"
        };

        DayOfWeek[] days = DayOfWeek.values();

        // Preserve existing weekday selections when editing.
        EnumSet<DayOfWeek> selectedDays =
                recurrenceRule.getFrequency() == RecurrenceFrequency.WEEKLY
                        ? recurrenceRule.getWeeklyDays()
                        : EnumSet.of(startDate.getDayOfWeek());

        // Initialize the checkbox states.
        boolean[] checkedDays = new boolean[days.length];

        for (int i = 0; i < days.length; i++) {
            checkedDays[i] = selectedDays.contains(days[i]);
        }

        // Create the weekday selection dialog.
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Repeat on")
                        .setMultiChoiceItems(
                                weekdays,
                                checkedDays,
                                (d, which, isChecked) -> {

                                    // Update the selected weekdays.
                                    if (isChecked) {
                                        selectedDays.add(days[which]);
                                    } else {
                                        selectedDays.remove(days[which]);
                                    }
                                })
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton("OK", null)
                        .create();

        // Require at least one selected weekday.
        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {

                            if (selectedDays.isEmpty()) {
                                android.widget.Toast.makeText(
                                        requireContext(),
                                        "Select at least one weekday.",
                                        android.widget.Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            dialog.dismiss();

                            // Construct and apply the weekly recurrence rule.
                            showRecurrenceEndDialog(
                                    view,
                                    RecurrenceRule.weekly(
                                            interval,
                                            selectedDays));
                        }));

        dialog.show();
    }

    /**
     * Select a relative day type and construct the recurrence rule.
     *
     * @param view The current event details view.
     * @param frequency The selected recurrence frequency.
     * @param startDate The event start date.
     * @param interval The number of months or years between repetitions.
     * @param position The selected relative position.
     */
    private void showRelativeDayDialog(
            View view,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            int interval,
            RelativePosition position) {

        // Define the available relative day types.
        String[] dayTypes = {
                "Day",
                "Weekday",
                "Weekend day",
                "Sunday",
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday"
        };

        // Preserve the relative day type when editing an existing pattern.
        int selectedDayType = -1;

        if (recurrenceRule.getFrequency() == frequency
                && recurrenceRule.getMonthlyType()
                == MonthlyRecurrenceType.RELATIVE_DAY) {

            selectedDayType =
                    recurrenceRule.getRelativeDayType().ordinal();
        }

        // Allow the user to select a relative day type.
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select day type")
                .setSingleChoiceItems(dayTypes, selectedDayType, (dialog, which) -> {

                    dialog.dismiss();

                    if (getView() != view) {
                        return;
                    }

                    RelativeDayType dayType =
                            RelativeDayType.values()[which];

                    // Construct the relative recurrence rule.
                    RecurrenceRule rule =
                            frequency == RecurrenceFrequency.MONTHLY
                                    ? RecurrenceRule.monthlyByRelativeDay(
                                    interval,
                                    position,
                                    dayType)
                                    : RecurrenceRule.yearlyByRelativeDay(
                                    interval,
                                    startDate.getMonth(),
                                    position,
                                    dayType);

                    // Configure recurrence termination.
                    showRecurrenceEndDialog(view, rule);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Allow the user to select a fixed or relative calendar pattern.
     *
     * @param view The current event details view.
     * @param frequency The selected recurrence frequency.
     * @param startDate The event start date.
     * @param interval The number of months or years between repetitions.
     */
    private void showCalendarPatternDialog(
            View view,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            int interval) {

        // Define the available calendar pattern types.
        String[] patterns = {
                "Fixed calendar date",
                "Relative calendar date"
        };

        // Preserve the calendar pattern when editing the same frequency.
        int selectedPattern = 0;

        if (recurrenceRule.getFrequency() == frequency) {
            selectedPattern =
                    recurrenceRule.getMonthlyType()
                            == MonthlyRecurrenceType.RELATIVE_DAY
                            ? 1 : 0;
        }

        // Display the available pattern types.
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Recurrence pattern")
                .setSingleChoiceItems(patterns, selectedPattern, (dialog, which) -> {

                    dialog.dismiss();

                    // Ignore changes intended for an obsolete view.
                    if (getView() != view) {
                        return;
                    }

                    if (which == 0) {

                        // Construct a fixed calendar date pattern.
                        RecurrenceRule rule =
                                frequency == RecurrenceFrequency.MONTHLY
                                        ? RecurrenceRule.monthlyByDay(
                                        interval,
                                        startDate.getDayOfMonth())
                                        : RecurrenceRule.yearlyByDay(
                                        interval,
                                        startDate.getMonth(),
                                        startDate.getDayOfMonth());

                        showRecurrenceEndDialog(view, rule);

                    } else {
                        showRelativePatternDialog(
                                view, frequency, startDate, interval);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Configure a relative monthly or yearly recurrence pattern.
     *
     * @param view The current event details view.
     * @param frequency The selected recurrence frequency.
     * @param startDate The event start date.
     * @param interval The number of months or years between repetitions.
     */
    private void showRelativePatternDialog(
            View view,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            int interval) {

        // Define the available relative positions.
        String[] positions = {
                "First",
                "Second",
                "Third",
                "Fourth",
                "Last"
        };

        // Preserve the relative position when editing an existing pattern.
        int selectedPosition = -1;

        if (recurrenceRule.getFrequency() == frequency
                && recurrenceRule.getMonthlyType()
                == MonthlyRecurrenceType.RELATIVE_DAY) {

            selectedPosition =
                    recurrenceRule.getRelativePosition().ordinal();
        }

        // Allow the user to select a relative position.
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select position")
                .setSingleChoiceItems(positions, selectedPosition, (dialog, which) -> {

                    dialog.dismiss();

                    if (getView() != view) {
                        return;
                    }

                    RelativePosition position =
                            RelativePosition.values()[which];

                    showRelativeDayDialog(
                            view,
                            frequency,
                            startDate,
                            interval,
                            position);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Allow the user to select how a recurring event terminates.
     *
     * @param view The current event details view.
     * @param rule The configured recurrence pattern.
     */
    private void showRecurrenceEndDialog(
            View view,
            RecurrenceRule rule) {

        // Define the available termination options.
        String[] endOptions = {
                "No end date",
                "End after a number of occurrences",
                "End on a specific date"
        };

        // Preserve the termination selection when editing the same frequency.
        int selectedEndOption = 0;

        if (recurrenceRule.getFrequency() == rule.getFrequency()) {
            switch (recurrenceRule.getEndType()) {
                case NO_END:
                    selectedEndOption = 0;
                    break;

                case OCCURRENCE_COUNT:
                    selectedEndOption = 1;
                    break;

                case END_DATE:
                    selectedEndOption = 2;
                    break;
            }
        }

        // Display the termination options.
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("End recurrence")
                .setSingleChoiceItems(endOptions, selectedEndOption, (dialog, which) -> {

                    dialog.dismiss();

                    // Ignore changes intended for an obsolete view.
                    if (getView() != view) {
                        return;
                    }

                    switch (which) {
                        case 0:
                            applyRecurrenceRule(view, rule.noEnd());
                            break;

                        case 1:
                            showOccurrenceCountDialog(view, rule);
                            break;

                        case 2:
                            showRecurrenceEndDateDialog(view, rule);
                            break;
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Configure the inclusive end date of a recurring series.
     *
     * @param view The current event details view.
     * @param rule The configured recurrence pattern.
     */
    private void showRecurrenceEndDateDialog(
            View view,
            RecurrenceRule rule) {

        // Retrieve the event start date.
        TextInputEditText editDate =
                view.findViewById(R.id.editDate);

        LocalDate startDate = LocalDate.parse(
                editDate.getText().toString().trim(),
                DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US));

        // Preserve the end date when editing the same frequency.
        LocalDate initialEndDate =
                recurrenceRule.getFrequency() == rule.getFrequency()
                        && recurrenceRule.getEndType()
                        == RecurrenceEndType.END_DATE
                        ? recurrenceRule.getEndDate()
                        : startDate.plusMonths(1);

        // Prevent the initial end date from preceding the start date.
        if (initialEndDate.isBefore(startDate)) {
            initialEndDate = startDate;
        }

        // Create the end date picker.
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (picker, year, month, dayOfMonth) -> {

                    // Construct the selected end date.
                    LocalDate endDate = LocalDate.of(
                            year, month + 1, dayOfMonth);

                    // Apply the configured recurrence end date.
                    applyRecurrenceRule(view, rule.endBy(endDate));
                },
                initialEndDate.getYear(),
                initialEndDate.getMonthValue() - 1,
                initialEndDate.getDayOfMonth());

        // Prevent selection of dates preceding the event start date.
        Calendar minimumDate = Calendar.getInstance();
        minimumDate.clear();
        minimumDate.set(
                startDate.getYear(),
                startDate.getMonthValue() - 1,
                startDate.getDayOfMonth());

        dialog.getDatePicker().setMinDate(
                minimumDate.getTimeInMillis());

        dialog.setTitle("End recurrence");
        dialog.show();
    }

    /**
     * Configure the maximum number of occurrences in a recurring series.
     *
     * @param view The current event details view.
     * @param rule The configured recurrence pattern.
     */
    private void showOccurrenceCountDialog(
            View view,
            RecurrenceRule rule) {

        // Create a Material input layout to display validation errors.
        TextInputLayout countLayout =
                new TextInputLayout(requireContext());

        countLayout.setHint("Occurrences");
        countLayout.setBoxBackgroundMode(
                TextInputLayout.BOX_BACKGROUND_OUTLINE);

        // Convert density-independent padding to pixels.
        float density = getResources().getDisplayMetrics().density;
        int horizontalPadding = Math.round(24 * density);
        int verticalPadding = Math.round(8 * density);

        // Apply consistent spacing around the input field.
        countLayout.setPadding(
                horizontalPadding,
                verticalPadding,
                horizontalPadding,
                verticalPadding);

        // Create the numeric input field.
        TextInputEditText countInput =
                new TextInputEditText(countLayout.getContext());

        // Add the input field to the Material layout.
        countLayout.addView(countInput);

        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Preserve the occurrence count when editing the same frequency.
        int initialCount =
                recurrenceRule.getFrequency() == rule.getFrequency()
                        && recurrenceRule.getEndType()
                        == RecurrenceEndType.OCCURRENCE_COUNT
                        ? recurrenceRule.getOccurrenceCount()
                        : 10;

        countInput.setText(String.valueOf(initialCount));

        countInput.setSelectAllOnFocus(true);

        // Create the occurrence count dialog.
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("End after")
                        .setMessage("Enter the number of occurrences.")
                        .setView(countLayout)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton("OK", null)
                        .create();

        // Validate the occurrence count before dismissing the dialog.
        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {

                            int count;

                            try {
                                count = Integer.parseInt(
                                        countInput.getText()
                                                .toString().trim());

                                if (count < 1) {
                                    throw new NumberFormatException();
                                }

                            } catch (NumberFormatException e) {
                                countLayout.setError(
                                        "Enter a positive integer.");
                                return;
                            }

                            dialog.dismiss();

                            // Apply the configured occurrence limit.
                            applyRecurrenceRule(
                                    view, rule.endAfter(count));
                        }));

        dialog.show();
    }

    /**
     * Apply a configured recurrence rule to the event editor.
     *
     * @param view The current event details view.
     * @param rule The configured recurrence rule.
     */
    private void applyRecurrenceRule(View view, RecurrenceRule rule) {

        // Ignore changes intended for an obsolete view.
        if (getView() != view) {
            return;
        }

        // Retain the configured recurrence rule.
        recurrenceRule = rule;

        // Convert the frequency to a readable label.
        String frequency = rule.getFrequency().name()
                .toLowerCase(Locale.US);

        frequency = frequency.substring(0, 1)
                .toUpperCase(Locale.US)
                + frequency.substring(1);

        // Display the configured frequency.
        MaterialButton buttonRecurrence =
                view.findViewById(R.id.buttonRecurrence);

        buttonRecurrence.setText("Recurrence: " + frequency);

        // Mark the event as modified.
        isDirty = true;

        // Update the Save button state.
        updateSaveButton(
                view.findViewById(R.id.buttonSave),
                view.findViewById(R.id.editEventName),
                view.findViewById(R.id.editDate),
                view.findViewById(R.id.editTime));
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
        view.findViewById(R.id.buttonRecurrence).setEnabled(true);
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

        // Enable recurrence only when an event date is available.
        View view = getView();

        if (view != null) {
            view.findViewById(R.id.buttonRecurrence).setEnabled(
                    !isLoadingEvent
                            && !isModifyingEvent
                            && !date.isEmpty());
        }
    }
}