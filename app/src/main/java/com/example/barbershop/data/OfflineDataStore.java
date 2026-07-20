package com.example.barbershop.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.example.barbershop.models.Appointment;
import com.example.barbershop.models.Barber;
import com.example.barbershop.models.BarberSchedule;
import com.example.barbershop.models.ShopService;
import com.google.firebase.Timestamp;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** SQLite cache and outbox used by screens while the device is offline. */
public final class OfflineDataStore {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(Exception exception);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private OfflineDataStore() { }

    public static void readServices(Context context, Callback<List<ShopService>> callback) {
        execute(context, callback, database -> {
            List<ShopService> result = new ArrayList<>();
            try (Cursor cursor = database.query("services", null, "active = 1", null,
                    null, null, "name COLLATE NOCASE ASC")) {
                while (cursor.moveToNext()) {
                    result.add(new ShopService(
                            string(cursor, "firestore_document_id"),
                            String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("service_id"))),
                            string(cursor, "name"), string(cursor, "category"),
                            string(cursor, "image_url"), number(cursor, "price"),
                            (int) cursor.getLong(cursor.getColumnIndexOrThrow("duration_minutes"))
                    ));
                }
            }
            return result;
        });
    }

    public static void readBarbers(Context context, Callback<List<Barber>> callback) {
        execute(context, callback, database -> {
            List<Barber> result = new ArrayList<>();
            try (Cursor cursor = database.query("barbers", null, "active = 1", null,
                    null, null, "rating DESC, name COLLATE NOCASE ASC")) {
                while (cursor.moveToNext()) {
                    String documentId = string(cursor, "firestore_document_id");
                    String barberId = String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("barber_id")));
                    result.add(new Barber(documentId, barberId, Arrays.asList(documentId, barberId),
                            string(cursor, "name"), string(cursor, "experience"), true,
                            string(cursor, "avatar_url"), number(cursor, "rating")));
                }
            }
            return result;
        });
    }

    public static void readSchedules(Context context, long barberId,
                                     Callback<List<BarberSchedule>> callback) {
        execute(context, callback, database -> {
            List<BarberSchedule> result = new ArrayList<>();
            try (Cursor cursor = database.query("barber_schedules", null, "barber_id = ?",
                    new String[]{String.valueOf(barberId)}, null, null, "start_at_ms ASC")) {
                while (cursor.moveToNext()) {
                    result.add(new BarberSchedule(
                            string(cursor, "firestore_document_id"), String.valueOf(barberId),
                            timestamp(cursor.getLong(cursor.getColumnIndexOrThrow("start_at_ms"))),
                            timestamp(cursor.getLong(cursor.getColumnIndexOrThrow("end_at_ms")))
                    ));
                }
            }
            return result;
        });
    }

    public static void readAppointmentsForBarber(Context context, long barberId,
                                                  Callback<List<Appointment>> callback) {
        execute(context, callback, database -> {
            List<Appointment> result = new ArrayList<>();
            try (Cursor cursor = database.query("appointments", null,
                    "barber_id = ? AND status != ?", new String[]{String.valueOf(barberId), "CANCELLED"},
                    null, null, "start_at_ms ASC")) {
                while (cursor.moveToNext()) {
                    result.add(appointmentFromCursor(cursor));
                }
            }
            return result;
        });
    }

    public static void readAppointmentsForUser(Context context, String userUid,
                                               Callback<List<Appointment>> callback) {
        execute(context, callback, database -> {
            List<Appointment> result = new ArrayList<>();
            try (Cursor cursor = database.query("appointments", null, "user_uid = ?",
                    new String[]{userUid}, null, null, "start_at_ms DESC")) {
                while (cursor.moveToNext()) {
                    result.add(appointmentFromCursor(cursor));
                }
            }
            return result;
        });
    }

    public static void cacheServices(Context context, List<ShopService> services) {
        executeWrite(context, database -> {
            long now = System.currentTimeMillis();
            database.beginTransaction();
            try {
                for (ShopService service : services) {
                    long id = parsePositiveLong(service.getServiceId());
                    if (id <= 0L) continue;
                    ContentValues values = new ContentValues();
                    values.put("service_id", id);
                    values.put("firestore_document_id", service.getId());
                    values.put("name", service.getName()); values.put("category", service.getCategory());
                    values.put("image_url", service.getImageUrl()); values.put("price", service.getPrice());
                    values.put("duration_minutes", service.getTimeMinutes()); values.put("active", 1);
                    values.put("synced_at_ms", now);
                    database.insertWithOnConflict("services", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
                database.setTransactionSuccessful();
            } finally { database.endTransaction(); }
        });
    }

    public static void cacheBarbers(Context context, List<Barber> barbers) {
        executeWrite(context, database -> {
            long now = System.currentTimeMillis();
            database.beginTransaction();
            try {
                for (Barber barber : barbers) {
                    long id = parsePositiveLong(barber.getBarberId());
                    if (id <= 0L) continue;
                    ContentValues values = new ContentValues();
                    values.put("barber_id", id); values.put("firestore_document_id", barber.getId());
                    values.put("name", barber.getName()); values.put("avatar_url", barber.getAvatarUrl());
                    values.put("experience", barber.getExperience()); values.put("rating", barber.getDisplayRating());
                    values.put("active", barber.isActive() ? 1 : 0); values.put("synced_at_ms", now);
                    database.insertWithOnConflict("barbers", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
                database.setTransactionSuccessful();
            } finally { database.endTransaction(); }
        });
    }

    public static void cacheSchedules(Context context, List<BarberSchedule> schedules) {
        executeWrite(context, database -> {
            long now = System.currentTimeMillis();
            database.beginTransaction();
            try {
                for (BarberSchedule schedule : schedules) {
                    long barberId = parsePositiveLong(schedule.getBarberId());
                    if (barberId <= 0L || schedule.getStartAt() == null || schedule.getEndAt() == null) continue;
                    ContentValues values = new ContentValues();
                    values.put("schedule_id", stableId(schedule.getId())); values.put("firestore_document_id", schedule.getId());
                    values.put("barber_id", barberId); values.put("start_at_ms", schedule.getStartAt().toDate().getTime());
                    values.put("end_at_ms", schedule.getEndAt().toDate().getTime()); values.put("synced_at_ms", now);
                    database.insertWithOnConflict("barber_schedules", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
                database.setTransactionSuccessful();
            } finally { database.endTransaction(); }
        });
    }

    public static void cacheAppointments(Context context, List<Appointment> appointments) {
        executeWrite(context, database -> {
            long now = System.currentTimeMillis();
            database.beginTransaction();
            try {
                for (Appointment appointment : appointments) {
                    if (appointment.getStartAt() == null || appointment.getEndAt() == null) continue;
                    String localId = appointment.getDocumentId();
                    ContentValues values = new ContentValues();
                    values.put("local_id", localId); values.put("firestore_document_id", localId);
                    values.put("appointment_id", stableId(localId)); values.put("user_uid", appointment.getUserUid());
                    values.put("barber_id", appointment.getBarberId()); values.put("service_id", appointment.getServiceId());
                    values.put("start_at_ms", appointment.getStartAt().toDate().getTime());
                    values.put("end_at_ms", appointment.getEndAt().toDate().getTime()); values.put("note", appointment.getNote());
                    values.put("status", appointment.getStatus()); values.put("payment_status", "UNPAID");
                    values.put("created_at_ms", now); values.put("sync_status", "SYNCED"); values.put("last_sync_at_ms", now);
                    database.insertWithOnConflict("appointments", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
                database.setTransactionSuccessful();
            } finally { database.endTransaction(); }
        });
    }

    public static void createPendingAppointment(Context context, String userUid, long barberId, long serviceId,
                                                Timestamp startAt, Timestamp endAt, String note,
                                                Callback<String> callback) {
        execute(context, callback, database -> {
            if (userUid == null || userUid.trim().isEmpty() || barberId <= 0L || serviceId <= 0L
                    || startAt == null || endAt == null || endAt.toDate().getTime() <= startAt.toDate().getTime()) {
                throw new IllegalArgumentException("Invalid offline appointment.");
            }
            String localId = "offline_" + UUID.randomUUID();
            long now = System.currentTimeMillis();
            ContentValues appointment = new ContentValues();
            appointment.put("local_id", localId); appointment.put("appointment_id", now);
            appointment.put("user_uid", userUid); appointment.put("barber_id", barberId); appointment.put("service_id", serviceId);
            appointment.put("start_at_ms", startAt.toDate().getTime()); appointment.put("end_at_ms", endAt.toDate().getTime());
            appointment.put("note", note == null ? "" : note.trim()); appointment.put("status", "UPCOMING");
            appointment.put("payment_status", "UNPAID"); appointment.put("created_at_ms", now);
            appointment.put("sync_status", "PENDING_SYNC"); appointment.put("sync_attempt_count", 0);

            JSONObject payload = new JSONObject();
            try {
                payload.put("userUid", userUid); payload.put("barberId", barberId); payload.put("serviceId", serviceId);
                payload.put("startAtMillis", startAt.toDate().getTime()); payload.put("endAtMillis", endAt.toDate().getTime());
                payload.put("note", note == null ? "" : note.trim()); payload.put("status", "UPCOMING");
            } catch (JSONException exception) { throw new IllegalStateException(exception); }

            database.beginTransaction();
            try {
                database.insertOrThrow("appointments", null, appointment);
                ContentValues queue = new ContentValues();
                queue.put("entity_type", "APPOINTMENT"); queue.put("entity_local_id", localId);
                queue.put("operation", "CREATE"); queue.put("payload_json", payload.toString());
                queue.put("queue_status", "PENDING"); queue.put("retry_count", 0); queue.put("created_at_ms", now);
                database.insertOrThrow("sync_queue", null, queue);
                database.setTransactionSuccessful();
            } finally { database.endTransaction(); }
            return localId;
        });
    }

    public static void cancelAppointment(Context context, String appointmentId, Callback<Void> callback) {
        execute(context, callback, database -> {
            String id = appointmentId == null ? "" : appointmentId.trim();
            if (id.isEmpty()) {
                throw new IllegalArgumentException("Appointment id is required.");
            }

            long now = System.currentTimeMillis();
            ContentValues appointmentValues = new ContentValues();
            appointmentValues.put("status", "CANCELLED");
            appointmentValues.put("cancelled_at_ms", now);
            appointmentValues.put("updated_at_ms", now);
            appointmentValues.put("sync_status", "PENDING_SYNC");

            database.beginTransaction();
            try {
                int changed = database.update("appointments", appointmentValues,
                        "local_id = ? OR firestore_document_id = ?", new String[]{id, id});
                if (changed == 0) {
                    throw new IllegalStateException("Appointment is not available offline.");
                }

                try (Cursor cursor = database.query("appointments", new String[]{"local_id"},
                        "local_id = ? OR firestore_document_id = ?", new String[]{id, id},
                        null, null, null)) {
                    if (!cursor.moveToFirst()) {
                        throw new IllegalStateException("Appointment is not available offline.");
                    }
                    String localId = cursor.getString(0);
                    JSONObject payload = new JSONObject();
                    payload.put("cancelledAtMillis", now);
                    ContentValues queue = new ContentValues();
                    queue.put("entity_type", "APPOINTMENT");
                    queue.put("entity_local_id", localId);
                    queue.put("operation", "CANCEL");
                    queue.put("payload_json", payload.toString());
                    queue.put("queue_status", "PENDING");
                    queue.put("retry_count", 0);
                    queue.put("created_at_ms", now);
                    database.insert("sync_queue", null, queue);
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
            return null;
        });
    }

    public static void markExpiredAppointmentsCompleted(Context context, String userUid) {
        executeWrite(context, database -> {
            ContentValues values = new ContentValues();
            values.put("status", "COMPLETED");
            values.put("updated_at_ms", System.currentTimeMillis());
            database.update("appointments", values,
                    "user_uid = ? AND status = ? AND end_at_ms <= ?",
                    new String[]{userUid, "UPCOMING", String.valueOf(System.currentTimeMillis())});
        });
    }

    private interface DatabaseOperation<T> { T run(SQLiteDatabase database) throws Exception; }
    private interface DatabaseWrite { void run(SQLiteDatabase database) throws Exception; }

    private static <T> void execute(Context context, Callback<T> callback, DatabaseOperation<T> operation) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            OfflineDatabaseHelper helper = new OfflineDatabaseHelper(appContext);
            try {
                T result = operation.run(helper.getWritableDatabase());
                MAIN_HANDLER.post(() -> callback.onSuccess(result));
            } catch (Exception exception) {
                MAIN_HANDLER.post(() -> callback.onError(exception));
            } finally { helper.close(); }
        });
    }

    private static void executeWrite(Context context, DatabaseWrite operation) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            OfflineDatabaseHelper helper = new OfflineDatabaseHelper(appContext);
            try { operation.run(helper.getWritableDatabase()); } catch (Exception ignored) { } finally { helper.close(); }
        });
    }

    private static Appointment appointmentFromCursor(Cursor cursor) {
        return new Appointment(string(cursor, "local_id"), cursor.getLong(cursor.getColumnIndexOrThrow("barber_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("service_id")), string(cursor, "user_uid"),
                timestamp(cursor.getLong(cursor.getColumnIndexOrThrow("start_at_ms"))),
                timestamp(cursor.getLong(cursor.getColumnIndexOrThrow("end_at_ms"))),
                string(cursor, "status"), string(cursor, "note"));
    }

    private static Timestamp timestamp(long value) { return new Timestamp(new Date(value)); }
    private static String string(Cursor cursor, String column) { int index = cursor.getColumnIndexOrThrow(column); return cursor.isNull(index) ? "" : cursor.getString(index).trim(); }
    private static double number(Cursor cursor, String column) { int index = cursor.getColumnIndexOrThrow(column); return cursor.isNull(index) ? 0.0 : cursor.getDouble(index); }
    private static long parsePositiveLong(String value) { try { long result = Long.parseLong(value == null ? "" : value.trim()); return result > 0L ? result : 0L; } catch (NumberFormatException ignored) { return 0L; } }
    private static long stableId(String value) { long id = parsePositiveLong(value); if (id > 0L) return id; long hash = Integer.toUnsignedLong((value == null ? "" : value).hashCode()); return hash == 0L ? 1L : hash; }
}
