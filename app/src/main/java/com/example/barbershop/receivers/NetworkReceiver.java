package com.example.barbershop.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.example.barbershop.services.SyncService;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            return;
        }
        if (SyncService.hasUsableNetwork(context)) {
            SyncService.scheduleSync(context, "network_available");
        }
    }
}
