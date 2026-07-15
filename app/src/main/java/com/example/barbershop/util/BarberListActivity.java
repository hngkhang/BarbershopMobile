package com.example.barbershop.util;

import com.example.barbershop.R;

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

<<<<<<< HEAD:app/src/main/java/com/example/barbershop/BarberListActivity.java
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
=======
import com.example.barbershop.adapters.BarberAdapter;
>>>>>>> origin:app/src/main/java/com/example/barbershop/util/BarberListActivity.java

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BarberListActivity extends AppCompatActivity {

    private static final String COLLECTION_BARBERS = "barbers";
    private static final String COLLECTION_RATINGS = "ratings";

    private static final String FIELD_BARBER_ID = "barberId";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_EXPERIENCE = "experience";
    private static final String FIELD_AVATAR_URL = "avatarUrl";
    private static final String FIELD_ACTIVE = "active";
    private static final String FIELD_RATE = "rate";

    private static final int FILTER_TOP_RATED = 1;
    private static final int FILTER_AVAILABLE = 2;

    private final List<BarberAdapter.BarberItem> allBarbers = new ArrayList<>();
    private final List<TextView> filterChips = new ArrayList<>();

    private FirebaseFirestore firestore;
    private BarberAdapter barberAdapter;

    private EditText searchEditText;
    private View emptyState;
    private View loadingState;
    private View errorState;

    private int selectedFilter = FILTER_TOP_RATED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_list);

        firestore = FirebaseFirestore.getInstance();

        setupTopBar();
        setupRecyclerView();
        setupSearch();
        setupFilterChips();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Load lại khi quay về từ ReviewActivity
         * để cập nhật rating trung bình mới.
         */
        loadBarbersFromFirebase();
    }

    private void setupTopBar() {
        findViewById(R.id.buttonBack).setOnClickListener(view -> finish());

        findViewById(R.id.buttonNotifications).setOnClickListener(view ->
                Toast.makeText(
                        this,
                        R.string.notifications_unavailable,
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerBarbers);

        emptyState = findViewById(R.id.layoutBarbersEmpty);
        loadingState = findViewById(R.id.layoutBarbersLoading);
        errorState = findViewById(R.id.layoutBarbersError);

        barberAdapter = new BarberAdapter(this::openBarberProfile);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(barberAdapter);

        errorState.setOnClickListener(view -> loadBarbersFromFirebase());
    }

    private void setupSearch() {
        searchEditText = findViewById(R.id.editTextSearchBarbers);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after
            ) {
                // Không xử lý.
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count
            ) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Không xử lý.
            }
        });
    }

    private void setupFilterChips() {
        addFilterChip(R.id.chipTopRated, FILTER_TOP_RATED);
        addFilterChip(R.id.chipAvailable, FILTER_AVAILABLE);

        /*
         * Firestore hiện chưa có specialty nên chưa thể
         * lọc chính xác Fade và Coloring.
         */
        findViewById(R.id.chipFade).setVisibility(View.GONE);
        findViewById(R.id.chipColoring).setVisibility(View.GONE);

        updateChipSelection();
    }

    private void addFilterChip(int chipId, int filterType) {
        TextView chip = findViewById(chipId);

        chip.setTag(filterType);
        chip.setOnClickListener(view -> {
            selectedFilter = filterType;
            updateChipSelection();
            applyFilters();
        });

        filterChips.add(chip);
    }

    private void updateChipSelection() {
        for (TextView chip : filterChips) {
            Integer chipFilter = (Integer) chip.getTag();
            boolean selected = chipFilter != null && chipFilter == selectedFilter;

            chip.setBackgroundResource(
                    selected
                            ? R.drawable.bg_chip_selected
                            : R.drawable.bg_chip_unselected
            );

            chip.setTextColor(ContextCompat.getColor(
                    this,
                    selected
                            ? R.color.color_text_inverse
                            : R.color.color_text_primary
            ));
        }
    }

    /**
     * Đọc collection ratings trước để tính tổng điểm
     * và số lượt đánh giá của từng barber.
     */
    private void loadBarbersFromFirebase() {
        showLoading();

        firestore.collection(COLLECTION_RATINGS)
                .get()
                .addOnSuccessListener(ratingSnapshot -> {
                    Map<Long, Double> ratingSums = new HashMap<>();
                    Map<Long, Long> ratingCounts = new HashMap<>();

                    for (QueryDocumentSnapshot document : ratingSnapshot) {
                        Long barberId = readLong(document.get(FIELD_BARBER_ID));
                        Double rate = readDouble(document.get(FIELD_RATE));

                        if (barberId == null || rate == null || rate <= 0 || rate > 5) {
                            continue;
                        }

                        double currentSum = ratingSums.getOrDefault(barberId, 0.0);
                        long currentCount = ratingCounts.getOrDefault(barberId, 0L);

                        ratingSums.put(barberId, currentSum + rate);
                        ratingCounts.put(barberId, currentCount + 1);
                    }

                    loadBarberDocuments(ratingSums, ratingCounts);
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(
                            this,
                            getString(
                                    R.string.ratings_load_failed,
                                    safeErrorMessage(exception)
                            ),
                            Toast.LENGTH_LONG
                    ).show();

                    /*
                     * Nếu chưa đọc được ratings thì vẫn load barber,
                     * nhưng rating tạm thời hiển thị là chưa có.
                     */
                    loadBarberDocuments(new HashMap<>(), new HashMap<>());
                });
    }

    /**
     * Đọc collection barbers và kết hợp dữ liệu rating.
     */
    private void loadBarberDocuments(
            Map<Long, Double> ratingSums,
            Map<Long, Long> ratingCounts
    ) {
        firestore.collection(COLLECTION_BARBERS)
                .get()
                .addOnSuccessListener(barberSnapshot -> {
                    allBarbers.clear();

                    for (QueryDocumentSnapshot document : barberSnapshot) {
                        Long barberId = readLong(document.get(FIELD_BARBER_ID));

                        /*
                         * Database phải có field barberId kiểu int64.
                         * Những document không có barberId hợp lệ sẽ bị bỏ qua.
                         */
                        if (barberId == null || barberId < 1) {
                            continue;
                        }

                        String name = readString(
                                document.get(FIELD_NAME),
                                getString(R.string.barber_name_unknown)
                        );

                        String experience = formatExperience(
                                document.get(FIELD_EXPERIENCE)
                        );

                        String avatarUrl = readString(
                                document.get(FIELD_AVATAR_URL),
                                ""
                        );

                        boolean active = Boolean.TRUE.equals(
                                document.getBoolean(FIELD_ACTIVE)
                        );

                        long ratingCount = ratingCounts.getOrDefault(barberId, 0L);
                        double ratingSum = ratingSums.getOrDefault(barberId, 0.0);

                        double averageRating = ratingCount == 0
                                ? 0.0
                                : ratingSum / ratingCount;

                        allBarbers.add(new BarberAdapter.BarberItem(
                                barberId,
                                name,
                                experience,
                                avatarUrl,
                                active,
                                averageRating,
                                ratingCount
                        ));
                    }

                    applyFilters();
                })
                .addOnFailureListener(this::showLoadError);
    }

    private void applyFilters() {
        if (barberAdapter == null || searchEditText == null) {
            return;
        }

        String query = searchEditText.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.US);

        List<BarberAdapter.BarberItem> filteredBarbers = new ArrayList<>();

        for (BarberAdapter.BarberItem barber : allBarbers) {
            boolean matchesAvailable = selectedFilter != FILTER_AVAILABLE
                    || barber.active;

            boolean matchesSearch = query.isEmpty()
                    || barber.name.toLowerCase(Locale.US).contains(query)
                    || barber.experience.toLowerCase(Locale.US).contains(query);

            if (matchesAvailable && matchesSearch) {
                filteredBarbers.add(barber);
            }
        }

        if (selectedFilter == FILTER_TOP_RATED) {
            filteredBarbers.sort((first, second) -> {
                int ratingComparison = Double.compare(
                        second.averageRating,
                        first.averageRating
                );

                if (ratingComparison != 0) {
                    return ratingComparison;
                }

                int countComparison = Long.compare(
                        second.ratingCount,
                        first.ratingCount
                );

                if (countComparison != 0) {
                    return countComparison;
                }

                return first.name.compareToIgnoreCase(second.name);
            });
        } else {
            filteredBarbers.sort(
                    (first, second) ->
                            first.name.compareToIgnoreCase(second.name)
            );
        }

        barberAdapter.submitList(filteredBarbers);
        showContent(filteredBarbers.isEmpty());
    }

    /**
     * Mở ReviewActivity như trang Barber Profile.
     */
    private void openBarberProfile(BarberAdapter.BarberItem barber) {
        if (!barber.active) {
            Toast.makeText(
                    this,
                    R.string.barber_unavailable,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("barberId", barber.barberId);
        startActivity(intent);
    }

    private void showLoading() {
        loadingState.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    private void showContent(boolean empty) {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showLoadError(Exception exception) {
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);

        Toast.makeText(
                this,
                getString(
                        R.string.barbers_load_failed,
                        safeErrorMessage(exception)
                ),
                Toast.LENGTH_LONG
        ).show();
    }

    private Long readLong(Object value) {
        if (!(value instanceof Number)) {
            return null;
        }

        return ((Number) value).longValue();
    }

    private Double readDouble(Object value) {
        if (!(value instanceof Number)) {
            return null;
        }

        return ((Number) value).doubleValue();
    }

    private String readString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }
    private String formatExperience(Object value) {
        if (value == null) {
            return getString(R.string.barber_experience_not_updated);
        }

        String experience = String.valueOf(value).trim();

        if (experience.isEmpty()) {
            return getString(R.string.barber_experience_not_updated);
        }

        if (experience.toLowerCase(Locale.US).contains("year")) {
            return experience;
        }

        return getString(
                R.string.barber_experience_format,
                experience
        );
    }

    private String safeErrorMessage(Exception exception) {
        if (exception == null
                || exception.getMessage() == null
                || exception.getMessage().trim().isEmpty()) {
            return getString(R.string.unknown_error);
        }

        return exception.getMessage();
    }
}