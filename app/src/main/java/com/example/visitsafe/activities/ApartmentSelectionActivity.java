package com.example.visitsafe.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.visitsafe.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApartmentSelectionActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apartment_selection);

        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("VisitSafePrefs", MODE_PRIVATE);

        checkExistingSession();

        findViewById(R.id.btnCreateApartment).setOnClickListener(v -> showCreateApartmentDialog());
        findViewById(R.id.btnJoinApartment).setOnClickListener(v -> showJoinApartmentDialog());
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            prefs.edit().putBoolean("isDirectLogin", true).apply();
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    private void checkExistingSession() {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String phone = auth.getCurrentUser().getPhoneNumber();
            if (phone != null) {
                db.collection("users").document(phone).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String role = doc.getString("role");
                                Intent intent = com.example.visitsafe.utils.RoleNavigator.dashboardIntent(this, role);
                                if (intent != null) {
                                    intent.putExtra("phone", phone);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            } else {
                                // User authenticated but no profile. Force them to register via invite or complete profile.
                                auth.signOut();
                            }
                        });
            }
        }
    }

    private void showCreateApartmentDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_apartment, null);
        EditText etName = view.findViewById(R.id.etApartmentName);
        EditText etId = view.findViewById(R.id.etApartmentId);
        EditText etAdminName = view.findViewById(R.id.etAdminName);
        EditText etAdminPassword = view.findViewById(R.id.etAdminPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Create Apartment")
                .setView(view)
                .setPositiveButton("Create", null) // Set to null first to override later
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Override the positive button to prevent auto-closing on validation error
        Button createButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        createButton.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String id = etId.getText().toString().trim();
            String adminName = etAdminName.getText().toString().trim();
            String adminPassword = etAdminPassword.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Required");
            } else if (id.isEmpty()) {
                etId.setError("Required");
            } else if (adminName.isEmpty()) {
                etAdminName.setError("Required");
            } else if (adminPassword.length() < 6) {
                etAdminPassword.setError("Minimum 6 characters");
            } else {
                createApartment(name, id, adminName, adminPassword);
                dialog.dismiss();
            }
        });
    }

    private void createApartment(String name, String id, String adminName, String adminPassword) {
        String adminCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Map<String, Object> apartment = new HashMap<>();
        apartment.put("name", name);
        apartment.put("apartmentId", id);
        apartment.put("adminCode", adminCode);
        apartment.put("adminPhone", null); // To be set during first admin login

        db.collection("apartments").document(id).set(apartment)
                .addOnSuccessListener(aVoid -> {
                    // Store details locally to identify the admin during next login
                    prefs.edit()
                            .putString("pendingApartmentId", id)
                            .putString("pendingAdminName", adminName)
                            .putString("pendingAdminPassword", adminPassword)
                            .apply();

                    showSuccessDialog(name, id, adminCode);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showSuccessDialog(String name, String id, String adminCode) {
        String message = "Apartment: " + name + "\nID: " + id + "\nAdmin Code: " + adminCode;

        new AlertDialog.Builder(this)
                .setTitle("Apartment Created Successfully")
                .setMessage(message + "\n\nImportant: Use this code to log in and register as Admin.")
                .setPositiveButton("Copy Code", (dialog, which) -> {
                    copyToClipboard(adminCode);
                    proceedToLogin();
                })
                .setNeutralButton("Save as Image", (dialog, which) -> {
                    saveDetailsAsImage(message);
                    proceedToLogin();
                })
                .setNegativeButton("Login", (dialog, which) -> proceedToLogin())
                .setCancelable(false)
                .show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Admin Code", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Admin Code copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void saveDetailsAsImage(String text) {
        Bitmap bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(24);
        paint.setAntiAlias(true);

        String[] lines = text.split("\n");
        int y = 50;
        for (String line : lines) {
            canvas.drawText(line, 50, y, paint);
            y += 40;
        }

        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                "Apartment_Details_" + System.currentTimeMillis(),
                "VisitSafe Apartment Admin Credentials"
        );

        if (savedImageURL != null) {
            Toast.makeText(this, "Details saved to Gallery", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void proceedToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showJoinApartmentDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_join_apartment, null);
        EditText etId = view.findViewById(R.id.etJoinApartmentId);
        EditText etCode = view.findViewById(R.id.etJoinCode);

        new AlertDialog.Builder(this)
                .setTitle("Join Apartment")
                .setView(view)
                .setPositiveButton("Join", (dialog, which) -> {
                    String id = etId.getText().toString().trim();
                    String code = etCode.getText().toString().trim();
                    validateApartment(id, code);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void validateApartment(String id, String code) {
        db.collection("apartments").document(id).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String adminCode = doc.getString("adminCode");
                        if (code.equals(adminCode)) {
                             prefs.edit().putString("pendingApartmentId", id).apply();
                             startActivity(new Intent(this, LoginActivity.class));
                        } else {
                            // Also check invites collection for resident/security codes
                            checkInviteCode(id, code);
                        }
                    } else {
                        Toast.makeText(this, "Apartment not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkInviteCode(String apartmentId, String code) {
        db.collection("invites").document(code).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && apartmentId.equals(doc.getString("apartmentId"))) {
                        startActivity(new Intent(this, LoginActivity.class));
                    } else {
                        Toast.makeText(this, "Invalid Code", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}