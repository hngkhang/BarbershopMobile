package com.example.barbershop.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.barbershop.data.OfflineDatabaseHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Synchronizes the local SQLite cache/outbox only while Android has a validated network.
 * JobScheduler makes the work safe to start from a BroadcastReceiver on Android 8+.
 */
public class SyncService extends JobService {

    private static final String TAG = "SyncService";
    private static final int JOB_ID = 240719;
    private static final long FIRESTORE_TIMEOUT_SECONDS = 30L;
    private static final String EXTRA_REASON = "sync_reason";

    private ExecutorService executor;

    /** Schedules one coalesced sync. Repeated calls replace the waiting job safely. */
    public static void scheduleSync(@NonNull Context context, @NonNull String reason) {
        Context appContext = context.getApplicationContext();
        JobScheduler scheduler = appContext.getSystemService(JobScheduler.class);
        if (scheduler == null) {
            Log.w(TAG, "JobScheduler is unavailable; sync was not scheduled.");
            return;
        }

        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_REASON, reason);
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(appContext, SyncService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(250L)
                .setExtras(extras)
                .build();

        int result = scheduler.schedule(job);
        if (result != JobScheduler.RESULT_SUCCESS) {
            Log.w(TAG, "Android rejected the sync job.");
        }
    }

    /** Exposed for NetworkReceiver and for a final defensive check inside the job. */
    public static boolean hasUsableNetwork(Context context) {
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        if (manager == null) {
            return false;
        }
        Network network = manager.getActiveNetwork();
        NetworkCapabilities capabilities = network == null ? null : manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @Override
    public boolean onStartJob(JobParameters parameters) {
        if (!hasUsableNetwork(this)) {
            return false;
        }

        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean retry = false;
            try {
                String reason = parameters.getExtras().getString(EXTRA_REASON, "unspecified");
                Log.d(TAG, "Starting sync: " + reason);
                runSync();
            } catch (Exception exception) {
                retry = true;
                Log.e(TAG, "Synchronization failed", exception);
            } finally {
                jobFinished(parameters, retry);
            }
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        if (executor != null) {
            executor.shutdownNow();
        }
        return true;
    }

    @Override
    public void onDestroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
        super.onDestroy();
    }

    private void runSync() throws Exception {
        OfflineDatabaseHelper helper = new OfflineDatabaseHelper(this);
        SQLiteDatabase database = helper.getWritableDatabase();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userUid = user == null ? "" : user.getUid();

        try {
            if (!userUid.isEmpty()) {
                pushPendingAppointments(database, firestore, userUid);
            }
            pullServices(database, firestore);
            pullBarbers(database, firestore);
            pullSchedules(database, firestore);
            if (!userUid.isEmpty()) {
                pullUser(database, firestore, userUid);
                pullAppointments(database, firestore, userUid);
            }
        } finally {
            database.close();
            helper.close();
        }
    }

    private void pushPendingAppointments(
            SQLiteDatabase database,
            FirebaseFirestore firestore,
            String currentUserUid
    ) {
        List<QueueItem> queueItems = readPendingQueue(database);
        for (QueueItem item : queueItems) {
            LocalAppointment appointment = readLocalAppointment(database, item.localId);
            if (appointment == null) {
                markQueueFailed(database, item.queueId, null, "Local appointment no longer exists.");
                continue;
            }
            if (!currentUserUid.equals(appointment.userUid)) {
                // Never upload a former user's records after sign-out/sign-in on a shared device.
                continue;
            }

            markQueueProcessing(database, item.queueId, appointment.localId);
            try {
                JSONObject payload = new JSONObject(item.payloadJson);
                writeQueuedAppointment(database, firestore, item, appointment, payload);
                markQueueDone(database, item.queueId, appointment.localId);
            } catch (Exception exception) {
                String message = safeError(exception);
                markQueueFailed(database, item.queueId, appointment.localId, message);
                Log.w(TAG, "Could not upload queue item " + item.queueId + ": " + message);
            }
        }
    }

