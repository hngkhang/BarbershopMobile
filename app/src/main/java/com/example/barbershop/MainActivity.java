package com.example.barbershop;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private View buttonAiBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        buttonAiBooking = findViewById(R.id.buttonAiBooking);

        setupBottomNavigation();

        // TODO: Load the signed-in user's navigation/home state from Firebase/SQLite.
        // TODO: Show appointment badge counts when appointment data is connected.
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return launchActivityIfAvailable("HomeActivity");
            } else if (itemId == R.id.nav_services) {
                return launchActivityIfAvailable("ServiceListActivity");
            } else if (itemId == R.id.nav_ai_booking) {
                return launchActivityIfAvailable("AIChatBookingActivity");
            } else if (itemId == R.id.nav_appointments) {
                return launchActivityIfAvailable("AppointmentActivity");
            } else if (itemId == R.id.nav_profile) {
                return launchActivityIfAvailable("ProfileActivity");
            }

            return false;
        });

        bottomNavigationView.setOnItemReselectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                launchActivityIfAvailable("HomeActivity");
            } else if (itemId == R.id.nav_services) {
                launchActivityIfAvailable("ServiceListActivity");
            } else if (itemId == R.id.nav_ai_booking) {
                launchActivityIfAvailable("AIChatBookingActivity");
            } else if (itemId == R.id.nav_appointments) {
                launchActivityIfAvailable("AppointmentActivity");
            } else if (itemId == R.id.nav_profile) {
                launchActivityIfAvailable("ProfileActivity");
            }
        });

        buttonAiBooking.setOnClickListener(v -> {
            if (bottomNavigationView.getSelectedItemId() == R.id.nav_ai_booking) {
                launchActivityIfAvailable("AIChatBookingActivity");
            } else {
                bottomNavigationView.setSelectedItemId(R.id.nav_ai_booking);
            }
        });
    }

    private boolean launchActivityIfAvailable(String simpleClassName) {
        String targetClassName = getPackageName() + "." + simpleClassName;

        try {
            Class<?> targetClass = Class.forName(targetClassName);
            Intent intent = new Intent(this, targetClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        } catch (ClassNotFoundException exception) {
            showUnavailableScreenMessage(simpleClassName);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, getString(R.string.nav_target_not_registered), Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    private void showUnavailableScreenMessage(String simpleClassName) {
        String screenName = simpleClassName.replace("Activity", "");
        Toast.makeText(this, getString(R.string.nav_target_unavailable, screenName), Toast.LENGTH_SHORT).show();
    }
}
