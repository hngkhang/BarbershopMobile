package com.example.barbershop.models;

import com.google.firebase.firestore.DocumentSnapshot;

public class ShopService {
    private final String id;
    private final String serviceId;
    private final String name;
    private final String category;
    private final String imageUrl;
    private final double price;
    private final int timeMinutes;

    public ShopService(
            String id,
            String serviceId,
            String name,
            String category,
            String imageUrl,
            double price,
            int timeMinutes
    ) {
        this.id = id;
        this.serviceId = serviceId;
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.price = price;
        this.timeMinutes = timeMinutes;
    }

    public static ShopService fromDocument(DocumentSnapshot document) {
        return new ShopService(
                document.getId(),
                getFieldAsString(document, "serviceId"),
                getString(document, "name"),
                getString(document, "category"),
                getImageUrl(document),
                getNumberAsDouble(document, "price"),
                getNumberAsInt(document, "time")
        );
    }

    private static String getImageUrl(DocumentSnapshot document) {
        String imageUrl = getString(document, "image_url");
        return imageUrl.isEmpty() ? getString(document, "imageUrl") : imageUrl;
    }

    private static String getString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        return value == null ? "" : value.trim();
    }

    private static String getFieldAsString(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static double getNumberAsDouble(DocumentSnapshot document, String field) {
        Number value = document.getDouble(field);
        if (value == null) {
            Object rawValue = document.get(field);
            if (rawValue instanceof Number) {
                value = (Number) rawValue;
            }
        }
        return value == null ? 0.0 : value.doubleValue();
    }

    private static int getNumberAsInt(DocumentSnapshot document, String field) {
        Number value = document.getLong(field);
        if (value == null) {
            Object rawValue = document.get(field);
            if (rawValue instanceof Number) {
                value = (Number) rawValue;
            }
        }
        return value == null ? 0 : value.intValue();
    }

    public String getId() {
        return id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getPrice() {
        return price;
    }

    public int getTimeMinutes() {
        return timeMinutes;
    }
}
