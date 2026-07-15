package com.example.barbershop.data;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AppointmentRepository {
    private static final String COLLECTION_APPOINTMENTS = "appointments";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public AppointmentRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public void getCurrentUserAppointments(BarberRepository.RepositoryCallback<List<AppointmentRecord>> callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new IllegalStateException("Please log in before viewing appointments."));
            return;
        }

        loadAppointments(currentUser.getUid(), callback);
    }

    private void loadAppointments(
            String userUid,
            BarberRepository.RepositoryCallback<List<AppointmentRecord>> callback
    ) {
        firestore.collection(COLLECTION_APPOINTMENTS)
                .whereEqualTo("userId", userUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AppointmentRecord> appointments = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        appointments.add(AppointmentRecord.fromDocument(document));
                    }
                    appointments.sort((left, right) -> compareNullableTimestamp(left.getStartAt(), right.getStartAt()));
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(callback::onError);
    }

    private static int compareNullableTimestamp(Timestamp left, Timestamp right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    public static class AppointmentRecord {
        private final String documentId;
        private final String appointmentId;
        private final String barberId;
        private final String serviceId;
        private final Timestamp createdAt;
        private final Timestamp startAt;
        private final Timestamp endAt;

        private AppointmentRecord(
                String documentId,
                String appointmentId,
                String barberId,
                String serviceId,
                Timestamp createdAt,
                Timestamp startAt,
                Timestamp endAt
        ) {
            this.documentId = documentId;
            this.appointmentId = appointmentId;
            this.barberId = barberId;
            this.serviceId = serviceId;
            this.createdAt = createdAt;
            this.startAt = startAt;
            this.endAt = endAt;
        }

        public static AppointmentRecord fromDocument(DocumentSnapshot document) {
            return new AppointmentRecord(
                    document.getId(),
                    readString(document, "appointmentId"),
                    readString(document, "barberId"),
                    readString(document, "serviceId"),
                    document.getTimestamp("createdAt"),
                    document.getTimestamp("startAt"),
                    document.getTimestamp("endAt")
            );
        }

        private static String readString(DocumentSnapshot document, String field) {
            Object value = document.get(field);
            return value == null ? "" : String.valueOf(value).trim();
        }

        public String getDisplayId() {
            return appointmentId.isEmpty() ? documentId : "#" + appointmentId;
        }

        public String getBarberId() {
            return barberId;
        }

        public String getServiceId() {
            return serviceId;
        }

        public Timestamp getCreatedAt() {
            return createdAt;
        }

        public Timestamp getStartAt() {
            return startAt;
        }

        public Timestamp getEndAt() {
            return endAt;
        }
    }
}
