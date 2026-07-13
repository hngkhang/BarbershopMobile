package com.example.barbershop;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private Button buttonLogin;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        buttonLogin = findViewById(R.id.buttonLogin);


        findViewById(R.id.linkCreateAccount).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        buttonLogin.setOnClickListener(v -> loginUser());

        findViewById(R.id.linkForgotPassword).setOnClickListener(v-> sendPasswordResetEmail());
    }

    private void sendPasswordResetEmail() {
        String email = getValue(inputEmail);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            inputEmail.setError("Please input valid email before reset your password");
            inputEmail.requestFocus();
            return;
        }
        firebaseAuth.setLanguageCode("en");
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "Reset password email has been sent.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        showError(
                                "Cannot send reset password email",
                                task.getException()
                        );
                    }
                });

    }

    private void loginUser(){
        String email = getValue(inputEmail);
        String password = getValue(inputPassword);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            inputEmail.setError("Invalid email address.");
            inputEmail.requestFocus();
            return;
        }
        if (password.isEmpty()){
            inputPassword.setError("Please input your password.");
            inputPassword.requestFocus();
            return;
        }
        setLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (!task.isSuccessful()){
                        firebaseAuth.signOut();
                        showError("Log in failed", task.getException());
                        return;
                    }
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if(user == null){
                        firebaseAuth.signOut();

                        Toast.makeText(this, "Cannot find user information", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!user.isEmailVerified()){
                        showEmailVerificationDialog(user);
                        return;
                    }
                    openHomeScreen();
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

    private void showEmailVerificationDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Unverified email!")
                .setMessage("You haven't verified your email yet. Please check your mail!")
                .setPositiveButton("Resend email.", (dialog, which) ->
                        resendVerificationEmail(user)
                        )
                .setNegativeButton("Close", (dialog, which) -> firebaseAuth.signOut())
                .setOnCancelListener(dialog -> firebaseAuth.signOut())
                .show();
    }

    private void resendVerificationEmail(FirebaseUser user) {
        firebaseAuth.setLanguageCode("en");
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    firebaseAuth.signOut();
                    setLoading(false);
                    if (task.isSuccessful()){
                        Toast.makeText(
                                this,
                                "Resend successfully, please check your email.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else{
                        showError(
                                "Cannot resend verification email.",
                                task.getException()
                        );
                    }
                });
    }

    private String getValue(TextInputEditText input) {
        if (input.getText() == null) {
            return "";
        }

        return input.getText().toString().trim();
    }
    private void setLoading(boolean loading){
        buttonLogin.setEnabled(!loading);
        buttonLogin.setText(loading ? "Đang đăng nhập..." : "Login");
    }
    private void showError(String defaultMessage, Exception exception){
        String message = defaultMessage;
        if (exception != null && exception.getMessage() != null) {
            message += ": " + exception.getMessage();
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
