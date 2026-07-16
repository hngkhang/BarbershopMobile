package com.example.barbershop.data;

import com.example.barbershop.models.Appointment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void getAppointmentsForBarber(long barberId, RepositoryCallback<List<Appointment>> callback) {
        firestore.collection("appointments")
                .whereEqualTo("barberId", barberId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Appointment> appointments = new ArrayList<>();
                    for (int index = 0; index < snapshot.size(); index++) {
                        Appointment appointment = Appointment.fromDocument(snapshot.getDocuments().get(index));
                        if (!appointment.isCancelled()
                                && appointment.getStartAt() != null
                                && appointment.getEndAt() != null) {
                            appointments.add(appointment);
                        }
                    }
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAppointmentsForUser(String userUid, RepositoryCallback<List<Appointment>> callback) {
        firestore.collection("appointments")
                .whereEqualTo("userUid", userUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Appointment> appointments = new ArrayList<>();
                    for (int index = 0; index < snapshot.size(); index++) {
                        appointments.add(Appointment.fromDocument(snapshot.getDocuments().get(index)));
                    }
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(callback::onError);
    }

    public void createAppointment(
            String userUid,
            long barberId,
            long serviceId,
            Timestamp startAt,
            Timestamp endAt,
            String note,
            RepositoryCallback<String> callback
    ) {
        Map<String, Object> values = new HashMap<>();
        values.put("userUid", userUid);
        values.put("barberId", barberId);
        values.put("serviceId", serviceId);
        values.put("startAt", startAt);
        values.put("endAt", endAt);
        values.put("createdAt", FieldValue.serverTimestamp());
        values.put("status", "UPCOMING");
        values.put("note", note == null ? "" : note);
        values.put("cancelledAt", null);

        firestore.collection("appointments")
                .add(values)
                .addOnSuccessListener(reference -> callback.onSuccess(reference.getId()))
                .addOnFailureListener(callback::onError);
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(Exception exception);
    }
}
