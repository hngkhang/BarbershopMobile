package com.example.barbershop.data;

import android.content.Context;

import com.example.barbershop.models.Appointment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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
