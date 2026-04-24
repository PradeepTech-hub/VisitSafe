package com.example.visitsafe.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visitsafe.databinding.ActivityCreateInviteBinding;
import com.example.visitsafe.models.Invite;
import com.example.visitsafe.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.Calendar;
import java.util.UUID;

public class CreateInviteActivity extends AppCompatActivity {

    private ActivityCreateInviteBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateInviteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        fetchCurrentUser();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create QR Invite");
        }

        binding.btnGenerate.setOnClickListener(v -> generateInvite());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void fetchCurrentUser() {
        String phone = getIntent().getStringExtra("phone");
        if (phone == null && mAuth.getCurrentUser() != null) {
            phone = mAuth.getCurrentUser().getPhoneNumber();
        }
        
        if (phone == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        db.collection("users").document(phone).get()
                .addOnSuccessListener(doc -> {
                    currentUser = doc.toObject(User.class);
                    if (currentUser == null) {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void generateInvite() {
        String name = binding.etVisitorName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentUser == null) {
            Toast.makeText(this, "Profile not loaded. Please wait.", Toast.LENGTH_SHORT).show();
            fetchCurrentUser();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        String inviteId = UUID.randomUUID().toString();
        
        // Valid for 24 hours
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Timestamp validTill = new Timestamp(cal.getTime());

        // Use phone as ID if UID is null (for password-based login)
        String creatorId = mAuth.getUid() != null ? mAuth.getUid() : currentUser.getPhone();

        Invite invite = new Invite(inviteId, creatorId, name, currentUser.getFlatNumber(), validTill);
        invite.setApartmentId(currentUser.getApartmentId());

        db.collection("invites").document(inviteId).set(invite)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(getVisibleViewCount() > 0 ? View.GONE : View.GONE);
                    binding.progressBar.setVisibility(View.GONE);
                    showQrCode(inviteId);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to create invite: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private int getVisibleViewCount() {
        return 0;
    }

    private void showQrCode(String data) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400);
            binding.ivQrCode.setImageBitmap(bitmap);
            binding.cvQr.setVisibility(View.VISIBLE);
            binding.btnGenerate.setVisibility(View.GONE);
            binding.btnShare.setVisibility(View.VISIBLE);
            binding.btnShare.setOnClickListener(v -> shareQrCode(bitmap, data));
        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQrCode(Bitmap bitmap, String inviteId) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Invite_" + inviteId, "QR Invite for VisitSafe");
        if (path == null) {
            Toast.makeText(this, "Failed to prepare image for sharing", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.parse(path);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, "Scan this QR code for your entry invite to VisitSafe.");
        startActivity(Intent.createChooser(intent, "Share Invite via"));
    }
}