package com.example.barbershop.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MakePaymentService extends Service {

    private static final String TAG = "MakePaymentService";
    public static final String METHOD_BANKING = "banking";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_UNPAID = "UNPAID";

    private final IBinder binder = new MakePaymentBinder();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    public class MakePaymentBinder extends Binder {
        public MakePaymentService getService() {
            return MakePaymentService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: payment bound service created");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: payment client bound");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: all payment clients unbound");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: payment bound service destroyed");
        super.onDestroy();
    }

    public void makePayment(
            String appointmentDocumentId,
            double amount,
            String method,
            PaymentCallback<PaymentResult> callback
    ) {
        if (!METHOD_BANKING.equalsIgnoreCase(safeString(method))) {
            callback.onError(new IllegalArgumentException("Only banking payment is supported right now."));
            return;
        }
        if (appointmentDocumentId == null || appointmentDocumentId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Appointment is missing."));
            return;
        }
        if (amount < 0.0) {
            callback.onError(new IllegalArgumentException("Payment amount is invalid."));
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new IllegalStateException("Please sign in before making a payment."));
            return;
        }

        DocumentReference appointmentReference = firestore.collection("appointments")
                .document(appointmentDocumentId.trim());
        long paymentId = System.currentTimeMillis();
        DocumentReference paymentReference = firestore.collection("payments")
                .document(String.valueOf(paymentId));

        firestore.runTransaction(transaction -> {
                    Map<String, Object> appointment = transaction.get(appointmentReference).getData();
                    if (appointment == null) {
                        throw new IllegalStateException("Appointment does not exist.");
                    }

                    String appointmentUserUid = safeString(appointment.get("userUid"));
                    if (!currentUser.getUid().equals(appointmentUserUid)) {
                        throw new IllegalStateException("This appointment does not belong to the current user.");
                    }

                    String paymentStatus = safeString(appointment.get("paymentStatus"));
                    if (STATUS_PAID.equalsIgnoreCase(paymentStatus)) {
                        throw new IllegalStateException("Payment has already been recorded.");
                    }

                    long appointmentId = numberValue(appointment.get("appointmentId"));
                    if (appointmentId <= 0L) {
                        appointmentId = paymentId;
                    }

                    Map<String, Object> payment = new HashMap<>();
                    payment.put("paymentId", paymentId);
                    payment.put("appointmentId", appointmentId);
                    payment.put("userId", currentUser.getUid());
                    payment.put("amount", amount);
                    payment.put("method", METHOD_BANKING);
                    payment.put("status", STATUS_PAID);
                    payment.put("transactionCode", transactionCode(paymentId));
                    payment.put("createdAt", FieldValue.serverTimestamp());
                    payment.put("paidAt", FieldValue.serverTimestamp());
                    payment.put("expiredAt", null);

                    Map<String, Object> appointmentUpdates = new HashMap<>();
                    appointmentUpdates.put("paymentId", paymentId);
                    appointmentUpdates.put("paymentStatus", STATUS_PAID);
                    if (numberValue(appointment.get("appointmentId")) <= 0L) {
                        appointmentUpdates.put("appointmentId", appointmentId);
                    }

                    transaction.set(paymentReference, payment);
                    transaction.update(appointmentReference, appointmentUpdates);
                    return new PaymentResult(paymentId, appointmentId, STATUS_PAID, METHOD_BANKING);
                })
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }

    public void getPaymentStatus(String appointmentDocumentId, PaymentCallback<String> callback) {
        if (appointmentDocumentId == null || appointmentDocumentId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Appointment is missing."));
            return;
        }

        firestore.collection("appointments")
                .document(appointmentDocumentId.trim())
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError(new IllegalStateException("Appointment does not exist."));
                        return;
                    }
                    String status = safeString(document.get("paymentStatus"));
                    callback.onSuccess(status.isEmpty() ? STATUS_UNPAID : status);
                })
                .addOnFailureListener(callback::onError);
    }

    private static String safeString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static long numberValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static String transactionCode(long paymentId) {
        return String.format(Locale.US, "BANKING-%d", paymentId);
    }

    public interface PaymentCallback<T> {
        void onSuccess(T data);
        void onError(Exception exception);
    }

    public static class PaymentResult {
        private final long paymentId;
        private final long appointmentId;
        private final String status;
        private final String method;

        PaymentResult(long paymentId, long appointmentId, String status, String method) {
            this.paymentId = paymentId;
            this.appointmentId = appointmentId;
            this.status = status;
            this.method = method;
        }

        public long getPaymentId() {
            return paymentId;
        }

        public long getAppointmentId() {
            return appointmentId;
        }

        public String getStatus() {
            return status;
        }

        public String getMethod() {
            return method;
        }
    }
}
