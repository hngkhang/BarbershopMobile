package com.example.barbershop.util;

import com.example.barbershop.R;

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

import com.example.barbershop.adapters.ServiceAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ServiceListActivity extends AppCompatActivity {

    private static final String FILTER_ALL = "All";

    private ServiceAdapter serviceAdapter;
    private EditText searchEditText;
    private View emptyState;
    private View loadingState;
    private View errorState;
    private String selectedCategory = FILTER_ALL;
    private final List<ServiceAdapter.ServiceItem> allServices = new ArrayList<>();
    private final List<TextView> filterChips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_list);

        setupTopBar();
        setupRecyclerView();
        setupSearch();
        setupFilterChips();

        showLoading(false);
        // TODO: Replace this temporary in-memory sample data with Firebase/SQLite service data.
        loadSampleServices();
        applyFilters();
    }

    private void setupTopBar() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonNotifications).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.nav_target_unavailable, "Notifications"), Toast.LENGTH_SHORT).show()
        );
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerServices);
        emptyState = findViewById(R.id.layoutServicesEmpty);
        loadingState = findViewById(R.id.layoutServicesLoading);
        errorState = findViewById(R.id.layoutServicesError);

        serviceAdapter = new ServiceAdapter(this::openBookingIfAvailable);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(serviceAdapter);
    }

    private void setupSearch() {
        searchEditText = findViewById(R.id.editTextSearchServices);
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
        addFilterChip(R.id.chipAll, FILTER_ALL);
        addFilterChip(R.id.chipHaircut, "Haircut");
        addFilterChip(R.id.chipShampoo, "Shampoo");
        addFilterChip(R.id.chipPerm, "Perm");
        addFilterChip(R.id.chipColoring, "Coloring");
        addFilterChip(R.id.chipCombo, "Combo");
        updateChipSelection();
    }

    private void addFilterChip(int chipId, String category) {
        TextView chip = findViewById(chipId);
        filterChips.add(chip);
        chip.setOnClickListener(v -> {
            selectedCategory = category;
            updateChipSelection();
            applyFilters();
        });
    }

    private void updateChipSelection() {
        for (TextView chip : filterChips) {
            boolean selected = chip.getText().toString().equals(selectedCategory);
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(
                    this,
                    selected ? R.color.color_text_inverse : R.color.color_text_primary
            ));
        }
    }

    private void loadSampleServices() {
        allServices.clear();
        allServices.addAll(Arrays.asList(
                new ServiceAdapter.ServiceItem(
                        "Haircut",
                        "Classic cut tailored to your style with precision and care.",
                        "30 min",
                        "$25.00",
                        "Haircut",
                        R.drawable.bg_service_tile_haircut,
                        R.drawable.ic_scissors
                ),
                new ServiceAdapter.ServiceItem(
                        "Shampoo",
                        "Relaxing wash with premium products and scalp massage.",
                        "15 min",
                        "$12.00",
                        "Shampoo",
                        R.drawable.bg_service_tile_shampoo,
                        R.drawable.ic_barber_pole
                ),
                new ServiceAdapter.ServiceItem(
                        "Perm",
                        "Natural waves or curls shaped for a fresh new look.",
                        "60 min",
                        "$60.00",
                        "Perm",
                        R.drawable.bg_service_tile_perm,
                        R.drawable.ic_sparkle
                ),
                new ServiceAdapter.ServiceItem(
                        "Coloring",
                        "Professional hair coloring for a vibrant new you.",
                        "45 min",
                        "$50.00",
                        "Coloring",
                        R.drawable.bg_service_tile_coloring,
                        R.drawable.ic_sparkle
                ),
                new ServiceAdapter.ServiceItem(
                        "Combo Package",
                        "Haircut, shampoo, and styling for complete grooming.",
                        "60 min",
                        "$55.00",
                        "Combo",
                        R.drawable.bg_service_tile_combo,
                        R.drawable.ic_calendar
                )
        ));
    }

    private void applyFilters() {
        String query = searchEditText == null
                ? ""
                : searchEditText.getText().toString().trim().toLowerCase(Locale.US);
        List<ServiceAdapter.ServiceItem> filteredServices = new ArrayList<>();

        for (ServiceAdapter.ServiceItem serviceItem : allServices) {
            boolean matchesCategory = FILTER_ALL.equals(selectedCategory)
                    || serviceItem.category.equals(selectedCategory);
            boolean matchesQuery = query.isEmpty()
                    || serviceItem.name.toLowerCase(Locale.US).contains(query)
                    || serviceItem.description.toLowerCase(Locale.US).contains(query);

            if (matchesCategory && matchesQuery) {
                filteredServices.add(serviceItem);
            }
        }

        serviceAdapter.submitList(filteredServices);
        showContentState(filteredServices.isEmpty());
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

    private void openBookingIfAvailable(ServiceAdapter.ServiceItem serviceItem) {
        try {
            Class<?> targetClass = Class.forName(getPackageName() + ".BookingActivity");
            Intent intent = new Intent(this, targetClass);
            intent.putExtra("selectedServiceName", serviceItem.name);
            intent.putExtra("selectedServicePrice", serviceItem.price);
            startActivity(intent);
        } catch (ClassNotFoundException exception) {
            Toast.makeText(this, getString(R.string.nav_target_unavailable, "Booking"), Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, getString(R.string.nav_target_not_registered), Toast.LENGTH_SHORT).show();
        }
    }
}
