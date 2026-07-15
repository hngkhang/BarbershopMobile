package com.example.barbershop.services;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private ImageLoader() {
    }

    public static void loadAvatar(ImageView imageView, TextView fallbackView, String imageUrl, String fallbackText) {
        fallbackView.setText(fallbackText);
        fallbackView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);

        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setTag(null);
            return;
        }

        imageView.setTag(imageUrl);
        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(imageUrl);
            MAIN_HANDLER.post(() -> {
                Object currentTag = imageView.getTag();
                if (bitmap == null || currentTag == null || !imageUrl.equals(currentTag.toString())) {
                    return;
                }

                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
                fallbackView.setVisibility(View.GONE);
            });
        });
    }

    public static void loadImage(ImageView imageView, String imageUrl, int placeholderRes) {
        imageView.setImageResource(placeholderRes);

        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setTag(null);
            return;
        }

        imageView.setTag(imageUrl);
        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(imageUrl);
            MAIN_HANDLER.post(() -> {
                Object currentTag = imageView.getTag();
                if (bitmap == null || currentTag == null || !imageUrl.equals(currentTag.toString())) {
                    return;
                }

                imageView.setImageBitmap(bitmap);
            });
        });
    }

    private static Bitmap downloadBitmap(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setDoInput(true);
            connection.connect();

            try (InputStream inputStream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (Exception exception) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
