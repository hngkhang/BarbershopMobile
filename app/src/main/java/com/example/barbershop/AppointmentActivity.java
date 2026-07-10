package com.example.barbershop;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppointmentActivity extends AppCompatActivity {

    private String selectedStatus = AppointmentAdapter.AppointmentItem.STATUS_UPCOMING;
    private AppointmentAdapter appointmentAdapter;
    private View emptyState;
    private View loadingState;
    private View errorState;
    private final List<AppointmentAdapter.AppointmentItem> allAppointments = new ArrayList<>();
    private final List<TextView> statusChips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        setupTopBar();
        setupRecyclerView();
        setupStatusChips();
        setupBottomNavigation();

        showLoading(false);
        // TODO: Replace this temporary in-memory sample data with Firebase/SQLite appointment data.
        loadSampleAppointments();
        applyStatusFilter();
    }

    private void setupTopBar() {
        findViewById(R.id.buttonAppointmentMenu).setOnClickListener(v ->
                Toast.makeText(this, R.string.appointment_more_todo, Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.buttonAppointmentNotifications).setOnClickListener(v ->
                Toast.makeText(this, R.string.appointment_notifications_todo, Toast.LENGTH_SHORT).show()
        );
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerAppointments);
        emptyState = findViewById(R.id.layoutAppointmentsEmpty);
        loadingState = findViewById(R.id.layoutAppointmentsLoading);
        errorState = findViewById(R.id.layoutAppointmentsError);

        appointmentAdapter = new AppointmentAdapter(this::openAppointmentDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(appointmentAdapter);
    }

    private void setupStatusChips() {
        addStatusChip(R.id.chipUpcoming, AppointmentAdapter.AppointmentItem.STATUS_UPCOMING);
        addStatusChip(R.id.chipPending, AppointmentAdapter.AppointmentItem.STATUS_PENDING);
        addStatusChip(R.id.chipCompleted, AppointmentAdapter.AppointmentItem.STATUS_COMPLETED);
        addStatusChip(R.id.chipCancelled, AppointmentAdapter.AppointmentItem.STATUS_CANCELLED);
        updateChipSelection();
    }

    private void addStatusChip(int chipId, String status) {
        TextView chip = findViewById(chipId);
        statusChips.add(chip);
        chip.setOnClickListener(v -> {
            selectedStatus = status;
            updateChipSelection();
            applyStatusFilter();
        });
    }

    private void updateChipSelection() {
        for (TextView chip : statusChips) {
            boolean selected = chip.getText().toString().equals(selectedStatus);
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(
                    this,
                    selected ? R.color.color_text_inverse : R.color.color_text_primary
            ));
            chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void loadSampleAppointments() {
        allAppointments.clear();
        allAppointments.addAll(Arrays.asList(
                new AppointmentAdapter.AppointmentItem(
                        "#AB25678", "Sat", "May 24", "Sat, May 24, 2025",
                        "11:00 AM", "11:45 AM", "45 min", "Michael",
                        "9+ years experience", "Specialty: Classic Cut",
                        "Haircut, Classic Cut", "$25.00",
                        AppointmentAdapter.AppointmentItem.STATUS_UPCOMING,
                        getString(R.string.appointment_payment_paid),
                        "Card **** 4567", getString(R.string.booking_default_note)
                ),
                new AppointmentAdapter.AppointmentItem(
                        "#AB25679", "Wed", "May 22", "Wed, May 22, 2025",
                        "2:00 PM", "2:45 PM", "45 min", "David",
                        "7 years experience", "Specialty: Haircut, Beard",
                        "Haircut, Beard", "$37.00",
                        AppointmentAdapter.AppointmentItem.STATUS_UPCOMING,
                        getString(R.string.appointment_payment_paid),
                        "Card **** 4567", getString(R.string.booking_no_note)
                ),
                new AppointmentAdapter.AppointmentItem(
                        "#AB25680", "Tue", "May 20", "Tue, May 20, 2025",
                        "10:00 AM", "10:45 AM", "45 min", "James",
                        "6 years experience", "Specialty: Shampoo, Styling",
                        "Haircut, Shampoo", "$37.00",
                        AppointmentAdapter.AppointmentItem.STATUS_PENDING,
                        getString(R.string.appointment_payment_not_paid),
                        "Pending", "Please prepare a natural finish."
                ),
                new AppointmentAdapter.AppointmentItem(
                        "#AB25681", "Sun", "May 18", "Sun, May 18, 2025",
                        "12:00 PM", "1:15 PM", "75 min", "Sophia",
                        "8 years experience", "Specialty: Coloring, Perm",
                        "Coloring, Perm", "$80.00",
                        AppointmentAdapter.AppointmentItem.STATUS_COMPLETED,
                        getString(R.string.appointment_payment_paid),
                        "Card **** 4567", getString(R.string.booking_no_note)
                ),
                new AppointmentAdapter.AppointmentItem(
                        "#AB25682", "Fri", "May 16", "Fri, May 16, 2025",
                        "4:00 PM", "4:45 PM", "45 min", "Ethan",
                        "5 years experience", "Specialty: Fade, Classic Cut",
                        "Haircut, Fade", "$30.00",
                        AppointmentAdapter.AppointmentItem.STATUS_CANCELLED,
                        getString(R.string.appointment_payment_refunded),
                        "Refunded", getString(R.string.booking_no_note)
                )
        ));
    }

    private void applyStatusFilter() {
        List<AppointmentAdapter.AppointmentItem> filteredAppointments = new ArrayList<>();
        for (AppointmentAdapter.AppointmentItem appointmentItem : allAppointments) {
            if (appointmentItem.status.equals(selectedStatus)) {
                filteredAppointments.add(appointmentItem);
            }
        }
        appointmentAdapter.submitList(filteredAppointments);
        showContentState(filteredAppointments.isEmpty());
    }

    private void showLoading(boolean loading) {
        loadingState.setVisibility(loading ? View.VISIBLE : View.GONE);
        errorState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void showContentState(boolean empty) {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void openAppointmentDetail(AppointmentAdapter.AppointmentItem appointmentItem) {
        Intent intent = new Intent(this, AppointmentDetailActivity.class);
        intent.putExtra("appointmentId", appointmentItem.id);
        intent.putExtra("appointmentStatus", appointmentItem.status);
        intent.putExtra("barberName", appointmentItem.barberName);
        intent.putExtra("barberExperience", appointmentItem.barberExperience);
        intent.putExtra("barberSpecialty", appointmentItem.barberSpecialty);
        intent.putExtra("serviceName", appointmentItem.serviceName);
        intent.putExtra("appointmentPrice", appointmentItem.price);
        intent.putExtra("appointmentDate", appointmentItem.fullDate);
        intent.putExtra("appointmentStartTime", appointmentItem.time);
        intent.putExtra("appointmentEndTime", appointmentItem.endTime);
        intent.putExtra("appointmentDuration", appointmentItem.duration);
        intent.putExtra("paymentStatus", appointmentItem.paymentStatus);
        intent.putExtra("paymentMethod", appointmentItem.paymentMethod);
        intent.putExtra("appointmentNote", appointmentItem.note);
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_appointments);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_appointments) {
                return true;
            } else if (itemId == R.id.nav_home) {
                return launchActivityIfAvailable("HomeActivity");
            } else if (itemId == R.id.nav_services) {
                return launchActivityIfAvailable("ServiceListActivity");
            } else if (itemId == R.id.nav_ai_booking) {
                return launchActivityIfAvailable("AIChatBookingActivity");
            } else if (itemId == R.id.nav_profile) {
                return launchActivityIfAvailable("ProfileActivity");
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
}
