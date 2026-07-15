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
import com.example.barbershop.data.AppointmentRepository;
import com.example.barbershop.data.BarberRepository;
import com.example.barbershop.data.ServiceRepository;
import com.example.barbershop.models.Barber;
import com.example.barbershop.models.ShopService;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class AppointmentActivity extends AppCompatActivity {

    private String selectedStatus = AppointmentAdapter.AppointmentItem.STATUS_UPCOMING;
    private AppointmentAdapter appointmentAdapter;
    private View emptyState;
    private View loadingState;
    private View errorState;
    private final List<AppointmentAdapter.AppointmentItem> allAppointments = new ArrayList<>();
    private final List<TextView> statusChips = new ArrayList<>();
    private final AppointmentRepository appointmentRepository = new AppointmentRepository();
    private final BarberRepository barberRepository = new BarberRepository();
    private final ServiceRepository serviceRepository = new ServiceRepository();
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US);
    private final SimpleDateFormat shortDateFormat = new SimpleDateFormat("MMM d", Locale.US);
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        setupTopBar();
        setupRecyclerView();
        setupStatusChips();
        setupBottomNavigation();

        loadAppointmentsForCurrentUser();
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

    private void loadAppointmentsForCurrentUser() {
        showLoading(true);
        appointmentRepository.getCurrentUserAppointments(new BarberRepository.RepositoryCallback<List<AppointmentRepository.AppointmentRecord>>() {
            @Override
            public void onSuccess(List<AppointmentRepository.AppointmentRecord> appointments) {
                loadLookupData(appointments);
            }

            @Override
            public void onError(Exception exception) {
                showError(exception);
            }
        });
    }

    private void loadLookupData(List<AppointmentRepository.AppointmentRecord> appointments) {
        barberRepository.getAllBarbersForAppointmentLookup(new BarberRepository.RepositoryCallback<List<Barber>>() {
            @Override
            public void onSuccess(List<Barber> barbers) {
                serviceRepository.getAllServices(new BarberRepository.RepositoryCallback<List<ShopService>>() {
                    @Override
                    public void onSuccess(List<ShopService> services) {
                        bindFirebaseAppointments(appointments, barbers, services);
                    }

                    @Override
                    public void onError(Exception exception) {
                        showError(exception);
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                showError(exception);
            }
        });
    }

    private void bindFirebaseAppointments(
            List<AppointmentRepository.AppointmentRecord> appointments,
            List<Barber> barbers,
            List<ShopService> services
    ) {
        Map<String, Barber> barberById = buildBarberLookup(barbers);
        Map<String, ShopService> serviceById = buildServiceLookup(services);

        allAppointments.clear();
        for (AppointmentRepository.AppointmentRecord appointment : appointments) {
            allAppointments.add(toAppointmentItem(appointment, barberById, serviceById));
        }
        applyStatusFilter();
    }

    private Map<String, Barber> buildBarberLookup(List<Barber> barbers) {
        Map<String, Barber> barberById = new HashMap<>();
        for (Barber barber : barbers) {
            for (String lookupId : barber.getLookupIds()) {
                barberById.put(lookupId, barber);
            }
        }
        return barberById;
    }

    private Map<String, ShopService> buildServiceLookup(List<ShopService> services) {
        Map<String, ShopService> serviceById = new HashMap<>();
        for (ShopService service : services) {
            putIfPresent(serviceById, service.getId(), service);
            putIfPresent(serviceById, service.getServiceId(), service);
        }
        return serviceById;
    }

    private void putIfPresent(Map<String, ShopService> serviceById, String key, ShopService service) {
        String normalizedKey = normalizeId(key);
        if (!normalizedKey.isEmpty()) {
            serviceById.put(normalizedKey, service);
        }
    }

    private AppointmentAdapter.AppointmentItem toAppointmentItem(
            AppointmentRepository.AppointmentRecord appointment,
            Map<String, Barber> barberById,
            Map<String, ShopService> serviceById
    ) {
        Date startDate = toDate(appointment.getStartAt());
        Date endDate = toDate(appointment.getEndAt());
        Barber barber = barberById.get(normalizeId(appointment.getBarberId()));
        ShopService service = serviceById.get(normalizeId(appointment.getServiceId()));
        String serviceName = service == null
                ? fallback("Service #" + appointment.getServiceId(), "Service")
                : fallback(service.getName(), "Service #" + appointment.getServiceId());
        String barberName = barber == null
                ? fallback("Barber #" + appointment.getBarberId(), "Barber")
                : fallback(barber.getName(), "Barber #" + appointment.getBarberId());
        String barberExperience = barber == null
                ? getString(R.string.barber_experience_not_updated)
                : fallback(barber.getExperience(), getString(R.string.barber_experience_not_updated));
        String serviceCategory = service == null
                ? getString(R.string.barber_specialty_not_available)
                : "Specialty: " + fallback(service.getCategory(), serviceName);

        return new AppointmentAdapter.AppointmentItem(
                appointment.getDisplayId(),
                formatDate(startDate, dayFormat, "--"),
                formatDate(startDate, shortDateFormat, "--"),
                formatDate(startDate, fullDateFormat, "Not scheduled"),
                formatDate(startDate, timeFormat, "--"),
                formatDate(endDate, timeFormat, "--"),
                formatDuration(startDate, endDate, service),
                barberName,
                barberExperience,
                serviceCategory,
                serviceName,
                service == null ? "$0.00" : formatPrice(service.getPrice()),
                resolveStatus(startDate, endDate),
                getString(R.string.appointment_payment_not_paid),
                getString(R.string.appointment_payment_not_paid),
                buildAppointmentNote(appointment.getCreatedAt()),
                formatDate(toDate(appointment.getCreatedAt()), dateTimeFormat, "")
        );
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
        if (loading) {
            appointmentAdapter.submitList(new ArrayList<>());
        }
    }

    private void showContentState(boolean empty) {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showError(Exception exception) {
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        appointmentAdapter.submitList(new ArrayList<>());
        Toast.makeText(this, safeErrorMessage(exception), Toast.LENGTH_LONG).show();
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
        intent.putExtra("appointmentCreatedAt", appointmentItem.createdAtText);
        startActivity(intent);
    }

    private String resolveStatus(Date startDate, Date endDate) {
        Date now = new Date();
        if (startDate == null || endDate == null) {
            return AppointmentAdapter.AppointmentItem.STATUS_PENDING;
        }
        if (endDate.before(now)) {
            return AppointmentAdapter.AppointmentItem.STATUS_COMPLETED;
        }
        return AppointmentAdapter.AppointmentItem.STATUS_UPCOMING;
    }

    private String formatDuration(Date startDate, Date endDate, ShopService service) {
        if (startDate != null && endDate != null && endDate.after(startDate)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(endDate.getTime() - startDate.getTime());
            return String.format(Locale.US, "%d min", minutes);
        }
        if (service != null && service.getTimeMinutes() > 0) {
            return String.format(Locale.US, "%d min", service.getTimeMinutes());
        }
        return "--";
    }

    private String buildAppointmentNote(Timestamp createdAt) {
        Date createdDate = toDate(createdAt);
        if (createdDate == null) {
            return getString(R.string.booking_no_note);
        }
        return "Booked at " + dateTimeFormat.format(createdDate);
    }

    private Date toDate(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toDate();
    }

    private String formatDate(Date date, SimpleDateFormat formatter, String fallback) {
        return date == null ? fallback : formatter.format(date);
    }

    private String formatPrice(double price) {
        return String.format(Locale.US, "$%.2f", price);
    }

    private String normalizeId(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.endsWith(".0")
                ? normalized.substring(0, normalized.length() - 2)
                : normalized;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String safeErrorMessage(Exception exception) {
        if (exception == null
                || exception.getMessage() == null
                || exception.getMessage().trim().isEmpty()) {
            return getString(R.string.state_error_placeholder);
        }
        return exception.getMessage();
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
