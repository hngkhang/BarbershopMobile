package com.example.barbershop.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class Appointment {
    private final String documentId;
    private final long barberId;
    private final long serviceId;
    private final String userUid;
    private final Timestamp startAt;
    private final Timestamp endAt;
    private final String status;
    private final String note;

    public Appointment(
            String documentId,
            long barberId,
            long serviceId,
            String userUid,
            Timestamp startAt,
            Timestamp endAt,
            String status,
            String note
    ) {
        this.documentId = documentId;
        this.barberId = barberId;
        this.serviceId = serviceId;
        this.userUid = userUid;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.note = note;
    }

    public static Appointment fromDocument(DocumentSnapshot document) {
        return new Appointment(
                document.getId(),
                numberValue(document.get("barberId")).longValue(),
                numberValue(document.get("serviceId")).longValue(),
                stringValue(document.get("userUid")),
                document.getTimestamp("startAt"),
                document.getTimestamp("endAt"),
                stringValue(document.get("status")),
                stringValue(document.get("note"))
        );
    }

    private static Number numberValue(Object value) {
        return value instanceof Number ? (Number) value : 0;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public String getDocumentId() { return documentId; }
    public long getBarberId() { return barberId; }
    public long getServiceId() { return serviceId; }
    public String getUserUid() { return userUid; }
    public Timestamp getStartAt() { return startAt; }
    public Timestamp getEndAt() { return endAt; }
    public String getStatus() { return status; }
    public String getNote() { return note; }
    public boolean isCancelled() { return "CANCELLED".equalsIgnoreCase(status); }
}
