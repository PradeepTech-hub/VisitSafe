package com.example.visitsafe.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visitsafe.databinding.ActivityCreateGroupInviteBinding;
import com.example.visitsafe.models.GroupInvite;
import com.example.visitsafe.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.UUID;

public class CreateGroupInviteActivity extends AppCompatActivity {

    private ActivityCreateGroupInviteBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateGroupInviteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        fetchCurrentUser();

        binding.btnGenerate.setOnClickListener(v -> generateGroupInvite());
    }

    private void fetchCurrentUser() {
        String phone = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getPhoneNumber() : null;
        if (phone == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users").document(phone).get()
                .addOnSuccessListener(doc -> {
                    currentUser = doc.toObject(User.class);
                });
    }

    private void generateGroupInvite() {
        String groupName = binding.etGroupName.getText().toString().trim();
        String totalVisitorsStr = binding.etTotalVisitors.getText().toString().trim();
        String purpose = binding.etPurpose.getText().toString().trim();

        if (groupName.isEmpty() || totalVisitorsStr.isEmpty() || purpose.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalVisitors = Integer.parseInt(totalVisitorsStr);
        if (currentUser == null) {
            Toast.makeText(this, "Loading profile...", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        String inviteId = "GRP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        GroupInvite invite = new GroupInvite(
                inviteId,
                currentUser.getPhone(),
                currentUser.getApartmentId(),
                currentUser.getFlatNumber(),
                groupName,
                totalVisitors,
                purpose
        );

        db.collection("groupInvites").document(inviteId).set(invite)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    showQrCode(inviteId);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showQrCode(String data) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400);
            binding.ivQrCode.setImageBitmap(bitmap);
            binding.cvQr.setVisibility(View.VISIBLE);
            binding.tvInviteId.setText("Invite Code: " + data);
            binding.tvInviteId.setVisibility(View.VISIBLE);
            binding.btnGenerate.setVisibility(View.GONE);
            binding.btnShare.setVisibility(View.VISIBLE);
            binding.btnShare.setOnClickListener(v -> shareInvite(bitmap, data));
        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareInvite(Bitmap bitmap, String inviteId) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "GroupInvite_" + inviteId, "Group Entry Invite");
        Uri uri = Uri.parse(path);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, "Group Entry Invite for VisitSafe.\nCode: " + inviteId);
        startActivity(Intent.createChooser(intent, "Share Invite"));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}