package com.example.barbershop;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BarberListActivity extends AppCompatActivity {

    private static final String FILTER_TOP_RATED = "Top Rated";

    private BarberAdapter barberAdapter;
    private EditText searchEditText;
    private View emptyState;
    private View loadingState;
    private View errorState;
    private String selectedFilter = FILTER_TOP_RATED;
    private final List<BarberAdapter.BarberItem> allBarbers = new ArrayList<>();
    private final List<TextView> filterChips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_list);

        setupTopBar();
        setupRecyclerView();
        setupSearch();
        setupFilterChips();

        showLoading(false);
        // TODO: Replace this temporary in-memory sample data with Firebase/SQLite barber data.
        loadSampleBarbers();
        applyFilters();
    }

    private void setupTopBar() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonNotifications).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.nav_target_unavailable, "Notifications"), Toast.LENGTH_SHORT).show()
        );
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerBarbers);
        emptyState = findViewById(R.id.layoutBarbersEmpty);
        loadingState = findViewById(R.id.layoutBarbersLoading);
        errorState = findViewById(R.id.layoutBarbersError);

        barberAdapter = new BarberAdapter(this::openBookingIfAvailable);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(barberAdapter);
    }

    private void setupSearch() {
        searchEditText = findViewById(R.id.editTextSearchBarbers);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });
    }

    private void setupFilterChips() {
        addFilterChip(R.id.chipTopRated, FILTER_TOP_RATED);
        addFilterChip(R.id.chipAvailable, "Available");
        addFilterChip(R.id.chipFade, "Fade");
        addFilterChip(R.id.chipColoring, "Coloring");
        updateChipSelection();
    }

    private void addFilterChip(int chipId, String filter) {
        TextView chip = findViewById(chipId);
        filterChips.add(chip);
        chip.setOnClickListener(v -> {
            selectedFilter = filter;
            updateChipSelection();
            applyFilters();
        });
    }

    private void updateChipSelection() {
        for (TextView chip : filterChips) {
            boolean selected = chip.getText().toString().equals(selectedFilter);
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(
                    this,
                    selected ? R.color.color_text_inverse : R.color.color_text_primary
            ));
        }
    }

    private void loadSampleBarbers() {
        allBarbers.clear();
        allBarbers.addAll(Arrays.asList(
                new BarberAdapter.BarberItem(
                        "Michael",
                        "M",
                        "8 years experience",
                        "Specialty: Fade, Classic Cut",
                        "4.9 (320)",
                        "Available Today",
                        "9:00 AM - 7:00 PM",
                        getString(R.string.action_view_profile),
                        true,
                        true
                ),
                new BarberAdapter.BarberItem(
                        "David",
                        "D",
                        "6 years experience",
                        "Specialty: Modern, Styling",
                        "4.8 (245)",
                        "Available Today",
                        "10:00 AM - 8:00 PM",
                        getString(R.string.action_select),
                        true,
                        true
                ),
                new BarberAdapter.BarberItem(
                        "James",
                        "J",
                        "7 years experience",
                        "Specialty: Coloring, Fade",
                        "4.8 (210)",
                        "Available Today",
                        "11:00 AM - 7:00 PM",
                        getString(R.string.action_view_profile),
                        true,
                        true
                ),
                new BarberAdapter.BarberItem(
                        "Sophia",
                        "S",
                        "5 years experience",
                        "Specialty: Coloring, Perm",
                        "4.7 (188)",
                        "Available Today",
                        "9:00 AM - 6:00 PM",
                        getString(R.string.action_select),
                        false,
                        true
                ),
                new BarberAdapter.BarberItem(
                        "Ethan",
                        "E",
                        "4 years experience",
                        "Specialty: Fade, Beard",
                        "4.6 (156)",
                        "Available Today",
                        "12:00 PM - 9:00 PM",
                        getString(R.string.action_select),
                        false,
                        true
                )
        ));
    }

    private void applyFilters() {
        String query = searchEditText == null
                ? ""
                : searchEditText.getText().toString().trim().toLowerCase(Locale.US);
        List<BarberAdapter.BarberItem> filteredBarbers = new ArrayList<>();

        for (BarberAdapter.BarberItem barberItem : allBarbers) {
            boolean matchesFilter = matchesSelectedFilter(barberItem);
            boolean matchesQuery = query.isEmpty()
                    || barberItem.name.toLowerCase(Locale.US).contains(query)
                    || barberItem.specialty.toLowerCase(Locale.US).contains(query)
                    || barberItem.experience.toLowerCase(Locale.US).contains(query);

            if (matchesFilter && matchesQuery) {
                filteredBarbers.add(barberItem);
            }
        }

        barberAdapter.submitList(filteredBarbers);
        showContentState(filteredBarbers.isEmpty());
    }

    private boolean matchesSelectedFilter(BarberAdapter.BarberItem barberItem) {
        if (FILTER_TOP_RATED.equals(selectedFilter)) {
            return barberItem.topRated;
        }
        if ("Available".equals(selectedFilter)) {
            return barberItem.available;
        }
        return barberItem.specialty.toLowerCase(Locale.US).contains(selectedFilter.toLowerCase(Locale.US));
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

    private void openBookingIfAvailable(BarberAdapter.BarberItem barberItem) {
        try {
            Class<?> targetClass = Class.forName(getPackageName() + ".BookingActivity");
            Intent intent = new Intent(this, targetClass);
            intent.putExtra("selectedBarberName", barberItem.name);
            startActivity(intent);
        } catch (ClassNotFoundException exception) {
            Toast.makeText(this, getString(R.string.nav_target_unavailable, "Booking"), Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, getString(R.string.nav_target_not_registered), Toast.LENGTH_SHORT).show();
        }
    }
}
