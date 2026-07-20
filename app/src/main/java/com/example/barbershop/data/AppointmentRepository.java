package com.example.barbershop.data;

import android.content.Context;

import com.example.barbershop.models.Appointment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.example.barbershop.services.SyncService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final Context appContext;

    public AppointmentRepository(Context context) {
        appContext = context.getApplicationContext();
    }

    public void getAppointmentsForBarber(long barberId, RepositoryCallback<List<Appointment>> callback) {
        if (!SyncService.hasUsableNetwork(appContext)) {
            loadCachedAppointments(barberId, callback);
            return;
        }
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
                    OfflineDataStore.cacheAppointments(appContext, appointments);
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(exception -> loadCachedAppointmentsOrError(barberId, callback, exception));
    }

    public void getAppointmentsForUser(String userUid, RepositoryCallback<List<Appointment>> callback) {
        if (!SyncService.hasUsableNetwork(appContext)) {
            OfflineDataStore.markExpiredAppointmentsCompleted(appContext, userUid);
            OfflineDataStore.readAppointmentsForUser(appContext, userUid, new OfflineDataStore.Callback<List<Appointment>>() {
                @Override public void onSuccess(List<Appointment> data) { callback.onSuccess(data); }
                @Override public void onError(Exception exception) { callback.onError(exception); }
            });
            return;
        }
        completeExpiredAppointmentsForUser(userUid, new RepositoryCallback<Void>() {
            @Override public void onSuccess(Void ignored) { loadRemoteAppointmentsForUser(userUid, callback); }
            @Override public void onError(Exception exception) { loadRemoteAppointmentsForUser(userUid, callback); }
        });
    }

    private void loadRemoteAppointmentsForUser(String userUid, RepositoryCallback<List<Appointment>> callback) {
        firestore.collection("appointments")
                .whereEqualTo("userUid", userUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Appointment> appointments = new ArrayList<>();
                    for (int index = 0; index < snapshot.size(); index++) {
                        appointments.add(Appointment.fromDocument(snapshot.getDocuments().get(index)));
                    }
                    OfflineDataStore.cacheAppointments(appContext, appointments);
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(exception -> OfflineDataStore.readAppointmentsForUser(appContext, userUid,
                        new OfflineDataStore.Callback<List<Appointment>>() {
                            @Override public void onSuccess(List<Appointment> data) {
                                if (data.isEmpty()) callback.onError(exception); else callback.onSuccess(data);
                            }
                            @Override public void onError(Exception ignored) { callback.onError(exception); }
                        }));
    }

    public void completeExpiredAppointmentsForUser(String userUid, RepositoryCallback<Void> callback) {
        if (userUid == null || userUid.trim().isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        firestore.collection("appointments")
                .whereEqualTo("userUid", userUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    long now = System.currentTimeMillis();
                    WriteBatch batch = firestore.batch();
                    int count = 0;
                    for (int index = 0; index < snapshot.size(); index++) {
                        com.google.firebase.firestore.DocumentSnapshot document = snapshot.getDocuments().get(index);
                        Timestamp endAt = document.getTimestamp("endAt");
                        String status = String.valueOf(document.get("status")).trim();
                        if (endAt != null && endAt.toDate().getTime() <= now
                                && "UPCOMING".equalsIgnoreCase(status)) {
                            Map<String, Object> values = new HashMap<>();
                            values.put("status", "COMPLETED");
                            values.put("updatedAt", FieldValue.serverTimestamp());
                            batch.update(document.getReference(), values);
                            count++;
                        }
                    }
                    if (count == 0) {
                        OfflineDataStore.markExpiredAppointmentsCompleted(appContext, userUid);
                        callback.onSuccess(null);
                        return;
                    }
                    batch.commit()
                            .addOnSuccessListener(ignored -> {
                                OfflineDataStore.markExpiredAppointmentsCompleted(appContext, userUid);
                                callback.onSuccess(null);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void cancelAppointment(String appointmentId, RepositoryCallback<Void> callback) {
        String id = appointmentId == null ? "" : appointmentId.trim();
        if (id.isEmpty()) {
            callback.onError(new IllegalArgumentException("Appointment id is required."));
            return;
        }
        if (!SyncService.hasUsableNetwork(appContext)) {
            OfflineDataStore.cancelAppointment(appContext, id, new OfflineDataStore.Callback<Void>() {
                @Override public void onSuccess(Void data) {
                    SyncService.scheduleSync(appContext, "appointment_cancelled_offline");
                    callback.onSuccess(null);
                }
                @Override public void onError(Exception exception) { callback.onError(exception); }
            });
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("status", "CANCELLED");
        values.put("cancelledAt", FieldValue.serverTimestamp());
        values.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection("appointments").document(id).update(values)
                .addOnSuccessListener(ignored -> {
                    OfflineDataStore.cancelAppointment(appContext, id, new OfflineDataStore.Callback<Void>() {
                        @Override public void onSuccess(Void data) { callback.onSuccess(null); }
                        @Override public void onError(Exception exception) { callback.onSuccess(null); }
                    });
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
        OfflineDataStore.createPendingAppointment(appContext, userUid, barberId, serviceId, startAt, endAt, note,
                new OfflineDataStore.Callback<String>() {
                    @Override
                    public void onSuccess(String localId) {
                        SyncService.scheduleSync(appContext, "appointment_created");
                        callback.onSuccess(localId);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                });
    }

    private void loadCachedAppointments(long barberId, RepositoryCallback<List<Appointment>> callback) {
        OfflineDataStore.readAppointmentsForBarber(appContext, barberId,
                new OfflineDataStore.Callback<List<Appointment>>() {
                    @Override public void onSuccess(List<Appointment> appointments) { callback.onSuccess(appointments); }
                    @Override public void onError(Exception exception) { callback.onError(exception); }
                });
    }

    private void loadCachedAppointmentsOrError(long barberId, RepositoryCallback<List<Appointment>> callback,
                                               Exception remoteException) {
        OfflineDataStore.readAppointmentsForBarber(appContext, barberId,
                new OfflineDataStore.Callback<List<Appointment>>() {
                    @Override public void onSuccess(List<Appointment> appointments) {
                        if (appointments.isEmpty()) callback.onError(remoteException); else callback.onSuccess(appointments);
                    }
                    @Override public void onError(Exception exception) { callback.onError(remoteException); }
                });
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(Exception exception);
    }
}
