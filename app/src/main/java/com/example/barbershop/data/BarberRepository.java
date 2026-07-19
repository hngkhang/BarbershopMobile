package com.example.barbershop.data;

import android.content.Context;

import com.example.barbershop.models.Barber;
import com.example.barbershop.models.BarberSchedule;
import com.example.barbershop.models.Feedback;
import com.example.barbershop.models.Rating;
import com.example.barbershop.models.UserProfile;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.barbershop.services.SyncService;

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
    private final Context appContext;

    public BarberRepository(Context context) {
        firestore = FirebaseFirestore.getInstance();
        appContext = context.getApplicationContext();
    }

    public void getAllBarbers(RepositoryCallback<List<Barber>> callback) {
        if (!SyncService.hasUsableNetwork(appContext)) {
            loadCachedBarbers(callback);
            return;
        }
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
                .addOnFailureListener(exception -> loadCachedBarbersOrError(callback, exception));
    }

    public void getBarberById(String barberId, RepositoryCallback<Barber> callback) {
        if (!SyncService.hasUsableNetwork(appContext)) {
            loadCachedBarberById(barberId, callback);
            return;
        }
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
                .addOnFailureListener(exception -> loadCachedBarberByIdOrError(barberId, callback, exception));
    }

    public void getBarberSchedule(String barberId, RepositoryCallback<List<BarberSchedule>> callback) {
        long numericBarberId = parsePositiveLong(barberId);
        if (!SyncService.hasUsableNetwork(appContext) && numericBarberId > 0L) {
            loadCachedSchedule(numericBarberId, callback);
            return;
        }
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
                    OfflineDataStore.cacheSchedules(appContext, schedules);
                    callback.onSuccess(schedules);
                })
                .addOnFailureListener(exception -> {
                    if (numericBarberId > 0L) {
                        loadCachedScheduleOrError(numericBarberId, callback, exception);
                    } else {
                        callback.onError(exception);
                    }
                });
    }

    public void getBarberSchedule(long barberId, RepositoryCallback<List<BarberSchedule>> callback) {
        if (!SyncService.hasUsableNetwork(appContext)) {
            loadCachedSchedule(barberId, callback);
            return;
        }
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
                    OfflineDataStore.cacheSchedules(appContext, schedules);
                    callback.onSuccess(schedules);
                })
                .addOnFailureListener(exception -> loadCachedScheduleOrError(barberId, callback, exception));
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
                    OfflineDataStore.cacheBarbers(appContext, barbers);
                    callback.onSuccess(barbers);
                })
                .addOnFailureListener(exception -> {
                    sortBarbersByDisplayedRating(barbers);
                    OfflineDataStore.cacheBarbers(appContext, barbers);
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

    private void loadCachedBarbers(RepositoryCallback<List<Barber>> callback) {
        OfflineDataStore.readBarbers(appContext, new OfflineDataStore.Callback<List<Barber>>() {
            @Override
            public void onSuccess(List<Barber> barbers) {
                sortBarbersByDisplayedRating(barbers);
                callback.onSuccess(barbers);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    private void loadCachedBarberById(String barberId, RepositoryCallback<Barber> callback) {
        OfflineDataStore.readBarbers(appContext, new OfflineDataStore.Callback<List<Barber>>() {
            @Override
            public void onSuccess(List<Barber> barbers) {
                for (Barber barber : barbers) {
                    if (barber.getId().equals(barberId) || barber.getBarberId().equals(Barber.normalizeId(barberId))) {
                        callback.onSuccess(barber);
                        return;
                    }
                }
                callback.onSuccess(null);
            }

            @Override
            public void onError(Exception exception) { callback.onError(exception); }
        });
    }

    private void loadCachedBarberByIdOrError(String barberId, RepositoryCallback<Barber> callback,
                                              Exception remoteException) {
        OfflineDataStore.readBarbers(appContext, new OfflineDataStore.Callback<List<Barber>>() {
            @Override
            public void onSuccess(List<Barber> barbers) {
                for (Barber barber : barbers) {
                    if (barber.getId().equals(barberId) || barber.getBarberId().equals(Barber.normalizeId(barberId))) {
                        callback.onSuccess(barber);
                        return;
                    }
                }
                callback.onError(remoteException);
            }

            @Override
            public void onError(Exception exception) { callback.onError(remoteException); }
        });
    }

    private void loadCachedBarbersOrError(RepositoryCallback<List<Barber>> callback, Exception remoteException) {
        OfflineDataStore.readBarbers(appContext, new OfflineDataStore.Callback<List<Barber>>() {
            @Override
            public void onSuccess(List<Barber> barbers) {
                if (barbers.isEmpty()) callback.onError(remoteException); else callback.onSuccess(barbers);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(remoteException);
            }
        });
    }

    private void loadCachedSchedule(long barberId, RepositoryCallback<List<BarberSchedule>> callback) {
        OfflineDataStore.readSchedules(appContext, barberId, new OfflineDataStore.Callback<List<BarberSchedule>>() {
            @Override
            public void onSuccess(List<BarberSchedule> schedules) {
                callback.onSuccess(schedules);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    private void loadCachedScheduleOrError(long barberId, RepositoryCallback<List<BarberSchedule>> callback,
                                            Exception remoteException) {
        OfflineDataStore.readSchedules(appContext, barberId, new OfflineDataStore.Callback<List<BarberSchedule>>() {
            @Override
            public void onSuccess(List<BarberSchedule> schedules) {
                if (schedules.isEmpty()) callback.onError(remoteException); else callback.onSuccess(schedules);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(remoteException);
            }
        });
    }

    private long parsePositiveLong(String value) {
        try {
            long parsed = Long.parseLong(value == null ? "" : value.trim());
            return parsed > 0L ? parsed : 0L;
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);

        void onError(Exception exception);
    }
}
