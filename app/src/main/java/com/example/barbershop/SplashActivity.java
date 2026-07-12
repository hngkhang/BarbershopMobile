package com.example.barbershop;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Source;

public class SplashActivity extends AppCompatActivity {

    private static final long FIREBASE_TIMEOUT_MS = 5000L;
    private static final long MESSAGE_DELAY_MS = 1200L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView textLoading;
    private boolean checkFinished = false;
    private boolean navigationScheduled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        textLoading = findViewById(R.id.textLoading);
        checkFirebaseConnection();
    }

    private void checkFirebaseConnection() {
        textLoading.setText("Đang kiểm tra kết nối Firebase...");

        FirebaseApp firebaseApp = FirebaseApp.initializeApp(this);
        if (firebaseApp == null) {
            finishCheck("Chưa cấu hình được Firebase.");
            return;
        }

        handler.postDelayed(() -> {
            if (!checkFinished) {
                finishCheck("Không thể kiểm tra Firebase. Vui lòng kiểm tra Internet.");
            }
        }, FIREBASE_TIMEOUT_MS);

        FirebaseFirestore.getInstance()
                .collection("services")
                .limit(1)
                .get(Source.SERVER)
                .addOnSuccessListener(result ->
                        finishCheck("Kết nối Firebase thành công."))
                .addOnFailureListener(exception -> {
                    if (exception instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) exception).getCode()
                            == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        // Firebase đã phản hồi nhưng Security Rules yêu cầu đăng nhập.
                        finishCheck("Firebase đã kết nối. Đang chuyển đến đăng nhập...");
                    } else {
                        finishCheck("Không thể kết nối Firebase. Vui lòng kiểm tra Internet.");
                    }
                });
    }

    private void finishCheck(String message) {
        if (checkFinished) {
            return;
        }

        checkFinished = true;
        textLoading.setText(message);
        openLoginActivity();
    }

    private void openLoginActivity() {
        if (navigationScheduled) {
            return;
        }

        navigationScheduled = true;
        handler.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, MESSAGE_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}