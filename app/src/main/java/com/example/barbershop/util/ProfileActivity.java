package com.example.barbershop.util;

import com.example.barbershop.R;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barbershop.adapters.ProfileMenuAdapter;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private static final String ACTION_CHANGE_PASSWORD = "change_password";
    private static final String ACTION_LOGOUT = "logout";
    private TextView textAvatar;
    private TextView textName;
    private TextView textEmail;
    private TextView textPhone;
    private TextInputLayout layoutEditPhone;
    private TextInputLayout layoutEditName;
    private TextInputEditText inputEditPhone;
    private TextInputEditText inputEditName;
    private View buttonEditProfile;
    private AppCompatButton buttonCancelEdit;
    private AppCompatButton buttonSaveProfile;
    private View layoutEditActions;
    private String currentName = "User";
    private String currentPhone = "";
    private boolean edittingProfile = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null || !currentUser.isEmailVerified()) {
            firebaseAuth.signOut();
            openLoginAndClearTask();
            return;
        }
        bindViews();
        showDefaultProfiles(currentUser);
        bindProfileData(currentUser);
        setupProfileMenu();
        setupActions();
        setupBottomNavigation();
    }

    private void showDefaultProfiles(FirebaseUser currentUser) {
        String email = currentUser.getEmail() == null ? "" : currentUser.getEmail();
        currentName = getDefaultName(email);
        currentPhone = "";
        textAvatar.setText(getInitial(currentName));
        textName.setText(currentName);
        textEmail.setText(email);
        textPhone.setText(getString(R.string.profile_phone_not_updated));
        
    }

    private String getDefaultName(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }

        return "User";
    }

    private void bindViews() {
        textAvatar = findViewById(R.id.textProfileAvatar);
        textName = findViewById(R.id.textProfileName);
        textEmail = findViewById(R.id.textProfileEmail);
        textPhone = findViewById(R.id.textProfilePhone);

        layoutEditName = findViewById(R.id.layoutEditProfileName);
        layoutEditPhone = findViewById(R.id.layoutEditProfilePhone);

        inputEditName = findViewById(R.id.inputEditProfileName);
        inputEditPhone = findViewById(R.id.inputEditProfilePhone);

        buttonEditProfile = findViewById(R.id.buttonEditProfile);
        buttonCancelEdit = findViewById(R.id.buttonCancelProfileEdit);
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile);

        layoutEditActions = findViewById(
                R.id.layoutProfileEditActions
        );
    }

    private void bindProfileData(FirebaseUser user) {
        buttonEditProfile.setEnabled(false);

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String phone = document.getString("phone");

                        if (name != null && !name.trim().isEmpty()) {
                            currentName = name.trim();
                        }

                        if (phone != null && !phone.trim().isEmpty()) {
                            currentPhone = phone.trim();
                        } else {
                            currentPhone = "";
                        }
                    }
                    displayCurrentProfile();

                    buttonEditProfile.setEnabled(true);
                })
                .addOnFailureListener(exception -> {
                    buttonEditProfile.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Không thể tải thông tin hồ sơ: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void displayCurrentProfile() {
        textName.setText(currentName);
        textAvatar.setText(getInitial(currentName));

        if (currentPhone.isEmpty()) {
            textPhone.setText(
                    getString(R.string.profile_phone_not_updated)
            );
        } else {
            textPhone.setText(currentPhone);
        }
    }

    private void saveProfile() {
        String newName = getValue(inputEditName);
        String newPhone = getValue(inputEditPhone);

        layoutEditName.setError(null);
        layoutEditPhone.setError(null);

        if(TextUtils.isEmpty(newName)){
            layoutEditName.setError(getString(R.string.profile_name_required));
            inputEditName.requestFocus();
            return;
        }
        if(newName.length() < 2 || newName.length() > 50){
            layoutEditName.setError(getString(R.string.profile_name_invalid));
            inputEditName.requestFocus();
            return;
        }
        if(TextUtils.isEmpty(newPhone)){
            layoutEditPhone.setError(getString(R.string.profile_phone_required));
            inputEditPhone.requestFocus();
            return;
        }
        String normalizedPhone = newPhone.replaceAll("[\\s()-]", "");
        if (!normalizedPhone.matches("^\\+?[0-9]{9,15}$")){
            layoutEditPhone.setError(getString(R.string.profile_phone_invalid));
            inputEditPhone.requestFocus();
            return;
        }
        setSavingState(true);
        Map<String,Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("phone", newPhone);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection("users")
                .document(currentUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    currentName = newName;
                    currentPhone = newPhone;
                    setSavingState(false);
                    exitEditMode();
                })
                .addOnFailureListener(exception ->{
                   setSavingState(false);

                    Toast.makeText(
                            this,
                            getString(
                                    R.string.profile_update_failed,
                                    exception.getMessage() == null
                                            ? "Unknown error"
                                            : exception.getMessage()
                            ),
                            Toast.LENGTH_LONG
                    ).show();
                });


    }

    private void setSavingState(boolean saving) {
        inputEditName.setEnabled(!saving);
        inputEditPhone.setEnabled(!saving);
        buttonCancelEdit.setEnabled(!saving);
        buttonSaveProfile.setEnabled(!saving);
        buttonSaveProfile.setText(saving ? getString(R.string.profile_saving) : getString(R.string.profile_save));
    }

    private String getValue(TextInputEditText input) {
        if (input.getText() == null) {
            return "";
        }

        return input.getText().toString().trim();
    }


    private void exitEditMode() {
        edittingProfile = false;
        layoutEditName.setError(null);
        layoutEditPhone.setError(null);
        layoutEditName.setVisibility(View.GONE);
        layoutEditPhone.setVisibility(View.GONE);
        textName.setVisibility(View.VISIBLE);
        textPhone.setVisibility(View.VISIBLE);
        layoutEditActions.setVisibility(View.GONE);
        buttonEditProfile.setVisibility(View.VISIBLE);
        textAvatar.setVisibility(View.VISIBLE);

        displayCurrentProfile();
    }

    private void enterEditMode() {
        edittingProfile = true;
        inputEditName.setText(currentName);
        inputEditPhone.setText(currentPhone);
        layoutEditName.setError(null);
        layoutEditPhone.setError(null);
        textPhone.setVisibility(View.GONE);
        textName.setVisibility(View.GONE);
        layoutEditName.setVisibility(View.VISIBLE);
        layoutEditPhone.setVisibility(View.VISIBLE);
        buttonEditProfile.setVisibility(View.GONE);
        textAvatar.setVisibility(View.GONE);
        layoutEditActions.setVisibility(View.VISIBLE);

        inputEditName.requestFocus();
    }


    private void setupProfileMenu() {
        RecyclerView recyclerView = findViewById(R.id.recyclerProfileMenu);
        ProfileMenuAdapter adapter = new ProfileMenuAdapter(this::handleProfileMenuClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        adapter.submitList(Arrays.asList(
                new ProfileMenuAdapter.ProfileMenuItem(getString(R.string.profile_change_password), R.drawable.ic_lock, ACTION_CHANGE_PASSWORD, false),
                new ProfileMenuAdapter.ProfileMenuItem(getString(R.string.profile_logout), R.drawable.ic_logout, ACTION_LOGOUT, true)
        ));
    }

    private void setupActions() {
        buttonEditProfile.setOnClickListener(v->enterEditMode());
        buttonCancelEdit.setOnClickListener(v->exitEditMode());
        buttonSaveProfile.setOnClickListener(V->saveProfile());
    }

    private void handleProfileMenuClick(ProfileMenuAdapter.ProfileMenuItem item) {
        if (ACTION_CHANGE_PASSWORD.equals(item.action)) {
            showChangePasswordDialog();
        } else if (ACTION_LOGOUT.equals(item.action)) {
            showLogoutConfirmation();
        }
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        String email = user == null ? null : user.getEmail();
        if (user == null || TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.profile_password_change_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputLayout layoutCurrentPassword = dialogView.findViewById(R.id.layoutCurrentPassword);
        TextInputLayout layoutNewPassword = dialogView.findViewById(R.id.layoutNewPassword);
        TextInputLayout layoutConfirmPassword = dialogView.findViewById(R.id.layoutConfirmPassword);
        TextInputEditText inputCurrentPassword = dialogView.findViewById(R.id.inputCurrentPassword);
        TextInputEditText inputNewPassword = dialogView.findViewById(R.id.inputNewPassword);
        TextInputEditText inputConfirmPassword = dialogView.findViewById(R.id.inputConfirmPassword);
        AppCompatButton buttonCancel = dialogView.findViewById(R.id.buttonCancelChangePassword);
        AppCompatButton buttonApply = dialogView.findViewById(R.id.buttonApplyChangePassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            buttonCancel.setOnClickListener(view -> dialog.dismiss());
            buttonApply.setOnClickListener(view -> changePassword(
                        dialog,
                        user,
                        layoutCurrentPassword,
                        layoutNewPassword,
                        layoutConfirmPassword,
                        inputCurrentPassword,
                        inputNewPassword,
                        inputConfirmPassword,
                        buttonApply,
                        buttonCancel
                ));
        });
        dialog.show();
    }

    private void changePassword(
            AlertDialog dialog,
            FirebaseUser user,
            TextInputLayout layoutCurrentPassword,
            TextInputLayout layoutNewPassword,
            TextInputLayout layoutConfirmPassword,
            TextInputEditText inputCurrentPassword,
            TextInputEditText inputNewPassword,
            TextInputEditText inputConfirmPassword,
            AppCompatButton buttonApply,
            AppCompatButton buttonCancel
    ) {
        String currentPassword = getValue(inputCurrentPassword);
        String newPassword = getValue(inputNewPassword);
        String confirmedPassword = getValue(inputConfirmPassword);

        layoutCurrentPassword.setError(null);
        layoutNewPassword.setError(null);
        layoutConfirmPassword.setError(null);

        if (TextUtils.isEmpty(currentPassword)) {
            layoutCurrentPassword.setError(getString(R.string.profile_current_password_required));
            inputCurrentPassword.requestFocus();
            return;
        }
        if (newPassword.length() < 6) {
            layoutNewPassword.setError(getString(R.string.profile_new_password_invalid));
            inputNewPassword.requestFocus();
            return;
        }
        if (!newPassword.equals(confirmedPassword)) {
            layoutConfirmPassword.setError(getString(R.string.profile_password_confirmation_mismatch));
            inputConfirmPassword.requestFocus();
            return;
        }

        setPasswordDialogLoading(
                inputCurrentPassword,
                inputNewPassword,
                inputConfirmPassword,
                buttonApply,
                buttonCancel,
                true
        );
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> user.updatePassword(newPassword)
                        .addOnSuccessListener(ignored -> {
                            dialog.dismiss();
                            Toast.makeText(this, R.string.profile_password_change_success, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(exception -> {
                            setPasswordDialogLoading(
                                    inputCurrentPassword,
                                    inputNewPassword,
                                    inputConfirmPassword,
                                    buttonApply,
                                    buttonCancel,
                                    false
                            );
                            Toast.makeText(this, R.string.profile_password_change_failed, Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(exception -> {
                    setPasswordDialogLoading(
                            inputCurrentPassword,
                            inputNewPassword,
                            inputConfirmPassword,
                            buttonApply,
                            buttonCancel,
                            false
                    );
                    layoutCurrentPassword.setError(getString(R.string.profile_current_password_invalid));
                    inputCurrentPassword.requestFocus();
                });
    }

    private void setPasswordDialogLoading(
            TextInputEditText inputCurrentPassword,
            TextInputEditText inputNewPassword,
            TextInputEditText inputConfirmPassword,
            AppCompatButton buttonApply,
            AppCompatButton buttonCancel,
            boolean loading
    ) {
        inputCurrentPassword.setEnabled(!loading);
        inputNewPassword.setEnabled(!loading);
        inputConfirmPassword.setEnabled(!loading);
        buttonApply.setEnabled(!loading);
        buttonCancel.setEnabled(!loading);
    }

    private void showLogoutConfirmation() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout_confirmation, null);
        AppCompatButton buttonCancel = dialogView.findViewById(R.id.buttonCancelLogout);
        AppCompatButton buttonLogout = dialogView.findViewById(R.id.buttonConfirmLogout);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            buttonCancel.setOnClickListener(view -> dialog.dismiss());
            buttonLogout.setOnClickListener(view -> {
                dialog.dismiss();
                performLogout();
            });
        });
        dialog.show();
    }

    private void performLogout() {
        firebaseAuth.signOut();
        Toast.makeText(this, R.string.profile_logout_success, Toast.LENGTH_SHORT).show();
        openLoginAndClearTask();
    }

    private void openLoginAndClearTask() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_profile) {
                return true;
            } else if (itemId == R.id.nav_home) {
                return launchActivityIfAvailable("HomeActivity");
            } else if (itemId == R.id.nav_services) {
                return launchActivityIfAvailable("ServiceListActivity");
            } else if (itemId == R.id.nav_ai_booking) {
                return launchActivityIfAvailable("AIChatBookingActivity");
            } else if (itemId == R.id.nav_appointments) {
                return launchActivityIfAvailable("AppointmentActivity");
            }

            return false;
        });
        findViewById(R.id.buttonAiBooking).setOnClickListener(v -> launchActivityIfAvailable("AIChatBookingActivity"));
    }

    private boolean launchActivityIfAvailable(String simpleClassName) {
        try {
            Class<?> targetClass = Class.forName(getClass().getPackage().getName() + "." + simpleClassName);
            Intent intent = new Intent(this, targetClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        } catch (ClassNotFoundException exception) {
            Toast.makeText(this, getString(R.string.nav_target_unavailable, simpleClassName.replace("Activity", "")), Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.nav_target_not_registered, Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private String getInitial(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "A";
        }

        String[] nameParts = value.trim().split("\\s+");
        String lastName = nameParts[nameParts.length - 1];
        return lastName.substring(0, 1).toUpperCase(Locale.US);
    }
}
