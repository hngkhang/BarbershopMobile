package com.example.barbershop;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewById(R.id.linkCreateAccount).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        findViewById(R.id.buttonLogin).setOnClickListener(v -> {
            // TODO: Authenticate with Firebase Auth before navigating to HomeActivity.
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        });
    }
}
