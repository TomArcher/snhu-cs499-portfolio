package com.thomasarcher.eventtrackerapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Hosts the application's fragments and configures edge-to-edge display.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Creates the activity and applies system-bar insets to the main layout.
     *
     * @param savedInstanceState Previously saved activity state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Allow the app content to draw behind the system bars.
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // Add padding so content remains visible around the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }
}