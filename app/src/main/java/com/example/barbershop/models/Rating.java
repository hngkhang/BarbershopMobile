package com.example.barbershop.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class Rating {
    private final String id;
    private final String feedbackId;
    private final String barberId;
    private final String userId;
    private final double rate;

    public Rating(String id, String feedbackId, String barberId, String userId, double rate) {
        this.id = id;
        this.feedbackId = feedbackId;
        this.barberId = barberId;
        this.userId = userId;
        this.rate = rate;
    }

    public static Rating fromDocument(DocumentSnapshot document) {
        return new Rating(
                document.getId(),
                getFieldAsString(document, "feedbackId"),
                getFieldAsString(document, "barberId"),
                getFieldAsString(document, "userId"),
                getDouble(document, "rate")
        );
    }

    private static String getFieldAsString(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static double getDouble(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException exception) {
                return 0.0;
            }
        }

        return 0.0;
    }

    public String getId() {
        return id;
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public String getBarberId() {
        return barberId;
    }

    public String getUserId() {
        return userId;
    }

    public double getRate() {
        return rate;
    }

    public boolean belongsToAnyBarberId(List<String> targetBarberIds) {
        String normalizedBarberId = Barber.normalizeId(barberId);
        for (String targetBarberId : targetBarberIds) {
            if (normalizedBarberId.equals(Barber.normalizeId(targetBarberId))) {
                return true;
            }
        }
        return false;
    }

    public boolean belongsToUser(String targetUserId) {
        return Barber.normalizeId(userId).equals(Barber.normalizeId(targetUserId));
    }

    public boolean belongsToFeedback(String targetFeedbackId) {
        return !Barber.normalizeId(feedbackId).isEmpty()
                && Barber.normalizeId(feedbackId).equals(Barber.normalizeId(targetFeedbackId));
    }
}
