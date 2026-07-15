package com.example.barbershop.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class Feedback {
    private final String id;
    private final String feedbackId;
    private final String barberId;
    private final String userId;
    private final String content;
    private double customerRating = -1.0;
    private String customerName = "";

    public Feedback(String id, String feedbackId, String barberId, String userId, String content) {
        this.id = id;
        this.feedbackId = feedbackId;
        this.barberId = barberId;
        this.userId = userId;
        this.content = content;
    }

    public static Feedback fromDocument(DocumentSnapshot document) {
        String barberId = getFieldAsString(document, "barberId");
        String userId = getFieldAsString(document, "userId");
        String content = document.getString("content");

        if (barberId.isEmpty()) {
            barberId = getFieldAsString(document, "barberid");
        }
        if (userId.isEmpty()) {
            userId = getFieldAsString(document, "userid");
        }

        return new Feedback(
                document.getId(),
                getFieldAsString(document, "feedbackId"),
                barberId,
                userId,
                content == null ? "" : content.trim()
        );
    }

    private static String getFieldAsString(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value == null ? "" : String.valueOf(value).trim();
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

    public String getContent() {
        return content;
    }

    public String getCustomerName() {
        return customerName;
    }

    public boolean hasCustomerName() {
        return customerName != null && !customerName.trim().isEmpty();
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName == null ? "" : customerName.trim();
    }

    public double getCustomerRating() {
        return customerRating;
    }

    public boolean hasCustomerRating() {
        return customerRating >= 0.0;
    }

    public void setCustomerRating(double customerRating) {
        this.customerRating = customerRating;
    }

    public boolean belongsToBarber(String targetBarberId) {
        return Barber.normalizeId(barberId).equals(Barber.normalizeId(targetBarberId));
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
}
