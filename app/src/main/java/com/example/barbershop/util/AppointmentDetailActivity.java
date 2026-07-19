package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.barbershop.adapters.AppointmentAdapter;

import java.util.Locale;

public class AppointmentDetailActivity extends AppCompatActivity {

    private String appointmentId;
    private String appointmentStatus;
    private String barberName;
    private String serviceName;
    private String price;
    private String appointmentDate;
    private String startTime;
    private String endTime;
    private String duration;
    private String paymentStatus;
    private String paymentMethod;
    private String barberExperience;
    private String barberSpecialty;
    private String appointmentNote;
    private String appointmentCreatedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_detail);

        readAppointmentExtras();
        bindAppointmentDetails();
        setupActions();
    }

    private void readAppointmentExtras() {
        Intent intent = getIntent();
        appointmentId = readStringExtra(intent, "appointmentId");
        appointmentStatus = readStringExtra(intent, "appointmentStatus");
        barberName = readStringExtra(intent, "barberName");
        serviceName = readStringExtra(intent, "serviceName");
        price = readStringExtra(intent, "appointmentPrice");
        appointmentDate = readStringExtra(intent, "appointmentDate");
        startTime = readStringExtra(intent, "appointmentStartTime");
        endTime = readStringExtra(intent, "appointmentEndTime");
        duration = readStringExtra(intent, "appointmentDuration");
        paymentStatus = readStringExtra(intent, "paymentStatus");
        paymentMethod = readStringExtra(intent, "paymentMethod");
        barberExperience = readStringExtra(intent, "barberExperience");
        barberSpecialty = readStringExtra(intent, "barberSpecialty");
        appointmentNote = readStringExtra(intent, "appointmentNote");
        appointmentCreatedAt = readStringExtra(intent, "appointmentCreatedAt");
    }

    private void bindAppointmentDetails() {
        TextView statusView = findViewById(R.id.textDetailStatus);
        statusView.setText(appointmentStatus);
        statusView.setBackgroundResource(statusBackground(appointmentStatus));
        statusView.setTextColor(ContextCompat.getColor(this, statusColor(appointmentStatus)));

        TextView paymentBadge = findViewById(R.id.textDetailPaymentBadge);
        boolean hasPaymentStatus = !paymentStatus.isEmpty();
        boolean isPaid = isPaidPayment();
        paymentBadge.setVisibility(hasPaymentStatus ? View.VISIBLE : View.GONE);
        if (hasPaymentStatus) {
            paymentBadge.setText(paymentStatus);
            paymentBadge.setBackgroundResource(paymentBadgeBackground(paymentStatus));
            paymentBadge.setTextColor(ContextCompat.getColor(this, paymentBadgeColor(paymentStatus)));
        }

        ((TextView) findViewById(R.id.textDetailBarberInitial)).setText(getInitial(barberName));
        findViewById(R.id.textDetailBarberInitial).setContentDescription(
                getString(R.string.barber_avatar_content_description, barberName)
        );
        ((TextView) findViewById(R.id.textDetailBarberName)).setText(barberName);
        ((TextView) findViewById(R.id.textDetailBarberExperience)).setText(
                formatBarberExperience(barberExperience)
        );
        TextView specialtyView = findViewById(R.id.textDetailBarberSpecialty);
        boolean hasSpecialty = !barberSpecialty.isEmpty()
                && !barberSpecialty.equalsIgnoreCase(getString(R.string.barber_specialty_not_available));
        specialtyView.setVisibility(hasSpecialty ? View.VISIBLE : View.GONE);
        if (hasSpecialty) {
            specialtyView.setText(barberSpecialty);
        }
        ((TextView) findViewById(R.id.textDetailService)).setText(serviceName);
        ((TextView) findViewById(R.id.textDetailPrice)).setText(price);
        ((TextView) findViewById(R.id.textDetailDate)).setText(detailText(getString(R.string.appointment_date_label), appointmentDate));
        ((TextView) findViewById(R.id.textDetailStartTime)).setText(detailText(getString(R.string.appointment_start_time_label), startTime));
        ((TextView) findViewById(R.id.textDetailEndTime)).setText(detailText(getString(R.string.appointment_end_time_label), endTime));
        ((TextView) findViewById(R.id.textDetailDuration)).setText(detailText(getString(R.string.appointment_duration_label), duration));
        ((TextView) findViewById(R.id.textDetailNote)).setText(
                appointmentNote.isEmpty() ? getString(R.string.booking_no_note) : appointmentNote
        );

        findViewById(R.id.textDetailPaymentStatus).setVisibility(View.GONE);
        findViewById(R.id.textDetailPaymentMethod).setVisibility(View.GONE);
        findViewById(R.id.buttonViewPayment).setVisibility(isPaid ? View.GONE : View.VISIBLE);

        TextView bookedTimeline = findViewById(R.id.textTimelineBooked);
        bookedTimeline.setVisibility(appointmentCreatedAt.isEmpty() ? View.GONE : View.VISIBLE);
        if (!appointmentCreatedAt.isEmpty()) {
            bookedTimeline.setText(String.format(Locale.US, "Booked          %s", appointmentCreatedAt));
        }
        findViewById(R.id.textTimelineConfirmed).setVisibility(View.GONE);
        findViewById(R.id.textTimelineReminder).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.textTimelineCurrent)).setText(
                String.format(Locale.US, "%s        %s - %s", appointmentStatus, appointmentDate, startTime)
        );
    }

    private void setupActions() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonViewPayment).setOnClickListener(v -> openPayment());
        findViewById(R.id.buttonCancelAppointment).setOnClickListener(v -> {
            // Cancellation will be connected to the Firestore booking flow separately.
            Toast.makeText(this, R.string.appointment_cancel_todo, Toast.LENGTH_SHORT).show();
        });
    }

    private void openPayment() {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("appointmentId", appointmentId);
        intent.putExtra("selectedServiceName", serviceName);
        intent.putExtra("selectedBarberName", barberName);
        intent.putExtra("selectedDateLabel", appointmentDate);
        intent.putExtra("selectedStartTime", startTime);
        intent.putExtra("selectedEndTime", endTime);
        intent.putExtra("barberExperience", barberExperience);
        intent.putExtra("barberSpecialty", barberSpecialty);
        intent.putExtra("appointmentNote", appointmentNote);
        intent.putExtra("appointmentCreatedAt", appointmentCreatedAt);
        intent.putExtra("totalDurationMinutes", parseDurationMinutes(duration));
        intent.putExtra("totalPrice", parsePrice(price));
        intent.putExtra("amount", price);
        startActivity(intent);
    }

    private String readStringExtra(Intent intent, String key) {
        String value = intent.getStringExtra(key);
        return value == null ? "" : value.trim();
    }

    private String detailText(String label, String value) {
        return label + "\n" + value;
    }

    private String formatBarberExperience(String value) {
        if (value == null || value.trim().isEmpty()) {
            return getString(R.string.barber_experience_not_updated);
        }

        String trimmedValue = value.trim();
        if (trimmedValue.matches("\\d+\\+?")) {
            return getString(R.string.barber_experience_format, trimmedValue);
        }
        return trimmedValue;
    }

    private boolean isPaidPayment() {
        return paymentStatus.equalsIgnoreCase(getString(R.string.appointment_payment_paid))
                || "PAID".equalsIgnoreCase(paymentStatus);
    }

    private String getInitial(String value) {
        return value == null || value.trim().isEmpty()
                ? ""
                : value.trim().substring(0, 1).toUpperCase(Locale.US);
    }

    private int parseDurationMinutes(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private double parsePrice(String value) {
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private int statusBackground(String status) {
        if (AppointmentAdapter.AppointmentItem.STATUS_PENDING.equals(status)) {
            return R.drawable.bg_badge_status_pending;
        } else if (AppointmentAdapter.AppointmentItem.STATUS_COMPLETED.equals(status)) {
            return R.drawable.bg_badge_status_completed;
        } else if (AppointmentAdapter.AppointmentItem.STATUS_CANCELLED.equals(status)) {
            return R.drawable.bg_badge_status_cancelled;
        }
        return R.drawable.bg_badge_status_upcoming;
    }

    private int statusColor(String status) {
        if (AppointmentAdapter.AppointmentItem.STATUS_PENDING.equals(status)) {
            return R.color.color_warning;
        } else if (AppointmentAdapter.AppointmentItem.STATUS_COMPLETED.equals(status)) {
            return R.color.color_neutral;
        } else if (AppointmentAdapter.AppointmentItem.STATUS_CANCELLED.equals(status)) {
            return R.color.color_error;
        }
        return R.color.color_success;
    }

    private int paymentBadgeBackground(String status) {
        if (status.equalsIgnoreCase(getString(R.string.appointment_payment_paid))
                || "PAID".equalsIgnoreCase(status)) {
            return R.drawable.bg_badge_payment_paid;
        }
        return R.drawable.bg_badge_payment_unpaid;
    }

    private int paymentBadgeColor(String status) {
        if (status.equalsIgnoreCase(getString(R.string.appointment_payment_paid))
                || "PAID".equalsIgnoreCase(status)) {
            return R.color.color_payment_paid;
        }
        return R.color.color_payment_unpaid;
    }
}
