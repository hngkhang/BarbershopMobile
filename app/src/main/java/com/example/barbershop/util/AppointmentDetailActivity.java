package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.Intent;
import android.os.Bundle;
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
        appointmentId = readStringExtra(intent, "appointmentId", "#AB25678");
        appointmentStatus = readStringExtra(intent, "appointmentStatus", AppointmentAdapter.AppointmentItem.STATUS_UPCOMING);
        barberName = readStringExtra(intent, "barberName", "Michael");
        serviceName = readStringExtra(intent, "serviceName", "Haircut, Classic Cut");
        price = readStringExtra(intent, "appointmentPrice", "$25.00");
        appointmentDate = readStringExtra(intent, "appointmentDate", "Sat, May 24, 2025");
        startTime = readStringExtra(intent, "appointmentStartTime", "11:00 AM");
        endTime = readStringExtra(intent, "appointmentEndTime", "11:45 AM");
        duration = readStringExtra(intent, "appointmentDuration", "45 min");
        paymentStatus = readStringExtra(intent, "paymentStatus", getString(R.string.appointment_payment_paid));
        paymentMethod = readStringExtra(intent, "paymentMethod", "Card **** 4567");
    }

    private void bindAppointmentDetails() {
        TextView statusView = findViewById(R.id.textDetailStatus);
        statusView.setText(appointmentStatus);
        statusView.setBackgroundResource(statusBackground(appointmentStatus));
        statusView.setTextColor(ContextCompat.getColor(this, statusColor(appointmentStatus)));

        String barberExperience = readStringExtra(getIntent(), "barberExperience", "9+ years experience");
        String barberSpecialty = readStringExtra(getIntent(), "barberSpecialty", "Specialty: Classic Cut");
        String note = readStringExtra(getIntent(), "appointmentNote", getString(R.string.booking_default_note));

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
        ((TextView) findViewById(R.id.textDetailPaymentStatus)).setText(detailText(getString(R.string.appointment_payment_status_label), paymentStatus));
        ((TextView) findViewById(R.id.textDetailPaymentMethod)).setText(detailText(getString(R.string.appointment_payment_method_label), paymentMethod));
        ((TextView) findViewById(R.id.textDetailNote)).setText(note);

        ((TextView) findViewById(R.id.textTimelineBooked)).setText("Booked          May 20, 2025 - 09:41 AM");
        ((TextView) findViewById(R.id.textTimelineConfirmed)).setText("Confirmed       May 20, 2025 - 09:43 AM");
        ((TextView) findViewById(R.id.textTimelineReminder)).setText("Reminder Sent   May 23, 2025 - 09:00 AM");
        ((TextView) findViewById(R.id.textTimelineCurrent)).setText(String.format(Locale.US, "%s        %s - %s", appointmentStatus, appointmentDate, startTime));
        // TODO: Replace timeline placeholder timestamps with real booking/reminder events from Firebase/SQLite.
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
            // TODO: Connect cancellation to the real appointment booking flow.
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

    private String readStringExtra(Intent intent, String key, String fallback) {
        String value = intent.getStringExtra(key);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String detailText(String label, String value) {
        return label + "\n" + value;
    }

    private String getInitial(String value) {
        return value == null || value.trim().isEmpty()
                ? "A"
                : value.trim().substring(0, 1).toUpperCase(Locale.US);
    }

    private int parseDurationMinutes(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException exception) {
            return 45;
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