    private List<QueueItem> readPendingQueue(SQLiteDatabase database) {
        List<QueueItem> items = new ArrayList<>();
        try (Cursor cursor = database.query(
                "sync_queue",
                new String[]{"queue_id", "entity_local_id", "operation", "payload_json"},
                "entity_type = ? AND queue_status IN ('PENDING', 'FAILED')",
                new String[]{"APPOINTMENT"},
                null,
                null,
                "created_at_ms ASC, queue_id ASC"
        )) {
            while (cursor.moveToNext()) {
                items.add(new QueueItem(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)
                ));
            }
        }
        return items;
    }

    private LocalAppointment readLocalAppointment(SQLiteDatabase database, String localId) {
        try (Cursor cursor = database.query(
                "appointments",
                null,
                "local_id = ?",
                new String[]{localId},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new LocalAppointment(
                    getString(cursor, "local_id"), getString(cursor, "firestore_document_id"),
                    getLong(cursor, "appointment_id"), getString(cursor, "user_uid"),
                    getLong(cursor, "barber_id"), getLong(cursor, "service_id"),
                    getLong(cursor, "start_at_ms"), getLong(cursor, "end_at_ms"),
                    getString(cursor, "note"), getString(cursor, "status"),
                    getString(cursor, "payment_status"), getLong(cursor, "created_at_ms"),
                    getLong(cursor, "cancelled_at_ms")
            );
        }
    }

    private void writeQueuedAppointment(
            SQLiteDatabase database,
            FirebaseFirestore firestore,
            QueueItem item,
            LocalAppointment appointment,
            JSONObject payload
    ) throws Exception {
        String operation = item.operation == null ? "" : item.operation.trim().toUpperCase();
        if (!"CREATE".equals(operation) && !"UPDATE".equals(operation) && !"CANCEL".equals(operation)) {
            throw new IllegalArgumentException("Unsupported queue operation: " + operation);
        }

        String documentId = appointment.firestoreDocumentId;
        if (documentId.isEmpty()) {
            if (!"CREATE".equals(operation)) {
                throw new IllegalStateException("An update cannot be sent before the create operation.");
            }
            documentId = documentIdFor(appointment.localId);
            ContentValues values = new ContentValues();
            values.put("firestore_document_id", documentId);
            database.update("appointments", values, "local_id = ?", new String[]{appointment.localId});
        }

        Map<String, Object> values = appointmentValues(appointment, payload, operation);
        await(firestore.collection("appointments").document(documentId).set(values, SetOptions.merge()));
        // A Firestore write task can finish after local persistence. Wait for server acknowledgement
        // before declaring the outbox item complete.
        await(firestore.waitForPendingWrites());
    }

    private Map<String, Object> appointmentValues(
            LocalAppointment appointment,
            JSONObject payload,
            String operation
    ) throws JSONException {
        Map<String, Object> values = new HashMap<>();
        if ("CANCEL".equals(operation)) {
            values.put("status", "CANCELLED");
            long cancelledAt = jsonLong(payload, "cancelledAtMillis", "cancelled_at_ms", appointment.cancelledAtMs);
            values.put("cancelledAt", new Timestamp(new java.util.Date(
                    cancelledAt > 0L ? cancelledAt : System.currentTimeMillis()
            )));
            values.put("updatedAt", FieldValue.serverTimestamp());
            return values;
        }

        String userUid = jsonString(payload, "userUid", "user_uid", appointment.userUid);
        long barberId = jsonLong(payload, "barberId", "barber_id", appointment.barberId);
        long serviceId = jsonLong(payload, "serviceId", "service_id", appointment.serviceId);
        long startAt = jsonLong(payload, "startAtMillis", "start_at_ms", appointment.startAtMs);
        long endAt = jsonLong(payload, "endAtMillis", "end_at_ms", appointment.endAtMs);
        if (userUid.isEmpty() || barberId <= 0L || serviceId <= 0L || startAt <= 0L || endAt <= startAt) {
            throw new IllegalArgumentException("Queued appointment has invalid required data.");
        }

        values.put("userUid", userUid);
        values.put("barberId", barberId);
        values.put("serviceId", serviceId);
        values.put("startAt", new Timestamp(new java.util.Date(startAt)));
        values.put("endAt", new Timestamp(new java.util.Date(endAt)));
        values.put("note", jsonString(payload, "note", "note", appointment.note));
        values.put("status", jsonString(payload, "status", "status", appointment.status));
        values.put("updatedAt", FieldValue.serverTimestamp());

        if ("CREATE".equals(operation)) {
            values.put("appointmentId", appointment.appointmentId > 0L
                    ? appointment.appointmentId : System.currentTimeMillis());
            values.put("paymentStatus", appointment.paymentStatus.isEmpty()
                    ? "UNPAID" : appointment.paymentStatus);
            values.put("paymentId", 0L);
            values.put("createdAt", FieldValue.serverTimestamp());
            values.put("cancelledAt", null);
        }
        return values;
    }

    private void pullServices(SQLiteDatabase database, FirebaseFirestore firestore) throws Exception {
        QuerySnapshot snapshot = await(firestore.collection("services").get());
        long now = System.currentTimeMillis();
        database.beginTransaction();
        try {
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                long serviceId = documentLong(document, "serviceId", document.getId());
                if (serviceId <= 0L) {
                    Log.w(TAG, "Skipping service without numeric serviceId: " + document.getId());
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put("service_id", serviceId);
                values.put("firestore_document_id", document.getId());
                values.put("name", stringValue(document.get("name")));
                values.put("category", stringValue(document.get("category")));
                values.put("image_url", firstNonEmpty(document.get("image_url"), document.get("imageUrl")));
                values.put("price", numberValue(document.get("price")));
                long durationMinutes = numberValueLong(document.get("time"));
                if (durationMinutes <= 0L) {
                    durationMinutes = numberValueLong(document.get("durationMinutes"));
                }
                values.put("duration_minutes", durationMinutes);
                values.put("active", booleanValue(document.get("active"), true) ? 1 : 0);
                values.put("synced_at_ms", now);
                database.insertWithOnConflict("services", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            markPullSuccess(database, "services", now);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void pullBarbers(SQLiteDatabase database, FirebaseFirestore firestore) throws Exception {
        QuerySnapshot snapshot = await(firestore.collection("barbers").get());
        long now = System.currentTimeMillis();
        database.beginTransaction();
        try {
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                long barberId = documentLong(document, "barberId", document.getId());
                if (barberId <= 0L) {
                    Log.w(TAG, "Skipping barber without numeric barberId: " + document.getId());
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put("barber_id", barberId);
                values.put("firestore_document_id", document.getId());
                values.put("name", stringValue(document.get("name")));
                values.put("avatar_url", stringValue(document.get("avatarUrl")));
                values.put("experience", stringValue(document.get("experience")));
                values.put("rating", numberValue(document.get("rating")));
                values.put("active", booleanValue(document.get("active"), true) ? 1 : 0);
                values.put("synced_at_ms", now);
                database.insertWithOnConflict("barbers", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            markPullSuccess(database, "barbers", now);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void pullSchedules(SQLiteDatabase database, FirebaseFirestore firestore) throws Exception {
        QuerySnapshot snapshot = await(firestore.collection("barberSchedules").get());
        long now = System.currentTimeMillis();
        database.beginTransaction();
        try {
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                long barberId = documentLong(document, "barberId", "");
                long startAt = timestampMillis(document.getTimestamp("startAt"));
                long endAt = timestampMillis(document.getTimestamp("endAt"));
                if (barberId <= 0L || startAt <= 0L || endAt <= startAt) {
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put("schedule_id", documentLong(document, "scheduleId", document.getId()));
                values.put("firestore_document_id", document.getId());
                values.put("barber_id", barberId);
                values.put("start_at_ms", startAt);
                values.put("end_at_ms", endAt);
                values.put("synced_at_ms", now);
                database.insertWithOnConflict("barber_schedules", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            markPullSuccess(database, "barberSchedules", now);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void pullUser(
            SQLiteDatabase database,
            FirebaseFirestore firestore,
            String userUid
    ) throws Exception {
        DocumentSnapshot document = await(firestore.collection("users").document(userUid).get());
        if (!document.exists()) {
            return;
        }
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("firebase_uid", userUid);
        putNullableLong(values, "user_id", numberValueLong(document.get("userId")));
        values.put("name", stringValue(document.get("name")));
        values.put("email", stringValue(document.get("email")));
        values.put("phone", stringValue(document.get("phone")));
        values.put("created_at_ms", timestampMillis(document.getTimestamp("createdAt")));
        values.put("updated_at_ms", timestampMillis(document.getTimestamp("updatedAt")));
        values.put("synced_at_ms", now);
        database.insertWithOnConflict("local_users", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        markPullSuccess(database, "user_" + userUid, now);
    }

    private void pullAppointments(
            SQLiteDatabase database,
            FirebaseFirestore firestore,
            String userUid
    ) throws Exception {
        QuerySnapshot snapshot = await(firestore.collection("appointments")
                .whereEqualTo("userUid", userUid)
                .get());
        long now = System.currentTimeMillis();
        database.beginTransaction();
        try {
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                upsertRemoteAppointment(database, document, now);
            }
            markPullSuccess(database, "appointments_" + userUid, now);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void upsertRemoteAppointment(SQLiteDatabase database, DocumentSnapshot document, long syncedAt) {
        String existingLocalId = "";
        String existingSyncStatus = "";
        try (Cursor cursor = database.query(
                "appointments",
                new String[]{"local_id", "sync_status"},
                "firestore_document_id = ?",
                new String[]{document.getId()}, null, null, null
        )) {
            if (cursor.moveToFirst()) {
                existingLocalId = cursor.getString(0);
                existingSyncStatus = cursor.getString(1);
            }
        }
        if (!existingSyncStatus.isEmpty() && !"SYNCED".equals(existingSyncStatus)) {
            // A newer local outbox mutation exists; never overwrite it with an older pull.
            return;
        }

        Timestamp start = document.getTimestamp("startAt");
        Timestamp end = document.getTimestamp("endAt");
        long startAt = timestampMillis(start);
        long endAt = timestampMillis(end);
        if (startAt <= 0L || endAt <= startAt) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("local_id", existingLocalId.isEmpty() ? document.getId() : existingLocalId);
        values.put("firestore_document_id", document.getId());
        putNullableLong(values, "appointment_id", numberValueLong(document.get("appointmentId")));
        values.put("user_uid", stringValue(document.get("userUid")));
        values.put("barber_id", numberValueLong(document.get("barberId")));
        values.put("service_id", numberValueLong(document.get("serviceId")));
        values.put("start_at_ms", startAt);
        values.put("end_at_ms", endAt);
        values.put("note", stringValue(document.get("note")));
        values.put("status", firstNonEmpty(document.get("status"), "UPCOMING"));
        putNullableString(values, "payment_id", stringValue(document.get("paymentId")));
        values.put("payment_status", firstNonEmpty(document.get("paymentStatus"), "UNPAID"));
        values.put("created_at_ms", timestampMillis(document.getTimestamp("createdAt")));
        values.put("updated_at_ms", timestampMillis(document.getTimestamp("updatedAt")));
        values.put("cancelled_at_ms", timestampMillis(document.getTimestamp("cancelledAt")));
        values.put("sync_status", "SYNCED");
        values.put("last_sync_at_ms", syncedAt);
        values.putNull("sync_error");
        database.insertWithOnConflict("appointments", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void markQueueProcessing(SQLiteDatabase database, long queueId, String localId) {
        long now = System.currentTimeMillis();
        // ContentValues cannot express arithmetic safely; use a parameterized statement.
        database.execSQL("UPDATE sync_queue SET queue_status = ?, last_attempt_at_ms = ?, "
                        + "retry_count = retry_count + 1, last_error = NULL WHERE queue_id = ?",
                new Object[]{"PROCESSING", now, queueId});
        database.execSQL("UPDATE appointments SET sync_attempt_count = sync_attempt_count + 1 "
                        + "WHERE local_id = ?",
                new Object[]{localId});
        updateAppointmentSyncState(database, localId, "SYNCING", null, now);
    }

    private void markQueueDone(SQLiteDatabase database, long queueId, String localId) {
        long now = System.currentTimeMillis();
        database.execSQL("UPDATE sync_queue SET queue_status = ?, completed_at_ms = ?, last_error = NULL "
                        + "WHERE queue_id = ?",
                new Object[]{"DONE", now, queueId});
        updateAppointmentSyncState(database, localId, "SYNCED", null, now);
    }

    private void markQueueFailed(SQLiteDatabase database, long queueId, String localId, String error) {
        long now = System.currentTimeMillis();
        database.execSQL("UPDATE sync_queue SET queue_status = ?, last_error = ? WHERE queue_id = ?",
                new Object[]{"FAILED", error, queueId});
        if (localId != null) {
            updateAppointmentSyncState(database, localId, "SYNC_FAILED", error, now);
        }
    }

    private void updateAppointmentSyncState(
            SQLiteDatabase database,
            String localId,
            String status,
            String error,
            long syncedAt
    ) {
        ContentValues values = new ContentValues();
        values.put("sync_status", status);
        values.put("last_sync_at_ms", syncedAt);
        if (error == null) {
            values.putNull("sync_error");
        } else {
            values.put("sync_error", error);
        }
        database.update("appointments", values, "local_id = ?", new String[]{localId});
    }

    private void markPullSuccess(SQLiteDatabase database, String key, long now) {
        ContentValues values = new ContentValues();
        values.put("sync_key", key);
        values.put("last_success_at_ms", now);
        values.putNull("last_error");
        database.insertWithOnConflict("sync_metadata", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private <T> T await(Task<T> task) throws Exception {
        return Tasks.await(task, FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static String documentIdFor(String localId) {
        String trimmed = localId == null ? "" : localId.trim();
        if (!trimmed.isEmpty() && !trimmed.contains("/")) {
            return trimmed;
        }
        return "offline_" + Long.toUnsignedString(Integer.toUnsignedLong(trimmed.hashCode()));
    }

    private static long documentLong(DocumentSnapshot document, String field, String fallback) {
        long value = numberValueLong(document.get(field));
        return value > 0L ? value : numericId(fallback);
    }

    private static long numericId(String value) {
        if (value == null) {
            return 0L;
        }
        String digits = value.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            long stableHash = Integer.toUnsignedLong(value.hashCode());
            return stableHash == 0L ? 1L : stableHash;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ignored) {
            return Integer.toUnsignedLong(value.hashCode());
        }
    }

    private static long jsonLong(JSONObject json, String firstKey, String secondKey, long fallback) {
        Object value = json.has(firstKey) ? json.opt(firstKey) : json.opt(secondKey);
        return value instanceof Number ? ((Number) value).longValue() : parseLong(String.valueOf(value), fallback);
    }

    private static String jsonString(JSONObject json, String firstKey, String secondKey, String fallback) {
        String value = json.has(firstKey) ? json.optString(firstKey, fallback) : json.optString(secondKey, fallback);
        return value == null ? fallback : value.trim();
    }

    private static long getLong(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? 0L : cursor.getLong(index);
    }

    private static String getString(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? "" : cursor.getString(index).trim();
    }

    private static long timestampMillis(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toDate().getTime();
    }

    private static long numberValueLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return parseLong(stringValue(value), 0L);
    }

    private static double numberValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(stringValue(value));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value == null ? "" : value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value
                : value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String firstNonEmpty(Object first, Object second) {
        String value = stringValue(first);
        return value.isEmpty() ? stringValue(second) : value;
    }

    private static void putNullableLong(ContentValues values, String key, long value) {
        if (value > 0L) {
            values.put(key, value);
        } else {
            values.putNull(key);
        }
    }

    private static void putNullableString(ContentValues values, String key, String value) {
        if (value == null || value.isEmpty() || "0".equals(value)) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    private static String safeError(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty()
                ? exception.getClass().getSimpleName() : message.trim();
    }

    private static final class QueueItem {
        final long queueId;
        final String localId;
        final String operation;
        final String payloadJson;

        QueueItem(long queueId, String localId, String operation, String payloadJson) {
            this.queueId = queueId;
            this.localId = localId;
            this.operation = operation;
            this.payloadJson = payloadJson;
        }
    }

    private static final class LocalAppointment {
        final String localId;
        final String firestoreDocumentId;
        final long appointmentId;
        final String userUid;
        final long barberId;
        final long serviceId;
        final long startAtMs;
        final long endAtMs;
        final String note;
        final String status;
        final String paymentStatus;
        final long createdAtMs;
        final long cancelledAtMs;

        LocalAppointment(String localId, String firestoreDocumentId, long appointmentId, String userUid,
                         long barberId, long serviceId, long startAtMs, long endAtMs, String note,
                         String status, String paymentStatus, long createdAtMs, long cancelledAtMs) {
            this.localId = localId;
            this.firestoreDocumentId = firestoreDocumentId;
            this.appointmentId = appointmentId;
            this.userUid = userUid;
            this.barberId = barberId;
            this.serviceId = serviceId;
            this.startAtMs = startAtMs;
            this.endAtMs = endAtMs;
            this.note = note;
            this.status = status;
            this.paymentStatus = paymentStatus;
            this.createdAtMs = createdAtMs;
            this.cancelledAtMs = cancelledAtMs;
        }
    }
}
