package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Demo home screen. The app currently has no remote data source, so this activity
 * routes the static dashboard content to the existing screens with sample extras.
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupDashboardActions();
        setupBottomNavigation();
    }

    private void setupDashboardActions() {
        findViewById(R.id.buttonNotifications).setOnClickListener(v ->
                Toast.makeText(this, R.string.demo_notifications_message, Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.searchBar).setOnClickListener(v -> openServices());
        findViewById(R.id.cardAiBooking).setOnClickListener(v -> openAiBooking());
        findViewById(R.id.buttonTryAi).setOnClickListener(v -> openAiBooking());

        findViewById(R.id.textViewAllAppointments).setOnClickListener(v -> openAppointments());
        findViewById(R.id.cardUpcomingAppointment).setOnClickListener(v -> openUpcomingAppointment());

        findViewById(R.id.textViewAllServices).setOnClickListener(v -> openServices());
        findViewById(R.id.serviceHaircut).setOnClickListener(v -> openBooking("Haircut", "$25.00", "Michael"));
        findViewById(R.id.serviceShampoo).setOnClickListener(v -> openBooking("Shampoo", "$12.00", "Michael"));
        findViewById(R.id.servicePerm).setOnClickListener(v -> openBooking("Perm", "$60.00", "Sophia"));
        findViewById(R.id.serviceColoring).setOnClickListener(v -> openBooking("Coloring", "$50.00", "Sophia"));
        findViewById(R.id.serviceCombo).setOnClickListener(v -> openBooking("Combo Package", "$55.00", "David"));

        findViewById(R.id.textViewAllBarbers).setOnClickListener(v -> openBarbers());
        findViewById(R.id.cardFeaturedMichael).setOnClickListener(v -> openBooking("Haircut", "$25.00", "Michael"));
        findViewById(R.id.cardFeaturedDavid).setOnClickListener(v -> openBooking("Haircut", "$25.00", "David"));
        findViewById(R.id.cardFeaturedJames).setOnClickListener(v -> openBooking("Haircut", "$25.00", "James"));
    }

    private void setupBottomNavigation() {
        findViewById(R.id.homeNavHome).setOnClickListener(v -> scrollToTop());
        findViewById(R.id.homeNavServices).setOnClickListener(v -> openServices());
        findViewById(R.id.homeNavAi).setOnClickListener(v -> openAiBooking());
        findViewById(R.id.homeNavAppointments).setOnClickListener(v -> openAppointments());
        findViewById(R.id.homeNavProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    private void openBooking(String serviceName, String servicePrice, String barberName) {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("selectedServiceName", serviceName);
        intent.putExtra("selectedServicePrice", servicePrice);
        intent.putExtra("selectedBarberName", barberName);
        startActivity(intent);
    }

    private void openUpcomingAppointment() {
        Intent intent = new Intent(this, AppointmentDetailActivity.class);
        intent.putExtra("appointmentId", "#AB25678");
        intent.putExtra("appointmentStatus", "Upcoming");
        intent.putExtra("barberName", "Michael");
        intent.putExtra("barberExperience", "9+ years experience");
        intent.putExtra("barberSpecialty", "Specialty: Classic Cut");
        intent.putExtra("serviceName", "Haircut, Classic Cut");
        intent.putExtra("appointmentPrice", "$25.00");
        intent.putExtra("appointmentDate", "Sat, May 24, 2025");
        intent.putExtra("appointmentStartTime", "11:00 AM");
        intent.putExtra("appointmentEndTime", "11:45 AM");
        intent.putExtra("appointmentDuration", "45 min");
        intent.putExtra("paymentStatus", getString(R.string.appointment_payment_paid));
        intent.putExtra("paymentMethod", "Card **** 4567");
        startActivity(intent);
    }

    private void openServices() {
        startActivity(new Intent(this, ServiceListActivity.class));
    }

    private void openBarbers() {
        startActivity(new Intent(this, BarberListActivity.class));
    }

    private void openAppointments() {
        startActivity(new Intent(this, AppointmentActivity.class));
    }

    private void openAiBooking() {
        startActivity(new Intent(this, AIChatBookingActivity.class));
    }

    private void scrollToTop() {
        ((ScrollView) findViewById(R.id.homeScroll)).smoothScrollTo(0, 0);
    }
}
