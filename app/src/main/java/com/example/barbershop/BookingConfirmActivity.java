package com.example.barbershop;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BookingConfirmActivity extends AppCompatActivity {

    private static final String DEFAULT_SERVICE_NAME = "Haircut";
    private static final String DEFAULT_SERVICE_PRICE = "$25.00";
    private static final String DEFAULT_ADDON_NAME = "Shampoo";
    private static final String DEFAULT_ADDON_PRICE = "$12.00";
    private static final String DEFAULT_BARBER_NAME = "Michael";
    private static final String DEFAULT_DATE = "Wed, May 22, 2024";
    private static final String DEFAULT_START_TIME = "10:00 AM";
    private static final String DEFAULT_END_TIME = "10:45 AM";
    private static final int DEFAULT_DURATION_MINUTES = 45;
    private static final double DEFAULT_TOTAL_PRICE = 37.0;

    private String selectedServiceName;
    private String selectedServicePrice;
    private String addonServiceName;
    private String addonServicePrice;
    private String selectedBarberName;
    private String selectedDateLabel;
    private String selectedStartTime;
    private String selectedEndTime;
    private String bookingNote;
    private int totalDurationMinutes;
    private double totalPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirm);

        readTemporaryBookingData();
        populateSummary();
        setupActions();
    }

    private void readTemporaryBookingData() {
        Intent intent = getIntent();
        // TODO: Replace temporary extras/defaults with a real pending booking model before payment.
        selectedServiceName = readStringExtra(intent, "selectedServiceName", DEFAULT_SERVICE_NAME);
        selectedServicePrice = readStringExtra(intent, "selectedServicePrice", DEFAULT_SERVICE_PRICE);
        addonServiceName = readStringExtra(intent, "addonServiceName", DEFAULT_ADDON_NAME);
        addonServicePrice = readStringExtra(intent, "addonServicePrice", DEFAULT_ADDON_PRICE);
        selectedBarberName = readStringExtra(intent, "selectedBarberName", DEFAULT_BARBER_NAME);
        selectedDateLabel = readStringExtra(intent, "selectedDateLabel", DEFAULT_DATE);
        selectedStartTime = readStringExtra(intent, "selectedStartTime", DEFAULT_START_TIME);
        selectedEndTime = readStringExtra(intent, "selectedEndTime", DEFAULT_END_TIME);
        bookingNote = readStringExtra(intent, "bookingNote", getString(R.string.booking_default_note));
        totalDurationMinutes = intent.getIntExtra("totalDurationMinutes", DEFAULT_DURATION_MINUTES);
        totalPrice = intent.getDoubleExtra("totalPrice", DEFAULT_TOTAL_PRICE);
    }

    private String readStringExtra(Intent intent, String key, String fallback) {
        String value = intent.getStringExtra(key);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void populateSummary() {
        ((TextView) findViewById(R.id.textConfirmService)).setText(
                String.format(Locale.US, "%s, %s", selectedServiceName, addonServiceName)
        );
        ((TextView) findViewById(R.id.textConfirmBarber)).setText(selectedBarberName);
        ((TextView) findViewById(R.id.textConfirmDate)).setText(selectedDateLabel);
        ((TextView) findViewById(R.id.textConfirmStartTime)).setText(selectedStartTime);
        ((TextView) findViewById(R.id.textConfirmEndTime)).setText(selectedEndTime);
        ((TextView) findViewById(R.id.textConfirmDuration)).setText(
                String.format(Locale.US, "%d min", totalDurationMinutes)
        );
        ((TextView) findViewById(R.id.textConfirmNote)).setText(bookingNote);

        ((TextView) findViewById(R.id.textConfirmPrimaryLabel)).setText(selectedServiceName);
        ((TextView) findViewById(R.id.textConfirmPrimaryPrice)).setText(selectedServicePrice);
        ((TextView) findViewById(R.id.textConfirmAddonLabel)).setText(addonServiceName);
        ((TextView) findViewById(R.id.textConfirmAddonPrice)).setText(addonServicePrice);
        ((TextView) findViewById(R.id.textConfirmSubtotal)).setText(formatPrice(totalPrice));
        ((TextView) findViewById(R.id.textConfirmTax)).setText(formatPrice(0.0));
        ((TextView) findViewById(R.id.textConfirmTotalAmount)).setText(formatPrice(totalPrice));
    }

    private void setupActions() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonEditBooking).setOnClickListener(v -> returnToBooking());
        findViewById(R.id.buttonConfirmPay).setOnClickListener(v -> openPaymentIfAvailable());
    }

    private void returnToBooking() {
        Intent intent = new Intent(this, BookingActivity.class);
        putBookingExtras(intent);
        startActivity(intent);
        finish();
    }

    private void openPaymentIfAvailable() {
        try {
            Class<?> paymentActivity = Class.forName(getPackageName() + ".PaymentActivity");
            Intent intent = new Intent(this, paymentActivity);
            putBookingExtras(intent);
            intent.putExtra("amount", formatPrice(totalPrice));
            startActivity(intent);
        } catch (ClassNotFoundException exception) {
            Toast.makeText(this, R.string.booking_payment_unavailable, Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.nav_target_not_registered, Toast.LENGTH_SHORT).show();
        }
    }

    private void putBookingExtras(Intent intent) {
        intent.putExtra("selectedServiceName", selectedServiceName);
        intent.putExtra("selectedServicePrice", selectedServicePrice);
        intent.putExtra("addonServiceName", addonServiceName);
        intent.putExtra("addonServicePrice", addonServicePrice);
        intent.putExtra("selectedBarberName", selectedBarberName);
        intent.putExtra("selectedDateLabel", selectedDateLabel);
        intent.putExtra("selectedStartTime", selectedStartTime);
        intent.putExtra("selectedEndTime", selectedEndTime);
        intent.putExtra("totalDurationMinutes", totalDurationMinutes);
        intent.putExtra("totalPrice", totalPrice);
        intent.putExtra("bookingNote", bookingNote);
    }

    private String formatPrice(double price) {
        return String.format(Locale.US, "$%.2f", price);
    }
}
