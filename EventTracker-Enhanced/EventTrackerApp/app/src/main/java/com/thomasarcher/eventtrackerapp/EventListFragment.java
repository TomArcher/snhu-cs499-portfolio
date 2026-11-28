package com.thomasarcher.eventtrackerapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import android.telephony.SmsManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.SharedPreferences;
import android.text.InputType;
import android.app.DatePickerDialog;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.thomasarcher.eventtrackerapp.recurrence.EventOccurrence;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Displays the event list and manages event selection, deletion,
 * creation, and SMS permission requests.
 */
public class EventListFragment extends Fragment {
    // Identifies the stored SMS notification settings.
    private static final String SMS_PREFERENCES = "sms_preferences";
    private static final String SMS_PHONE_NUMBER = "sms_phone_number";

    // Phone number awaiting SMS permission approval.
    private String pendingPhoneNumber;

    // Request code used to identify the SMS permission response.
    private static final int SMS_PERMISSION_REQUEST = 100;

    // Adapter used to display events in the RecyclerView.
    private EventAdapter eventAdapter;

    // Identify the most recent occurrence generation request.
    private long occurrenceRequestId;

    // Identify the most recent SMS reminder generation request.
    private long reminderRequestId;

    private static final DateTimeFormatter EVENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);

    private LocalDate rangeStart;
    private LocalDate rangeEnd;
    private List<Event> currentEvents = new ArrayList<>();

    /**
     * Required empty public constructor.
     */
    public EventListFragment() {
    }

    /**
     * Creates and initializes the event list screen.
     *
     * @param inflater LayoutInflater used to inflate the fragment layout
     * @param container Parent view that contains the fragment
     * @param savedInstanceState Previously saved fragment state
     * @return Inflated fragment view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_event_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewEvents);

        MaterialButton smsButton =
                view.findViewById(R.id.buttonEnableSms);

        // Get the saved SMS notification settings.
        SharedPreferences preferences =
                requireContext().getSharedPreferences(
                        SMS_PREFERENCES,
                        requireContext().MODE_PRIVATE
                );

        String phoneNumber =
                preferences.getString(SMS_PHONE_NUMBER, "");

        // Determine whether SMS reminders are currently enabled.
        boolean smsEnabled =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.SEND_SMS)
                        == PackageManager.PERMISSION_GRANTED
                        && !phoneNumber.isEmpty();

        // Display the appropriate SMS action.
        smsButton.setText(
                smsEnabled
                        ? R.string.disable_sms
                        : R.string.enable_sms
        );

        // Enable or disable SMS reminders.
        smsButton.setOnClickListener(v -> {

            SharedPreferences smsPreferences =
                    requireContext().getSharedPreferences(
                            SMS_PREFERENCES,
                            requireContext().MODE_PRIVATE
                    );

            String savedPhoneNumber =
                    smsPreferences.getString(SMS_PHONE_NUMBER, "");

            // Determine whether SMS permission has been granted.
            boolean permissionGranted =
                    ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.SEND_SMS)
                            == PackageManager.PERMISSION_GRANTED;

            // Determine whether SMS reminders are currently enabled.
            boolean currentlySmsEnabled =
                    permissionGranted && !savedPhoneNumber.isEmpty();

            // If SMS is currently enabled, disable it.
            if (currentlySmsEnabled) {

                smsPreferences.edit()
                        .remove(SMS_PHONE_NUMBER)
                        .apply();

                smsButton.setText(R.string.enable_sms);
                return;
            }

            // Remove a stored number if SMS permission was revoked.
            if (!permissionGranted && !savedPhoneNumber.isEmpty()) {
                smsPreferences.edit()
                        .remove(SMS_PHONE_NUMBER)
                        .apply();
            }

            // Ask the user where SMS reminders should be sent.
            TextInputLayout phoneNumberLayout =
                    new TextInputLayout(requireContext());

            TextInputEditText phoneNumberInput =
                    new TextInputEditText(requireContext());

            phoneNumberInput.setInputType(InputType.TYPE_CLASS_PHONE);

            phoneNumberLayout.addView(phoneNumberInput);

            MaterialAlertDialogBuilder builder =
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.sms_phone_number_title)
                            .setMessage(R.string.sms_phone_number_message)
                            .setView(phoneNumberLayout)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.enable_sms, null);

            AlertDialog dialog = builder.create();

            dialog.setOnShowListener(dialogInterface -> {

                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v1 -> {

                            pendingPhoneNumber =
                                    phoneNumberInput.getText().toString().trim();

                            if (pendingPhoneNumber.isEmpty()) {
                                phoneNumberLayout.setError(
                                        getString(R.string.phone_number_required)
                                );
                                return;
                            }

                            // Clear any previous validation error.
                            phoneNumberLayout.setError(null);

                            // If SMS permission was previously granted, save the phone number
                            // and enable reminders without requesting permission again.
                            if (ContextCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.SEND_SMS)
                                    == PackageManager.PERMISSION_GRANTED) {

                                smsPreferences.edit()
                                        .putString(SMS_PHONE_NUMBER, pendingPhoneNumber)
                                        .apply();

                                smsButton.setText(R.string.disable_sms);
                                dialog.dismiss();
                                return;
                            }

                            // Close the phone-number dialog before requesting SMS permission.
                            dialog.dismiss();

                            // Request permission to send SMS reminders.
                            requestPermissions(
                                    new String[]{Manifest.permission.SEND_SMS},
                                    SMS_PERMISSION_REQUEST
                            );
                        });
            });

            dialog.show();
        });

        // Display events in a single-column grid.
        recyclerView.setLayoutManager(
                new GridLayoutManager(requireContext(), 1)
        );

        EventRepository repository =
                EventRepository.getInstance(requireContext());

        // Create the adapter and define delete and item-click behavior.
        eventAdapter = new EventAdapter(
                new ArrayList<>(),

                event -> new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete_event_title)
                        .setMessage(R.string.delete_event_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.delete, (dialog, which) ->
                                repository.deleteEvent(event, new EventRepository.RepositoryCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {

                                        // Ignore results intended for a view that is no longer active.
                                        // This addresses lifecycle cases in which Android destroys the
                                        // view and then creates a new view for the same Fragment before
                                        // the database operation finishes.
                                        if (getView() != view) {
                                            return;
                                        }

                                        // Let Room's LiveData refresh the event list.
                                    }

                                    @Override
                                    public void onError(Exception error) {

                                        // Ignore results intended for a view that is no longer active.
                                        // This addresses lifecycle cases in which Android destroys the
                                        // view and then creates a new view for the same Fragment before
                                        // the database operation finishes.
                                        if (getView() != view) {
                                            return;
                                        }

                                        // Notify the user that the event could not be deleted.
                                        Snackbar.make(
                                                view,
                                                "Unable to delete the event.",
                                                Snackbar.LENGTH_LONG
                                        ).show();
                                    }
                                })
                        )
                        .show(),

                event -> {
                    Bundle args = new Bundle();
                    args.putInt("eventId", event.getId());

                    NavHostFragment.findNavController(EventListFragment.this)
                            .navigate(
                                    R.id.action_eventListFragment_to_eventDetailsFragment,
                                    args
                            );
                }
        );

        recyclerView.setAdapter(eventAdapter);

        // Open the event details screen to create a new event.
        view.findViewById(R.id.fabAddEvent).setOnClickListener(v ->
                NavHostFragment.findNavController(EventListFragment.this)
                        .navigate(R.id.action_eventListFragment_to_eventDetailsFragment)
        );

        return view;
    }

    /**
     * Observes database changes and updates the event list and title count.
     *
     * @param view Fragment root view
     * @param savedInstanceState Previously saved fragment state
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // Initialize the viewing range dates if not already
        // set. This prevents us from overwriting it date
        // ranges already specified.
        if (rangeStart == null || rangeEnd == null) {
            rangeStart = LocalDate.now();
            rangeEnd = rangeStart.plusDays(30);
        }

        // Obtain references to the date range buttons.
        MaterialButton buttonRangeStart =
                view.findViewById(R.id.buttonRangeStart);

        MaterialButton buttonRangeEnd =
                view.findViewById(R.id.buttonRangeEnd);

        // Display the initial date range.
        buttonRangeStart.setText(getString(
                R.string.range_start_format,
                rangeStart.format(EVENT_DATE_FORMATTER)));

        buttonRangeEnd.setText(getString(
                R.string.range_end_format,
                rangeEnd.format(EVENT_DATE_FORMATTER)));

        TextView titleTextView =
                view.findViewById(R.id.textViewUpcomingEventsTitle);

        EventRepository repository =
                EventRepository.getInstance(requireContext());

        // Get the ViewModel containing the current user session.
        SessionViewModel sessionViewModel =
                new ViewModelProvider(requireActivity())
                        .get(SessionViewModel.class);

        // Get the logged-in user's ID.
        int userId = sessionViewModel.getUserId();

// Allow the user to select the beginning of the viewing range.
        buttonRangeStart.setOnClickListener(v ->
                new DatePickerDialog(
                        requireContext(),
                        (picker, year, month, day) -> {

                            if (getView() != view) {
                                return;
                            }

                            LocalDate selectedDate =
                                    LocalDate.of(year, month + 1, day);

                            // Reject an invalid date range.
                            if (selectedDate.isAfter(rangeEnd)) {
                                Snackbar.make(
                                        view,
                                        "From date cannot follow Through date.",
                                        Snackbar.LENGTH_LONG
                                ).show();
                                return;
                            }

                            rangeStart = selectedDate;

                            buttonRangeStart.setText(getString(
                                    R.string.range_start_format,
                                    rangeStart.format(EVENT_DATE_FORMATTER)));

                            refreshOccurrences(view, repository, titleTextView);
                        },
                        rangeStart.getYear(),
                        rangeStart.getMonthValue() - 1,
                        rangeStart.getDayOfMonth()
                ).show());

// Allow the user to select the end of the viewing range.
        buttonRangeEnd.setOnClickListener(v ->
                new DatePickerDialog(
                        requireContext(),
                        (picker, year, month, day) -> {

                            if (getView() != view) {
                                return;
                            }

                            LocalDate selectedDate =
                                    LocalDate.of(year, month + 1, day);

                            // Reject an invalid date range.
                            if (selectedDate.isBefore(rangeStart)) {
                                Snackbar.make(
                                        view,
                                        "Through date cannot precede From date.",
                                        Snackbar.LENGTH_LONG
                                ).show();
                                return;
                            }

                            rangeEnd = selectedDate;

                            buttonRangeEnd.setText(getString(
                                    R.string.range_end_format,
                                    rangeEnd.format(EVENT_DATE_FORMATTER)));

                            refreshOccurrences(view, repository, titleTextView);
                        },
                        rangeEnd.getYear(),
                        rangeEnd.getMonthValue() - 1,
                        rangeEnd.getDayOfMonth()
                ).show());

        repository.getAllEvents(userId).observe(
                getViewLifecycleOwner(),
                events -> {

                    // Cache the current events.
                    currentEvents = events == null
                            ? new ArrayList<>()
                            : new ArrayList<>(events);

                    // Generate SMS reminders for today's event occurrences.
                    generateTodayReminders(view, repository);

                    // Refresh the displayed occurrences.
                    refreshOccurrences(view, repository, titleTextView);
                });
    }

    /**
     * Generate and display event occurrences within the selected date range.
     *
     * @param view The current fragment view.
     * @param repository The event repository.
     * @param titleTextView The event count heading.
     */
    private void refreshOccurrences(
            View view,
            EventRepository repository,
            TextView titleTextView) {

        // Identify the current request.
        long requestId = ++occurrenceRequestId;

        // Generate occurrences on the background executor.
        repository.generateEventOccurrences(
                new ArrayList<>(currentEvents),
                rangeStart,
                rangeEnd,
                new EventRepository.RepositoryCallback<List<EventOccurrence>>() {

                    @Override
                    public void onSuccess(List<EventOccurrence> result) {

                        // Ignore obsolete results.
                        if (getView() != view
                                || requestId != occurrenceRequestId) {
                            return;
                        }

                        // Display the generated occurrences.
                        eventAdapter.setOccurrences(result);

                        titleTextView.setText(
                                getString(
                                        R.string.upcoming_events,
                                        result.size()));
                    }

                    @Override
                    public void onError(Exception error) {

                        // Ignore obsolete errors.
                        if (getView() != view
                                || requestId != occurrenceRequestId) {
                            return;
                        }

                        Snackbar.make(
                                view,
                                "Unable to generate event occurrences.",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                });
    }

    /**
     * Generate event occurrences scheduled for today
     * independently of the selected viewing range.
     *
     * @param view The current fragment view.
     * @param repository The event repository.
     */
    private void generateTodayReminders(
            View view,
            EventRepository repository) {

        // Identify the current reminder request.
        long requestId = ++reminderRequestId;

        // Restrict occurrence generation to today's date.
        LocalDate today = LocalDate.now();

        repository.generateEventOccurrences(
                new ArrayList<>(currentEvents),
                today,
                today,
                new EventRepository.RepositoryCallback<List<EventOccurrence>>() {

                    @Override
                    public void onSuccess(List<EventOccurrence> result) {

                        // Ignore obsolete results.
                        if (getView() != view
                                || requestId != reminderRequestId) {
                            return;
                        }

                        sendTodayReminders(result);
                    }

                    @Override
                    public void onError(Exception error) {

                        // Ignore obsolete errors.
                        if (getView() != view
                                || requestId != reminderRequestId) {
                            return;
                        }

                        Snackbar.make(
                                view,
                                "Unable to generate SMS reminders.",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                });
    }

    /**
     * Sends SMS reminders for today's events that have not already
     * generated a reminder.
     *
     * @param occurrences Event occurrences scheduled for today.
     */
    private void sendTodayReminders(List<EventOccurrence> occurrences) {

        boolean permissionGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.SEND_SMS)
                        == PackageManager.PERMISSION_GRANTED;

        // Verify that SMS permission has been granted.
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Get the saved SMS notification settings.
        SharedPreferences preferences =
                requireContext().getSharedPreferences(
                        SMS_PREFERENCES,
                        requireContext().MODE_PRIVATE
                );

        String phoneNumber =
                preferences.getString(SMS_PHONE_NUMBER, "");

        // Verify that SMS reminders are enabled.
        if (phoneNumber.isEmpty()) {
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();

        // Send one reminder for each occurrence scheduled for today.
        for (EventOccurrence occurrence : occurrences) {

            // Retrieve the original event and format the occurrence date.
            Event event = occurrence.getEvent();
            String today = occurrence.getDate().format(EVENT_DATE_FORMATTER);

            // Build a unique key to prevent duplicate reminders.
            String reminderKey =
                    "sms_sent_" + event.getId() + "_" + today;

            if (preferences.getBoolean(reminderKey, false)) {
                continue;
            }

            String message =
                    getString(
                            R.string.sms_event_reminder,
                            event.getTitle(),
                            event.getTime()
                    );

            smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
            );

            // Record that today's reminder was sent for this event.
            preferences.edit()
                    .putBoolean(reminderKey, true)
                    .apply();
        }
    }

    /**
     * Handles the result of the SMS permission request.
     *
     * @param requestCode Permission request identifier
     * @param permissions Requested permissions
     * @param grantResults Permission results
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST) {

            MaterialButton smsButton =
                    requireView().findViewById(R.id.buttonEnableSms);

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Save the phone number used to receive SMS reminders.
                SharedPreferences preferences =
                        requireContext().getSharedPreferences(
                                SMS_PREFERENCES,
                                requireContext().MODE_PRIVATE
                        );

                preferences.edit()
                        .putString(SMS_PHONE_NUMBER, pendingPhoneNumber)
                        .apply();

                smsButton.setText(R.string.disable_sms);
            }
        }
    }}