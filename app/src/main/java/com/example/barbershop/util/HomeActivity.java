package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barbershop.data.BarberRepository;
import com.example.barbershop.data.ServiceRepository;
import com.example.barbershop.models.ShopService;
import com.example.barbershop.services.ImageLoader;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private static final int MAX_HOME_SERVICES = 5;

    private static final String COLLECTION_BARBERS = "barbers";
    private static final String COLLECTION_RATINGS = "ratings";

    private static final String FIELD_BARBER_ID = "barberId";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_EXPERIENCE = "experience";
    private static final String FIELD_ACTIVE = "active";
    private static final String FIELD_RATE = "rate";

    private FirebaseFirestore firestore;
    private ServiceRepository serviceRepository;
    private FeaturedBarberAdapter featuredBarberAdapter;
    private final List<ShopService> homeServices = new ArrayList<>();
    private final List<FeaturedBarber> featuredBarbers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        firestore = FirebaseFirestore.getInstance();
        serviceRepository = new ServiceRepository();

        setupFeaturedBarbers();
        setupDashboardActions();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHomeServices();
        loadFeaturedBarbers();
    }

    private void setupFeaturedBarbers() {
        RecyclerView recyclerView = findViewById(R.id.recyclerFeaturedBarbers);
        featuredBarberAdapter = new FeaturedBarberAdapter(this::openBarberProfile);

        recyclerView.setLayoutManager(new LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
        ));
        recyclerView.setAdapter(featuredBarberAdapter);
        recyclerView.setHasFixedSize(false);
    }

    private void setupDashboardActions() {
        findViewById(R.id.buttonNotifications).setOnClickListener(view ->
                Toast.makeText(this, R.string.demo_notifications_message, Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.searchBar).setOnClickListener(view -> openServices());
        findViewById(R.id.cardAiBooking).setOnClickListener(view -> openAiBooking());
        findViewById(R.id.buttonTryAi).setOnClickListener(view -> openAiBooking());
        findViewById(R.id.textViewAllAppointments).setOnClickListener(view -> openAppointments());
        findViewById(R.id.cardUpcomingAppointment).setOnClickListener(view -> openUpcomingAppointment());
        findViewById(R.id.textViewAllServices).setOnClickListener(view -> openServices());
        findViewById(R.id.textViewAllBarbers).setOnClickListener(view -> openBarbers());

        setupDefaultServiceClicks();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.homeNavHome).setOnClickListener(view -> scrollToTop());
        findViewById(R.id.homeNavServices).setOnClickListener(view -> openServices());
        findViewById(R.id.homeNavAi).setOnClickListener(view -> openAiBooking());
        findViewById(R.id.homeNavAppointments).setOnClickListener(view -> openAppointments());
        findViewById(R.id.homeNavProfile).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    private void setupDefaultServiceClicks() {
        LinearLayout serviceRow = findViewById(R.id.serviceRow);
        for (int index = 0; index < serviceRow.getChildCount(); index++) {
            serviceRow.getChildAt(index).setOnClickListener(view -> openServices());
        }
    }

    private void loadHomeServices() {
        serviceRepository.getAllServices(new BarberRepository.RepositoryCallback<List<ShopService>>() {
            @Override
            public void onSuccess(List<ShopService> data) {
                homeServices.clear();
                int itemCount = Math.min(data.size(), MAX_HOME_SERVICES);
                for (int index = 0; index < itemCount; index++) {
                    homeServices.add(data.get(index));
                }
                bindHomeServices();
            }

            @Override
            public void onError(Exception exception) {
                showLoadError(R.string.services_load_failed, exception);
            }
        });
    }

    private void bindHomeServices() {
        LinearLayout serviceRow = findViewById(R.id.serviceRow);

        for (int index = 0; index < serviceRow.getChildCount(); index++) {
            View serviceSlot = serviceRow.getChildAt(index);
            if (index >= homeServices.size()) {
                serviceSlot.setVisibility(View.GONE);
                continue;
            }

            serviceSlot.setVisibility(View.VISIBLE);
            bindServiceSlot((LinearLayout) serviceSlot, homeServices.get(index));
        }
    }

    private void bindServiceSlot(LinearLayout serviceSlot, ShopService service) {
        String category = formatCategory(service.getCategory());
        String serviceName = fallback(service.getName(), fallback(category, "Service"));
        String servicePrice = formatPrice(service.getPrice());
        FrameLayout imageContainer = (FrameLayout) serviceSlot.getChildAt(0);
        ImageView imageView = (ImageView) imageContainer.getChildAt(0);
        TextView textName = (TextView) serviceSlot.getChildAt(1);

        imageContainer.setBackgroundResource(getServiceBackground(category));
        imageContainer.setClipToOutline(true);
        configureHomeServiceImage(imageView, service.getImageUrl());
        ImageLoader.loadImage(imageView, service.getImageUrl(), getServiceIcon(category));
        imageView.setContentDescription(getString(R.string.service_image_content_description, serviceName));
        textName.setText(serviceName);
        serviceSlot.setOnClickListener(view ->
                openBooking(serviceName, servicePrice, getPrimaryBarberName())
        );
    }

    private void configureHomeServiceImage(ImageView imageView, String imageUrl) {
        boolean hasRemoteImage = !TextUtils.isEmpty(imageUrl);
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) imageView.getLayoutParams();
        imageView.clearColorFilter();

        if (hasRemoteImage) {
            layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            int iconSize = getResources().getDimensionPixelSize(R.dimen.space_24);
            layoutParams.width = iconSize;
            layoutParams.height = iconSize;
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        imageView.setLayoutParams(layoutParams);
    }

    private void loadFeaturedBarbers() {
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
                        ratingCounts.put(barberId, currentCount + 1L);
                    }

                    loadActiveBarbers(ratingSums, ratingCounts);
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(
                            this,
                            getString(R.string.ratings_load_failed, safeErrorMessage(exception)),
                            Toast.LENGTH_LONG
                    ).show();

                    loadActiveBarbers(new HashMap<>(), new HashMap<>());
                });
    }

    private void loadActiveBarbers(
            Map<Long, Double> ratingSums,
            Map<Long, Long> ratingCounts
    ) {
        firestore.collection(COLLECTION_BARBERS)
                .whereEqualTo(FIELD_ACTIVE, true)
                .get()
                .addOnSuccessListener(barberSnapshot -> {
                    List<FeaturedBarber> barbers = new ArrayList<>();

                    for (QueryDocumentSnapshot document : barberSnapshot) {
                        Long barberId = readLong(document.get(FIELD_BARBER_ID));
                        if (barberId == null || barberId < 1) {
                            continue;
                        }

                        String name = readString(
                                document.get(FIELD_NAME),
                                getString(R.string.barber_name_unknown)
                        );
                        String experience = formatBarberExperience(document.get(FIELD_EXPERIENCE));
                        long ratingCount = ratingCounts.getOrDefault(barberId, 0L);
                        double ratingSum = ratingSums.getOrDefault(barberId, 0.0);
                        double averageRating = ratingCount == 0 ? 0.0 : ratingSum / ratingCount;

                        barbers.add(new FeaturedBarber(
                                barberId,
                                name,
                                experience,
                                averageRating,
                                ratingCount
                        ));
                    }

                    sortFeaturedBarbers(barbers);
                    featuredBarbers.clear();
                    featuredBarbers.addAll(barbers);
                    featuredBarberAdapter.submitList(barbers);
                })
                .addOnFailureListener(exception -> {
                    featuredBarbers.clear();
                    featuredBarberAdapter.submitList(new ArrayList<>());
                    Toast.makeText(
                            this,
                            getString(
                                    R.string.home_featured_barbers_load_failed,
                                    safeErrorMessage(exception)
                            ),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void sortFeaturedBarbers(List<FeaturedBarber> barbers) {
        barbers.sort((first, second) -> {
            int ratingComparison = Double.compare(second.averageRating, first.averageRating);
            if (ratingComparison != 0) {
                return ratingComparison;
            }

            int countComparison = Long.compare(second.ratingCount, first.ratingCount);
            if (countComparison != 0) {
                return countComparison;
            }

            return first.name.compareToIgnoreCase(second.name);
        });
    }

    private void openBarberProfile(FeaturedBarber barber) {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("barberId", barber.barberId);
        startActivity(intent);
    }

    private void openBooking(String serviceName, String servicePrice, String barberName) {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("selectedServiceName", serviceName);
        intent.putExtra("selectedServicePrice", servicePrice);
        intent.putExtra("selectedBarberName", barberName);
        startActivity(intent);
    }

    private void openUpcomingAppointment() {
        Intent intent = new Intent(this, AppointmentDetailActivity.class);
        intent.putExtra("appointmentId", "#AB25678");
        intent.putExtra("appointmentStatus", "Upcoming");
        intent.putExtra("barberName", "Michael");
        intent.putExtra("barberExperience", "9+ years experience");
        intent.putExtra("barberSpecialty", "Specialty: Classic Cut");
        intent.putExtra("serviceName", "Haircut, Classic Cut");
        intent.putExtra("appointmentPrice", "$25.00");
        intent.putExtra("appointmentDate", "Sat, May 24, 2025");
        intent.putExtra("appointmentStartTime", "11:00 AM");
        intent.putExtra("appointmentEndTime", "11:45 AM");
        intent.putExtra("appointmentDuration", "45 min");
        intent.putExtra("paymentStatus", getString(R.string.appointment_payment_paid));
        intent.putExtra("paymentMethod", "Card **** 4567");
        startActivity(intent);
    }

    private void openServices() {
        startActivity(new Intent(this, ServiceListActivity.class));
    }

    private void openBarbers() {
        startActivity(new Intent(this, BarberListActivity.class));
    }

    private void openAppointments() {
        startActivity(new Intent(this, AppointmentActivity.class));
    }

    private void openAiBooking() {
        startActivity(new Intent(this, AIChatBookingActivity.class));
    }

    private void scrollToTop() {
        ((ScrollView) findViewById(R.id.homeScroll)).smoothScrollTo(0, 0);
    }

    private String getPrimaryBarberName() {
        return featuredBarbers.isEmpty() ? null : featuredBarbers.get(0).name;
    }

    private Long readLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private Double readDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private String readString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private String formatBarberExperience(Object value) {
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

        return getString(R.string.barber_experience_format, experience);
    }

    private String formatPrice(double price) {
        return String.format(Locale.US, "$%.2f", price);
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

    private void showLoadError(int messageRes, Exception exception) {
        String message = safeErrorMessage(exception);
        Toast.makeText(this, getString(messageRes, message), Toast.LENGTH_SHORT).show();
    }

    private String safeErrorMessage(Exception exception) {
        if (exception == null
                || exception.getMessage() == null
                || exception.getMessage().trim().isEmpty()) {
            return getString(R.string.unknown_error);
        }

        return exception.getMessage();
    }

    private String getInitial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.home_featured_initial_placeholder);
        }

        return name.trim().substring(0, 1).toUpperCase(Locale.US);
    }

    private interface OnFeaturedBarberClickListener {
        void onClick(FeaturedBarber barber);
    }

    private static class FeaturedBarber {
        final long barberId;
        final String name;
        final String experience;
        final double averageRating;
        final long ratingCount;

        FeaturedBarber(
                long barberId,
                String name,
                String experience,
                double averageRating,
                long ratingCount
        ) {
            this.barberId = barberId;
            this.name = name;
            this.experience = experience;
            this.averageRating = averageRating;
            this.ratingCount = ratingCount;
        }
    }

    private class FeaturedBarberAdapter
            extends RecyclerView.Adapter<FeaturedBarberAdapter.FeaturedBarberViewHolder> {
        private final List<FeaturedBarber> barbers = new ArrayList<>();
        private final OnFeaturedBarberClickListener listener;

        FeaturedBarberAdapter(OnFeaturedBarberClickListener listener) {
            this.listener = listener;
        }

        void submitList(List<FeaturedBarber> nextBarbers) {
            barbers.clear();
            if (nextBarbers != null) {
                barbers.addAll(nextBarbers);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FeaturedBarberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_featured_barber, parent, false);
            return new FeaturedBarberViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FeaturedBarberViewHolder holder, int position) {
            holder.bind(barbers.get(position));
        }

        @Override
        public int getItemCount() {
            return barbers.size();
        }

        private class FeaturedBarberViewHolder extends RecyclerView.ViewHolder {
            private final TextView initialView;
            private final TextView nameView;
            private final TextView experienceView;
            private final TextView ratingView;

            FeaturedBarberViewHolder(@NonNull View itemView) {
                super(itemView);
                initialView = itemView.findViewById(R.id.textFeaturedBarberInitial);
                nameView = itemView.findViewById(R.id.textFeaturedBarberName);
                experienceView = itemView.findViewById(R.id.textFeaturedBarberExperience);
                ratingView = itemView.findViewById(R.id.textFeaturedBarberRating);
            }

            void bind(FeaturedBarber barber) {
                initialView.setText(getInitial(barber.name));
                nameView.setText(barber.name);
                experienceView.setText(barber.experience);

                if (barber.ratingCount == 0) {
                    ratingView.setText(R.string.barber_no_ratings);
                } else {
                    ratingView.setText(getString(
                            R.string.home_featured_rating_format,
                            barber.averageRating
                    ));
                }

                itemView.setOnClickListener(view -> {
                    if (listener != null) {
                        listener.onClick(barber);
                    }
                });
            }
        }
    }
}
