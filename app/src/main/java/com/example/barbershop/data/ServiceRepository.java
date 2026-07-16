package com.example.barbershop.data;

import com.example.barbershop.models.ShopService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ServiceRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void getAllServices(BarberRepository.RepositoryCallback<List<ShopService>> callback) {
        firestore.collection("services")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ShopService> services = new ArrayList<>();
                    for (int index = 0; index < snapshot.size(); index++) {
                        services.add(ShopService.fromDocument(snapshot.getDocuments().get(index)));
                    }
                    callback.onSuccess(services);
                })
                .addOnFailureListener(callback::onError);
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

}
