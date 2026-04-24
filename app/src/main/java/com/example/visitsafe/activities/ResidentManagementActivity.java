package com.example.visitsafe.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.visitsafe.R;
import com.example.visitsafe.databinding.ActivityResidentManagementBinding;
import com.example.visitsafe.models.User;
import com.example.visitsafe.utils.ResidentAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResidentManagementActivity extends AppCompatActivity implements ResidentAdapter.OnResidentActionListener {

    private ActivityResidentManagementBinding binding;
    private FirebaseFirestore db;
    private ResidentAdapter adapter;
    private final List<User> residentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResidentManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        fetchResidents();

        binding.fabAddResident.setOnClickListener(v -> showResidentDialog(null));
    }

    private void setupRecyclerView() {
        adapter = new ResidentAdapter(residentList, this);
        binding.rvResidents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvResidents.setAdapter(adapter);
    }

    private void fetchResidents() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("users")
                .whereIn("role", Arrays.asList("resident", "Resident"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBar.setVisibility(View.GONE);
                    residentList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setPhone(TextUtils.isEmpty(user.getPhone()) ? doc.getId() : user.getPhone());
                            residentList.add(user);
                        }
                    }
                    adapter.updateList(new ArrayList<>(residentList));
                    binding.tvEmptyState.setVisibility(residentList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Failed to fetch residents: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showResidentDialog(User existingUser) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_resident, null);
        EditText etName = view.findViewById(R.id.etName);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etFlat = view.findViewById(R.id.etFlat);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (existingUser != null) {
            etName.setText(safe(existingUser.getName()));
            etPhone.setText(safe(existingUser.getPhone()));
            etPhone.setEnabled(false);
            etFlat.setText(safe(existingUser.getFlatNumber()));
            builder.setTitle("Edit Resident");
        } else {
            builder.setTitle("Add Resident");
        }

        builder.setView(view)
                .setPositiveButton(existingUser == null ? "Add" : "Update", (dialog, which) -> {
                    String name = safe(etName.getText().toString());
                    String phone = normalizePhone(etPhone.getText().toString());
                    String flat = safe(etFlat.getText().toString());

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(flat)) {
                        Toast.makeText(this, "Name, phone and flat are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!phone.matches("^\\+91\\d{10}$")) {
                        Toast.makeText(this, "Phone must be in +91XXXXXXXXXX format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    User resident = new User();
                    resident.setUserId(phone);
                    resident.setPhone(phone);
                    resident.setName(name);
                    resident.setFlatNumber(flat);
                    resident.setRole("resident");

                    saveResident(resident, existingUser == null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveResident(User user, boolean isNew) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", user.getName());
        updates.put("phone", user.getPhone());
        updates.put("flatNumber", user.getFlatNumber());
        updates.put("role", "resident");
        updates.put("userId", user.getUserId());

        db.collection("users").document(user.getPhone()).set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isNew ? "Resident added" : "Resident updated", Toast.LENGTH_SHORT).show();
                    fetchResidents();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String normalizePhone(String phoneRaw) {
        String digits = phoneRaw == null ? "" : phoneRaw.trim();
        if (digits.startsWith("+91")) {
            return digits;
        }
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        return digits;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public void onEdit(User user) {
        showResidentDialog(user);
    }

    @Override
    public void onDelete(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Resident")
                .setMessage("Are you sure you want to delete " + safe(user.getName()) + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("users").document(user.getPhone()).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Resident deleted", Toast.LENGTH_SHORT).show();
                                fetchResidents();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}