package com.example.barbershop.data;

import com.example.barbershop.models.ShopService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRepository {
    private static final String COLLECTION_SERVICES = "services";

    private final FirebaseFirestore firestore;

    public ServiceRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void getAllServices(BarberRepository.RepositoryCallback<List<ShopService>> callback) {
        firestore.collection(COLLECTION_SERVICES)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ShopService> services = new ArrayList<>();
                    for (int index = 0; index < querySnapshot.size(); index++) {
                        services.add(ShopService.fromDocument(querySnapshot.getDocuments().get(index)));
                    }

                    Collections.sort(services, (left, right) ->
                            compareServiceOrder(left, right)
                    );
                    callback.onSuccess(services);
                })
                .addOnFailureListener(callback::onError);
    }

    private static int compareServiceOrder(ShopService left, ShopService right) {
        int numericCompare = Integer.compare(
                parseServiceOrder(left),
                parseServiceOrder(right)
        );
        if (numericCompare != 0) {
            return numericCompare;
        }

        return left.getName().compareToIgnoreCase(right.getName());
    }

    private static int parseServiceOrder(ShopService service) {
        String serviceId = service.getServiceId();
        if (serviceId == null || serviceId.trim().isEmpty()) {
            return Integer.MAX_VALUE;
        }

        try {
            return Integer.parseInt(serviceId.trim().replaceAll("\\D+", ""));
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }
}
