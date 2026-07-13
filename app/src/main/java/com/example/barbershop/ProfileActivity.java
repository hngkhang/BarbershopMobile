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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private static final String ACTION_APPOINTMENTS = "appointments";
    private static final String ACTION_LOGOUT = "logout";
    private static final String ACTION_PLACEHOLDER = "placeholder";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null || !currentUser.isEmailVerified()) {
            firebaseAuth.signOut();
            openLoginAndClearTask();
            return;
        }

        bindProfileData(currentUser);
        setupProfileMenu();
        setupActions();
        setupBottomNavigation();
    }

    private void bindProfileData(FirebaseUser user) {
        TextView textAvatar = findViewById(R.id.textProfileAvatar);
        TextView textName = findViewById(R.id.textProfileName);
        TextView textEmail = findViewById(R.id.textProfileEmail);
        TextView textPhone = findViewById(R.id.textProfilePhone);

        String email = user.getEmail() == null
                ? ""
                : user.getEmail();

        String defaultName = "User";

        if (email.contains("@")) {
            defaultName = email.substring(0, email.indexOf("@"));
        }

        textAvatar.setText(getInitial(defaultName));
        textName.setText(defaultName);
        textEmail.setText(email);
        textPhone.setText("Chưa cập nhật");

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        return;
                    }

                    String name = document.getString("name");
                    String phone = document.getString("phone");

                    if (name != null && !name.trim().isEmpty()) {
                        textName.setText(name);
                        textAvatar.setText(getInitial(name));
                    }

                    if (phone != null && !phone.trim().isEmpty()) {
                        textPhone.setText(phone);
                    }
                })
                .addOnFailureListener(exception ->
                        Toast.makeText(
                                this,
                                "Không thể tải thông tin hồ sơ",
                                Toast.LENGTH_SHORT
                        ).show()
                );
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
            firebaseAuth.signOut();

            Toast.makeText(
                    this,
                    "Signed out.",
                    Toast.LENGTH_SHORT
            ).show();

            openLoginAndClearTask();
        } else {
            Toast.makeText(this, getString(R.string.profile_menu_todo, item.title), Toast.LENGTH_SHORT).show();
        }
    }

    private void openLoginAndClearTask() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
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
