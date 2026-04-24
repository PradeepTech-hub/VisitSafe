package com.example.visitsafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visitsafe.databinding.ActivityLoginBinding;
import com.example.visitsafe.utils.RoleNavigator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Login");
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // FIX 1: Disable Auto-Login and force Logout on start
        FirebaseAuth.getInstance().signOut();

        binding.btnLogin.setOnClickListener(v -> {
            String phoneNumber = binding.etPhoneNumber.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (phoneNumber.length() != 10) {
                binding.etPhoneNumber.setError("Enter valid 10-digit number");
                return;
            }
            if (password.isEmpty()) {
                binding.etPassword.setError("Enter password");
                return;
            }
            
            loginWithPassword(phoneNumber, password);
        });

        binding.btnGoToRegister.setOnClickListener(v -> {
            String phoneNumber = binding.etPhoneNumber.getText().toString().trim();
            if (phoneNumber.length() != 10) {
                binding.etPhoneNumber.setError("Enter valid 10-digit number");
                return;
            }
            sendOtp(phoneNumber);
        });
    }

    private void loginWithPassword(String phoneNumber, String password) {
        String fullPhone = "+91" + phoneNumber;
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("users").document(fullPhone).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String savedPassword = doc.getString("password");
                        if (password.equals(savedPassword)) {
                            String role = doc.getString("role");
                            // Direct navigation if password matches and already authenticated
                            if (FirebaseAuth.getInstance().getCurrentUser() != null && 
                                fullPhone.equals(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber())) {
                                navigateToDashboard(role, fullPhone);
                            } else {
                                // If not authenticated, we MUST do OTP but mark password as verified
                                Toast.makeText(this, "Phone verification required", Toast.LENGTH_SHORT).show();
                                sendOtp(phoneNumber, true);
                            }
                        } else {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Invalid Password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "User not found. Please register.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private void sendOtp(String phoneNumber) {
        sendOtp(phoneNumber, false);
    }

    private void sendOtp(String phoneNumber, boolean passwordVerified) {
        String fullPhone = "+91" + phoneNumber;
        Log.d("AUTH_FLOW", "Starting OTP flow for: " + fullPhone);
        
        binding.progressBar.setVisibility(View.VISIBLE);
        Intent intent = new Intent(LoginActivity.this, OTPVerificationActivity.class);
        intent.putExtra("phoneNumber", fullPhone);
        intent.putExtra("passwordVerified", passwordVerified);
        startActivity(intent);
        binding.progressBar.setVisibility(View.GONE);
    }
}