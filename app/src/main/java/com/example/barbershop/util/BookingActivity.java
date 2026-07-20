package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.barbershop.data.AppointmentRepository;
import com.example.barbershop.data.BarberRepository;
import com.example.barbershop.data.ServiceRepository;
import com.example.barbershop.models.Appointment;
import com.example.barbershop.models.BarberSchedule;
import com.example.barbershop.models.ShopService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    public static final String EXTRA_SERVICE_SELECTION_MODE = "serviceSelectionMode";
    public static final String EXTRA_BARBER_SELECTION_MODE = "barberSelectionMode";
    public static final String EXTRA_SERVICE_ID = "serviceId";
    public static final String EXTRA_BARBER_ID = "barberId";
    public static final String EXTRA_BARBER_NAME = "barberName";
    public static final String EXTRA_BARBER_EXPERIENCE = "barberExperience";
    public static final String EXTRA_START_AT_MILLIS = "startAtMillis";
    public static final String EXTRA_END_AT_MILLIS = "endAtMillis";
    public static final String EXTRA_AI_BOOKING = "aiBooking";

    private static final int REQUEST_SELECT_SERVICE = 1001;
    private static final int REQUEST_SELECT_BARBER = 1002;

    private final List<DateOption> dateOptions = new ArrayList<>();
    private final List<BarberSchedule> barberSchedules = new ArrayList<>();
    private final List<Appointment> barberAppointments = new ArrayList<>();

    private ServiceRepository serviceRepository;
    private BarberRepository barberRepository;
    private AppointmentRepository appointmentRepository;
    private ShopService selectedService;
    private long selectedBarberId = -1L;
    private String selectedBarberName = "";
    private String selectedBarberExperience = "";
    private DateOption selectedDateOption;
    private TimeSlot selectedTimeSlot;
    private long suggestedStartAtMillis = -1L;
    private long suggestedEndAtMillis = -1L;

    private TextView textSelectedServiceName;
    private TextView textSelectedServicePrice;
    private TextView textSelectedBarberInitial;
    private TextView textSelectedBarberName;
    private TextView textTotalDuration;
    private TextView textTotalPrice;
    private TextView textNoteCounter;
    private TextInputEditText editTextBookingNote;
    private LinearLayout dateSelectorRow;
    private GridLayout timeSlotGrid;
    private View buttonContinue;
    private boolean availabilityLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        serviceRepository = new ServiceRepository(this);
        barberRepository = new BarberRepository(this);
        appointmentRepository = new AppointmentRepository(this);

        bindViews();
        setupTopBar();
        setupServiceSection();
        setupBarberSection();
        setupNoteInput();
        setupSummary();
        readBookingSelection();
        refreshBookingUi();
    }

    private void bindViews() {
        textSelectedServiceName = findViewById(R.id.textSelectedServiceName);
        textSelectedServicePrice = findViewById(R.id.textSelectedServicePrice);
        textSelectedBarberInitial = findViewById(R.id.textSelectedBarberInitial);
        textSelectedBarberName = findViewById(R.id.textSelectedBarberName);
        textTotalDuration = findViewById(R.id.textTotalDuration);
        textTotalPrice = findViewById(R.id.textTotalPrice);
        textNoteCounter = findViewById(R.id.textNoteCounter);
        editTextBookingNote = findViewById(R.id.editTextBookingNote);
        dateSelectorRow = findViewById(R.id.dateSelectorRow);
        timeSlotGrid = findViewById(R.id.timeSlotGrid);
        buttonContinue = findViewById(R.id.buttonContinue);
        findViewById(R.id.addonServiceCard).setVisibility(View.GONE);
    }

    private void readBookingSelection() {
        long serviceId = getIntent().getLongExtra(EXTRA_SERVICE_ID, -1L);
        long barberId = getIntent().getLongExtra(EXTRA_BARBER_ID, -1L);
        suggestedStartAtMillis = getIntent().getLongExtra(EXTRA_START_AT_MILLIS, -1L);
        suggestedEndAtMillis = getIntent().getLongExtra(EXTRA_END_AT_MILLIS, -1L);
        if (serviceId > 0L) {
            loadSelectedService(serviceId);
        }
        if (barberId > 0L) {
            loadSelectedBarber(barberId);
        }
    }

    private void setupTopBar() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }

    private void setupServiceSection() {
    }

    private void setupBarberSection() {
        findViewById(R.id.buttonChangeBarber).setOnClickListener(v -> {
            Intent intent = new Intent(this, BarberListActivity.class);
            intent.putExtra(EXTRA_BARBER_SELECTION_MODE, true);
            startActivityForResult(intent, REQUEST_SELECT_BARBER);
        });
    }

    private void setupNoteInput() {
        editTextBookingNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textNoteCounter.setText(String.format(Locale.US, "%d/200", s.length()));
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        textNoteCounter.setText(String.format(Locale.US, "%d/200", 0));
    }

    private void setupSummary() {
        buttonContinue.setOnClickListener(v -> openBookingConfirmation());
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        if (requestCode == REQUEST_SELECT_SERVICE) {
            clearSuggestedSlot();
            long serviceId = data.getLongExtra(EXTRA_SERVICE_ID, -1L);
            if (serviceId > 0L) {
                loadSelectedService(serviceId);
            }
        } else if (requestCode == REQUEST_SELECT_BARBER) {
            clearSuggestedSlot();
            long barberId = data.getLongExtra(EXTRA_BARBER_ID, -1L);
            if (barberId > 0L) {
                selectedBarberId = barberId;
                selectedBarberName = valueOrEmpty(data.getStringExtra(EXTRA_BARBER_NAME));
                selectedBarberExperience = valueOrEmpty(data.getStringExtra(EXTRA_BARBER_EXPERIENCE));
                selectedDateOption = null;
                selectedTimeSlot = null;
                loadBarberAvailability();
                refreshBookingUi();
            }
        }
    }

    private void loadSelectedService(long serviceId) {
        serviceRepository.getServiceById(serviceId, new BarberRepository.RepositoryCallback<ShopService>() {
            @Override
            public void onSuccess(ShopService service) {
                if (service == null) {
                    Toast.makeText(BookingActivity.this, R.string.state_error_placeholder, Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedService = service;
                selectedDateOption = null;
                selectedTimeSlot = null;
                if (selectedBarberId > 0L) {
                    rebuildAvailableDates();
                }
                refreshBookingUi();
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(BookingActivity.this, R.string.state_error_placeholder, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSelectedBarber(long barberId) {
        barberRepository.getAllBarbers(new BarberRepository.RepositoryCallback<List<com.example.barbershop.models.Barber>>() {
            @Override
            public void onSuccess(List<com.example.barbershop.models.Barber> barbers) {
                for (com.example.barbershop.models.Barber barber : barbers) {
                    try {
                        if (Long.parseLong(barber.getBarberId()) == barberId) {
                            selectedBarberId = barberId;
                            selectedBarberName = barber.getName();
                            selectedBarberExperience = barber.getExperience();
                            loadBarberAvailability();
                            refreshBookingUi();
                            return;
                        }
                    } catch (NumberFormatException ignored) {
                        // Ignore a malformed barberId document and continue searching.
                    }
                }
                Toast.makeText(BookingActivity.this, R.string.state_error_placeholder, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(BookingActivity.this, R.string.state_error_placeholder, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBarberAvailability() {
        barberSchedules.clear();
        barberAppointments.clear();
        availabilityLoading = true;
        refreshBookingUi();
        barberRepository.getBarberSchedule(selectedBarberId, new BarberRepository.RepositoryCallback<List<BarberSchedule>>() {
            @Override
            public void onSuccess(List<BarberSchedule> schedules) {
                barberSchedules.addAll(schedules);
                appointmentRepository.getAppointmentsForBarber(
                        selectedBarberId,
                        new AppointmentRepository.RepositoryCallback<List<Appointment>>() {
                            @Override
                            public void onSuccess(List<Appointment> appointments) {
                                barberAppointments.addAll(appointments);
                                availabilityLoading = false;
                                rebuildAvailableDates();
                                applySuggestedSlotIfStillAvailable();
                                refreshBookingUi();
                            }

                            @Override
                            public void onError(Exception exception) {
                                showAvailabilityError();
                            }
                        }
                );
            }

            @Override
            public void onError(Exception exception) {
                showAvailabilityError();
            }
        });
    }

    private void showAvailabilityError() {
        availabilityLoading = false;
        dateOptions.clear();
        selectedDateOption = null;
        selectedTimeSlot = null;
        refreshBookingUi();
        Toast.makeText(this, R.string.state_error_placeholder, Toast.LENGTH_SHORT).show();
    }

    private void rebuildAvailableDates() {
        dateOptions.clear();
        selectedDateOption = null;
        selectedTimeSlot = null;
        if (selectedService == null || selectedService.getTimeMinutes() <= 0) {
            return;
        }

        List<BarberSchedule> sortedSchedules = new ArrayList<>(barberSchedules);
        Collections.sort(sortedSchedules, (left, right) -> left.getStartAt().compareTo(right.getStartAt()));
        for (BarberSchedule schedule : sortedSchedules) {
            DateOption option = new DateOption(startOfDay(schedule.getStartAt().toDate()));
            if (!containsDate(option) && !calculateSlotsForDate(option).isEmpty()) {
                dateOptions.add(option);
            }
        }
    }

    private void applySuggestedSlotIfStillAvailable() {
        if (suggestedStartAtMillis <= 0L || suggestedEndAtMillis <= 0L) {
            return;
        }
        for (DateOption option : dateOptions) {
            if (!sameDay(option.dayStart.getTime(), suggestedStartAtMillis)) {
                continue;
            }
            for (TimeSlot slot : calculateSlotsForDate(option)) {
                if (slot.startAt == suggestedStartAtMillis && slot.endAt == suggestedEndAtMillis) {
                    selectedDateOption = option;
                    selectedTimeSlot = slot;
                    return;
                }
            }
        }
        clearSuggestedSlot();
    }

    private void clearSuggestedSlot() {
        suggestedStartAtMillis = -1L;
        suggestedEndAtMillis = -1L;
    }

    private boolean containsDate(DateOption target) {
        for (DateOption option : dateOptions) {
            if (option.dayStart.getTime() == target.dayStart.getTime()) {
                return true;
            }
        }
        return false;
    }

    private List<TimeSlot> calculateSlotsForDate(DateOption option) {
        List<TimeSlot> slots = new ArrayList<>();
        if (selectedService == null || selectedService.getTimeMinutes() <= 0) {
            return slots;
        }

        long now = System.currentTimeMillis();
        for (BarberSchedule schedule : barberSchedules) {
            long scheduleStart = schedule.getStartAt().toDate().getTime();
            long scheduleEnd = schedule.getEndAt().toDate().getTime();
            if (!sameDay(scheduleStart, option.dayStart.getTime())) {
                continue;
            }

            List<Appointment> busyAppointments = appointmentsOverlapping(scheduleStart, scheduleEnd);
            long freeStart = Math.max(scheduleStart, now);
            for (Appointment appointment : busyAppointments) {
                long busyStart = appointment.getStartAt().toDate().getTime();
                long busyEnd = appointment.getEndAt().toDate().getTime();
                if (busyEnd <= freeStart) {
                    continue;
                }
                if (busyStart > freeStart) {
                    appendSlots(slots, freeStart, Math.min(busyStart, scheduleEnd));
                }
                freeStart = Math.max(freeStart, busyEnd);
                if (freeStart >= scheduleEnd) {
                    break;
                }
            }
            if (freeStart < scheduleEnd) {
                appendSlots(slots, freeStart, scheduleEnd);
            }
        }
        return slots;
    }

    private List<Appointment> appointmentsOverlapping(long start, long end) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment appointment : barberAppointments) {
            long appointmentStart = appointment.getStartAt().toDate().getTime();
            long appointmentEnd = appointment.getEndAt().toDate().getTime();
            if (appointmentStart < end && appointmentEnd > start) {
                result.add(appointment);
            }
        }
        Collections.sort(result, (left, right) -> left.getStartAt().compareTo(right.getStartAt()));
        return result;
    }

    private void appendSlots(List<TimeSlot> slots, long freeStart, long freeEnd) {
        long durationMillis = selectedService.getTimeMinutes() * 60_000L;
        long cursor = freeStart;
        while (cursor + durationMillis <= freeEnd) {
            slots.add(new TimeSlot(cursor, cursor + durationMillis));
            cursor += durationMillis;
        }
    }

    private void refreshBookingUi() {
        bindServiceUi();
        bindBarberUi();
        bindDateUi();
        bindTimeUi();
        bindAvailabilityMessages();
        bindSummaryUi();
    }

    private void bindServiceUi() {
        if (selectedService == null) {
            textSelectedServiceName.setText(R.string.booking_add_more);
            textSelectedServicePrice.setText("");
            return;
        }
        textSelectedServiceName.setText(selectedService.getName());
        if (selectedService.getTimeMinutes() > 0) {
            textSelectedServicePrice.setText(getString(
                    R.string.booking_service_meta_format,
                    formatPrice(selectedService.getPrice()),
                    String.format(Locale.US, "%d min", selectedService.getTimeMinutes())
            ));
        } else {
            textSelectedServicePrice.setText(getString(
                    R.string.booking_service_meta_format,
                    formatPrice(selectedService.getPrice()),
                    getString(R.string.booking_duration_unavailable)
            ));
        }
    }

    private void bindBarberUi() {
        textSelectedBarberInitial.setText(initial(selectedBarberName));
        textSelectedBarberInitial.setContentDescription(
                getString(R.string.booking_barber_avatar_content_description, selectedBarberName)
        );
        textSelectedBarberName.setText(selectedBarberName);
    }

    private void bindDateUi() {
        dateSelectorRow.removeAllViews();
        for (DateOption option : dateOptions) {
            TextView chip = new TextView(this);
            chip.setTextAppearance(this, R.style.TextAppearance_ArtBarbershop_Caption);
            chip.setText(option.chipLabel());
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextColor(ContextCompat.getColor(this, option == selectedDateOption
                    ? R.color.color_text_inverse : R.color.color_text_primary));
            chip.setTypeface(null, option == selectedDateOption
                    ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            chip.setBackgroundResource(option == selectedDateOption
                    ? R.drawable.bg_booking_date_selected : R.drawable.bg_booking_date_unselected);
            chip.setOnClickListener(v -> {
                selectedDateOption = option;
                selectedTimeSlot = null;
                refreshBookingUi();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.booking_date_chip_width),
                    getResources().getDimensionPixelSize(R.dimen.space_64)
            );
            params.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.space_8));
            dateSelectorRow.addView(chip, params);
        }
    }

    private void bindTimeUi() {
        timeSlotGrid.removeAllViews();
        if (selectedDateOption == null) {
            return;
        }
        List<TimeSlot> slots = calculateSlotsForDate(selectedDateOption);
        timeSlotGrid.setColumnCount(3);
        timeSlotGrid.setRowCount((slots.size() + 2) / 3);
        for (TimeSlot slot : slots) {
            TextView chip = new TextView(this);
            boolean selected = slot.equals(selectedTimeSlot);
            chip.setTextAppearance(this, R.style.TextAppearance_ArtBarbershop_Caption);
            chip.setText(formatTime(slot.startAt));
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextColor(ContextCompat.getColor(this, selected
                    ? R.color.color_text_inverse : R.color.color_text_primary));
            chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            chip.setBackgroundResource(selected
                    ? R.drawable.bg_booking_date_selected : R.drawable.bg_booking_date_unselected);
            chip.setOnClickListener(v -> {
                selectedTimeSlot = slot;
                refreshBookingUi();
            });
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = getResources().getDimensionPixelSize(R.dimen.booking_time_slot_width);
            params.height = getResources().getDimensionPixelSize(R.dimen.touch_target_min);
            params.setMargins(0, 0, getResources().getDimensionPixelSize(R.dimen.space_8),
                    getResources().getDimensionPixelSize(R.dimen.space_8));
            timeSlotGrid.addView(chip, params);
        }
    }

    private void bindAvailabilityMessages() {
    }

    private void bindSummaryUi() {
        boolean canContinue = selectedService != null
                && selectedBarberId > 0L
                && selectedDateOption != null
                && selectedTimeSlot != null;
        textTotalDuration.setText(selectedService == null || selectedService.getTimeMinutes() <= 0
                ? "—" : String.format(Locale.US, "%d min", selectedService.getTimeMinutes()));
        textTotalPrice.setText(selectedService == null ? "" : formatPrice(selectedService.getPrice()));
        buttonContinue.setEnabled(canContinue);
        buttonContinue.setAlpha(canContinue ? 1.0f : 0.5f);
    }

    private void openBookingConfirmation() {
        if (selectedService == null || selectedBarberId <= 0L || selectedTimeSlot == null) {
            Toast.makeText(this, R.string.state_error_placeholder, Toast.LENGTH_SHORT).show();
            return;
        }
        final long serviceId;
        try {
            serviceId = Long.parseLong(selectedService.getServiceId());
        } catch (NumberFormatException exception) {
            Toast.makeText(this, R.string.state_error_placeholder, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, BookingConfirmActivity.class);
        intent.putExtra(EXTRA_SERVICE_ID, serviceId);
        intent.putExtra(EXTRA_BARBER_ID, selectedBarberId);
        intent.putExtra(EXTRA_BARBER_NAME, selectedBarberName);
        intent.putExtra(EXTRA_BARBER_EXPERIENCE, selectedBarberExperience);
        intent.putExtra("serviceName", selectedService.getName());
        intent.putExtra("servicePrice", selectedService.getPrice());
        intent.putExtra("serviceDuration", selectedService.getTimeMinutes());
        intent.putExtra("startAtMillis", selectedTimeSlot.startAt);
        intent.putExtra("endAtMillis", selectedTimeSlot.endAt);
        intent.putExtra("bookingNote", bookingNote());
        intent.putExtra(EXTRA_AI_BOOKING, getIntent().getBooleanExtra(EXTRA_AI_BOOKING, false));
        startActivity(intent);
    }

    private String bookingNote() {
        return editTextBookingNote.getText() == null ? "" : editTextBookingNote.getText().toString().trim();
    }

    private boolean sameDay(long first, long second) {
        Calendar firstCalendar = Calendar.getInstance();
        firstCalendar.setTimeInMillis(first);
        Calendar secondCalendar = Calendar.getInstance();
        secondCalendar.setTimeInMillis(second);
        return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
                && firstCalendar.get(Calendar.DAY_OF_YEAR) == secondCalendar.get(Calendar.DAY_OF_YEAR);
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String formatTime(long timeMillis) {
        return new SimpleDateFormat("h:mm a", Locale.US).format(new Date(timeMillis));
    }

    private String formatPrice(double value) {
        return String.format(Locale.US, "$%.2f", value);
    }

    private String initial(String value) {
        return value.isEmpty() ? "" : value.substring(0, 1).toUpperCase(Locale.US);
    }

    private String formatExperience(String value) {
        if (value.isEmpty()) {
            return getString(R.string.barber_experience_not_updated);
        }
        return value.toLowerCase(Locale.US).contains("year")
                ? value : getString(R.string.barber_experience_years_format, value);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static class DateOption {
        final Date dayStart;

        DateOption(Date dayStart) {
            this.dayStart = dayStart;
        }

        String chipLabel() {
            return new SimpleDateFormat("EEE\nMMM d", Locale.US).format(dayStart);
        }
    }

    private static class TimeSlot {
        final long startAt;
        final long endAt;

        TimeSlot(long startAt, long endAt) {
            this.startAt = startAt;
            this.endAt = endAt;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TimeSlot && ((TimeSlot) other).startAt == startAt;
        }

        @Override
        public int hashCode() {
            return Long.valueOf(startAt).hashCode();
        }
    }
}
