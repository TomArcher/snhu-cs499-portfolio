package com.thomasarcher.eventtrackerapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Displays the login screen and provides access to the account creation flow.
 */
public class LoginFragment extends Fragment {

    // Track whether an authentication operation is awaiting completion.
    private boolean isAuthenticating = false;

    /**
     * Required empty public constructor.
     */
    public LoginFragment() {
    }

    /**
     * Creates and initializes the login screen.
     *
     * @param inflater LayoutInflater used to inflate the fragment layout
     * @param container Parent view that contains the fragment
     * @param savedInstanceState Previously saved fragment state
     * @return Inflated fragment view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Retrieve references to the login fields and action buttons.
        TextInputEditText editUsername =
                view.findViewById(R.id.editTextUsername);

        TextInputEditText editPassword =
                view.findViewById(R.id.editTextPassword);

        MaterialButton buttonLogin =
                view.findViewById(R.id.buttonLogin);

        MaterialButton buttonCreateAccount =
                view.findViewById(R.id.buttonCreateAccount);

        // Get the ViewModel used to store the current user session.
        SessionViewModel sessionViewModel =
                new ViewModelProvider(requireActivity())
                        .get(SessionViewModel.class);

        // Monitor both fields and enable the buttons when credentials are entered.
        TextWatcher loginTextWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count) {

                String username = editUsername.getText() == null
                        ? ""
                        : editUsername.getText().toString().trim();

                String password = editPassword.getText() == null
                        ? ""
                        : editPassword.getText().toString().trim();

                boolean enableButtons =
                        !isAuthenticating
                                && !username.isEmpty()
                                && !password.isEmpty();

                buttonLogin.setEnabled(enableButtons);
                buttonCreateAccount.setEnabled(enableButtons);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };

        editUsername.addTextChangedListener(loginTextWatcher);
        editPassword.addTextChangedListener(loginTextWatcher);

        // Keep both actions disabled until the required fields contain values.
        buttonLogin.setEnabled(false);
        buttonCreateAccount.setEnabled(false);

        // Validate the entered credentials against the database.
        buttonLogin.setOnClickListener(v -> {

            // Get the user-entered data.
            String username = editUsername.getText() == null
                    ? ""
                    : editUsername.getText().toString().trim();

            String password = editPassword.getText() == null
                    ? ""
                    : editPassword.getText().toString().trim();

            // Get the repository used to query the database for
            // the username and password.
            EventRepository repository =
                    EventRepository.getInstance(requireContext());

            // Mark authentication as pending.
            isAuthenticating = true;

            // Prevent additional authentication requests while login is pending.
            buttonLogin.setEnabled(false);
            buttonCreateAccount.setEnabled(false);

            // Query for the entered username and password.
            repository.getUser(username, password, new EventRepository.RepositoryCallback<User>() {
                @Override
                public void onSuccess(User user) {

                    // Ignore results intended for a view that is no longer active.
                    if (getView() != view) {
                        return;
                    }

                    // Reject credentials that do not match an existing user.
                    if (user == null) {

                        // Allow another authentication attempt.
                        isAuthenticating = false;
                        buttonLogin.setEnabled(true);
                        buttonCreateAccount.setEnabled(true);

                        // Notify the user that the login failed.
                        Toast.makeText(
                                requireContext(),
                                R.string.login_failed,
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // Mark authentication as complete.
                    isAuthenticating = false;

                    // Store the authenticated user's ID.
                    sessionViewModel.setUserId(user.getId());

                    // Initialize the username and password fields values.
                    editUsername.setText("");
                    editPassword.setText("");

                    // Navigate to the event list.
                    NavHostFragment.findNavController(LoginFragment.this)
                            .navigate(R.id.action_loginFragment_to_eventListFragment);
                }

                @Override
                public void onError(Exception error) {
                    // Ignore callbacks intended for a view that is no longer active.
                    if (getView() != view) {
                        return;
                    }

                    // Allow another authentication attempt.
                    isAuthenticating = false;
                    buttonLogin.setEnabled(true);
                    buttonCreateAccount.setEnabled(true);

                    // Report a database failure separately from invalid credentials.
                    Toast.makeText(
                            requireContext(),
                            "Unable to complete login.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });

        // Confirm the account information before creating the account.
        buttonCreateAccount.setOnClickListener(v -> {

            // Get the user-entered data.
            String username = editUsername.getText() == null
                    ? ""
                    : editUsername.getText().toString().trim();

            String password = editPassword.getText() == null
                    ? ""
                    : editPassword.getText().toString().trim();

            // Get the repository used to check whether the username
            // already exists and, if not, create the user record.
            EventRepository repository =
                    EventRepository.getInstance(requireContext());

            // Prevent additional requests while checking username availability.
            isAuthenticating = true;
            buttonLogin.setEnabled(false);
            buttonCreateAccount.setEnabled(false);

            // Do not allow duplicate usernames.
            // Check for an existing username without blocking the UI thread.
            repository.getUserByUsername(username, new EventRepository.RepositoryCallback<User>() {
                @Override
                public void onSuccess(User existingUser) {

                    // Ignore callbacks intended for a view that is no longer active.
                    if (getView() != view) {
                        return;
                    }

                    // Reject usernames that are already registered.
                    if (existingUser != null) {

                        // Allow another authentication or account-creation attempt.
                        isAuthenticating = false;
                        buttonLogin.setEnabled(true);
                        buttonCreateAccount.setEnabled(true);

                        // Notify the user that the username already exists.
                        Toast.makeText(
                                requireContext(),
                                R.string.username_exists,
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // Continue account creation when the username is available.
                    // Build create-account confirmation message
                    // using only the entered username.
                    String message = getString(
                            R.string.create_account_message,
                            username
                    );

                    // Build and display account-creation
                    // confirmation dialog box.
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.create_account_title)
                            .setMessage(message)
                            .setNegativeButton(R.string.cancel, (dialog, which) -> {

                                // Restore authentication controls when account creation is canceled.
                                isAuthenticating = false;
                                buttonLogin.setEnabled(true);
                                buttonCreateAccount.setEnabled(true);
                            })
                            .setPositiveButton(R.string.create_account, (dialog, which) -> {

                                // Instantiate a User object.
                                User user = new User(username, password);

                                // Insert the new user without blocking the UI thread.
                                repository.insertUser(user, new EventRepository.RepositoryCallback<Long>() {
                                    @Override
                                    public void onSuccess(Long userId) {

                                        // Ignore callbacks intended for a view that is no longer active.
                                        if (getView() != view) {
                                            return;
                                        }

                                        // Mark authentication as complete.
                                        isAuthenticating = false;

                                        // Store the new user's ID.
                                        sessionViewModel.setUserId(userId.intValue());

                                        // Notify the user that the account was created.
                                        Toast.makeText(
                                                requireContext(),
                                                R.string.account_created,
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        // Clear the credentials.
                                        editUsername.setText("");
                                        editPassword.setText("");

                                        // Navigate to the event list.
                                        NavHostFragment.findNavController(LoginFragment.this)
                                                .navigate(R.id.action_loginFragment_to_eventListFragment);
                                    }

                                    @Override
                                    public void onError(Exception error) {

                                        // Ignore callbacks intended for a view that is no longer active.
                                        if (getView() != view) {
                                            return;
                                        }

                                        // Allow another authentication or account-creation attempt.
                                        isAuthenticating = false;
                                        buttonLogin.setEnabled(true);
                                        buttonCreateAccount.setEnabled(true);

                                        // Notify the user that account creation failed.
                                        Toast.makeText(
                                                requireContext(),
                                                "Unable to create the account.",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                });
                            })
                            .setOnCancelListener(dialog -> {

                                // Restore authentication controls when account creation is canceled.
                                isAuthenticating = false;
                                buttonLogin.setEnabled(true);
                                buttonCreateAccount.setEnabled(true);
                            })
                            .show();
                }

                @Override
                public void onError(Exception error) {

                    // Ignore callbacks intended for a view that is no longer active.
                    if (getView() != view) {
                        return;
                    }

                    // Allow another authentication or account-creation attempt.
                    isAuthenticating = false;
                    buttonLogin.setEnabled(true);
                    buttonCreateAccount.setEnabled(true);

                    // Report a failure to check username availability.
                    Toast.makeText(
                            requireContext(),
                            "Unable to check username availability.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });

        return view;
    }
}