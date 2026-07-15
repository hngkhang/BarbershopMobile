package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private static final String COLLECTION_BARBERS = "barbers";
    private static final String COLLECTION_RATINGS = "ratings";

    private static final String FIELD_BARBER_ID = "barberId";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_EXPERIENCE = "experience";
    private static final String FIELD_ACTIVE = "active";
    private static final String FIELD_RATE = "rate";

    private FirebaseFirestore firestore;
    private FeaturedBarberAdapter featuredBarberAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        firestore = FirebaseFirestore.getInstance();

        setupFeaturedBarbers();
        setupDashboardActions();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Load lại Featured Barbers khi quay về Home.
         * Rating mới vừa gửi trong ReviewActivity
         * sẽ được cập nhật trên trang chủ.
         */
        loadFeaturedBarbers();
    }

    /**
     * Khởi tạo RecyclerView nằm ngang.
     */
    private void setupFeaturedBarbers() {
        RecyclerView recyclerView = findViewById(R.id.recyclerFeaturedBarbers);

        featuredBarberAdapter = new FeaturedBarberAdapter(this::openBarberProfile);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.HORIZONTAL,
                        false
                )
        );

        recyclerView.setAdapter(featuredBarberAdapter);
        recyclerView.setHasFixedSize(false);
    }

    /**
     * Giữ nguyên các chức năng hiện tại của Home.
     * Phần Featured Barbers không còn hardcode tại đây.
     */
    private void setupDashboardActions() {
        findViewById(R.id.buttonNotifications).setOnClickListener(view ->
                Toast.makeText(
                        this,
                        R.string.demo_notifications_message,
                        Toast.LENGTH_SHORT
                ).show()
        );

        findViewById(R.id.searchBar).setOnClickListener(view -> openServices());
        findViewById(R.id.cardAiBooking).setOnClickListener(view -> openAiBooking());
        findViewById(R.id.buttonTryAi).setOnClickListener(view -> openAiBooking());

        findViewById(R.id.textViewAllAppointments).setOnClickListener(
                view -> openAppointments()
        );

        findViewById(R.id.cardUpcomingAppointment).setOnClickListener(
                view -> openUpcomingAppointment()
        );

        findViewById(R.id.textViewAllServices).setOnClickListener(
                view -> openServices()
        );

        /*
         * Các service này vẫn giữ logic hiện tại.
         * Chưa thay đổi vì phạm vi đang xử lý
         * chỉ là Featured Barbers.
         */
        findViewById(R.id.serviceHaircut).setOnClickListener(
                view -> openBooking("Haircut", "$25.00", "Michael")
        );

        findViewById(R.id.serviceShampoo).setOnClickListener(
                view -> openBooking("Shampoo", "$12.00", "Michael")
        );

        findViewById(R.id.servicePerm).setOnClickListener(
                view -> openBooking("Perm", "$60.00", "Sophia")
        );

        findViewById(R.id.serviceColoring).setOnClickListener(
                view -> openBooking("Coloring", "$50.00", "Sophia")
        );

        findViewById(R.id.serviceCombo).setOnClickListener(
                view -> openBooking("Combo Package", "$55.00", "David")
        );

        findViewById(R.id.textViewAllBarbers).setOnClickListener(
                view -> openBarbers()
        );
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

    /**
     * Bước 1:
     * Đọc collection ratings và gom dữ liệu theo barberId.
     */
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
                    /*
                     * Nếu collection ratings chưa đọc được,
                     * vẫn hiển thị barber nhưng rating tạm thời
                     * được coi là chưa có.
                     */
                    Toast.makeText(
                            this,
                            getString(
                                    R.string.ratings_load_failed,
                                    safeErrorMessage(exception)
                            ),
                            Toast.LENGTH_LONG
                    ).show();

                    loadActiveBarbers(new HashMap<>(), new HashMap<>());
                });
    }

    /**
     * Bước 2:
     * Load tất cả barber đang active từ Firestore.
     */
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

                        /*
                         * Bỏ qua document không có barberId hợp lệ.
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

                        long ratingCount = ratingCounts.getOrDefault(barberId, 0L);
                        double ratingSum = ratingSums.getOrDefault(barberId, 0.0);
                        double averageRating = ratingCount == 0
                                ? 0.0
                                : ratingSum / ratingCount;

                        barbers.add(new FeaturedBarber(
                                barberId,
                                name,
                                experience,
                                averageRating,
                                ratingCount
                        ));
                    }

                    sortFeaturedBarbers(barbers);

                    /*
                     * Không giới hạn ba barber.
                     * RecyclerView sẽ hiển thị toàn bộ barber active.
                     */
                    featuredBarberAdapter.submitList(barbers);
                })
                .addOnFailureListener(exception -> {
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

    /**
     * Barber có rating cao hơn sẽ đứng trước.
     *
     * Nếu rating bằng nhau:
     * - Barber có nhiều lượt đánh giá hơn đứng trước.
     * - Sau đó sắp xếp theo tên.
     */
    private void sortFeaturedBarbers(List<FeaturedBarber> barbers) {
        barbers.sort((first, second) -> {
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
    }

    /**
     * Mở profile barber để người dùng xem,
     * rate và gửi feedback.
     */
    private void openBarberProfile(FeaturedBarber barber) {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("barberId", barber.barberId);
        startActivity(intent);
    }

    private void openBooking(
            String serviceName,
            String servicePrice,
            String barberName
    ) {
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
        intent.putExtra(
                "paymentStatus",
                getString(R.string.appointment_payment_paid)
        );
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
        ScrollView homeScroll = findViewById(R.id.homeScroll);
        homeScroll.smoothScrollTo(0, 0);
    }

    private Long readLong(Object value) {
        return value instanceof Number
                ? ((Number) value).longValue()
                : null;
    }

    private Double readDouble(Object value) {
        return value instanceof Number
                ? ((Number) value).doubleValue()
                : null;
    }

    private String readString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    /**
     * Database hiện tại lưu experience dưới dạng:
     *
     * "5"
     * "3"
     * "7"
     *
     * Hàm chuyển thành:
     *
     * "5 years experience"
     */
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

    private String getInitial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.home_featured_initial_placeholder);
        }

        return name.trim()
                .substring(0, 1)
                .toUpperCase(Locale.US);
    }

    private String safeErrorMessage(Exception exception) {
        if (exception == null
                || exception.getMessage() == null
                || exception.getMessage().trim().isEmpty()) {
            return getString(R.string.unknown_error);
        }

        return exception.getMessage();
    }

    /**
     * Listener nội bộ cho Featured Barber.
     */
    private interface OnFeaturedBarberClickListener {
        void onClick(FeaturedBarber barber);
    }

    /**
     * Model nội bộ.
     * Không cần tạo thêm file Java.
     */
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

    /**
     * Adapter nội bộ cho RecyclerView Featured Barbers.
     * Không cần tạo FeaturedBarberAdapter.java riêng.
     */
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
        public FeaturedBarberViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.item_featured_barber,
                            parent,
                            false
                    );

            return new FeaturedBarberViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull FeaturedBarberViewHolder holder,
                int position
        ) {
            holder.bind(barbers.get(position));
        }

        @Override
        public int getItemCount() {
            return barbers.size();
        }

        private class FeaturedBarberViewHolder
                extends RecyclerView.ViewHolder {

            private final TextView initialView;
            private final TextView nameView;
            private final TextView experienceView;
            private final TextView ratingView;

            FeaturedBarberViewHolder(@NonNull View itemView) {
                super(itemView);

                initialView = itemView.findViewById(
                        R.id.textFeaturedBarberInitial
                );

                nameView = itemView.findViewById(
                        R.id.textFeaturedBarberName
                );

                experienceView = itemView.findViewById(
                        R.id.textFeaturedBarberExperience
                );

                ratingView = itemView.findViewById(
                        R.id.textFeaturedBarberRating
                );
            }

            void bind(FeaturedBarber barber) {
                initialView.setText(getInitial(barber.name));
                nameView.setText(barber.name);
                experienceView.setText(barber.experience);

                if (barber.ratingCount == 0) {
                    ratingView.setText(
                            R.string.barber_no_ratings
                    );
                } else {
                    ratingView.setText(
                            getString(
                                    R.string.home_featured_rating_format,
                                    barber.averageRating
                            )
                    );
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