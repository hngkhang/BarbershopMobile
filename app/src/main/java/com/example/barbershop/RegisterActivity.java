package com.example.barbershop;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        findViewById(R.id.linkLogin).setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));

        findViewById(R.id.buttonCreateAccount).setOnClickListener(v -> {
            // TODO: Create Firebase Auth user and persist profile to Firebase/SQLite.
            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
        });
    }
}
