package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private static final String DEFAULT_SERVICE_NAME = "Haircut";
    private static final String DEFAULT_SERVICE_PRICE = "$25.00";
    private static final String ADDON_SERVICE_NAME = "Shampoo";
    private static final String ADDON_SERVICE_PRICE = "$12.00";
    private static final String DEFAULT_BARBER_NAME = "Michael";

    private final List<DateOption> dateOptions = new ArrayList<>();
    private final List<TextView> dateChips = new ArrayList<>();
    private final List<TextView> timeChips = new ArrayList<>();

    private String selectedServiceName;
    private String selectedServicePrice;
    private String selectedBarberName;
    private String pendingSelectedDateLabel;
    private String initialBookingNote;
    private DateOption selectedDateOption;
    private String selectedStartTime = "10:00 AM";
    private int totalDurationMinutes;
    private double totalPrice;

    private TextView textSelectedServiceName;
    private TextView textSelectedServicePrice;
    private TextView textAddonServicePrice;
    private TextView textSelectedBarberInitial;
    private TextView textSelectedBarberName;
    private TextView textSelectedBarberExperience;
    private TextView textSelectedBarberSpecialty;
    private TextView textTotalDuration;
    private TextView textTotalPrice;
    private TextView textNoteCounter;
    private TextInputEditText editTextBookingNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        readTemporaryBookingData();
        bindViews();
        setupTopBar();
        setupServiceSection();
        setupBarberSection();
        setupDateSelector();
        setupTimeSelector();
        setupNoteInput();
        setupSummary();
    }

    private void readTemporaryBookingData() {
        Intent intent = getIntent();
        // TODO: Replace temporary extras/defaults with selected Firebase/SQLite service and barber models.
        selectedServiceName = intent.getStringExtra("selectedServiceName");
        selectedServicePrice = intent.getStringExtra("selectedServicePrice");
        selectedBarberName = intent.getStringExtra("selectedBarberName");
        pendingSelectedDateLabel = intent.getStringExtra("selectedDateLabel");
        initialBookingNote = intent.getStringExtra("bookingNote");

        if (selectedServiceName == null || selectedServiceName.trim().isEmpty()) {
            selectedServiceName = DEFAULT_SERVICE_NAME;
        }
        if (selectedServicePrice == null || selectedServicePrice.trim().isEmpty()) {
            selectedServicePrice = DEFAULT_SERVICE_PRICE;
        }
        if (selectedBarberName == null || selectedBarberName.trim().isEmpty()) {
            selectedBarberName = DEFAULT_BARBER_NAME;
        }
        String restoredStartTime = intent.getStringExtra("selectedStartTime");
        if (restoredStartTime != null && !restoredStartTime.trim().isEmpty()) {
            selectedStartTime = restoredStartTime;
        }

        totalDurationMinutes = getTemporaryDurationForService(selectedServiceName) + 15;
        totalPrice = parsePrice(selectedServicePrice) + parsePrice(ADDON_SERVICE_PRICE);
    }

    private void bindViews() {
        textSelectedServiceName = findViewById(R.id.textSelectedServiceName);
        textSelectedServicePrice = findViewById(R.id.textSelectedServicePrice);
        textAddonServicePrice = findViewById(R.id.textAddonServicePrice);
        textSelectedBarberInitial = findViewById(R.id.textSelectedBarberInitial);
        textSelectedBarberName = findViewById(R.id.textSelectedBarberName);
        textSelectedBarberExperience = findViewById(R.id.textSelectedBarberExperience);
        textSelectedBarberSpecialty = findViewById(R.id.textSelectedBarberSpecialty);
        textTotalDuration = findViewById(R.id.textTotalDuration);
        textTotalPrice = findViewById(R.id.textTotalPrice);
        textNoteCounter = findViewById(R.id.textNoteCounter);
        editTextBookingNote = findViewById(R.id.editTextBookingNote);
    }

    private void setupTopBar() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }

    private void setupServiceSection() {
        textSelectedServiceName.setText(selectedServiceName);
        textSelectedServicePrice.setText(selectedServicePrice);
        textAddonServicePrice.setText(ADDON_SERVICE_PRICE);

        findViewById(R.id.buttonAddMoreServices).setOnClickListener(v -> {
            Toast.makeText(this, R.string.booking_add_more_demo, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ServiceListActivity.class));
        });
    }

    private void setupBarberSection() {
        textSelectedBarberInitial.setText(selectedBarberName.substring(0, 1).toUpperCase(Locale.US));
        textSelectedBarberInitial.setContentDescription(
                getString(R.string.booking_barber_avatar_content_description, selectedBarberName)
        );
        textSelectedBarberName.setText(selectedBarberName);
        textSelectedBarberExperience.setText("8 years experience");
        textSelectedBarberSpecialty.setText("Specialty: Fade, Classic Cut");

        findViewById(R.id.buttonChangeBarber).setOnClickListener(v -> {
            Toast.makeText(this, R.string.booking_change_barber_demo, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, BarberListActivity.class));
        });
    }

    private void setupDateSelector() {
        dateChips.add(findViewById(R.id.chipDateMonday));
        dateChips.add(findViewById(R.id.chipDateTuesday));
        dateChips.add(findViewById(R.id.chipDateWednesday));
        dateChips.add(findViewById(R.id.chipDateThursday));
        dateChips.add(findViewById(R.id.chipDateFriday));
        dateChips.add(findViewById(R.id.chipDateSaturday));
        dateChips.add(findViewById(R.id.chipDateSunday));

        buildTemporaryDateOptions();

        for (int index = 0; index < dateChips.size(); index++) {
            TextView chip = dateChips.get(index);
            DateOption option = dateOptions.get(index);
            chip.setText(option.chipLabel);
            chip.setOnClickListener(v -> {
                selectedDateOption = option;
                updateDateSelection();
            });
        }

        selectedDateOption = findRestoredDateOption();
        updateDateSelection();
    }

    private void setupTimeSelector() {
        addTimeChip(R.id.chipTime0930, "9:30 AM");
        addTimeChip(R.id.chipTime1000, "10:00 AM");
        addTimeChip(R.id.chipTime1030, "10:30 AM");
        addTimeChip(R.id.chipTime1100, "11:00 AM");
        addTimeChip(R.id.chipTime1130, "11:30 AM");
        addTimeChip(R.id.chipTime1230, "12:30 PM");

        ((TextView) findViewById(R.id.chipTime0900)).setText("9:00 AM");
        ((TextView) findViewById(R.id.chipTime1200)).setText("12:00 PM");
        updateTimeSelection();
    }

    private void addTimeChip(int chipId, String time) {
        TextView chip = findViewById(chipId);
        chip.setText(time);
        timeChips.add(chip);
        chip.setOnClickListener(v -> {
            selectedStartTime = time;
            updateTimeSelection();
        });
    }

    private void setupNoteInput() {
        if (initialBookingNote != null
                && !initialBookingNote.trim().isEmpty()
                && !initialBookingNote.equals(getString(R.string.booking_default_note))) {
            editTextBookingNote.setText(initialBookingNote);
        }
        editTextBookingNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textNoteCounter.setText(String.format(Locale.US, "%d/200", s.length()));
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });
        int noteLength = editTextBookingNote.getText() == null ? 0 : editTextBookingNote.getText().length();
        textNoteCounter.setText(String.format(Locale.US, "%d/200", noteLength));
    }

    private void setupSummary() {
        textTotalDuration.setText(String.format(Locale.US, "%d min", totalDurationMinutes));
        textTotalPrice.setText(formatPrice(totalPrice));
        findViewById(R.id.buttonContinue).setOnClickListener(v -> openBookingConfirmation());
    }

    private void updateDateSelection() {
        for (int index = 0; index < dateChips.size(); index++) {
            TextView chip = dateChips.get(index);
            boolean selected = dateOptions.get(index) == selectedDateOption;
            chip.setBackgroundResource(selected
                    ? R.drawable.bg_booking_date_selected
                    : R.drawable.bg_booking_date_unselected);
            chip.setTextColor(ContextCompat.getColor(
                    this,
                    selected ? R.color.color_text_inverse : R.color.color_text_primary
            ));
            chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void updateTimeSelection() {
        for (TextView chip : timeChips) {
            boolean selected = selectedStartTime.contentEquals(chip.getText());
            chip.setBackgroundResource(selected
                    ? R.drawable.bg_booking_date_selected
                    : R.drawable.bg_booking_date_unselected);
            chip.setTextColor(ContextCompat.getColor(
                    this,
                    selected ? R.color.color_text_inverse : R.color.color_text_primary
            ));
            chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void openBookingConfirmation() {
        Intent intent = new Intent(this, BookingConfirmActivity.class);
        intent.putExtra("selectedServiceName", selectedServiceName);
        intent.putExtra("selectedServicePrice", selectedServicePrice);
        intent.putExtra("addonServiceName", ADDON_SERVICE_NAME);
        intent.putExtra("addonServicePrice", ADDON_SERVICE_PRICE);
        intent.putExtra("selectedBarberName", selectedBarberName);
        intent.putExtra("selectedDateLabel", selectedDateOption.confirmationLabel);
        intent.putExtra("selectedStartTime", selectedStartTime);
        intent.putExtra("selectedEndTime", calculateEndTime(selectedStartTime, totalDurationMinutes));
        intent.putExtra("totalDurationMinutes", totalDurationMinutes);
        intent.putExtra("totalPrice", totalPrice);
        intent.putExtra("bookingNote", getBookingNote());
        startActivity(intent);
    }

    private String getBookingNote() {
        String note = editTextBookingNote.getText() == null
                ? ""
                : editTextBookingNote.getText().toString().trim();
        return note.isEmpty() ? getString(R.string.booking_default_note) : note;
    }

    private void buildTemporaryDateOptions() {
        dateOptions.clear();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat chipFormat = new SimpleDateFormat("EEE\nMMM d", Locale.US);
        SimpleDateFormat confirmationFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);

        for (int index = 0; index < dateChips.size(); index++) {
            Calendar optionCalendar = (Calendar) calendar.clone();
            optionCalendar.add(Calendar.DATE, index);
            dateOptions.add(new DateOption(
                    chipFormat.format(optionCalendar.getTime()),
                    confirmationFormat.format(optionCalendar.getTime())
            ));
        }
    }

    private DateOption findRestoredDateOption() {
        if (pendingSelectedDateLabel != null) {
            for (DateOption option : dateOptions) {
                if (pendingSelectedDateLabel.equals(option.confirmationLabel)) {
                    return option;
                }
            }
        }
        return dateOptions.get(2);
    }

    private int getTemporaryDurationForService(String serviceName) {
        // TODO: Replace this temporary mapping with service duration from Firebase/SQLite.
        String normalized = serviceName.toLowerCase(Locale.US);
        if (normalized.contains("perm") || normalized.contains("combo")) {
            return 60;
        }
        if (normalized.contains("color")) {
            return 45;
        }
        if (normalized.contains("shampoo")) {
            return 15;
        }
        return 30;
    }

    private double parsePrice(String priceText) {
        try {
            return Double.parseDouble(priceText.replace("$", "").trim());
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private String formatPrice(double price) {
        return String.format(Locale.US, "$%.2f", price);
    }

    private String calculateEndTime(String startTime, int durationMinutes) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timeFormat.parse(startTime));
            calendar.add(Calendar.MINUTE, durationMinutes);
            return timeFormat.format(calendar.getTime());
        } catch (ParseException exception) {
            return startTime;
        }
    }

    private static class DateOption {
        final String chipLabel;
        final String confirmationLabel;

        DateOption(String chipLabel, String confirmationLabel) {
            this.chipLabel = chipLabel;
            this.confirmationLabel = confirmationLabel;
        }
    }
}
