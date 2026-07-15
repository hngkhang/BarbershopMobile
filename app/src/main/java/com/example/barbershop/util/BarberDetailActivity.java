package com.example.barbershop.util;

import com.example.barbershop.R;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barbershop.adapters.BarberScheduleAdapter;
import com.example.barbershop.adapters.FeedbackAdapter;
import com.example.barbershop.data.BarberRepository;
import com.example.barbershop.models.Barber;
import com.example.barbershop.models.BarberSchedule;
import com.example.barbershop.models.Feedback;
import com.example.barbershop.models.Rating;
import com.example.barbershop.models.UserProfile;
import com.example.barbershop.services.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarberDetailActivity extends AppCompatActivity {
    private BarberRepository barberRepository;
    private BarberScheduleAdapter scheduleAdapter;
    private FeedbackAdapter feedbackAdapter;

    private View loadingState;
    private TextView errorState;
    private View contentState;
    private View emptyScheduleState;
    private View emptyFeedbackState;

    private ImageView imageBarberAvatar;
    private TextView textBarberInitial;
    private TextView textBarberName;
    private TextView textBarberExperience;
    private TextView textBarberRating;
    private TextView textBarberTotalReviews;
    private final List<Feedback> currentFeedbacks = new ArrayList<>();
    private final List<Rating> currentRatings = new ArrayList<>();
    private final List<UserProfile> currentUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_detail);

        barberRepository = new BarberRepository();
        bindViews();
        setupTopBar();
        setupLists();
        loadBarberDetail();
    }

    private void bindViews() {
        loadingState = findViewById(R.id.layoutBarberDetailLoading);
        errorState = findViewById(R.id.textBarberDetailError);
        contentState = findViewById(R.id.barberDetailContent);
        emptyScheduleState = findViewById(R.id.textScheduleEmpty);
        emptyFeedbackState = findViewById(R.id.textFeedbackEmpty);

        imageBarberAvatar = findViewById(R.id.imageDetailBarberAvatar);
        textBarberInitial = findViewById(R.id.textDetailBarberInitial);
        textBarberName = findViewById(R.id.textDetailBarberName);
        textBarberExperience = findViewById(R.id.textDetailBarberExperience);
        textBarberRating = findViewById(R.id.textDetailBarberRating);
        textBarberTotalReviews = findViewById(R.id.textDetailBarberTotalReviews);
    }

    private void setupTopBar() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonNotifications).setOnClickListener(v ->
                Toast.makeText(this, R.string.demo_notifications_message, Toast.LENGTH_SHORT).show()
        );
    }

    private void setupLists() {
        RecyclerView recyclerSchedules = findViewById(R.id.recyclerBarberSchedules);
        scheduleAdapter = new BarberScheduleAdapter();
        recyclerSchedules.setLayoutManager(new LinearLayoutManager(this));
        recyclerSchedules.setAdapter(scheduleAdapter);
        recyclerSchedules.setNestedScrollingEnabled(false);

        RecyclerView recyclerFeedbacks = findViewById(R.id.recyclerBarberFeedbacks);
        feedbackAdapter = new FeedbackAdapter();
        recyclerFeedbacks.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeedbacks.setAdapter(feedbackAdapter);
        recyclerFeedbacks.setNestedScrollingEnabled(false);
    }

    private void loadBarberDetail() {
        String barberId = getIntent().getStringExtra(BarberListActivity.EXTRA_BARBER_ID);
        if (barberId == null || barberId.trim().isEmpty()) {
            showError(getString(R.string.barber_not_found));
            return;
        }

        showLoading();
        barberRepository.getBarberById(barberId, new BarberRepository.RepositoryCallback<Barber>() {
            @Override
            public void onSuccess(Barber barber) {
                if (barber == null) {
                    showError(getString(R.string.barber_not_found));
                    return;
                }

                bindBarber(barber);
                showContent();
                loadSchedule(barber.getBarberId());
                loadFeedbacks(barber);
                loadRatings(barber);
            }

            @Override
            public void onError(Exception exception) {
                showError(getErrorMessage(exception));
            }
        });
    }

    private void bindBarber(Barber barber) {
        ImageLoader.loadAvatar(
                imageBarberAvatar,
                textBarberInitial,
                barber.getAvatarUrl(),
                barber.getInitial()
        );
        textBarberInitial.setContentDescription(
                getString(R.string.barber_avatar_content_description, barber.getName())
        );
        textBarberName.setText(barber.getName());
        textBarberExperience.setText(formatExperience(barber.getExperience()));
        textBarberRating.setText(String.format(Locale.US, "%.1f", barber.getDisplayRating()));
        textBarberTotalReviews.setText(getString(R.string.barber_total_reviews_format, 0));
    }

    private void loadSchedule(String barberId) {
        if (barberId == null || barberId.trim().isEmpty()) {
            scheduleAdapter.submitList(new ArrayList<>());
            emptyScheduleState.setVisibility(View.VISIBLE);
            return;
        }

        barberRepository.getBarberSchedule(barberId, new BarberRepository.RepositoryCallback<List<BarberSchedule>>() {
            @Override
            public void onSuccess(List<BarberSchedule> data) {
                scheduleAdapter.submitList(data);
                emptyScheduleState.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception exception) {
                emptyScheduleState.setVisibility(View.VISIBLE);
                ((TextView) emptyScheduleState).setText(getString(R.string.barber_schedule_load_failed));
            }
        });
    }

    private void loadFeedbacks(Barber barber) {
        barberRepository.getBarberReviews(barber, new BarberRepository.RepositoryCallback<List<Feedback>>() {
            @Override
            public void onSuccess(List<Feedback> data) {
                currentFeedbacks.clear();
                currentFeedbacks.addAll(data);
                bindFeedbackDetails();
                loadReviewUsers();
            }

            @Override
            public void onError(Exception exception) {
                emptyFeedbackState.setVisibility(View.VISIBLE);
                ((TextView) emptyFeedbackState).setText(getString(R.string.barber_feedback_load_failed));
            }
        });
    }

    private void loadRatings(Barber barber) {
        barberRepository.getBarberRatings(barber, new BarberRepository.RepositoryCallback<List<Rating>>() {
            @Override
            public void onSuccess(List<Rating> data) {
                currentRatings.clear();
                currentRatings.addAll(data);
                bindRatingSummary(data);
                bindFeedbackDetails();
            }

            @Override
            public void onError(Exception exception) {
                textBarberRating.setText(String.format(Locale.US, "%.1f", barber.getRating()));
                textBarberTotalReviews.setText(getString(R.string.barber_total_reviews_format, 0));
            }
        });
    }

    private void bindRatingSummary(List<Rating> ratings) {
        if (ratings.isEmpty()) {
            textBarberRating.setText(getString(R.string.barber_rating_empty_value));
            textBarberTotalReviews.setText(getString(R.string.barber_total_reviews_format, 0));
            return;
        }

        double total = 0.0;
        for (Rating rating : ratings) {
            total += rating.getRate();
        }

        double average = total / ratings.size();
        textBarberRating.setText(String.format(Locale.US, "%.1f", average));
        textBarberTotalReviews.setText(getString(R.string.barber_total_reviews_format, ratings.size()));
    }

    private void loadReviewUsers() {
        barberRepository.getUsers(new BarberRepository.RepositoryCallback<List<UserProfile>>() {
            @Override
            public void onSuccess(List<UserProfile> data) {
                currentUsers.clear();
                currentUsers.addAll(data);
                bindFeedbackDetails();
            }

            @Override
            public void onError(Exception exception) {
                bindFeedbackDetails();
            }
        });
    }

    private void bindFeedbackDetails() {
        for (Feedback feedback : currentFeedbacks) {
            feedback.setCustomerName("");
            for (UserProfile user : currentUsers) {
                if (user.matchesUserId(feedback.getUserId())) {
                    feedback.setCustomerName(user.getName());
                    break;
                }
            }

            feedback.setCustomerRating(-1.0);
            for (Rating rating : currentRatings) {
                if (rating.belongsToFeedback(feedback.getFeedbackId())
                        || rating.belongsToUser(feedback.getUserId())) {
                    feedback.setCustomerRating(rating.getRate());
                    break;
                }
            }
        }

        feedbackAdapter.submitList(currentFeedbacks);
        emptyFeedbackState.setVisibility(currentFeedbacks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showLoading() {
        loadingState.setVisibility(View.VISIBLE);
        contentState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        contentState.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        loadingState.setVisibility(View.GONE);
        contentState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        errorState.setText(message);
    }

    private String getErrorMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return getString(R.string.state_error_placeholder);
        }
        return exception.getMessage();
    }

    private String formatExperience(String experience) {
        if (experience == null || experience.trim().isEmpty()) {
            return getString(R.string.barber_experience_unknown);
        }

        String trimmedExperience = experience.trim();
        if (trimmedExperience.toLowerCase(Locale.US).contains("year")) {
            return trimmedExperience;
        }

        return getString(R.string.barber_experience_years_format, trimmedExperience);
    }
}
