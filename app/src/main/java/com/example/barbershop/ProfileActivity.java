package com.example.barbershop;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String ACTION_APPOINTMENTS = "appointments";
    private static final String ACTION_LOGOUT = "logout";
    private static final String ACTION_PLACEHOLDER = "placeholder";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        bindTemporaryProfileData();
        setupProfileMenu();
        setupActions();
        setupBottomNavigation();
    }

    private void bindTemporaryProfileData() {
        String userName = "Alex Johnson";
        String email = "hello@artbarbershop.com";
        String phone = "+1 123 456 7890";

        ((TextView) findViewById(R.id.textProfileAvatar)).setText(getInitial(userName));
        ((TextView) findViewById(R.id.textProfileName)).setText(userName);
        ((TextView) findViewById(R.id.textProfileEmail)).setText(email);
        ((TextView) findViewById(R.id.textProfilePhone)).setText(phone);
        // TODO: Replace this temporary profile data with Firebase Auth/profile data and local SQLite cache.
    }

    private void setupProfileMenu() {
        RecyclerView recyclerView = findViewById(R.id.recyclerProfileMenu);
        ProfileMenuAdapter adapter = new ProfileMenuAdapter(this::handleProfileMenuClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        adapter.submitList(Arrays.asList(
                new ProfileMenuAdapter.ProfileMenuItem(getString(R.string.profile_my_appointments), R.drawable.ic_calendar, ACTION_APPOINTMENTS, false),
                new ProfileMenuAdapter.ProfileMenuItem(getString(R.string.profile_payment_history), R.drawable.ic_payment, ACTION_PLACEHOLDER, false),
                new ProfileMenuAdapter.ProfileMenuItem(getString(R.string.profile_notification_settings), R.drawable.ic_bell, ACTION_PLACEHOLDER, false),
                new ProfileMenuAdapter.ProfileMenuItem(getString(R.string.profile_ai_booking_history), R.drawable.ic_robot, ACTION_PLACEHOLDER, false),
                new ProfileMenuAdapter.ProfileMenuItem(getString(R.string.profile_about), R.drawable.ic_info, ACTION_PLACEHOLDER, false),
                new ProfileMenuAdapter.ProfileMenuItem(getString(R.string.profile_privacy), R.drawable.ic_shield, ACTION_PLACEHOLDER, false),
                new ProfileMenuAdapter.ProfileMenuItem(getString(R.string.profile_logout), R.drawable.ic_logout, ACTION_LOGOUT, true)
        ));
        // TODO: Replace temporary profile menu actions with real profile/payment/notification destinations as they are implemented.
    }

    private void setupActions() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonEditProfile).setOnClickListener(v ->
                Toast.makeText(this, R.string.profile_edit_todo, Toast.LENGTH_SHORT).show()
        );
    }

    private void handleProfileMenuClick(ProfileMenuAdapter.ProfileMenuItem item) {
        if (ACTION_APPOINTMENTS.equals(item.action)) {
            launchActivityIfAvailable("AppointmentActivity");
        } else if (ACTION_LOGOUT.equals(item.action)) {
            Toast.makeText(this, R.string.profile_logout_demo, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        } else {
            Toast.makeText(this, getString(R.string.profile_menu_todo, item.title), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_profile) {
                return true;
            } else if (itemId == R.id.nav_home) {
                return launchActivityIfAvailable("HomeActivity");
            } else if (itemId == R.id.nav_services) {
                return launchActivityIfAvailable("ServiceListActivity");
            } else if (itemId == R.id.nav_ai_booking) {
                return launchActivityIfAvailable("AIChatBookingActivity");
            } else if (itemId == R.id.nav_appointments) {
                return launchActivityIfAvailable("AppointmentActivity");
            }

            return false;
        });
        findViewById(R.id.buttonAiBooking).setOnClickListener(v -> launchActivityIfAvailable("AIChatBookingActivity"));
    }

    private boolean launchActivityIfAvailable(String simpleClassName) {
        try {
            Class<?> targetClass = Class.forName(getPackageName() + "." + simpleClassName);
            Intent intent = new Intent(this, targetClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        } catch (ClassNotFoundException exception) {
            Toast.makeText(this, getString(R.string.nav_target_unavailable, simpleClassName.replace("Activity", "")), Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.nav_target_not_registered, Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private String getInitial(String value) {
        return value == null || value.trim().isEmpty()
                ? "A"
                : value.trim().substring(0, 1).toUpperCase(Locale.US);
    }
}
