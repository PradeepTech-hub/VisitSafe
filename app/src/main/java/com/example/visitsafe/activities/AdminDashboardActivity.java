package com.example.visitsafe.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.visitsafe.databinding.ActivityAdminDashboardBinding;
import com.example.visitsafe.models.Visitor;
import com.example.visitsafe.utils.VisitorAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AdminDashboardActivity extends AppCompatActivity implements VisitorAdapter.OnVisitorActionListener {

    private enum DateFilterType { ALL, TODAY, RANGE }

    private ActivityAdminDashboardBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private VisitorAdapter adapter;
    private final List<Visitor> visitorList = new ArrayList<>();
    private final List<Visitor> filteredList = new ArrayList<>();

    private String selectedStatus = "ALL";
    private String selectedFlat = "ALL";
    private String searchQuery = "";

    private DateFilterType dateFilterType = DateFilterType.ALL;
    private Calendar rangeStartDate;
    private Calendar rangeEndDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        setupListeners();
        fetchApartmentName();
        fetchVisitors();
        updateDateFilterButtonText();

        binding.ivLogout.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchVisitors();
    }

    private void fetchApartmentName() {
        String phone = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getPhoneNumber() : null;
        if (phone != null) {
            db.collection("users").document(phone).get().addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    String apartmentId = userDoc.getString("apartmentId");
                    if (apartmentId != null && !apartmentId.isEmpty()) {
                        db.collection("apartments").document(apartmentId).get().addOnSuccessListener(aptDoc -> {
                            if (aptDoc.exists()) {
                                String aptName = aptDoc.getString("name");
                                binding.tvApartmentName.setText(aptName != null ? aptName : "Unnamed Apartment");
                            } else {
                                binding.tvApartmentName.setText("Apartment Not Found (" + apartmentId + ")");
                            }
                        });
                    } else {
                        binding.tvApartmentName.setText("No Apartment Linked");
                    }
                } else {
                    binding.tvApartmentName.setText("Admin Profile Not Found");
                }
            });
        }
    }

    private void setupListeners() {
        binding.btnManageResidents.setOnClickListener(v ->
                startActivity(new Intent(this, ResidentManagementActivity.class)));

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().toLowerCase(Locale.US).trim();
                applyFilters();
            }
        });

        binding.btnFilterDate.setOnClickListener(v -> showDateFilterDialog());
        binding.btnFilterStatus.setOnClickListener(v -> showStatusFilter());
        binding.btnFilterFlat.setOnClickListener(v -> showFlatFilter());
        binding.btnDownloadReport.setOnClickListener(v -> exportReport());
        binding.btnGenerateInvite.setOnClickListener(v -> {
            Intent intent = new Intent(this, EnterInviteActivity.class); // Or a new dedicated one
            // Requirement says: AdminDashboard, add button "Generate Invite"
            // Let's reuse or create a simple dialog/activity for Admin to generate
            showGenerateInviteDialog();
        });
    }

    private void showGenerateInviteDialog() {
        String[] options = {"User Invite (Resident/Security)", "Group Entry Invite"};
        new AlertDialog.Builder(this)
                .setTitle("Select Invite Type")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showUserInviteDialog();
                    } else {
                        startActivity(new Intent(this, CreateGroupInviteActivity.class));
                    }
                })
                .show();
    }

    private void showUserInviteDialog() {
        String[] roles = {"resident", "security"};
        new AlertDialog.Builder(this)
                .setTitle("Select Role")
                .setItems(roles, (dialog, which) -> {
                    String selectedRole = roles[which];
                    generateInviteCode(selectedRole);
                })
                .show();
    }

    private void generateInviteCode(String role) {
        String inviteId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String adminPhone = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getPhoneNumber() : "Admin";
        
        db.collection("users").document(adminPhone).get().addOnSuccessListener(userDoc -> {
            String apartmentId = userDoc.getString("apartmentId");
            if (apartmentId == null) {
                Toast.makeText(this, "Admin profile not linked to an apartment", Toast.LENGTH_SHORT).show();
                return;
            }

            com.example.visitsafe.models.Invite invite = new com.example.visitsafe.models.Invite(
                    inviteId, apartmentId, role, adminPhone
            );

            db.collection("invites").document(inviteId).set(invite)
                    .addOnSuccessListener(aVoid -> {
                        new AlertDialog.Builder(this)
                                .setTitle("Invite Generated")
                                .setMessage("Code: " + inviteId + "\nRole: " + role)
                                .setPositiveButton("Copy", (d, w) -> {
                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                    android.content.ClipData clip = android.content.ClipData.newPlainText("Invite Code", inviteId);
                                    clipboard.setPrimaryClip(clip);
                                    Toast.makeText(this, "Code copied", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Close", null)
                                .show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void setupRecyclerView() {
        adapter = new VisitorAdapter(filteredList, false, this);
        binding.rvVisitors.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVisitors.setAdapter(adapter);
    }

    private void fetchVisitors() {
        String phone = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getPhoneNumber() : null;
        if (phone == null) return;

        db.collection("users").document(phone).get().addOnSuccessListener(userDoc -> {
            String apartmentId = userDoc.getString("apartmentId");
            if (apartmentId == null) return;

            binding.progressBar.setVisibility(View.VISIBLE);
            db.collection("visitors")
                    .whereEqualTo("apartmentId", apartmentId)
                    .orderBy("entryTime", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        binding.progressBar.setVisibility(View.GONE);
                        visitorList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Visitor visitor = doc.toObject(Visitor.class);
                            if (visitor != null) {
                                visitor.setVisitorId(doc.getId());
                                visitorList.add(visitor);
                            }
                        }
                        applyFilters();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to load visitors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        visitorList.clear();
                        applyFilters();
                    });
        });
    }

    private void showDateFilterDialog() {
        String[] options = {"All", "Today", "Custom range"};
        new AlertDialog.Builder(this)
                .setTitle("Filter by Date")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        dateFilterType = DateFilterType.ALL;
                        rangeStartDate = null;
                        rangeEndDate = null;
                        updateDateFilterButtonText();
                        applyFilters();
                    } else if (which == 1) {
                        dateFilterType = DateFilterType.TODAY;
                        rangeStartDate = null;
                        rangeEndDate = null;
                        updateDateFilterButtonText();
                        applyFilters();
                    } else {
                        pickStartDateForRange();
                    }
                })
                .show();
    }

    private void pickStartDateForRange() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            rangeStartDate = Calendar.getInstance();
            rangeStartDate.set(year, month, dayOfMonth, 0, 0, 0);
            rangeStartDate.set(Calendar.MILLISECOND, 0);
            pickEndDateForRange();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickEndDateForRange() {
        Calendar from = rangeStartDate != null ? rangeStartDate : Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            rangeEndDate = Calendar.getInstance();
            rangeEndDate.set(year, month, dayOfMonth, 23, 59, 59);
            rangeEndDate.set(Calendar.MILLISECOND, 999);

            if (rangeStartDate != null && rangeEndDate.before(rangeStartDate)) {
                Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show();
                return;
            }

            dateFilterType = DateFilterType.RANGE;
            updateDateFilterButtonText();
            applyFilters();
        }, from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateFilterButtonText() {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (dateFilterType == DateFilterType.TODAY) {
            binding.btnFilterDate.setText("Today");
        } else if (dateFilterType == DateFilterType.RANGE && rangeStartDate != null && rangeEndDate != null) {
            binding.btnFilterDate.setText(format.format(rangeStartDate.getTime()) + " - " + format.format(rangeEndDate.getTime()));
        } else {
            binding.btnFilterDate.setText("All Dates");
        }
    }

    private void showStatusFilter() {
        String[] options = {"ALL", "APPROVED", "ENTERED", "EXITED"};
        new AlertDialog.Builder(this)
                .setTitle("Filter by Status")
                .setItems(options, (dialog, which) -> {
                    selectedStatus = options[which];
                    binding.btnFilterStatus.setText(selectedStatus);
                    applyFilters();
                })
                .show();
    }

    private void showFlatFilter() {
        Set<String> flatSet = new LinkedHashSet<>();
        flatSet.add("ALL");
        for (Visitor visitor : visitorList) {
            String flat = sanitize(visitor.getFlatNumber());
            if (!flat.equals("N/A")) {
                flatSet.add(flat);
            }
        }

        String[] options = flatSet.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Filter by Flat")
                .setItems(options, (dialog, which) -> {
                    selectedFlat = options[which];
                    binding.btnFilterFlat.setText("Flat: " + selectedFlat);
                    applyFilters();
                })
                .show();
    }

    private void applyFilters() {
        filteredList.clear();

        for (Visitor visitor : visitorList) {
            String status = sanitize(visitor.getStatus());
            String flat = sanitize(visitor.getFlatNumber());
            String name = sanitize(visitor.getName()).toLowerCase(Locale.US);
            String phone = sanitize(visitor.getPhone()).toLowerCase(Locale.US);

            boolean matchesStatus = "ALL".equalsIgnoreCase(selectedStatus) || status.equalsIgnoreCase(selectedStatus);
            boolean matchesFlat = "ALL".equalsIgnoreCase(selectedFlat) || flat.equalsIgnoreCase(selectedFlat);
            boolean matchesSearch = searchQuery.isEmpty() || name.contains(searchQuery) || phone.contains(searchQuery);
            boolean matchesDate = matchesDateFilter(resolveRelevantDate(visitor));

            if (matchesStatus && matchesFlat && matchesSearch && matchesDate) {
                filteredList.add(visitor);
            }
        }

        adapter.updateList(filteredList);
        binding.tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private Timestamp resolveRelevantDate(Visitor visitor) {
        if (visitor.getEntryTime() != null) {
            return visitor.getEntryTime();
        }
        return visitor.getCreatedAt();
    }

    private boolean matchesDateFilter(Timestamp timestamp) {
        if (dateFilterType == DateFilterType.ALL) {
            return true;
        }
        if (timestamp == null) {
            return false;
        }

        Calendar visitorDate = Calendar.getInstance();
        visitorDate.setTime(timestamp.toDate());

        if (dateFilterType == DateFilterType.TODAY) {
            Calendar today = Calendar.getInstance();
            return today.get(Calendar.YEAR) == visitorDate.get(Calendar.YEAR)
                    && today.get(Calendar.DAY_OF_YEAR) == visitorDate.get(Calendar.DAY_OF_YEAR);
        }

        if (dateFilterType == DateFilterType.RANGE && rangeStartDate != null && rangeEndDate != null) {
            return !visitorDate.before(rangeStartDate) && !visitorDate.after(rangeEndDate);
        }

        return true;
    }

    private void exportReport() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No records to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("name,phone,flat,purpose,status,entryTime,exitTime\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (Visitor visitor : filteredList) {
            csv.append(csvValue(visitor.getName())).append(",");
            csv.append(csvValue(visitor.getPhone())).append(",");
            csv.append(csvValue(visitor.getFlatNumber())).append(",");
            csv.append(csvValue(visitor.getPurpose())).append(",");
            csv.append(csvValue(visitor.getStatus())).append(",");
            csv.append(csvValue(visitor.getEntryTime() != null ? sdf.format(visitor.getEntryTime().toDate()) : "N/A")).append(",");
            csv.append(csvValue(visitor.getExitTime() != null ? sdf.format(visitor.getExitTime().toDate()) : "N/A")).append("\n");
        }

        try {
            File reportsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (reportsDir == null) {
                Toast.makeText(this, "Storage unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!reportsDir.exists() && !reportsDir.mkdirs()) {
                Toast.makeText(this, "Unable to create report folder", Toast.LENGTH_SHORT).show();
                return;
            }

            String timeTag = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Calendar.getInstance().getTime());
            File file = new File(reportsDir, "AdminVisitorReport_" + timeTag + ".csv");
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            }

            Toast.makeText(this, "Report saved: " + file.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String csvValue(String value) {
        String safe = sanitize(value).replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private String sanitize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        return value.trim();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.example.visitsafe.R.menu.dashboard_menu, menu);
        MenuItem editProfile = menu.findItem(com.example.visitsafe.R.id.action_edit_profile);
        if (editProfile != null) {
            editProfile.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == com.example.visitsafe.R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onApprove(Visitor visitor) {}
    @Override public void onReject(Visitor visitor) {}
    @Override public void onVerifyEntry(Visitor visitor) {}
    @Override public void onMarkExit(Visitor visitor) {}
}