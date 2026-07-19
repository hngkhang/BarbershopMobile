package com.example.barbershop;

import android.app.Application;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;

import androidx.core.content.ContextCompat;

import com.example.barbershop.receivers.NetworkReceiver;
import com.example.barbershop.services.AppointmentReminderScheduler;
import com.example.barbershop.services.SyncService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;


public class BarbershopApplication extends Application {

    private NetworkReceiver networkReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
        }
        AppointmentReminderScheduler.createNotificationChannel(this);

        // CONNECTIVITY_ACTION must be registered at runtime on Android 7+.
        networkReceiver = new NetworkReceiver();
        IntentFilter connectivityFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        ContextCompat.registerReceiver(
                this,
                networkReceiver,
                connectivityFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        SyncService.scheduleSync(this, "application_started");
    }
}
