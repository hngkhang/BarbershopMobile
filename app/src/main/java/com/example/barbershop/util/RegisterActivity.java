package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText inputName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private TextInputEditText inputPassword;
    private TextInputEditText inputConfirmPassword;
    private CheckBox checkTerms;
    private Button buttonCreateAccount;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputRegisterEmail);
        inputPhone = findViewById(R.id.inputPhone);
        inputPassword = findViewById(R.id.inputRegisterPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        checkTerms = findViewById(R.id.checkTerms);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);

        findViewById(R.id.linkLogin).setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));

        findViewById(R.id.buttonCreateAccount).setOnClickListener(v -> registerUser());
    }
    private String getValue(TextInputEditText input) {
        if (input.getText() == null) {
            return "";
        }

        return input.getText().toString().trim();
    }
    private void registerUser(){

        String name = getValue(inputName);
        String email = getValue(inputEmail);
        String phone = getValue(inputPhone);
        String password = getValue(inputPassword);
        String confirmPassword = getValue(inputConfirmPassword);
        // Validate empty fields
        if (TextUtils.isEmpty(name)){
            inputName.setError("Please input your name.");
            inputName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)){
            inputEmail.setError("Please input your email.");
            inputEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)){
            inputEmail.setError("Please input your phone.");
            inputEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)){
            inputEmail.setError("Please input your password.");
            inputEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)){
            inputEmail.setError("Please confirm your password.");
            inputEmail.requestFocus();
            return;
        }
        if (!checkTerms.isChecked()) {
            Toast.makeText(
                    this,
                    "You need to agree with our terms and policy.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        // Validate emails
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            inputEmail.setError("Invalid email address.");
            inputEmail.requestFocus();
            return;
        }
        // Validate password
        if (password.length()<6){
            inputPassword.setError("Passwords must have at least 6 characters.");
            inputPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)){
            inputConfirmPassword.setError("Your confirmed password is not correct.");
            inputConfirmPassword.requestFocus();
            return;
        }
        setLoading(true);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                   if (!task.isSuccessful()){
                       setLoading(false);
                       showError("Cannot create account", task.getException());
                       return;
                   }
                   FirebaseUser user = firebaseAuth.getCurrentUser();
                   if(user == null){
                       setLoading(false);
                       Toast.makeText(
                               this,
                               "Cannot find created account",
                               Toast.LENGTH_SHORT
                       ).show();
                       return;
                   }

                   saveUserProfile(user, name, email, phone);
                });
    }
    private void setLoading(boolean loading) {
        buttonCreateAccount.setEnabled(!loading);
        buttonCreateAccount.setText(
                loading ? "Creating account..." : "Create Account"
        );
    }
    private void showError(String defaultMessage, Exception exception){
        String message = defaultMessage;
        if(exception != null && exception.getMessage()!= null){
            message += ": "+ exception.getMessage();
        }

        Toast.makeText(this, message,Toast.LENGTH_LONG).show();
    }
    private void saveUserProfile(FirebaseUser user, String name, String email, String phone){
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", user.getUid());
        profile.put("name", name);
        profile.put("email", email);
        profile.put("phone", phone);
        profile.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(user.getUid())
                .set(profile)
                .addOnSuccessListener(unused -> sendVerificationEmail(user))
                .addOnFailureListener(exception ->{
                    user.delete().addOnCompleteListener(deleteTask -> {
                        firebaseAuth.signOut();
                        setLoading(false);

                        Toast.makeText(
                                this,
                                "Cannot save user profile: " + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
                });
    }
    private void sendVerificationEmail(FirebaseUser user){
        firebaseAuth.setLanguageCode("en");

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    firebaseAuth.signOut();
                    setLoading(false);
                    if (task.isSuccessful()){
                        Toast.makeText(
                                this,
                                "Register successfully, please check your email to verify your account.",
                                Toast.LENGTH_LONG
                        ).show();
                        openLoginScreen();
                    } else{
                        showError(
                                "Account created but cannot send verification email.",
                                task.getException()
                        );
                        openLoginScreen();
                    }
                });
    }
    private void openLoginScreen(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
