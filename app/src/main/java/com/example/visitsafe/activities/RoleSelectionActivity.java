package com.example.visitsafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visitsafe.databinding.ActivityRoleSelectionBinding;
import com.example.visitsafe.models.User;
import com.example.visitsafe.utils.RoleNavigator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RoleSelectionActivity extends AppCompatActivity {

    private static final String TAG = "RoleSelection";

    private ActivityRoleSelectionBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoleSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Complete Profile");
        }

        // Check if profile already exists
        checkExistingUser();

        binding.rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.rbSecurity.getId() || checkedId == binding.rbAdmin.getId()) {
                binding.tilFlat.setVisibility(View.GONE);
            } else {
                binding.tilFlat.setVisibility(View.VISIBLE);
            }
        });

        binding.btnComplete.setOnClickListener(v -> saveProfile());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void checkExistingUser() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String phone = mAuth.getCurrentUser().getPhoneNumber();
        if (phone == null) return;
        
        Log.d("AUTH_FLOW", "Checking existing user in RoleSelection: " + phone);
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(phone).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Log.d("AUTH_FLOW", "Existing role found: " + role);
                        if (role != null && !role.trim().isEmpty()) {
                            // Already has a role, go to dashboard directly
                            navigateToDashboard(role);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("AUTH_FLOW", "Error checking existing user: " + e.getMessage());
                });
    }

    private void saveProfile() {
        String name = binding.etName.getText() == null ? "" : binding.etName.getText().toString().trim();
        String flat = binding.etFlat.getText() == null ? "" : binding.etFlat.getText().toString().trim();
        String password = binding.etPassword.getText() == null ? "" : binding.etPassword.getText().toString().trim();
        
        String role;
        if (binding.rbAdmin.isChecked()) {
            role = "admin";
        } else if (binding.rbSecurity.isChecked()) {
            role = "security";
        } else {
            role = "resident";
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String phone = mAuth.getCurrentUser().getPhoneNumber();
        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this, "Phone number not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (name.isEmpty()) {
            binding.etName.setError("Name required");
            return;
        }
        if ("resident".equals(role) && flat.isEmpty()) {
            binding.etFlat.setError("Flat number required");
            return;
        }
        
        // Removed password requirement if not strictly needed for OTP flow, 
        // but user's User model has it, so I'll keep it or set default.
        // The user requirements didn't mention password for registration, but I'll keep it for the model.

        Log.d("AUTH_FLOW", "Attempting to save profile for: " + phone + " with role: " + role);
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Double check if document exists to prevent overwriting
        db.collection("users").document(phone).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String existingRole = documentSnapshot.getString("role");
                        if (existingRole != null && !existingRole.trim().isEmpty()) {
                            Log.d("AUTH_FLOW", "User already exists with role: " + existingRole + ". Not overwriting.");
                            binding.progressBar.setVisibility(View.GONE);
                            navigateToDashboard(existingRole);
                            return;
                        }
                    }

                    // Proceed to save new user
                    String finalFlat = ("security".equals(role) || "admin".equals(role)) ? "N/A" : flat;
                    User newUser = new User(mAuth.getUid(), name, phone, role, finalFlat, password);

                    db.collection("users").document(phone).set(newUser)
                            .addOnSuccessListener(unused -> {
                                Log.d("AUTH_FLOW", "Profile saved successfully for: " + phone);
                                // Try to get FCM Token
                                try {
                                    com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                                            .addOnSuccessListener(token -> db.collection("users").document(phone).update("fcmToken", token));
                                } catch (Exception ignored) {}
                                
                                binding.progressBar.setVisibility(View.GONE);
                                navigateToDashboard(role);
                            })
                            .addOnFailureListener(e -> {
                                binding.progressBar.setVisibility(View.GONE);
                                Log.e("AUTH_FLOW", "Error saving profile: " + e.getMessage());
                                Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("AUTH_FLOW", "Error checking document before save: " + e.getMessage());
                    Toast.makeText(this, "Firestore error. Please retry.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToDashboard(String role) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String phone = mAuth.getCurrentUser().getPhoneNumber();
        Intent intent = RoleNavigator.dashboardIntent(this, role);
        Log.d(TAG, "Navigating with role: " + role + " and phone: " + phone);
        if (intent == null) {
            Toast.makeText(this, "Please select a valid role.", Toast.LENGTH_SHORT).show();
            return;
        }

        intent.putExtra("phone", phone);
        startActivity(intent);
        finish();
    }
}