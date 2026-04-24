package com.example.visitsafe.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visitsafe.databinding.ActivityAddVisitorBinding;
import com.example.visitsafe.models.Visitor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

public class AddVisitorActivity extends AppCompatActivity {

    private ActivityAddVisitorBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddVisitorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add New Visitor");
        }

        binding.btnSubmit.setOnClickListener(v -> submitRequest());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void submitRequest() {
        String name = binding.etVisitorName.getText().toString().trim();
        String phone = binding.etVisitorPhone.getText().toString().trim();
        String flat = binding.etFlatNumber.getText().toString().trim();
        String purpose = binding.etPurpose.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || flat.isEmpty() || purpose.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSubmit.setEnabled(false);

        // Fetch user's apartmentId first
        String currentUserPhone = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getPhoneNumber() : null;
        if (currentUserPhone == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUserPhone).get().addOnSuccessListener(userDoc -> {
            String apartmentId = userDoc.getString("apartmentId");
            
            String visitorId = UUID.randomUUID().toString();
            String securityId = mAuth.getCurrentUser() != null ? mAuth.getUid() : "Manual_Entry";
            
            Visitor visitor = new Visitor(name, "+91" + phone, flat, purpose, securityId);
            visitor.setVisitorId(visitorId);
            visitor.setApartmentId(apartmentId);
            visitor.setCreatedAt(com.google.firebase.Timestamp.now());

            db.collection("visitors").document(visitorId).set(visitor)
                    .addOnSuccessListener(aVoid -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Approval request sent to Resident", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSubmit.setEnabled(true);
                        Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
        });
    }
}