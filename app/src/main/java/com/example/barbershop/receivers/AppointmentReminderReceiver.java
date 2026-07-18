package com.example.barbershop.receivers;

import com.example.barbershop.R;
import com.example.barbershop.services.AppointmentReminderScheduler;
import com.example.barbershop.util.AppointmentActivity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppointmentReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String appointmentId = intent.getStringExtra(AppointmentReminderScheduler.EXTRA_APPOINTMENT_ID);
        String serviceName = clean(intent.getStringExtra(AppointmentReminderScheduler.EXTRA_SERVICE_NAME));
        String barberName = clean(intent.getStringExtra(AppointmentReminderScheduler.EXTRA_BARBER_NAME));
        long startAtMillis = intent.getLongExtra(AppointmentReminderScheduler.EXTRA_START_AT_MILLIS, -1L);

        AppointmentReminderScheduler.createNotificationChannel(context);

        Intent openAppointmentsIntent = new Intent(context, AppointmentActivity.class);
        openAppointmentsIntent.putExtra(AppointmentReminderScheduler.EXTRA_APPOINTMENT_ID, appointmentId);
        openAppointmentsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                AppointmentReminderScheduler.requestCodeFor(appointmentId),
                openAppointmentsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                AppointmentReminderScheduler.CHANNEL_ID
        )
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(context.getString(R.string.appointment_reminder_title))
                .setContentText(buildReminderText(context, serviceName, barberName, startAtMillis))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        buildReminderText(context, serviceName, barberName, startAtMillis)
                ))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context).notify(
                AppointmentReminderScheduler.requestCodeFor(appointmentId),
                builder.build()
        );
    }

    private String buildReminderText(
            Context context,
            String serviceName,
            String barberName,
            long startAtMillis
    ) {
        String serviceLabel = serviceName.isEmpty()
                ? context.getString(R.string.appointment_reminder_default_service)
                : serviceName;
        String barberLabel = barberName.isEmpty()
                ? context.getString(R.string.appointment_reminder_default_barber)
                : barberName;
        String timeLabel = startAtMillis <= 0L
                ? context.getString(R.string.appointment_reminder_default_time)
                : new SimpleDateFormat("h:mm a", Locale.US).format(new Date(startAtMillis));

        return context.getString(
                R.string.appointment_reminder_content,
                serviceLabel,
                barberLabel,
                timeLabel
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
