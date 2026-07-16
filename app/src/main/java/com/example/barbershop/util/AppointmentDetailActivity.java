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
    }

    private void bindAppointmentDetails() {
        TextView statusView = findViewById(R.id.textDetailStatus);
        statusView.setText(appointmentStatus);
        statusView.setBackgroundResource(statusBackground(appointmentStatus));
        statusView.setTextColor(ContextCompat.getColor(this, statusColor(appointmentStatus)));

        String barberExperience = readStringExtra(getIntent(), "barberExperience");
        String barberSpecialty = readStringExtra(getIntent(), "barberSpecialty");
        String note = readStringExtra(getIntent(), "appointmentNote");
        String createdAt = readStringExtra(getIntent(), "appointmentCreatedAt");

        ((TextView) findViewById(R.id.textAppointmentId)).setText(appointmentId);
        ((TextView) findViewById(R.id.textDetailBarberInitial)).setText(getInitial(barberName));
        findViewById(R.id.textDetailBarberInitial).setContentDescription(
                getString(R.string.barber_avatar_content_description, barberName)
        );
        ((TextView) findViewById(R.id.textDetailBarberName)).setText(barberName);
        ((TextView) findViewById(R.id.textDetailBarberExperience)).setText(barberExperience);
        ((TextView) findViewById(R.id.textDetailBarberSpecialty)).setText(barberSpecialty);
        ((TextView) findViewById(R.id.textDetailService)).setText(serviceName);
        ((TextView) findViewById(R.id.textDetailPrice)).setText(price);
        ((TextView) findViewById(R.id.textDetailDate)).setText(detailText(getString(R.string.appointment_date_label), appointmentDate));
        ((TextView) findViewById(R.id.textDetailStartTime)).setText(detailText(getString(R.string.appointment_start_time_label), startTime));
        ((TextView) findViewById(R.id.textDetailEndTime)).setText(detailText(getString(R.string.appointment_end_time_label), endTime));
        ((TextView) findViewById(R.id.textDetailDuration)).setText(detailText(getString(R.string.appointment_duration_label), duration));
        ((TextView) findViewById(R.id.textDetailNote)).setText(
                note.isEmpty() ? getString(R.string.booking_no_note) : note
        );

        boolean hasPaymentData = !paymentStatus.isEmpty() || !paymentMethod.isEmpty();
        findViewById(R.id.textDetailPaymentStatus).setVisibility(hasPaymentData ? View.VISIBLE : View.GONE);
        findViewById(R.id.textDetailPaymentMethod).setVisibility(hasPaymentData ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonViewPayment).setVisibility(hasPaymentData ? View.VISIBLE : View.GONE);
        if (hasPaymentData) {
            ((TextView) findViewById(R.id.textDetailPaymentStatus)).setText(
                    detailText(getString(R.string.appointment_payment_status_label), paymentStatus)
            );
            ((TextView) findViewById(R.id.textDetailPaymentMethod)).setText(
                    detailText(getString(R.string.appointment_payment_method_label), paymentMethod)
            );
        }

        TextView bookedTimeline = findViewById(R.id.textTimelineBooked);
        bookedTimeline.setVisibility(createdAt.isEmpty() ? View.GONE : View.VISIBLE);
        if (!createdAt.isEmpty()) {
            bookedTimeline.setText(String.format(Locale.US, "Booked          %s", createdAt));
        }
        findViewById(R.id.textTimelineConfirmed).setVisibility(View.GONE);
        findViewById(R.id.textTimelineReminder).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.textTimelineCurrent)).setText(
                String.format(Locale.US, "%s        %s - %s", appointmentStatus, appointmentDate, startTime)
        );
    }

    private void setupActions() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonDetailMore).setOnClickListener(v ->
                Toast.makeText(this, R.string.appointment_more_todo, Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.buttonMessageBarber).setOnClickListener(v -> {
            Intent intent = new Intent(this, AIChatBookingActivity.class);
            intent.putExtra("selectedBarberName", barberName);
            startActivity(intent);
        });
        findViewById(R.id.buttonViewPayment).setOnClickListener(v -> openPayment());
        findViewById(R.id.buttonCancelAppointment).setOnClickListener(v -> {
            // Cancellation will be connected to the Firestore booking flow separately.
            Toast.makeText(this, R.string.appointment_cancel_todo, Toast.LENGTH_SHORT).show();
        });
    }

    private void openPayment() {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("selectedServiceName", serviceName);
        intent.putExtra("selectedBarberName", barberName);
        intent.putExtra("selectedDateLabel", appointmentDate);
        intent.putExtra("selectedStartTime", startTime);
        intent.putExtra("totalDurationMinutes", parseDurationMinutes(duration));
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
}
