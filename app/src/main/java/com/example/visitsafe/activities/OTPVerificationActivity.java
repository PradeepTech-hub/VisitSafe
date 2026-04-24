package com.example.visitsafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.visitsafe.databinding.ActivityOtpVerificationBinding;
import android.content.SharedPreferences;
import com.example.visitsafe.models.User;
import com.example.visitsafe.utils.RoleNavigator;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OTPVerificationActivity extends AppCompatActivity {

    private static final String TAG = "OTPVerification";

    private ActivityOtpVerificationBinding binding;
    private FirebaseAuth mAuth;
    private String verificationId;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        mAuth = FirebaseAuth.getInstance();
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        binding.tvSentTo.setText("Code sent to " + phoneNumber);

        updateDebugConsole("Status: Initializing Firebase Auth...");
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Verify Phone");
        }
        
        sendVerificationCode(phoneNumber);

        binding.btnVerifyOtp.setOnClickListener(v -> {
            String code = binding.etOtp.getText().toString().trim();
            if (code.length() < 6) {
                binding.etOtp.setError("Enter 6-digit OTP");
                return;
            }
            verifyCode(code);
        });

        binding.btnResendOtp.setOnClickListener(v -> {
            updateDebugConsole("Status: Resending code...");
            sendVerificationCode(phoneNumber);
        });
    }

    private void updateDebugConsole(String message) {
        if (binding.tvDebugConsole != null) {
            String currentText = binding.tvDebugConsole.getText().toString();
            binding.tvDebugConsole.setText(message + "\n" + currentText);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void sendVerificationCode(String phone) {
        binding.progressBar.setVisibility(View.VISIBLE);
        updateDebugConsole("Requesting OTP for: " + phone);
        
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = 
        new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                updateDebugConsole("Status: Verification Auto-Completed");
                // Sign in directly with the credential provided
                signInWithCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                binding.progressBar.setVisibility(View.GONE);
                updateDebugConsole("ERROR: " + e.getMessage());
                
                android.util.Log.e("AUTH_ERROR", "Detailed Error:", e);
                
                if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                    updateDebugConsole("Reason: Invalid Request (SHA-1/Package Name mismatch?)");
                } else if (e instanceof com.google.firebase.FirebaseTooManyRequestsException) {
                    updateDebugConsole("Reason: SMS Quota Exceeded or Blocked");
                }
                
                Toast.makeText(OTPVerificationActivity.this, "Failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                binding.progressBar.setVisibility(View.GONE);
                verificationId = s;
                updateDebugConsole("Status: OTP Sent Successfully to your phone");
                Toast.makeText(OTPVerificationActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
            }
        };

    private void verifyCode(String code) {
        if (verificationId == null) {
            updateDebugConsole("Error: Verification ID is null. Try resending OTP.");
            Toast.makeText(this, "Please wait, OTP not yet sent", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        try {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithCredential(credential);
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            updateDebugConsole("Error creating credential: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        updateDebugConsole("Signing in with credential...");
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateDebugConsole("Auth Success! Fetching profile...");
                        checkUserRoleAndNavigate();
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        updateDebugConsole("Auth Failed: " + error);
                        Toast.makeText(OTPVerificationActivity.this, "Verification Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRoleAndNavigate() {
        com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            binding.progressBar.setVisibility(View.GONE);
            updateDebugConsole("ERROR: Firebase user is null after sign-in");
            Toast.makeText(this, "Session error. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String phone = user.getPhoneNumber();
        if (phone == null || phone.isEmpty()) {
            binding.progressBar.setVisibility(View.GONE);
            updateDebugConsole("ERROR: Phone number missing from user profile");
            Toast.makeText(this, "Phone number not found in session", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("ADMIN_FLOW", "Checking role for phone: " + phone);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users").document(phone).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing()) return;

                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        String savedPassword = documentSnapshot.getString("password");
                        boolean passwordVerified = getIntent().getBooleanExtra("passwordVerified", false);

                        // Clear direct login flag if set
                        getSharedPreferences("VisitSafePrefs", MODE_PRIVATE).edit().remove("isDirectLogin").apply();
                        
                        if (passwordVerified || savedPassword == null || savedPassword.isEmpty()) {
                            navigateToDashboard(role, phone);
                        } else {
                            // User exists and has a password, but didn't come from password login.
                            // To enforce security, we could ask for password here or just let them in since OTP is strong.
                            // The user requested: "make sure the user when login again the user should login based in the password"
                            // So if they just typed phone and got OTP, we should probably still ask for password if it exists.
                            
                            binding.progressBar.setVisibility(View.GONE);
                            showPasswordConfirmationDialog(role, phone, savedPassword);
                        }
                    } else {
                        // Check if this was a direct login attempt
                        SharedPreferences prefs = getSharedPreferences("VisitSafePrefs", MODE_PRIVATE);
                        boolean isDirectLogin = prefs.getBoolean("isDirectLogin", false);
                        
                        if (isDirectLogin) {
                            prefs.edit().remove("isDirectLogin").apply();
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(OTPVerificationActivity.this, "User not registered. Please join or create apartment.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            finish();
                        } else {
                            // User does not exist, check if they are registering as a new Admin
                            checkPendingAdminRegistration(user, phone);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkPendingAdminRegistration(com.google.firebase.auth.FirebaseUser user, String phone) {
        SharedPreferences prefs = getSharedPreferences("VisitSafePrefs", MODE_PRIVATE);
        String pendingApartmentId = prefs.getString("pendingApartmentId", null);
        String adminName = prefs.getString("pendingAdminName", "Admin");
        String adminPassword = prefs.getString("pendingAdminPassword", "");

        if (pendingApartmentId != null) {
            Log.d("ADMIN_FLOW", "Pending apartment found: " + pendingApartmentId);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            
            db.collection("apartments").document(pendingApartmentId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getString("adminPhone") == null) {
                            // Show confirmation dialog
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Admin Registration")
                                    .setMessage("Do you want to register as the Admin for " + doc.getString("name") + "?")
                                    .setPositiveButton("Yes", (dialog, which) -> {
                                        registerAsAdmin(user, phone, pendingApartmentId, adminName, adminPassword);
                                    })
                                    .setNegativeButton("No", (dialog, which) -> {
                                        prefs.edit()
                                                .remove("pendingApartmentId")
                                                .remove("pendingAdminName")
                                                .remove("pendingAdminPassword")
                                                .apply();
                                        goToInviteScreen();
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            prefs.edit()
                                    .remove("pendingApartmentId")
                                    .remove("pendingAdminName")
                                    .remove("pendingAdminPassword")
                                    .apply();
                            goToInviteScreen();
                        }
                    });
        } else {
            goToInviteScreen();
        }
    }

    private void registerAsAdmin(com.google.firebase.auth.FirebaseUser user, String phone, String apartmentId, String name, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // 1. Update Apartment with Admin Phone
        db.collection("apartments").document(apartmentId).update("adminPhone", phone)
                .addOnSuccessListener(aVoid1 -> {
                    // 2. Create User Profile
                    User adminUser = new User(user.getUid(), name, phone, "admin", "N/A", password);
                    adminUser.setApartmentId(apartmentId);

                    db.collection("users").document(phone).set(adminUser)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d("ADMIN_FLOW", "Admin registered successfully");
                                getSharedPreferences("VisitSafePrefs", MODE_PRIVATE).edit()
                                        .remove("pendingApartmentId")
                                        .remove("pendingAdminName")
                                        .remove("pendingAdminPassword")
                                        .apply();
                                navigateToDashboard("admin", phone);
                            })
                            .addOnFailureListener(e -> {
                                binding.progressBar.setVisibility(View.GONE);
                                Log.e("ADMIN_FLOW", "Failed to create admin user profile", e);
                                Toast.makeText(this, "Failed to create user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("ADMIN_FLOW", "Failed to update apartment with admin phone", e);
                    Toast.makeText(this, "Failed to link apartment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToInviteScreen() {
        binding.progressBar.setVisibility(View.GONE);
        Log.d("ADMIN_FLOW", "No pending admin registration. Going to Invite screen.");
        Intent intent = new Intent(this, EnterInviteActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToDashboard(String role, String phone) {
        binding.progressBar.setVisibility(View.GONE);
        Intent intent = RoleNavigator.dashboardIntent(this, role);
        if (intent != null) {
            intent.putExtra("phone", phone);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void showPasswordConfirmationDialog(String role, String phone, String savedPassword) {
        android.widget.EditText etPassword = new android.widget.EditText(this);
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setHint("Enter your password");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Password Required")
                .setMessage("Please enter your password to continue.")
                .setView(etPassword)
                .setPositiveButton("Login", (dialog, which) -> {
                    String input = etPassword.getText().toString();
                    if (input.equals(savedPassword)) {
                        navigateToDashboard(role, phone);
                    } else {
                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        showPasswordConfirmationDialog(role, phone, savedPassword);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    mAuth.signOut();
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}