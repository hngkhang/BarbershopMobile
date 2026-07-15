package com.example.barbershop.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BarberSchedule {
    private final String id;
    private final String barberId;
    private final Timestamp startAt;
    private final Timestamp endAt;

    public BarberSchedule(String id, String barberId, Timestamp startAt, Timestamp endAt) {
        this.id = id;
        this.barberId = barberId;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static BarberSchedule fromDocument(DocumentSnapshot document) {
        String barberId = getFieldAsString(document, "barberId");
        Timestamp startAt = document.getTimestamp("startAt");
        Timestamp endAt = document.getTimestamp("endAt");

        return new BarberSchedule(
                document.getId(),
                barberId,
                startAt,
                endAt
        );
    }

    private static String getFieldAsString(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public String getId() {
        return id;
    }

    public String getBarberId() {
        return barberId;
    }

    public boolean belongsToBarberId(String targetBarberId) {
        return Barber.normalizeId(barberId).equals(Barber.normalizeId(targetBarberId));
    }

    public Timestamp getStartAt() {
        return startAt;
    }

    public Timestamp getEndAt() {
        return endAt;
    }

    public String getDateLabel() {
        return format(startAt, "EEE, MMM d");
    }

    public String getTimeRangeLabel() {
        if (startAt == null || endAt == null) {
            return "";
        }

        return format(startAt, "h:mm a") + " - " + format(endAt, "h:mm a");
    }

    private String format(Timestamp timestamp, String pattern) {
        if (timestamp == null) {
            return "";
        }

        Date date = timestamp.toDate();
        return new SimpleDateFormat(pattern, Locale.US).format(date);
    }
}
