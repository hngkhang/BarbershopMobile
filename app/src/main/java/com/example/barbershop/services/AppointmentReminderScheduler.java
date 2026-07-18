package com.example.barbershop.services;

import com.example.barbershop.R;
import com.example.barbershop.receivers.AppointmentReminderReceiver;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class AppointmentReminderScheduler {
    public static final String CHANNEL_ID = "appointment_reminders";
    public static final String EXTRA_APPOINTMENT_ID = "appointmentId";
    public static final String EXTRA_SERVICE_NAME = "serviceName";
    public static final String EXTRA_BARBER_NAME = "barberName";
    public static final String EXTRA_START_AT_MILLIS = "startAtMillis";

    private static final long ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L;
    private static final long IMMEDIATE_REMINDER_DELAY_MILLIS = 5_000L;

    private AppointmentReminderScheduler() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.appointment_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.appointment_reminder_channel_description));

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void scheduleReminder(
            Context context,
            String appointmentId,
            String serviceName,
            String barberName,
            long startAtMillis
    ) {
        if (appointmentId == null || appointmentId.trim().isEmpty()
                || startAtMillis <= System.currentTimeMillis()) {
            return;
        }

        Context appContext = context.getApplicationContext();
        createNotificationChannel(appContext);

        Intent intent = new Intent(appContext, AppointmentReminderReceiver.class);
        intent.putExtra(EXTRA_APPOINTMENT_ID, appointmentId);
        intent.putExtra(EXTRA_SERVICE_NAME, serviceName);
        intent.putExtra(EXTRA_BARBER_NAME, barberName);
        intent.putExtra(EXTRA_START_AT_MILLIS, startAtMillis);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                requestCodeFor(appointmentId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        long triggerAtMillis = startAtMillis - ONE_DAY_MILLIS;
        long now = System.currentTimeMillis();
        if (triggerAtMillis <= now) {
            triggerAtMillis = now + IMMEDIATE_REMINDER_DELAY_MILLIS;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static int requestCodeFor(String appointmentId) {
        return appointmentId == null ? 0 : appointmentId.hashCode();
    }
}
