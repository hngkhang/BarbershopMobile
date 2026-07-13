package com.example.barbershop;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1200L;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        firebaseAuth = FirebaseAuth.getInstance();

        new Handler(Looper.getMainLooper())
                .postDelayed(this::checkLoginSession, SPLASH_DELAY_MS);
    }

    private void checkLoginSession() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        currentUser.reload()
                .addOnCompleteListener(task -> {
                    FirebaseUser refreshedUser =
                            firebaseAuth.getCurrentUser();

                    if (task.isSuccessful()
                            && refreshedUser != null
                            && refreshedUser.isEmailVerified()) {
                        openHomeScreen();
                    } else {
                        firebaseAuth.signOut();
                        openLoginScreen();
                    }
                });
    }

    private void openHomeScreen() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }
}