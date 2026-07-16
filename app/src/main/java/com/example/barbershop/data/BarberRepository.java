package com.example.barbershop.data;

import com.example.barbershop.models.Barber;
import com.example.barbershop.models.BarberSchedule;
import com.example.barbershop.models.Feedback;
import com.example.barbershop.models.Rating;
import com.example.barbershop.models.UserProfile;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BarberRepository {
    private static final String COLLECTION_BARBERS = "barbers";
    private static final String COLLECTION_SCHEDULES = "barberSchedules";
    private static final String COLLECTION_FEEDBACKS = "feedbacks";
    private static final String COLLECTION_RATINGS = "ratings";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore firestore;

    public BarberRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void getAllBarbers(RepositoryCallback<List<Barber>> callback) {
        firestore.collection(COLLECTION_BARBERS)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Barber> barbers = new ArrayList<>();
                    for (int index = 0; index < querySnapshot.size(); index++) {
                        barbers.add(Barber.fromDocument(querySnapshot.getDocuments().get(index)));
                    }

                    loadRatingSummaries(barbers, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getBarberById(String barberId, RepositoryCallback<Barber> callback) {
        firestore.collection(COLLECTION_BARBERS)
                .document(barberId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onSuccess(null);
                        return;
                    }

                    Barber barber = Barber.fromDocument(document);
                    callback.onSuccess(barber.isActive() ? barber : null);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getBarberSchedule(String barberId, RepositoryCallback<List<BarberSchedule>> callback) {
        firestore.collection(COLLECTION_SCHEDULES)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BarberSchedule> schedules = new ArrayList<>();
                    for (int index = 0; index < querySnapshot.size(); index++) {
                        BarberSchedule schedule = BarberSchedule.fromDocument(querySnapshot.getDocuments().get(index));
                        if (schedule.belongsToBarberId(barberId)
                                && schedule.getStartAt() != null
                                && schedule.getEndAt() != null) {
                            schedules.add(schedule);
                        }
                    }

                    Collections.sort(schedules, (left, right) ->
                            left.getStartAt().compareTo(right.getStartAt())
                    );
                    callback.onSuccess(schedules);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getBarberSchedule(long barberId, RepositoryCallback<List<BarberSchedule>> callback) {
        firestore.collection(COLLECTION_SCHEDULES)
                .whereEqualTo("barberId", barberId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BarberSchedule> schedules = new ArrayList<>();
                    for (int index = 0; index < querySnapshot.size(); index++) {
                        BarberSchedule schedule = BarberSchedule.fromDocument(querySnapshot.getDocuments().get(index));
                        if (schedule.getStartAt() != null && schedule.getEndAt() != null) {
                            schedules.add(schedule);
                        }
                    }

                    Collections.sort(schedules, (left, right) ->
                            left.getStartAt().compareTo(right.getStartAt())
                    );
                    callback.onSuccess(schedules);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getBarberReviews(String barberId, RepositoryCallback<List<Feedback>> callback) {
        List<String> lookupIds = new ArrayList<>();
        lookupIds.add(barberId);
        getBarberReviewsByLookupIds(lookupIds, callback);
    }

    public void getBarberReviews(Barber barber, RepositoryCallback<List<Feedback>> callback) {
        getBarberReviewsByLookupIds(barber.getLookupIds(), callback);
    }

    public void getBarberRatings(Barber barber, RepositoryCallback<List<Rating>> callback) {
        firestore.collection(COLLECTION_RATINGS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Rating> ratings = new ArrayList<>();
                    for (int index = 0; index < querySnapshot.size(); index++) {
                        Rating rating = Rating.fromDocument(querySnapshot.getDocuments().get(index));
                        if (rating.belongsToAnyBarberId(barber.getLookupIds())) {
                            ratings.add(rating);
                        }
                    }
                    callback.onSuccess(ratings);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getUsers(RepositoryCallback<List<UserProfile>> callback) {
        firestore.collection(COLLECTION_USERS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserProfile> users = new ArrayList<>();
                    for (int index = 0; index < querySnapshot.size(); index++) {
                        users.add(UserProfile.fromDocument(querySnapshot.getDocuments().get(index)));
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(callback::onError);
    }

    private void getBarberReviewsByLookupIds(List<String> lookupIds, RepositoryCallback<List<Feedback>> callback) {
        firestore.collection(COLLECTION_FEEDBACKS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Feedback> feedbacks = new ArrayList<>();
                    for (int index = 0; index < querySnapshot.size(); index++) {
                        Feedback feedback = Feedback.fromDocument(querySnapshot.getDocuments().get(index));
                        // Firestore data has used both document ids and numeric barber ids.
                        if (feedback.belongsToAnyBarberId(lookupIds)) {
                            feedbacks.add(feedback);
                        }
                    }
                    callback.onSuccess(feedbacks);
                })
                .addOnFailureListener(callback::onError);
    }

    private void loadRatingSummaries(List<Barber> barbers, RepositoryCallback<List<Barber>> callback) {
        firestore.collection(COLLECTION_RATINGS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Rating> ratings = new ArrayList<>();
                    for (int index = 0; index < querySnapshot.size(); index++) {
                        ratings.add(Rating.fromDocument(querySnapshot.getDocuments().get(index)));
                    }

                    attachRatingSummaries(barbers, ratings);
                    sortBarbersByDisplayedRating(barbers);
                    callback.onSuccess(barbers);
                })
                .addOnFailureListener(exception -> {
                    sortBarbersByDisplayedRating(barbers);
                    callback.onSuccess(barbers);
                });
    }

    private void attachRatingSummaries(List<Barber> barbers, List<Rating> ratings) {
        for (Barber barber : barbers) {
            double total = 0.0;
            int count = 0;

            for (Rating rating : ratings) {
                if (rating.belongsToAnyBarberId(barber.getLookupIds())) {
                    total += rating.getRate();
                    count++;
                }
            }

            if (count > 0) {
                barber.setRatingSummary(total / count, count);
            }
        }
    }

    private void sortBarbersByDisplayedRating(List<Barber> barbers) {
        Collections.sort(barbers, (left, right) ->
                Double.compare(right.getDisplayRating(), left.getDisplayRating())
        );
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);

        void onError(Exception exception);
    }
}
