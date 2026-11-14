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
                        !username.isEmpty() && !password.isEmpty();

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

            // Query for the entered username and password.
            User user = repository.getUser(username, password);

            // If user found...
            if (user != null) {

                // Store the logged-in user's ID.
                sessionViewModel.setUserId(user.getId());

                // Navigate to the list fragment.
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_loginFragment_to_eventListFragment);

                // Initialize the username and password fields values.
                editUsername.setText("");
                editPassword.setText("");

            } else { // If user NOT found...

                // Display login-failure message.
                Toast.makeText(
                        requireContext(),
                        R.string.login_failed,
                        Toast.LENGTH_SHORT
                ).show();
            }
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

            // Do not allow duplicate usernames.
            if (repository.getUserByUsername(username) != null) {

                // Display username-in-use message.
                Toast.makeText(
                        requireContext(),
                        R.string.username_exists,
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

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
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.create_account, (dialog, which) -> {

                        // Instantiate a User object.
                        User user = new User(username, password);

                        // Insert the new user into the database.
                        long userId = repository.insertUser(user);

                        // Store the new user's ID.
                        sessionViewModel.setUserId((int) userId);

                        // Display success message.
                        Toast.makeText(
                                requireContext(),
                                R.string.account_created,
                                Toast.LENGTH_SHORT
                        ).show();

                        // Navigate to the list fragment.
                        NavHostFragment.findNavController(LoginFragment.this)
                                .navigate(R.id.action_loginFragment_to_eventListFragment);

                        // Initialize the username and password fields values.
                        editUsername.setText("");
                        editPassword.setText("");
                    })
                    .show();
        });

        return view;
    }
}