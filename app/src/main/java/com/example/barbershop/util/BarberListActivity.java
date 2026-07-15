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

import com.example.barbershop.adapters.BarberAdapter;
import com.example.barbershop.data.BarberRepository;
import com.example.barbershop.models.Barber;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarberListActivity extends AppCompatActivity {

    public static final String EXTRA_BARBER_ID = "barberId";

    private BarberAdapter barberAdapter;
    private BarberRepository barberRepository;
    private EditText searchEditText;
    private View emptyState;
    private View loadingState;
    private TextView errorState;
    private String selectedFilter = "";
    private final List<Barber> allBarbers = new ArrayList<>();
    private final List<TextView> filterChips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_list);
        barberRepository = new BarberRepository();

        setupTopBar();
        setupRecyclerView();
        setupSearch();
        setupFilterChips();

        loadBarbers();
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
        selectedFilter = getString(R.string.filter_top_rated);
        addFilterChip(R.id.chipTopRated, getString(R.string.filter_top_rated));
        addFilterChip(R.id.chipAvailable, getString(R.string.filter_available));
        findViewById(R.id.chipFade).setVisibility(View.GONE);
        findViewById(R.id.chipColoring).setVisibility(View.GONE);
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

    private void loadBarbers() {
        showLoading(true);
        allBarbers.clear();
        barberRepository.getAllBarbers(new BarberRepository.RepositoryCallback<List<Barber>>() {
            @Override
            public void onSuccess(List<Barber> data) {
                allBarbers.clear();
                allBarbers.addAll(data);
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
        List<Barber> filteredBarbers = new ArrayList<>();

        for (Barber barber : allBarbers) {
            boolean matchesFilter = matchesSelectedFilter(barber);
            boolean matchesQuery = query.isEmpty()
                    || barber.getName().toLowerCase(Locale.US).contains(query)
                    || barber.getExperience().toLowerCase(Locale.US).contains(query);

            if (matchesFilter && matchesQuery) {
                filteredBarbers.add(barber);
            }
        }

        barberAdapter.submitList(filteredBarbers);
        showContentState(filteredBarbers.isEmpty());
    }

    private boolean matchesSelectedFilter(Barber barber) {
        return barber.isActive();
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
        errorState.setText(getString(R.string.barbers_load_failed, message));
    }

    private void openBookingIfAvailable(Barber barber) {
        try {
            Intent intent = new Intent(this, BarberDetailActivity.class);
            intent.putExtra(EXTRA_BARBER_ID, barber.getId());
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, getString(R.string.nav_target_not_registered), Toast.LENGTH_SHORT).show();
        }
    }
}
