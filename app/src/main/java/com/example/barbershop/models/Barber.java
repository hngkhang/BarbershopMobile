package com.example.barbershop.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Barber {
    private final String id;
    private final String barberId;
    private final List<String> lookupIds;
    private final String name;
    private final String experience;
    private final boolean active;
    private final String avatarUrl;
    private final double rating;
    private double displayRating;
    private int ratingCount;

    public Barber(
            String id,
            String barberId,
            List<String> lookupIds,
            String name,
            String experience,
            boolean active,
            String avatarUrl,
            double rating
    ) {
        this.id = id;
        this.barberId = barberId;
        this.lookupIds = lookupIds;
        this.name = name;
        this.experience = experience;
        this.active = active;
        this.avatarUrl = avatarUrl;
        this.rating = rating;
        this.displayRating = rating;
        this.ratingCount = 0;
    }

    public static Barber fromDocument(DocumentSnapshot document) {
        String name = getString(document, "name");
        String experience = getString(document, "experience");
        String avatarUrl = getString(document, "avatarUrl");
        Boolean active = document.getBoolean("active");
        Double rating = document.getDouble("rating");

        return new Barber(
                document.getId(),
                getFieldAsString(document, "barberId"),
                buildLookupIds(document),
                name,
                experience,
                active != null && active,
                avatarUrl,
                rating == null ? 0.0 : rating
        );
    }

    private static String getString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        return value == null ? "" : value.trim();
    }

    private static List<String> buildLookupIds(DocumentSnapshot document) {
        List<String> ids = new ArrayList<>();
        addLookupId(ids, document.getId());
        addLookupId(ids, getFieldAsString(document, "barberId"));
        addLookupId(ids, getFieldAsString(document, "barberid"));
        addLookupId(ids, getFieldAsString(document, "id"));

        String digits = document.getId().replaceAll("\\D+", "");
        addLookupId(ids, digits);
        if (!digits.isEmpty()) {
            try {
                addLookupId(ids, String.valueOf(Integer.parseInt(digits)));
            } catch (NumberFormatException exception) {
                // Keep the raw digits fallback when the document id is too large.
            }
        }

        return Collections.unmodifiableList(ids);
    }

    private static String getFieldAsString(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static void addLookupId(List<String> ids, String value) {
        String normalized = normalizeId(value);
        if (!normalized.isEmpty() && !ids.contains(normalized)) {
            ids.add(normalized);
        }
    }

    public static String normalizeId(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        return normalized.endsWith(".0")
                ? normalized.substring(0, normalized.length() - 2)
                : normalized;
    }

    public String getId() {
        return id;
    }

    public String getBarberId() {
        return barberId;
    }

    public List<String> getLookupIds() {
        return lookupIds;
    }

    public String getName() {
        return name;
    }

    public String getExperience() {
        return experience;
    }

    public boolean isActive() {
        return active;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public double getRating() {
        return rating;
    }

    public double getDisplayRating() {
        return displayRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingSummary(double displayRating, int ratingCount) {
        this.displayRating = displayRating;
        this.ratingCount = ratingCount;
    }

    public String getInitial() {
        return name == null || name.trim().isEmpty()
                ? "B"
                : name.trim().substring(0, 1).toUpperCase(Locale.US);
    }
}
