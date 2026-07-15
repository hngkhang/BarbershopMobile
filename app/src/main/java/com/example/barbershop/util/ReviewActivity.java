package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ReviewActivity extends AppCompatActivity {

    private final List<TextView> feedbackChips = new ArrayList<>();
    private RatingBar ratingBar;
    private TextView ratingLabel;
    private TextView reviewCounter;
    private EditText reviewEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        bindTemporaryReviewSummary();
        setupRating();
        setupFeedbackChips();
        setupReviewInput();
        setupActions();
    }

    private void bindTemporaryReviewSummary() {
        Intent intent = getIntent();
        String barberName = readStringExtra(intent, "barberName", "Michael");
        String serviceName = readStringExtra(intent, "serviceName", "Haircut, Classic Cut");
        String appointmentDate = readStringExtra(intent, "appointmentDate", "Sat, May 24, 2025");
        String appointmentStartTime = readStringExtra(intent, "appointmentStartTime", "11:00 AM");

        ((TextView) findViewById(R.id.textReviewBarberInitial)).setText(getInitial(barberName));
        findViewById(R.id.textReviewBarberInitial).setContentDescription(
                getString(R.string.barber_avatar_content_description, barberName)
        );
        ((TextView) findViewById(R.id.textReviewBarberName)).setText(barberName);
        ((TextView) findViewById(R.id.textReviewService)).setText(serviceName);
        ((TextView) findViewById(R.id.textReviewDate)).setText(
                String.format(Locale.US, "%s at %s", appointmentDate, appointmentStartTime)
        );
        // TODO: Replace this temporary review summary with the selected completed appointment data.
    }

    private void setupRating() {
        ratingBar = findViewById(R.id.ratingBarExperience);
        ratingLabel = findViewById(R.id.textRatingLabel);
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (rating >= 4) {
                ratingLabel.setText(R.string.review_rating_label_good);
            } else if (rating > 0) {
                ratingLabel.setText(String.format(Locale.US, "%.0f / 5", rating));
            } else {
                ratingLabel.setText(R.string.review_rating_label_default);
            }
        });
    }

    private void setupFeedbackChips() {
        feedbackChips.addAll(Arrays.asList(
                findViewById(R.id.chipFriendly),
                findViewById(R.id.chipProfessional),
                findViewById(R.id.chipOnTime),
                findViewById(R.id.chipGoodHaircut),
                findViewById(R.id.chipCleanShop),
                findViewById(R.id.chipGreatService)
        ));

        for (TextView chip : feedbackChips) {
            chip.setTag(false);
            chip.setOnClickListener(v -> {
                boolean selected = !(Boolean) chip.getTag();
                chip.setTag(selected);
                chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
                chip.setTextColor(ContextCompat.getColor(
                        this,
                        selected ? R.color.color_text_inverse : R.color.color_text_primary
                ));
                chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            });
        }
    }

    private void setupReviewInput() {
        reviewEditText = findViewById(R.id.editTextReview);
        reviewCounter = findViewById(R.id.textReviewCounter);
        reviewEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                reviewCounter.setText(getString(R.string.review_character_counter_format, s.length()));
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });
    }

    private void setupActions() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonSubmitReview).setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        if (ratingBar.getRating() <= 0) {
            Toast.makeText(this, R.string.review_validation_rating, Toast.LENGTH_SHORT).show();
            return;
        }

        // Demo-only success state. Replace with Firebase persistence when review storage is implemented.
        Toast.makeText(this, R.string.review_submit_demo, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String readStringExtra(Intent intent, String key, String fallback) {
        String value = intent.getStringExtra(key);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String getInitial(String value) {
        return value == null || value.trim().isEmpty()
                ? "A"
                : value.trim().substring(0, 1).toUpperCase(Locale.US);
    }
}
