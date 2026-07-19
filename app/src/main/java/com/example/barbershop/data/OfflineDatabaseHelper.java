package com.example.barbershop.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class OfflineDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "art_barbershop_offline.db";
    private static final int DATABASE_VERSION = 1;

    public OfflineDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        super.onConfigure(database);
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE schema_info (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        database.execSQL("INSERT INTO schema_info (key, value) VALUES ('schema_version', '1')");
        database.execSQL("INSERT INTO schema_info (key, value) VALUES ('purpose', 'Offline booking cache and sync queue')");

        database.execSQL("CREATE TABLE local_users ("
                + "firebase_uid TEXT PRIMARY KEY, user_id INTEGER, name TEXT NOT NULL DEFAULT '', "
                + "email TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '', "
                + "created_at_ms INTEGER, updated_at_ms INTEGER, synced_at_ms INTEGER)");

        database.execSQL("CREATE TABLE services ("
                + "service_id INTEGER PRIMARY KEY, firestore_document_id TEXT UNIQUE, "
                + "name TEXT NOT NULL, category TEXT, image_url TEXT, "
                + "price REAL NOT NULL DEFAULT 0, duration_minutes INTEGER NOT NULL DEFAULT 0, "
                + "active INTEGER NOT NULL DEFAULT 1, synced_at_ms INTEGER)");

        database.execSQL("CREATE TABLE barbers ("
                + "barber_id INTEGER PRIMARY KEY, firestore_document_id TEXT UNIQUE, "
                + "name TEXT NOT NULL, avatar_url TEXT, experience TEXT, "
                + "rating REAL NOT NULL DEFAULT 0, active INTEGER NOT NULL DEFAULT 1, synced_at_ms INTEGER)");

        database.execSQL("CREATE TABLE barber_schedules ("
                + "schedule_id INTEGER PRIMARY KEY, firestore_document_id TEXT UNIQUE, "
                + "barber_id INTEGER NOT NULL, start_at_ms INTEGER NOT NULL, end_at_ms INTEGER NOT NULL, "
                + "synced_at_ms INTEGER, CHECK (end_at_ms > start_at_ms))");

        database.execSQL("CREATE TABLE appointments ("
                + "local_id TEXT PRIMARY KEY, firestore_document_id TEXT UNIQUE, appointment_id INTEGER UNIQUE, "
                + "user_uid TEXT NOT NULL, barber_id INTEGER NOT NULL, service_id INTEGER NOT NULL, "
                + "start_at_ms INTEGER NOT NULL, end_at_ms INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '', "
                + "status TEXT NOT NULL DEFAULT 'UPCOMING', payment_id TEXT, "
                + "payment_status TEXT NOT NULL DEFAULT 'UNPAID', created_at_ms INTEGER NOT NULL, "
                + "updated_at_ms INTEGER, cancelled_at_ms INTEGER, "
                + "sync_status TEXT NOT NULL DEFAULT 'PENDING_SYNC', last_sync_at_ms INTEGER, "
                + "sync_error TEXT, sync_attempt_count INTEGER NOT NULL DEFAULT 0, "
                + "CHECK (end_at_ms > start_at_ms))");

        database.execSQL("CREATE TABLE sync_queue ("
                + "queue_id INTEGER PRIMARY KEY AUTOINCREMENT, entity_type TEXT NOT NULL, "
                + "entity_local_id TEXT NOT NULL, operation TEXT NOT NULL, payload_json TEXT NOT NULL, "
                + "queue_status TEXT NOT NULL DEFAULT 'PENDING', retry_count INTEGER NOT NULL DEFAULT 0, "
                + "created_at_ms INTEGER NOT NULL, last_attempt_at_ms INTEGER, completed_at_ms INTEGER, "
                + "last_error TEXT, FOREIGN KEY (entity_local_id) REFERENCES appointments(local_id) ON DELETE CASCADE)");

        database.execSQL("CREATE TABLE sync_metadata ("
                + "sync_key TEXT PRIMARY KEY, last_success_at_ms INTEGER, last_error TEXT)");

        database.execSQL("CREATE UNIQUE INDEX idx_sync_queue_pending_create ON sync_queue "
                + "(entity_local_id, operation) WHERE operation = 'CREATE' "
                + "AND queue_status IN ('PENDING', 'PROCESSING', 'FAILED')");
        database.execSQL("CREATE INDEX idx_appointments_user_start ON appointments (user_uid, start_at_ms)");
        database.execSQL("CREATE INDEX idx_appointments_barber_time ON appointments (barber_id, start_at_ms, end_at_ms)");
        database.execSQL("CREATE INDEX idx_appointments_sync_status ON appointments (sync_status, created_at_ms)");
        database.execSQL("CREATE INDEX idx_barber_schedules_barber_time ON barber_schedules (barber_id, start_at_ms, end_at_ms)");
        database.execSQL("CREATE INDEX idx_sync_queue_status ON sync_queue (queue_status, created_at_ms)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // Version 1 is the initial schema. Add non-destructive migrations here for later versions.
    }
}
