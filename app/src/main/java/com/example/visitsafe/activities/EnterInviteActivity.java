package com.example.visitsafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visitsafe.databinding.ActivityEnterInviteBinding;
import com.example.visitsafe.models.Invite;
import com.example.visitsafe.models.User;
import com.example.visitsafe.utils.RoleNavigator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EnterInviteActivity extends AppCompatActivity {

    private ActivityEnterInviteBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Invite currentInvite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEnterInviteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.btnVerifyInvite.setOnClickListener(v -> {
            if (currentInvite == null) {
                String code = binding.etInviteCode.getText().toString().trim().toUpperCase();
                if (code.isEmpty()) {
                    binding.etInviteCode.setError("Enter invite code");
                    return;
                }
                validateInvite(code);
            } else {
                String name = binding.etFullName.getText().toString().trim();
                String flat = binding.etFlatNumber.getText().toString().trim();
                String password = binding.etPassword.getText().toString().trim();
                String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

                if (name.isEmpty()) {
                    binding.etFullName.setError("Required");
                    return;
                }
                if (currentInvite.getRole().equals("resident") && flat.isEmpty()) {
                    binding.etFlatNumber.setError("Required");
                    return;
                }
                if (password.isEmpty()) {
                    binding.etPassword.setError("Required");
                    return;
                }
                if (password.length() < 6) {
                    binding.etPassword.setError("Password must be at least 6 characters");
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    binding.etConfirmPassword.setError("Passwords do not match");
                    return;
                }

                createUserWithDetails(name, flat, password);
            }
        });
    }

    private void validateInvite(String code) {
        binding.progressBar.setVisibility(View.VISIBLE);
        Log.d("INVITE_SYSTEM", "Validating invite: " + code);

        db.collection("invites").document(code).get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        currentInvite = documentSnapshot.toObject(Invite.class);
                        if (currentInvite != null && !currentInvite.isUsed()) {
                            showDetailsForm();
                        } else {
                            Toast.makeText(this, "Invite code already used or invalid", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Invalid invite code", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("INVITE_SYSTEM", "Error validating invite: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDetailsForm() {
        binding.tilInviteCode.setVisibility(View.GONE);
        binding.tvDescription.setText("Complete your profile to join " + currentInvite.getRole());
        binding.llUserDetails.setVisibility(View.VISIBLE);
        binding.btnVerifyInvite.setText("Complete Registration");

        if (currentInvite.getRole().equals("security")) {
            binding.tilFlatNumber.setVisibility(View.GONE);
        }
    }

    private void createUserWithDetails(String name, String flat, String password) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        String phone = mAuth.getCurrentUser().getPhoneNumber();
        String uid = mAuth.getUid();

        User newUser = new User(uid, name, phone, currentInvite.getRole(), flat, password, currentInvite.getApartmentId());

        db.collection("users").document(phone).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    // Mark invite as used
                    db.collection("invites").document(currentInvite.getInviteId()).update("isUsed", true);
                    
                    Log.d("INVITE_SYSTEM", "User created successfully with role: " + currentInvite.getRole());
                    navigateToDashboard(currentInvite.getRole(), phone);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to create user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDashboard(String role, String phone) {
        Intent intent = RoleNavigator.dashboardIntent(this, role);
        if (intent != null) {
            intent.putExtra("phone", phone);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}