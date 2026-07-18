package com.example.barbershop.util;

import com.example.barbershop.R;
import com.example.barbershop.services.MakePaymentService;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_RETURN_TO_APPOINTMENTS = "returnToAppointments";
    private static final long PAYMENT_WINDOW_MILLIS = 15 * 60 * 1000L;
    private static final long ONE_SECOND_MILLIS = 1000L;

    private CountDownTimer paymentCountdownTimer;
    private String transferContent;
    private String appointmentDocumentId;
    private String serviceName;
    private String barberName;
    private String barberExperience;
    private String barberSpecialty;
    private String dateLabel;
    private String startTime;
    private String endTime;
    private String amount;
    private String appointmentNote;
    private String appointmentCreatedAt;
    private int durationMinutes;
    private double totalPrice;
    private MakePaymentService makePaymentService;
    private boolean paymentServiceBound;
    private boolean returnToAppointmentsAfterPayment;
    private final ServiceConnection paymentServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MakePaymentService.MakePaymentBinder binder = (MakePaymentService.MakePaymentBinder) service;
            makePaymentService = binder.getService();
            paymentServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            makePaymentService = null;
            paymentServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        bindService(
                new Intent(this, MakePaymentService.class),
                paymentServiceConnection,
                Context.BIND_AUTO_CREATE
        );
        bindTemporaryPaymentDetails();
        setupActions();
        startCountdown();
    }

    @Override
    protected void onDestroy() {
        if (paymentCountdownTimer != null) {
            paymentCountdownTimer.cancel();
        }
        if (paymentServiceBound) {
            unbindService(paymentServiceConnection);
            paymentServiceBound = false;
        }
        super.onDestroy();
    }

    private void bindTemporaryPaymentDetails() {
        Intent intent = getIntent();
        appointmentDocumentId = readStringExtra(intent, "appointmentId", "");
        serviceName = readStringExtra(intent, "selectedServiceName", "Haircut");
        String addonServiceName = readStringExtra(intent, "addonServiceName", "Classic Cut");
        barberName = readStringExtra(intent, "selectedBarberName", "Michael");
        barberExperience = readStringExtra(intent, "barberExperience", "");
        barberSpecialty = readStringExtra(intent, "barberSpecialty", getString(R.string.barber_specialty_not_available));
        dateLabel = readStringExtra(intent, "selectedDateLabel", "Tomorrow, May 25, 2025");
        startTime = readStringExtra(intent, "selectedStartTime", "2:00 PM");
        endTime = readStringExtra(intent, "selectedEndTime", "");
        appointmentNote = readStringExtra(intent, "appointmentNote", "");
        appointmentCreatedAt = readStringExtra(intent, "appointmentCreatedAt", "");
        returnToAppointmentsAfterPayment = intent.getBooleanExtra(
                EXTRA_RETURN_TO_APPOINTMENTS,
                false
        );
        durationMinutes = intent.getIntExtra("totalDurationMinutes", 45);
        totalPrice = intent.getDoubleExtra("totalPrice", parseAmount(readStringExtra(intent, "amount", "$25.00")));
        amount = readStringExtra(intent, "amount", String.format(Locale.US, "$%.2f", totalPrice));

        ((TextView) findViewById(R.id.textPaymentBarberInitial)).setText(getInitial(barberName));
        ((TextView) findViewById(R.id.textPaymentBarber)).setText(barberName);
        ((TextView) findViewById(R.id.textPaymentService)).setText(
                String.format(Locale.US, "%s, %s", serviceName, addonServiceName)
        );
        ((TextView) findViewById(R.id.textPaymentDate)).setText(dateLabel);
        ((TextView) findViewById(R.id.textPaymentTime)).setText(
                String.format(Locale.US, "%s - %d min", startTime, durationMinutes)
        );
        ((TextView) findViewById(R.id.textPaymentAmount)).setText(amount);

        transferContent = getString(R.string.payment_transfer_content_value);
        ((TextView) findViewById(R.id.textPaymentBankName)).setText(rowText(
                getString(R.string.payment_bank_name),
                getString(R.string.payment_bank_name_value)
        ));
        ((TextView) findViewById(R.id.textPaymentAccountNumber)).setText(rowText(
                getString(R.string.payment_account_number),
                getString(R.string.payment_account_number_value)
        ));
        ((TextView) findViewById(R.id.textPaymentAccountName)).setText(rowText(
                getString(R.string.payment_account_name),
                getString(R.string.payment_account_name_value)
        ));
        ((TextView) findViewById(R.id.textPaymentTransferContent)).setText(rowText(
                getString(R.string.payment_transfer_content),
                transferContent
        ));

        // TODO: Replace the QR placeholder with QR data generated by PaymentService or the payment provider.
    }

    private void setupActions() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonPaymentMore).setOnClickListener(v ->
                Toast.makeText(this, R.string.payment_security_note, Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.buttonCopyTransferContent).setOnClickListener(v -> copyTransferContent());
        findViewById(R.id.buttonIHavePaid).setOnClickListener(v -> makeBankingPayment());
        findViewById(R.id.buttonCheckStatus).setOnClickListener(v -> checkPaymentStatus());
    }

    private void makeBankingPayment() {
        if (!hasPaymentService()) {
            return;
        }
        if (appointmentDocumentId.isEmpty()) {
            Toast.makeText(this, R.string.payment_missing_appointment, Toast.LENGTH_LONG).show();
            return;
        }

        View paidButton = findViewById(R.id.buttonIHavePaid);
        paidButton.setEnabled(false);
        paidButton.setAlpha(0.5f);
        ((TextView) paidButton).setText(R.string.payment_recording);

        makePaymentService.makePayment(
                appointmentDocumentId,
                totalPrice,
                MakePaymentService.METHOD_BANKING,
                new MakePaymentService.PaymentCallback<MakePaymentService.PaymentResult>() {
                    @Override
                    public void onSuccess(MakePaymentService.PaymentResult data) {
                        ((TextView) paidButton).setText(R.string.payment_demo_confirmed_action);
                        Toast.makeText(PaymentActivity.this, R.string.payment_banking_recorded, Toast.LENGTH_SHORT).show();
                        openPaidAppointmentDetail(data);
                    }

                    @Override
                    public void onError(Exception exception) {
                        paidButton.setEnabled(true);
                        paidButton.setAlpha(1.0f);
                        ((TextView) paidButton).setText(R.string.payment_action_paid);
                        Toast.makeText(PaymentActivity.this, errorMessage(exception), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void openPaidAppointmentDetail(MakePaymentService.PaymentResult paymentResult) {
        if (returnToAppointmentsAfterPayment) {
            Intent intent = new Intent(this, AppointmentActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        Intent intent = new Intent(this, AppointmentDetailActivity.class);
        intent.putExtra("appointmentId", appointmentDocumentId);
        intent.putExtra("appointmentStatus", getString(R.string.appointment_status_upcoming));
        intent.putExtra("barberName", barberName);
        intent.putExtra("barberExperience", barberExperience);
        intent.putExtra("barberSpecialty", barberSpecialty);
        intent.putExtra("serviceName", serviceName);
        intent.putExtra("appointmentPrice", amount);
        intent.putExtra("appointmentDate", dateLabel);
        intent.putExtra("appointmentStartTime", startTime);
        intent.putExtra("appointmentEndTime", endTime);
        intent.putExtra("appointmentDuration", String.format(Locale.US, "%d min", durationMinutes));
        intent.putExtra("paymentStatus", displayPaymentStatus(paymentResult.getStatus()));
        intent.putExtra("paymentMethod", paymentResult.getMethod());
        intent.putExtra("appointmentNote", appointmentNote);
        intent.putExtra("appointmentCreatedAt", appointmentCreatedAt);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void checkPaymentStatus() {
        if (!hasPaymentService()) {
            return;
        }
        if (appointmentDocumentId.isEmpty()) {
            Toast.makeText(this, R.string.payment_missing_appointment, Toast.LENGTH_LONG).show();
            return;
        }

        makePaymentService.getPaymentStatus(
                appointmentDocumentId,
                new MakePaymentService.PaymentCallback<String>() {
                    @Override
                    public void onSuccess(String status) {
                        Toast.makeText(
                                PaymentActivity.this,
                                getString(R.string.payment_status_format, displayPaymentStatus(status)),
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onError(Exception exception) {
                        Toast.makeText(PaymentActivity.this, errorMessage(exception), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private boolean hasPaymentService() {
        if (!paymentServiceBound || makePaymentService == null) {
            Toast.makeText(this, R.string.payment_service_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void startCountdown() {
        paymentCountdownTimer = new CountDownTimer(PAYMENT_WINDOW_MILLIS, ONE_SECOND_MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / ONE_SECOND_MILLIS;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                String timeLeft = String.format(Locale.US, "%02d:%02d", minutes, seconds);
                ((TextView) findViewById(R.id.textPaymentCountdown)).setText(
                        getString(R.string.payment_countdown_format, timeLeft)
                );
            }

            @Override
            public void onFinish() {
                ((TextView) findViewById(R.id.textPaymentCountdown)).setText(
                        getString(R.string.payment_countdown_format, "00:00")
                );
            }
        };
        paymentCountdownTimer.start();
    }

    private void copyTransferContent() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(
                    getString(R.string.payment_transfer_content),
                    transferContent
            ));
            Toast.makeText(this, R.string.payment_copy_success, Toast.LENGTH_SHORT).show();
        }
    }

    private String readStringExtra(Intent intent, String key, String fallback) {
        String value = intent.getStringExtra(key);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String getInitial(String value) {
        return value == null || value.trim().isEmpty()
                ? "A"
                : value.trim().substring(0, 1).toUpperCase(Locale.US);
    }

    private String rowText(String label, String value) {
        return String.format(Locale.US, "%s  %s", label, value);
    }

    private double parseAmount(String value) {
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private String displayPaymentStatus(String value) {
        if ("PAID".equalsIgnoreCase(value)) {
            return getString(R.string.appointment_payment_paid);
        } else if ("REFUNDED".equalsIgnoreCase(value)) {
            return getString(R.string.appointment_payment_refunded);
        }
        return getString(R.string.appointment_payment_not_paid);
    }

    private String errorMessage(Exception exception) {
        return exception == null || exception.getMessage() == null
                ? getString(R.string.state_error_placeholder)
                : exception.getMessage();
    }
}
