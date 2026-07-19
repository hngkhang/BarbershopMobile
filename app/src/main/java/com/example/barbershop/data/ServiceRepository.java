package com.example.barbershop.data;

import android.content.Context;

import com.example.barbershop.models.ShopService;
import com.example.barbershop.services.SyncService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ServiceRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final Context appContext;

    public ServiceRepository(Context context) {
        appContext = context.getApplicationContext();
    }

    public void getAllServices(BarberRepository.RepositoryCallback<List<ShopService>> callback) {
        if (!SyncService.hasUsableNetwork(appContext)) {
            loadCachedServices(callback);
            return;
        }
        firestore.collection("services")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ShopService> services = new ArrayList<>();
                    for (int index = 0; index < snapshot.size(); index++) {
                        services.add(ShopService.fromDocument(snapshot.getDocuments().get(index)));
                    }
                    OfflineDataStore.cacheServices(appContext, services);
                    callback.onSuccess(services);
                })
                .addOnFailureListener(exception -> loadCachedServicesOrError(callback, exception));
    }

    public void getServiceById(long serviceId, BarberRepository.RepositoryCallback<ShopService> callback) {
        getAllServices(new BarberRepository.RepositoryCallback<List<ShopService>>() {
            @Override
            public void onSuccess(List<ShopService> services) {
                for (ShopService service : services) {
                    if (String.valueOf(serviceId).equals(service.getServiceId())) {
                        callback.onSuccess(service);
                        return;
                    }
                }
                callback.onSuccess(null);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    private void loadCachedServices(BarberRepository.RepositoryCallback<List<ShopService>> callback) {
        OfflineDataStore.readServices(appContext, new OfflineDataStore.Callback<List<ShopService>>() {
            @Override
            public void onSuccess(List<ShopService> services) {
                callback.onSuccess(services);
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    private void loadCachedServicesOrError(
            BarberRepository.RepositoryCallback<List<ShopService>> callback,
            Exception remoteException
    ) {
        OfflineDataStore.readServices(appContext, new OfflineDataStore.Callback<List<ShopService>>() {
            @Override
            public void onSuccess(List<ShopService> services) {
                if (services.isEmpty()) {
                    callback.onError(remoteException);
                } else {
                    callback.onSuccess(services);
                }
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(remoteException);
            }
        });
    }

}
