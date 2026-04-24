package com.example.visitsafe.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.visitsafe.databinding.ActivityEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseFirestore db;
    private String userPhone;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }

        db = FirebaseFirestore.getInstance();
        
        // Get phone from Intent if available (from Login password flow), else from Auth
        userPhone = getIntent().getStringExtra("phone");
        if (userPhone == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        }

        if (userPhone != null) {
            binding.etPhone.setText(userPhone);
            fetchUserData();
        } else {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
        }

        binding.btnSave.setOnClickListener(v -> updateUserData());
    }

    private void fetchUserData() {
        db.collection("users").document(userPhone).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        userRole = documentSnapshot.getString("role");
                        String flat = documentSnapshot.getString("flatNumber");
                        String password = documentSnapshot.getString("password");

                        binding.etName.setText(name);
                        binding.etPassword.setText(password);

                        if ("resident".equalsIgnoreCase(userRole)) {
                            binding.tilFlat.setVisibility(android.view.View.VISIBLE);
                            binding.etFlat.setText(flat);
                        } else {
                            binding.tilFlat.setVisibility(android.view.View.GONE);
                        }
                    }
                });
    }

    private void updateUserData() {
        String newName = binding.etName.getText().toString().trim();
        String newPassword = binding.etPassword.getText().toString().trim();
        String newFlat = binding.etFlat.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("password", newPassword);
        
        if ("resident".equalsIgnoreCase(userRole)) {
            if (newFlat.isEmpty()) {
                Toast.makeText(this, "Flat number required", Toast.LENGTH_SHORT).show();
                return;
            }
            updates.put("flatNumber", newFlat);
        }

        db.collection("users").document(userPhone).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}