package com.example.barbershop.util;

import com.example.barbershop.R;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.barbershop.data.AppointmentRepository;
import com.example.barbershop.services.AppointmentReminderScheduler;
import com.example.barbershop.services.SyncService;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingConfirmActivity extends AppCompatActivity {
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    private long serviceId;
    private long barberId;
    private String serviceName;
    private double servicePrice;
    private int serviceDuration;
    private String barberName;
    private String bookingNote;
    private long startAtMillis;
    private long endAtMillis;
    private boolean isAiBooking;
    private AppointmentRepository appointmentRepository;
    private View confirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirm);

        appointmentRepository = new AppointmentRepository(this);
        confirmButton = findViewById(R.id.buttonConfirmPay);
        readBookingData();
        populateSummary();
        setupActions();
    }

    private void readBookingData() {
        Intent intent = getIntent();
        serviceId = readLongExtra(intent, BookingActivity.EXTRA_SERVICE_ID);
        barberId = readLongExtra(intent, BookingActivity.EXTRA_BARBER_ID);
        serviceName = readStringExtra(intent, "serviceName");
        servicePrice = intent.getDoubleExtra("servicePrice", 0.0);
        serviceDuration = intent.getIntExtra("serviceDuration", 0);
        barberName = readStringExtra(intent, BookingActivity.EXTRA_BARBER_NAME);
        bookingNote = readStringExtra(intent, "bookingNote");
        startAtMillis = intent.getLongExtra("startAtMillis", -1L);
        endAtMillis = intent.getLongExtra("endAtMillis", -1L);
        isAiBooking = intent.getBooleanExtra(BookingActivity.EXTRA_AI_BOOKING, false);
        if (serviceDuration <= 0 && endAtMillis > startAtMillis) {
            serviceDuration = (int) ((endAtMillis - startAtMillis) / 60_000L);
        }
    }

    private void populateSummary() {
        ((TextView) findViewById(R.id.textConfirmService)).setText(serviceName);
        ((TextView) findViewById(R.id.textConfirmBarber)).setText(barberName);
        ((TextView) findViewById(R.id.textConfirmDate)).setText(formatDate(startAtMillis));
        ((TextView) findViewById(R.id.textConfirmStartTime)).setText(formatTime(startAtMillis));
        ((TextView) findViewById(R.id.textConfirmEndTime)).setText(formatTime(endAtMillis));
        ((TextView) findViewById(R.id.textConfirmDuration)).setText(
                String.format(Locale.US, "%d min", serviceDuration)
        );
        ((TextView) findViewById(R.id.textConfirmNote)).setText(
                bookingNote.isEmpty() ? getString(R.string.booking_no_note) : bookingNote
        );

        ((TextView) findViewById(R.id.textConfirmPrimaryLabel)).setText(serviceName);
        ((TextView) findViewById(R.id.textConfirmPrimaryPrice)).setText(formatPrice(servicePrice));
        findViewById(R.id.confirmAddonRow).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.textConfirmSubtotal)).setText(formatPrice(servicePrice));
        ((TextView) findViewById(R.id.textConfirmTax)).setText(formatPrice(0.0));
        ((TextView) findViewById(R.id.textConfirmTotalAmount)).setText(formatPrice(servicePrice));
        ((TextView) confirmButton).setText(R.string.booking_confirm_pay);
    }

    private void setupActions() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonEditBooking).setOnClickListener(v -> finish());
        confirmButton.setOnClickListener(v -> createAppointment());
    }

    private void createAppointment() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.booking_login_required, Toast.LENGTH_LONG).show();
            return;
        }
        if (!hasValidBooking()) {
            Toast.makeText(this, R.string.booking_invalid_selection, Toast.LENGTH_LONG).show();
            return;
        }

        requestNotificationPermissionIfNeeded();
        setSubmitting(true);
        saveAppointment(currentUser);
    }

    private void saveAppointment(FirebaseUser currentUser) {
        final boolean savedOffline = !SyncService.hasUsableNetwork(this);
        appointmentRepository.createAppointment(
                currentUser.getUid(),
                barberId,
                serviceId,
                new Timestamp(new Date(startAtMillis)),
                new Timestamp(new Date(endAtMillis)),
                bookingNote,
                new AppointmentRepository.RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String documentId) {
                        AppointmentReminderScheduler.scheduleReminder(
                                BookingConfirmActivity.this,
                                documentId,
                                serviceName,
                                barberName,
                                startAtMillis
                        );
                        Toast.makeText(BookingConfirmActivity.this,
                                savedOffline ? R.string.booking_created_offline : R.string.booking_created_success,
                                Toast.LENGTH_LONG).show();
                        if (savedOffline) {
                            Intent homeIntent = new Intent(BookingConfirmActivity.this, HomeActivity.class);
                            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(homeIntent);
                            finish();
                            return;
                        }
                        Intent intent = new Intent(BookingConfirmActivity.this, PaymentActivity.class);
                        intent.putExtra("appointmentId", documentId);
                        intent.putExtra("selectedServiceName", serviceName);
                        intent.putExtra("selectedBarberName", barberName);
                        intent.putExtra("selectedDateLabel", formatDate(startAtMillis));
                        intent.putExtra("selectedStartTime", formatTime(startAtMillis));
                        intent.putExtra("selectedEndTime", formatTime(endAtMillis));
                        intent.putExtra("appointmentNote", bookingNote);
                        intent.putExtra("totalDurationMinutes", serviceDuration);
                        intent.putExtra("totalPrice", servicePrice);
                        intent.putExtra("amount", formatPrice(servicePrice));
                        intent.putExtra(PaymentActivity.EXTRA_RETURN_TO_APPOINTMENTS, isAiBooking);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(Exception exception) {
                        setSubmitting(false);
                        String message = exception == null || exception.getMessage() == null
                                ? getString(R.string.state_error_placeholder)
                                : exception.getMessage();
                        Toast.makeText(BookingConfirmActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private boolean hasValidBooking() {
        return serviceId > 0L && barberId > 0L
                && startAtMillis > 0L && endAtMillis > startAtMillis;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_POST_NOTIFICATIONS
        );
    }

    private void setSubmitting(boolean submitting) {
        confirmButton.setEnabled(!submitting);
        confirmButton.setAlpha(submitting ? 0.5f : 1.0f);
    }

    private String readStringExtra(Intent intent, String key) {
        String value = intent.getStringExtra(key);
        return value == null ? "" : value.trim();
    }

    private long readLongExtra(Intent intent, String key) {
        Object value = intent.getExtras() == null ? null : intent.getExtras().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                // Invalid IDs are rejected by hasValidBooking() before any data is written.
            }
        }
        return -1L;
    }

    private String formatDate(long timeMillis) {
        return timeMillis <= 0L ? "" : new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
                .format(new Date(timeMillis));
    }

    private String formatTime(long timeMillis) {
        return timeMillis <= 0L ? "" : new SimpleDateFormat("h:mm a", Locale.US)
                .format(new Date(timeMillis));
    }

    private String formatPrice(double value) {
        return String.format(Locale.US, "$%.2f", value);
    }
}
