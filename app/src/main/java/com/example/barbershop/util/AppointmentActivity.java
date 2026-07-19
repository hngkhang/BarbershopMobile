package com.example.barbershop.util;

import com.example.barbershop.R;

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

import com.example.barbershop.adapters.AppointmentAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppointmentActivity extends AppCompatActivity {

    private String selectedStatus = AppointmentAdapter.AppointmentItem.STATUS_UPCOMING;
    private AppointmentAdapter appointmentAdapter;
    private View emptyState;
    private View loadingState;
    private View errorState;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private final List<AppointmentAdapter.AppointmentItem> allAppointments = new ArrayList<>();
    private final List<TextView> statusChips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        setupRecyclerView();
        setupStatusChips();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerAppointments);
        emptyState = findViewById(R.id.layoutAppointmentsEmpty);
        loadingState = findViewById(R.id.layoutAppointmentsLoading);
        errorState = findViewById(R.id.layoutAppointmentsError);

        appointmentAdapter = new AppointmentAdapter(this::openAppointmentDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(appointmentAdapter);
        errorState.setOnClickListener(v -> loadAppointments());
    }

    private void setupStatusChips() {
        addStatusChip(R.id.chipUpcoming, AppointmentAdapter.AppointmentItem.STATUS_UPCOMING);
        // The current Firestore schema does not contain a PENDING state.
        findViewById(R.id.chipPending).setVisibility(View.GONE);
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

    private void loadAppointments() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            allAppointments.clear();
            appointmentAdapter.submitList(new ArrayList<>());
            showError(getString(R.string.appointment_login_required));
            return;
        }

        showLoading(true);
        firestore.collection("appointments")
                .whereEqualTo("userUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> loadLookupData(snapshot.getDocuments()))
                .addOnFailureListener(this::showError);
    }

    private void loadLookupData(List<DocumentSnapshot> appointmentDocuments) {
        firestore.collection("barbers")
                .get()
                .addOnSuccessListener(barberSnapshot -> {
                    Map<String, BarberInfo> barbersById = new HashMap<>();
                    for (DocumentSnapshot document : barberSnapshot.getDocuments()) {
                        String barberId = normalizedId(document.get("barberId"));
                        if (!barberId.isEmpty()) {
                            barbersById.put(barberId, new BarberInfo(
                                    readString(document, "name"),
                                    readString(document, "experience")
                            ));
                        }
                    }
                    loadServices(appointmentDocuments, barbersById);
                })
                .addOnFailureListener(this::showError);
    }

    private void loadServices(
            List<DocumentSnapshot> appointmentDocuments,
            Map<String, BarberInfo> barbersById
    ) {
        firestore.collection("services")
                .get()
                .addOnSuccessListener(serviceSnapshot -> {
                    Map<String, ServiceInfo> servicesById = new HashMap<>();
                    for (DocumentSnapshot document : serviceSnapshot.getDocuments()) {
                        String serviceId = normalizedId(document.get("serviceId"));
                        if (!serviceId.isEmpty()) {
                            servicesById.put(serviceId, new ServiceInfo(
                                    readString(document, "name"),
                                    readNumber(document, "price")
                            ));
                        }
                    }
                    bindAppointments(appointmentDocuments, barbersById, servicesById);
                })
                .addOnFailureListener(this::showError);
    }

    private void bindAppointments(
            List<DocumentSnapshot> appointmentDocuments,
            Map<String, BarberInfo> barbersById,
            Map<String, ServiceInfo> servicesById
    ) {
        List<AppointmentAdapter.AppointmentItem> loadedAppointments = new ArrayList<>();
        List<DocumentSnapshot> sortedDocuments = new ArrayList<>(appointmentDocuments);
        Collections.sort(sortedDocuments, (left, right) -> {
            Timestamp leftStartAt = left.getTimestamp("startAt");
            Timestamp rightStartAt = right.getTimestamp("startAt");
            if (leftStartAt == null) {
                return rightStartAt == null ? 0 : 1;
            }
            if (rightStartAt == null) {
                return -1;
            }
            return rightStartAt.compareTo(leftStartAt);
        });

        for (DocumentSnapshot document : sortedDocuments) {
            Timestamp startAt = document.getTimestamp("startAt");
            Timestamp endAt = document.getTimestamp("endAt");
            if (startAt == null || endAt == null) {
                continue;
            }

            BarberInfo barber = barbersById.get(normalizedId(document.get("barberId")));
            ServiceInfo service = servicesById.get(normalizedId(document.get("serviceId")));
            loadedAppointments.add(new AppointmentAdapter.AppointmentItem(
                    document.getId(),
                    formatTimestamp(startAt, "EEE"),
                    formatTimestamp(startAt, "MMM d"),
                    formatTimestamp(startAt, "EEE, MMM d, yyyy"),
                    formatTimestamp(startAt, "h:mm a"),
                    formatTimestamp(endAt, "h:mm a"),
                    durationLabel(startAt, endAt),
                    barber == null ? "" : barber.name,
                    barber == null ? getString(R.string.barber_experience_not_updated) : barber.experience,
                    getString(R.string.barber_specialty_not_available),
                    service == null ? "" : service.name,
                    service == null ? "" : formatPrice(service.price),
                    displayStatus(readString(document, "status")),
                    displayPaymentStatus(readString(document, "paymentStatus")),
                    numberValue(document.get("paymentId")) > 0L ? "banking" : "",
                    readString(document, "note"),
                    formatTimestamp(document.getTimestamp("createdAt"), "MMM d, yyyy - h:mm a")
            ));
        }

        allAppointments.clear();
        allAppointments.addAll(loadedAppointments);
        applyStatusFilter();
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

    private String displayStatus(String firebaseStatus) {
        String normalizedStatus = firebaseStatus.trim().toUpperCase(Locale.US);
        if ("COMPLETED".equals(normalizedStatus)) {
            return AppointmentAdapter.AppointmentItem.STATUS_COMPLETED;
        } else if ("CANCELLED".equals(normalizedStatus)) {
            return AppointmentAdapter.AppointmentItem.STATUS_CANCELLED;
        }
        return AppointmentAdapter.AppointmentItem.STATUS_UPCOMING;
    }

    private String displayPaymentStatus(String firebaseStatus) {
        String normalizedStatus = firebaseStatus.trim().toUpperCase(Locale.US);
        if ("PAID".equals(normalizedStatus)) {
            return getString(R.string.appointment_payment_paid);
        } else if ("REFUNDED".equals(normalizedStatus)) {
            return getString(R.string.appointment_payment_refunded);
        }
        return getString(R.string.appointment_payment_not_paid);
    }

    private String durationLabel(Timestamp startAt, Timestamp endAt) {
        long durationMinutes = Math.max(0L, (endAt.toDate().getTime() - startAt.toDate().getTime()) / 60000L);
        return String.format(Locale.US, "%d min", durationMinutes);
    }

    private String formatTimestamp(Timestamp timestamp, String pattern) {
        if (timestamp == null) {
            return "";
        }
        return new SimpleDateFormat(pattern, Locale.US).format(timestamp.toDate());
    }

    private String formatPrice(Double price) {
        return price == null ? "" : String.format(Locale.US, "$%.2f", price);
    }

    private String normalizedId(Object value) {
        if (value instanceof Number) {
            return String.valueOf(((Number) value).longValue());
        }
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String readString(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Double readNumber(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private long numberValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
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

    private void showError(Exception exception) {
        String message = exception == null || exception.getMessage() == null
                ? getString(R.string.state_error_placeholder)
                : exception.getMessage();
        showError(message);
    }

    private void showError(String message) {
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        if (errorState instanceof TextView) {
            ((TextView) errorState).setText(message);
        }
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
        intent.putExtra("appointmentCreatedAt", appointmentItem.createdAt);
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        findViewById(R.id.appointmentNavHome).setOnClickListener(v -> launchActivityIfAvailable("HomeActivity"));
        findViewById(R.id.appointmentNavServices).setOnClickListener(v -> launchActivityIfAvailable("ServiceListActivity"));
        findViewById(R.id.appointmentNavAi).setOnClickListener(v -> launchActivityIfAvailable("AIChatBookingActivity"));
        findViewById(R.id.appointmentNavAppointments).setOnClickListener(v -> {
            RecyclerView recyclerView = findViewById(R.id.recyclerAppointments);
            recyclerView.smoothScrollToPosition(0);
        });
        findViewById(R.id.appointmentNavProfile).setOnClickListener(v -> launchActivityIfAvailable("ProfileActivity"));
    }

    private boolean launchActivityIfAvailable(String simpleClassName) {
        try {
            Class<?> targetClass = Class.forName(getClass().getPackage().getName() + "." + simpleClassName);
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

    private static class BarberInfo {
        final String name;
        final String experience;

        BarberInfo(String name, String experience) {
            this.name = name;
            this.experience = experience;
        }
    }

    private static class ServiceInfo {
        final String name;
        final Double price;

        ServiceInfo(String name, Double price) {
            this.name = name;
            this.price = price;
        }
    }
}
