package com.example.barbershop;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReviewActivity extends AppCompatActivity {
    private static final String COLLECTION_BARBERS = "barbers";
    private static final String COLLECTION_RATINGS = "ratings";
    private static final String COLLECTION_FEEDBACKS = "feedbacks";
    private static final long INVALID_BARBER_ID = -1L;
    private static final int MAX_CREATE_ATTEMPTS = 3;
    private final List<TextView> feedbackChips = new ArrayList<>();
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private long barberId = INVALID_BARBER_ID;
    private boolean barberLoaded;
    private boolean submitting;
    private RatingBar ratingBar;
    private TextView ratingLabel;
    private TextView reviewCounter;
    private EditText reviewEditText;
    private AppCompatButton submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        barberId = getIntent().getLongExtra("barberId", INVALID_BARBER_ID);
        setupBarberProfileHeader();
        setupRating();
        setupFeedbackChips();
        setupReviewInput();
        setupActions();
        if (barberId < 1) {
            Toast.makeText(
                    this,
                    "Cannot open barber profile because barberId is missing.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }
        loadBarberProfile();
    }
    private void setupBarberProfileHeader() {
        TextView titleView = findViewById(R.id.textReviewTitle);
        TextView nameView = findViewById(R.id.textReviewBarberName);
        TextView experienceView = findViewById(R.id.textReviewService);
        TextView averageRatingView = findViewById(R.id.textReviewDate);
        titleView.setText("Barber Profile");
        nameView.setText("Loading...");
        experienceView.setText("Loading...");
        averageRatingView.setText("");
    }
    private void loadBarberProfile() {
        barberLoaded = false;
        setSubmitEnabled(false);
        firestore.collection(COLLECTION_BARBERS)
                .whereEqualTo(
                        "barberId", barberId
                )
                .limit(1)
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {
                            if (querySnapshot.isEmpty()) {
                                Toast.makeText(
                                        this,
                                        "Barber does not exist.",
                                        Toast.LENGTH_LONG
                                ).show();
                                finish();
                                return;
                            }
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                            bindBarberInformation(document);
                            loadBarberRatingSummary();
                        }
                )
                .addOnFailureListener(
                        exception -> {
                            Toast.makeText(
                                    this,
                                    "Cannot load barber: "
                                            + safeErrorMessage(
                                            exception
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                            finish();
                        }
                );
    }
    private void bindBarberInformation(DocumentSnapshot document) {
        String barberName = readText(document.get("name"), "Unnamed barber");
        String experience = formatExperience(document.get("experience"));
        TextView initialView = findViewById(R.id.textReviewBarberInitial);
        TextView nameView = findViewById(R.id.textReviewBarberName);
        TextView experienceView = findViewById(R.id.textReviewService);
        initialView.setText(getInitial(barberName));
        initialView.setContentDescription(
                getString(
                        R.string.barber_avatar_content_description,
                        barberName
                )
        );
        nameView.setText(barberName);
        experienceView.setText(experience);
    }
    private void loadBarberRatingSummary() {
        firestore.collection(COLLECTION_RATINGS)
                .whereEqualTo(
                        "barberId",
                        barberId
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {
                            double totalRate = 0.0;
                            long ratingCount = 0L;
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                Double rate = readDouble(document.get("rate"));
                                if (rate == null || rate <= 0 || rate > 5) continue;
                                totalRate += rate;
                                ratingCount++;
                            }
                            displayAverageRating(totalRate, ratingCount);
                            barberLoaded = true;
                            setSubmitEnabled(true);
                        }
                )
                .addOnFailureListener(
                        exception -> {
                            /*
                             * Barber vẫn được load thành công.
                             * Nếu chưa đọc được ratings thì vẫn
                             * cho phép người dùng gửi đánh giá.
                             */
                            TextView averageRatingView =
                                    findViewById(
                                            R.id.textReviewDate
                                    );

                            averageRatingView.setText(
                                    "Cannot load average rating"
                            );

                            barberLoaded = true;
                            setSubmitEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Cannot load ratings: "
                                            + safeErrorMessage(
                                            exception
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void displayAverageRating(
            double totalRate,
            long ratingCount
    ) {
        TextView averageRatingView =
                findViewById(
                        R.id.textReviewDate
                );

        if (ratingCount == 0) {
            averageRatingView.setText(
                    "No ratings yet"
            );

            return;
        }

        double averageRating =
                totalRate / ratingCount;

        averageRatingView.setText(
                String.format(
                        Locale.US,
                        "Average rating: %.1f/5 (%d ratings)",
                        averageRating,
                        ratingCount
                )
        );
    }

    private void setupRating() {
        ratingBar =
                findViewById(
                        R.id.ratingBarExperience
                );

        ratingLabel =
                findViewById(
                        R.id.textRatingLabel
                );

        ratingBar.setOnRatingBarChangeListener(
                (
                        bar,
                        rating,
                        fromUser
                ) -> {
                    if (rating >= 4) {
                        ratingLabel.setText(
                                R.string.review_rating_label_good
                        );
                    } else if (rating > 0) {
                        ratingLabel.setText(
                                String.format(
                                        Locale.US,
                                        "%.0f / 5",
                                        rating
                                )
                        );
                    } else {
                        ratingLabel.setText(
                                R.string.review_rating_label_default
                        );
                    }
                }
        );
    }

    private void setupFeedbackChips() {
        feedbackChips.addAll(
                Arrays.asList(
                        findViewById(
                                R.id.chipFriendly
                        ),
                        findViewById(
                                R.id.chipProfessional
                        ),
                        findViewById(
                                R.id.chipOnTime
                        ),
                        findViewById(
                                R.id.chipGoodHaircut
                        ),
                        findViewById(
                                R.id.chipCleanShop
                        ),
                        findViewById(
                                R.id.chipGreatService
                        )
                )
        );

        for (TextView chip : feedbackChips) {
            chip.setTag(false);

            chip.setOnClickListener(view -> {
                if (submitting) {
                    return;
                }

                boolean selected =
                        !Boolean.TRUE.equals(
                                chip.getTag()
                        );

                chip.setTag(selected);

                applyChipStyle(
                        chip,
                        selected
                );
            });
        }
    }

    private void applyChipStyle(
            TextView chip,
            boolean selected
    ) {
        chip.setBackgroundResource(
                selected
                        ? R.drawable.bg_chip_selected
                        : R.drawable.bg_chip_unselected
        );

        chip.setTextColor(
                ContextCompat.getColor(
                        this,
                        selected
                                ? R.color.color_text_inverse
                                : R.color.color_text_primary
                )
        );

        chip.setTypeface(
                null,
                selected
                        ? Typeface.BOLD
                        : Typeface.NORMAL
        );
    }

    private void setupReviewInput() {
        reviewEditText =
                findViewById(
                        R.id.editTextReview
                );

        reviewCounter =
                findViewById(
                        R.id.textReviewCounter
                );

        reviewEditText.addTextChangedListener(
                new TextWatcher() {

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
                        reviewCounter.setText(
                                getString(
                                        R.string.review_character_counter_format,
                                        text.length()
                                )
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        // Không xử lý.
                    }
                }
        );
    }

    private void setupActions() {
        submitButton =
                findViewById(
                        R.id.buttonSubmitReview
                );

        findViewById(
                R.id.buttonBack
        ).setOnClickListener(
                view -> {
                    if (!submitting) {
                        finish();
                    }
                }
        );

        submitButton.setOnClickListener(
                view -> submitReview()
        );

        setSubmitEnabled(false);
    }

    private void submitReview() {
        if (
                submitting
                        || !barberLoaded
        ) {
            return;
        }

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Please sign in before submitting a review.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        double rate =
                ratingBar.getRating();

        if (
                rate <= 0
                        || rate > 5
        ) {
            Toast.makeText(
                    this,
                    R.string.review_validation_rating,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String feedbackContent =
                buildFeedbackContent();

        /*
         * EditText cho phép 300 ký tự,
         * nhưng nội dung chip cũng được ghép vào
         * content nên cần kiểm tra lại tổng độ dài.
         */
        if (feedbackContent.length() > 300) {
            Toast.makeText(
                    this,
                    "Feedback must not exceed 300 characters.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        setSubmitting(true);

        createReviewWithRetry(
                currentUser.getUid(),
                rate,
                feedbackContent,
                1
        );
    }

    /**
     * Lấy ratingId lớn nhất hiện có.
     * Nếu collection ratings chưa tồn tại
     * hoặc chưa có document, latestRatingId = 0.
     */
    private void createReviewWithRetry(
            String userId,
            double rate,
            String feedbackContent,
            int attempt
    ) {
        loadLatestId(
                COLLECTION_RATINGS,
                "ratingId",
                latestRatingId -> {
                    if (feedbackContent.isEmpty()) {
                        runCreateReviewTransaction(
                                userId,
                                rate,
                                feedbackContent,
                                latestRatingId,
                                0L,
                                attempt
                        );

                        return;
                    }

                    loadLatestId(
                            COLLECTION_FEEDBACKS,
                            "feedbackId",
                            latestFeedbackId ->
                                    runCreateReviewTransaction(
                                            userId,
                                            rate,
                                            feedbackContent,
                                            latestRatingId,
                                            latestFeedbackId,
                                            attempt
                                    ),
                            exception ->
                                    handleSubmitFailure(
                                            exception
                                    )
                    );
                },
                exception ->
                        handleSubmitFailure(
                                exception
                        )
        );
    }

    /**
     * Lấy ID lớn nhất theo field:
     *
     * ratingId hoặc feedbackId.
     */
    private void loadLatestId(
            String collectionName,
            String fieldName,
            OnSuccessListener<Long> successListener,
            OnFailureListener failureListener
    ) {
        firestore.collection(
                        collectionName
                )
                .orderBy(
                        fieldName,
                        Query.Direction.DESCENDING
                )
                .limit(1)
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {
                            if (querySnapshot.isEmpty()) {
                                successListener.onSuccess(
                                        0L
                                );

                                return;
                            }

                            DocumentSnapshot document =
                                    querySnapshot
                                            .getDocuments()
                                            .get(0);

                            Long latestId =
                                    readLong(
                                            document.get(
                                                    fieldName
                                            )
                                    );

                            successListener.onSuccess(
                                    latestId == null
                                            ? 0L
                                            : latestId
                            );
                        }
                )
                .addOnFailureListener(
                        failureListener
                );
    }

    /**
     * Tạo rating và feedback trong cùng
     * một Firestore transaction.
     *
     * Rating luôn được tạo.
     * Feedback chỉ được tạo khi content
     * không rỗng.
     */
    private void runCreateReviewTransaction(
            String userId,
            double rate,
            String feedbackContent,
            long latestRatingId,
            long latestFeedbackId,
            int attempt
    ) {
        long nextRatingId =
                latestRatingId + 1L;

        long nextFeedbackId =
                latestFeedbackId + 1L;

        DocumentReference ratingReference =
                firestore.collection(
                                COLLECTION_RATINGS
                        )
                        .document(
                                formatRatingDocumentId(
                                        nextRatingId
                                )
                        );

        DocumentReference feedbackReference =
                feedbackContent.isEmpty()
                        ? null
                        : firestore.collection(
                                COLLECTION_FEEDBACKS
                        )
                        .document(
                                formatFeedbackDocumentId(
                                        nextFeedbackId
                                )
                        );

        firestore.runTransaction(
                transaction -> {
                    /*
                     * Tất cả thao tác đọc phải được
                     * thực hiện trước thao tác ghi.
                     */
                    DocumentSnapshot existingRating =
                            transaction.get(
                                    ratingReference
                            );

                    DocumentSnapshot existingFeedback =
                            null;

                    if (feedbackReference != null) {
                        existingFeedback =
                                transaction.get(
                                        feedbackReference
                                );
                    }

                    /*
                     * Nếu ID đã được thiết bị khác tạo,
                     * dừng transaction và thử lấy ID mới.
                     */
                    if (existingRating.exists()) {
                        throw new IdConflictException();
                    }

                    if (
                            existingFeedback != null
                                    && existingFeedback.exists()
                    ) {
                        throw new IdConflictException();
                    }

                    Map<String, Object> ratingData =
                            new HashMap<>();

                    ratingData.put(
                            "ratingId",
                            nextRatingId
                    );

                    ratingData.put(
                            "userId",
                            userId
                    );

                    ratingData.put(
                            "barberId",
                            barberId
                    );

                    ratingData.put(
                            "rate",
                            rate
                    );

                    ratingData.put(
                            "createdAt",
                            FieldValue.serverTimestamp()
                    );

                    transaction.set(
                            ratingReference,
                            ratingData
                    );

                    if (feedbackReference != null) {
                        Map<String, Object> feedbackData =
                                new HashMap<>();

                        feedbackData.put(
                                "feedbackId",
                                nextFeedbackId
                        );

                        feedbackData.put(
                                "userId",
                                userId
                        );

                        feedbackData.put(
                                "barberId",
                                barberId
                        );

                        feedbackData.put(
                                "content",
                                feedbackContent
                        );

                        feedbackData.put(
                                "createdAt",
                                FieldValue.serverTimestamp()
                        );

                        transaction.set(
                                feedbackReference,
                                feedbackData
                        );
                    }

                    return null;
                }
        ).addOnSuccessListener(
                unused -> {
                    setSubmitting(false);

                    Toast.makeText(
                            this,
                            "Your rating and feedback were submitted.",
                            Toast.LENGTH_SHORT
                    ).show();

                    setResult(
                            RESULT_OK
                    );

                    /*
                     * BarberListActivity.onResume()
                     * sẽ load lại rating trung bình.
                     */
                    finish();
                }
        ).addOnFailureListener(
                exception -> {
                    if (
                            isIdConflict(exception)
                                    && attempt
                                    < MAX_CREATE_ATTEMPTS
                    ) {
                        /*
                         * Có thiết bị khác vừa sử dụng ID.
                         * Đọc lại ID lớn nhất rồi thử lại.
                         */
                        createReviewWithRetry(
                                userId,
                                rate,
                                feedbackContent,
                                attempt + 1
                        );

                        return;
                    }

                    handleSubmitFailure(
                            exception
                    );
                }
        );
    }

    /**
     * Tạo tên document:
     *
     * 1  -> rating01
     * 2  -> rating02
     * 10 -> rating10
     */
    private String formatRatingDocumentId(
            long ratingId
    ) {
        return String.format(
                Locale.US,
                "rating%02d",
                ratingId
        );
    }

    /**
     * Tạo tên document:
     *
     * 1  -> fb01
     * 2  -> fb02
     * 10 -> fb10
     */
    private String formatFeedbackDocumentId(
            long feedbackId
    ) {
        return String.format(
                Locale.US,
                "fb%02d",
                feedbackId
        );
    }

    private String buildFeedbackContent() {
        List<String> selectedLabels =
                new ArrayList<>();

        for (TextView chip : feedbackChips) {
            if (
                    Boolean.TRUE.equals(
                            chip.getTag()
                    )
            ) {
                selectedLabels.add(
                        chip.getText()
                                .toString()
                                .trim()
                );
            }
        }

        String typedContent =
                reviewEditText.getText() == null
                        ? ""
                        : reviewEditText
                        .getText()
                        .toString()
                        .trim();

        String chipContent =
                String.join(
                        ", ",
                        selectedLabels
                );

        if (chipContent.isEmpty()) {
            return typedContent;
        }

        if (typedContent.isEmpty()) {
            return chipContent;
        }

        return chipContent
                + " - "
                + typedContent;
    }

    private void setSubmitting(
            boolean submitting
    ) {
        this.submitting = submitting;

        ratingBar.setEnabled(
                !submitting
        );

        reviewEditText.setEnabled(
                !submitting
        );

        for (TextView chip : feedbackChips) {
            chip.setEnabled(
                    !submitting
            );
        }

        submitButton.setText(
                submitting
                        ? "Submitting..."
                        : getString(
                        R.string.review_submit
                )
        );

        setSubmitEnabled(
                !submitting
                        && barberLoaded
        );
    }

    private void setSubmitEnabled(
            boolean enabled
    ) {
        if (submitButton != null) {
            submitButton.setEnabled(
                    enabled
            );
        }
    }

    private void handleSubmitFailure(
            Exception exception
    ) {
        setSubmitting(false);

        Toast.makeText(
                this,
                "Cannot submit review: "
                        + safeErrorMessage(
                        exception
                ),
                Toast.LENGTH_LONG
        ).show();
    }

    private boolean isIdConflict(
            Throwable throwable
    ) {
        Throwable current =
                throwable;

        while (current != null) {
            if (
                    current
                            instanceof IdConflictException
            ) {
                return true;
            }

            current =
                    current.getCause();
        }

        return false;
    }

    private Long readLong(
            Object value
    ) {
        if (!(value instanceof Number)) {
            return null;
        }

        return ((Number) value)
                .longValue();
    }

    private Double readDouble(
            Object value
    ) {
        if (!(value instanceof Number)) {
            return null;
        }

        return ((Number) value)
                .doubleValue();
    }

    private String readText(
            Object value,
            String fallback
    ) {
        if (value == null) {
            return fallback;
        }

        String text =
                String.valueOf(value)
                        .trim();

        return text.isEmpty()
                ? fallback
                : text;
    }

    /**
     * Field experience hiện tại trong ảnh
     * đang là String "5".
     */
    private String formatExperience(
            Object value
    ) {
        if (value == null) {
            return "Experience not updated";
        }

        String experience =
                String.valueOf(value)
                        .trim();

        if (experience.isEmpty()) {
            return "Experience not updated";
        }

        if (
                experience
                        .toLowerCase(Locale.US)
                        .contains("year")
        ) {
            return experience;
        }

        return experience
                + " years experience";
    }

    private String getInitial(
            String name
    ) {
        if (
                name == null
                        || name.trim().isEmpty()
        ) {
            return "B";
        }

        return name.trim()
                .substring(0, 1)
                .toUpperCase(Locale.US);
    }

    private String safeErrorMessage(
            Exception exception
    ) {
        if (
                exception == null
                        || exception.getMessage() == null
                        || exception.getMessage()
                        .trim()
                        .isEmpty()
        ) {
            return "Unknown error";
        }

        return exception.getMessage();
    }

    /**
     * Exception nội bộ dùng khi document ID
     * vừa được thiết bị khác tạo.
     */
    private static class IdConflictException
            extends RuntimeException {

        IdConflictException() {
            super(
                    "The generated review ID already exists."
            );
        }
    }
}