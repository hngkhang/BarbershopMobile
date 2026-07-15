package com.example.barbershop.models;

import com.google.firebase.firestore.DocumentSnapshot;

public class UserProfile {
    private final String id;
    private final String userId;
    private final String uid;
    private final String name;

    public UserProfile(String id, String userId, String uid, String name) {
        this.id = id;
        this.userId = userId;
        this.uid = uid;
        this.name = name;
    }

    public static UserProfile fromDocument(DocumentSnapshot document) {
        return new UserProfile(
                document.getId(),
                getFieldAsString(document, "userId"),
                getFieldAsString(document, "uid"),
                getFieldAsString(document, "name")
        );
    }

    private static String getFieldAsString(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public String getName() {
        return name;
    }

    public boolean matchesUserId(String targetUserId) {
        String normalizedTarget = Barber.normalizeId(targetUserId);
        return Barber.normalizeId(id).equals(normalizedTarget)
                || Barber.normalizeId(uid).equals(normalizedTarget)
                || Barber.normalizeId(userId).equals(normalizedTarget);
    }
}
