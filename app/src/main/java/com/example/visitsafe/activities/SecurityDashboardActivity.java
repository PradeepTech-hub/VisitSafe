package com.example.visitsafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.visitsafe.databinding.ActivitySecurityDashboardBinding;
import com.example.visitsafe.models.GroupInvite;
import com.example.visitsafe.models.Invite;
import com.example.visitsafe.models.Visitor;
import com.example.visitsafe.utils.VisitorAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.view.View;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SecurityDashboardActivity extends AppCompatActivity implements VisitorAdapter.OnVisitorActionListener {

    private ActivitySecurityDashboardBinding binding;
    private FirebaseFirestore db;
    private VisitorAdapter adapter;
    private List<Visitor> visitorList = new ArrayList<>();
    private List<Visitor> filteredList = new ArrayList<>();
    private String userPhone;
    private String apartmentName = "";
    private String userApartmentId = "";
    private String selectedStatus = "ALL";
    private Calendar selectedDate = null;

    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            validateQrInvite(result.getContents());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySecurityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userPhone = getIntent().getStringExtra("phone");

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        fetchApartmentName();
        listenForAllVisitors();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isTaskRoot()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                } else {
                    finish();
                }
            }
        });

        binding.btnAddVisitor.setOnClickListener(v -> startActivity(new Intent(this, AddVisitorActivity.class)));

        binding.btnScanQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan Visitor Invite QR");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            qrScannerLauncher.launch(options);
        });

        setupFilters();
        binding.ivLogout.setOnClickListener(v -> logoutUser());
    }

    private void setupFilters() {
        binding.btnFilterDate.setOnClickListener(v -> showDatePicker());
        binding.btnFilterStatus.setOnClickListener(v -> showStatusFilter());
        binding.btnDownloadReport.setOnClickListener(v -> exportReport());
    }

    private void showDatePicker() {
        Calendar c = selectedDate != null ? selectedDate : Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            binding.btnFilterDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.getTime()));
            applyFilters();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showStatusFilter() {
        String[] options = {"ALL", "ENTERED", "EXITED"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Filter by Status")
                .setItems(options, (dialog, which) -> {
                    selectedStatus = options[which];
                    binding.btnFilterStatus.setText(selectedStatus);
                    applyFilters();
                }).show();
    }

    private void applyFilters() {
        filteredList.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        
        for (Visitor v : visitorList) {
            boolean matchesStatus = selectedStatus.equals("ALL") || v.getStatus().equalsIgnoreCase(selectedStatus);
            boolean matchesDate = true;
            
            if (selectedDate != null && v.getCreatedAt() != null) {
                String d1 = sdf.format(selectedDate.getTime());
                String d2 = sdf.format(v.getCreatedAt().toDate());
                matchesDate = d1.equals(d2);
            }
            
            if (matchesStatus && matchesDate) {
                filteredList.add(v);
            }
        }
        
        adapter.updateList(filteredList);
        binding.tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void exportReport() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No records to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Name,Phone,Flat,Purpose,Status,Entry Time,Exit Time\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (Visitor v : filteredList) {
            csv.append(v.getName()).append(",");
            csv.append(v.getPhone()).append(",");
            csv.append(v.getFlatNumber()).append(",");
            csv.append(v.getPurpose()).append(",");
            csv.append(v.getStatus()).append(",");
            csv.append(v.getEntryTime() != null ? sdf.format(v.getEntryTime().toDate()) : "N/A").append(",");
            csv.append(v.getExitTime() != null ? sdf.format(v.getExitTime().toDate()) : "N/A").append("\n");
        }

        try {
            File file = new File(getExternalFilesDir(null), "VisitorReport.csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(csv.toString().getBytes());
            out.close();

            Uri path = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, path);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Report"));
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.example.visitsafe.R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == com.example.visitsafe.R.id.action_edit_profile) {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("phone", userPhone);
            startActivity(intent);
            return true;
        } else if (id == com.example.visitsafe.R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }


    private void logoutUser() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchApartmentName() {
        db.collection("users").document(userPhone).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        userApartmentId = userDoc.getString("apartmentId");
                        if (userApartmentId != null && !userApartmentId.isEmpty()) {
                            db.collection("apartments").document(userApartmentId).get()
                                    .addOnSuccessListener(aptDoc -> {
                                        if (aptDoc.exists()) {
                                            apartmentName = aptDoc.getString("name");
                                            binding.tvApartmentName.setText(apartmentName != null ? apartmentName : "Unnamed Apartment");
                                        } else {
                                            binding.tvApartmentName.setText("Apartment Not Found (" + userApartmentId + ")");
                                        }
                                    });
                        } else {
                            binding.tvApartmentName.setText("No Apartment Linked");
                        }
                    } else {
                        binding.tvApartmentName.setText("Security Profile Not Found");
                    }
                });
    }

    private void validateQrInvite(String visitorId) {
        if (visitorId.startsWith("GRP_")) {
            validateGroupInvite(visitorId);
            return;
        }
        db.collection("visitors").document(visitorId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Visitor visitor = documentSnapshot.toObject(Visitor.class);
                        if (visitor != null) {
                            visitor.setVisitorId(documentSnapshot.getId());
                            // Security isolation check
                            if (userApartmentId != null && !userApartmentId.equals(visitor.getApartmentId())) {
                                Toast.makeText(this, "QR Code belongs to another building", Toast.LENGTH_LONG).show();
                                return;
                            }
                            showVisitorActionDialog(visitor);
                        }
                    } else {
                        // Document doesn't exist in visitors, check invites collection
                        db.collection("invites").document(visitorId).get()
                                .addOnSuccessListener(inviteDoc -> {
                                    if (inviteDoc.exists()) {
                                        Invite invite = inviteDoc.toObject(Invite.class);
                                        if (invite != null) {
                                            // Security isolation check
                                            if (userApartmentId != null && !userApartmentId.equals(invite.getApartmentId())) {
                                                Toast.makeText(this, "QR Code belongs to another building", Toast.LENGTH_LONG).show();
                                                return;
                                            }
                                            showInviteActionDialog(invite);
                                        }
                                    } else {
                                        Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showVisitorActionDialog(Visitor visitor) {
        String status = visitor.getStatus() == null ? "" : visitor.getStatus().toLowerCase(Locale.US);
        String action;
        String nextStatus;
        String timeField;

        if ("approved".equals(status) || "pending".equals(status)) {
            action = "Mark Entry";
            nextStatus = "entered";
            timeField = "entryTime";
        } else if ("entered".equals(status)) {
            action = "Mark Exit";
            nextStatus = "exited";
            timeField = "exitTime";
        } else if ("exited".equals(status)) {
            Toast.makeText(this, "Visitor has already exited", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, "Invalid visitor status: " + status, Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Visitor Details")
                .setMessage("Name: " + visitor.getName() +
                        "\nFlat: " + visitor.getFlatNumber() +
                        "\nPurpose: " + visitor.getPurpose() +
                        "\nPhone: " + visitor.getPhone() +
                        "\nStatus: " + status.toUpperCase())
                .setPositiveButton(action, (dialog, which) -> {
                    updateVisitorStatus(visitor.getVisitorId(), nextStatus, timeField);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showInviteActionDialog(Invite invite) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("New Invite Scanned")
                .setMessage("Visitor: " + invite.getVisitorName() +
                        "\nFlat: " + invite.getFlatNumber() +
                        "\nType: QR Invite")
                .setPositiveButton("Confirm Entry", (dialog, which) -> {
                    createNewVisitorFromInvite(invite);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateVisitorStatus(String visitorId, String newStatus, String timeField) {
        db.collection("visitors").document(visitorId)
                .update("status", newStatus, timeField, Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    String msg = "entered".equalsIgnoreCase(newStatus) ? "Visitor Entered" : "Visitor Exited";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewVisitorFromInvite(Invite invite) {
        // Create visitor record for the first time
        Visitor visitor = new Visitor(invite.getVisitorName(), "N/A", invite.getFlatNumber(), "QR Invite", "Security");
        visitor.setVisitorId(invite.getInviteId());
        visitor.setApartmentId(invite.getApartmentId());
        visitor.setStatus("entered");
        visitor.setEntryTime(Timestamp.now());
        visitor.setCreatedAt(Timestamp.now());

        db.collection("visitors").document(invite.getInviteId()).set(visitor)
                .addOnSuccessListener(aVoid -> {
                    // Also mark invite as used
                    db.collection("invites").document(invite.getInviteId()).update("isUsed", true);
                    Toast.makeText(this, "Visitor Entered", Toast.LENGTH_SHORT).show();
                });
    }

    private void validateGroupInvite(String inviteId) {
        db.collection("groupInvites").document(inviteId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        GroupInvite invite = doc.toObject(GroupInvite.class);
                        if (invite != null) {
                            // Security isolation check
                            if (userApartmentId != null && !userApartmentId.equals(invite.getApartmentId())) {
                                Toast.makeText(this, "QR Code belongs to another building", Toast.LENGTH_LONG).show();
                                return;
                            }
                            showGroupInviteActionDialog(invite);
                        }
                    } else {
                        Toast.makeText(this, "Invalid Group Invite", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showGroupInviteActionDialog(GroupInvite invite) {
        String status = invite.getStatus();
        String action;
        String nextStatus;
        String timeField;

        if ("PENDING".equals(status)) {
            action = "Mark Group Entry";
            nextStatus = "ENTERED";
            timeField = "entryTime";
        } else if ("ENTERED".equals(status)) {
            action = "Mark Group Exit";
            nextStatus = "EXITED";
            timeField = "exitTime";
        } else {
            Toast.makeText(this, "Group already exited", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Group Invite Details")
                .setMessage("Group: " + invite.getGroupName() +
                        "\nSize: " + invite.getTotalVisitors() +
                        "\nFlat: " + invite.getFlatNumber() +
                        "\nPurpose: " + invite.getPurpose() +
                        "\nStatus: " + status)
                .setPositiveButton(action, (dialog, which) -> {
                    updateGroupStatus(invite, nextStatus, timeField);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateGroupStatus(GroupInvite invite, String newStatus, String timeField) {
        db.collection("groupInvites").document(invite.getInviteId())
                .update("status", newStatus, timeField, Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    // Update entryTime in invite object if we just entered
                    if ("ENTERED".equals(newStatus)) {
                        invite.setEntryTime(Timestamp.now());
                    }
                    
                    // Create/Update a visitor record to show in the list
                    Visitor v = new Visitor(
                            invite.getGroupName() + " (Group of " + invite.getTotalVisitors() + ")",
                            "N/A",
                            invite.getFlatNumber(),
                            invite.getPurpose(),
                            userPhone
                    );
                    v.setVisitorId(invite.getInviteId());
                    v.setApartmentId(invite.getApartmentId());
                    v.setStatus(newStatus.toLowerCase());
                    if ("ENTERED".equals(newStatus)) {
                        v.setEntryTime(Timestamp.now());
                        v.setExitTime(null);
                    } else {
                        // For EXITED, we need the original entryTime
                        v.setEntryTime(invite.getEntryTime());
                        v.setExitTime(Timestamp.now());
                    }
                    v.setCreatedAt(invite.getCreatedAt());

                    db.collection("visitors").document(invite.getInviteId()).set(v)
                            .addOnSuccessListener(a -> {
                                String msg = "ENTERED".equals(newStatus) ? "Group Entered" : "Group Exited";
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void setupRecyclerView() {
        adapter = new VisitorAdapter(visitorList, false, this);
        binding.rvVisitors.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVisitors.setAdapter(adapter);
    }

    private void listenForAllVisitors() {
        if (userPhone == null) return;
        
        db.collection("users").document(userPhone).get().addOnSuccessListener(userDoc -> {
            userApartmentId = userDoc.getString("apartmentId");
            if (userApartmentId == null) return;

            binding.progressBar.setVisibility(View.VISIBLE);
            db.collection("visitors")
                    .whereEqualTo("apartmentId", userApartmentId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener((value, error) -> {
                        binding.progressBar.setVisibility(View.GONE);
                        if (error != null) {
                            Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (value != null) {
                            visitorList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                Visitor visitor = doc.toObject(Visitor.class);
                                if (visitor != null) {
                                    visitor.setVisitorId(doc.getId());
                                    visitorList.add(visitor);
                                }
                            }
                            applyFilters();
                        }
                    });
        });
    }

    @Override
    public void onVerifyEntry(Visitor visitor) {
        if (visitor.getStatus() != null && visitor.getStatus().equalsIgnoreCase("approved")) {
            db.collection("visitors").document(visitor.getVisitorId())
                    .update("status", "entered", "entryTime", Timestamp.now())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Visitor Entered", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onMarkExit(Visitor visitor) {
        if (visitor.getStatus() != null && visitor.getStatus().equalsIgnoreCase("entered")) {
            db.collection("visitors").document(visitor.getVisitorId())
                    .update("status", "exited", "exitTime", Timestamp.now())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Visitor Exited", Toast.LENGTH_SHORT).show());
        }
    }

    @Override public void onApprove(Visitor visitor) {}
    @Override public void onReject(Visitor visitor) {}
}