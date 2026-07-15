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
import com.example.barbershop.data.BarberRepository;
import com.example.barbershop.data.ServiceRepository;
import com.example.barbershop.models.ShopService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServiceListActivity extends AppCompatActivity {

    private static final String FILTER_ALL = "All";

    private ServiceAdapter serviceAdapter;
    private ServiceRepository serviceRepository;
    private EditText searchEditText;
    private View emptyState;
    private View loadingState;
    private TextView errorState;
    private String selectedCategory = FILTER_ALL;
    private final List<ServiceAdapter.ServiceItem> allServices = new ArrayList<>();
    private final List<TextView> filterChips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_list);
        serviceRepository = new ServiceRepository();

        setupTopBar();
        setupRecyclerView();
        setupSearch();
        setupFilterChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadServices();
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
        errorState.setOnClickListener(v -> loadServices());
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

    private void loadServices() {
        showLoading(true);
        allServices.clear();
        serviceAdapter.submitList(new ArrayList<>());
        serviceRepository.getAllServices(new BarberRepository.RepositoryCallback<List<ShopService>>() {
            @Override
            public void onSuccess(List<ShopService> data) {
                allServices.clear();
                for (ShopService service : data) {
                    allServices.add(toServiceItem(service));
                }
                applyFilters();
            }

            @Override
            public void onError(Exception exception) {
                showError(exception);
            }
        });
    }

    private void applyFilters() {
        String query = searchEditText == null
                ? ""
                : searchEditText.getText().toString().trim().toLowerCase(Locale.US);
        List<ServiceAdapter.ServiceItem> filteredServices = new ArrayList<>();

        for (ServiceAdapter.ServiceItem serviceItem : allServices) {
            boolean matchesCategory = FILTER_ALL.equals(selectedCategory)
                    || serviceItem.category.equalsIgnoreCase(selectedCategory);
            boolean matchesQuery = query.isEmpty()
                    || serviceItem.name.toLowerCase(Locale.US).contains(query)
                    || serviceItem.description.toLowerCase(Locale.US).contains(query)
                    || serviceItem.category.toLowerCase(Locale.US).contains(query);

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

    private void showError(Exception exception) {
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        String message = exception == null || exception.getMessage() == null
                ? getString(R.string.state_error_placeholder)
                : exception.getMessage();
        errorState.setText(getString(R.string.services_load_failed, message));
    }

    private ServiceAdapter.ServiceItem toServiceItem(ShopService service) {
        String category = formatCategory(service.getCategory());
        return new ServiceAdapter.ServiceItem(
                fallback(service.getName(), category),
                buildDescription(category),
                formatDuration(service.getTimeMinutes()),
                formatPrice(service.getPrice()),
                category,
                service.getImageUrl(),
                getServiceBackground(category),
                getServiceIcon(category)
        );
    }

    private String buildDescription(String category) {
        return category.isEmpty()
                ? getString(R.string.service_description_default)
                : getString(R.string.service_description_category_format, category);
    }

    private String formatDuration(int minutes) {
        return getString(R.string.service_duration_minutes_format, Math.max(minutes, 0));
    }

    private String formatPrice(double price) {
        if (Math.abs(price - Math.round(price)) < 0.0001) {
            return getString(R.string.service_price_whole_format, Math.round(price));
        }

        return getString(R.string.service_price_decimal_format, price);
    }

    private String formatCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "";
        }

        String normalized = category.trim().toLowerCase(Locale.US);
        return normalized.substring(0, 1).toUpperCase(Locale.US) + normalized.substring(1);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private int getServiceBackground(String category) {
        String normalizedCategory = category.toLowerCase(Locale.US);
        if (normalizedCategory.contains("shampoo")) {
            return R.drawable.bg_service_tile_shampoo;
        }
        if (normalizedCategory.contains("perm")) {
            return R.drawable.bg_service_tile_perm;
        }
        if (normalizedCategory.contains("color")) {
            return R.drawable.bg_service_tile_coloring;
        }
        if (normalizedCategory.contains("combo")) {
            return R.drawable.bg_service_tile_combo;
        }

        return R.drawable.bg_service_tile_haircut;
    }

    private int getServiceIcon(String category) {
        String normalizedCategory = category.toLowerCase(Locale.US);
        if (normalizedCategory.contains("shampoo")) {
            return R.drawable.ic_barber_pole;
        }
        if (normalizedCategory.contains("perm") || normalizedCategory.contains("color")) {
            return R.drawable.ic_sparkle;
        }
        if (normalizedCategory.contains("combo")) {
            return R.drawable.ic_calendar;
        }

        return R.drawable.ic_scissors;
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
