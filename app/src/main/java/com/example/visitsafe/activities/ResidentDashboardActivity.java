package com.example.visitsafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.visitsafe.databinding.ActivityResidentDashboardBinding;
import com.example.visitsafe.models.User;
import com.example.visitsafe.models.Visitor;
import com.example.visitsafe.utils.VisitorAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ResidentDashboardActivity extends AppCompatActivity implements VisitorAdapter.OnVisitorActionListener {

    private ActivityResidentDashboardBinding binding;
    private FirebaseFirestore db;
    private VisitorAdapter adapter;
    private List<Visitor> visitorList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResidentDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        fetchApartmentName();
        fetchCurrentUser();

        binding.ivLogout.setOnClickListener(v -> logoutUser());
        binding.btnCreateInvite.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateInviteActivity.class);
            if (currentUser != null) {
                intent.putExtra("phone", currentUser.getPhone());
            }
            startActivity(intent);
        });

        binding.btnGroupInvite.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateGroupInviteActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.example.visitsafe.R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == com.example.visitsafe.R.id.action_edit_profile) {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("phone", currentUser != null ? currentUser.getPhone() : null);
            startActivity(intent);
            return true;
        } else if (id == com.example.visitsafe.R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchApartmentName() {
        String phone = getIntent().getStringExtra("phone");
        if (phone == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            phone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        }
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
                        }).addOnFailureListener(e -> {
                            binding.tvApartmentName.setText("Error loading Apartment");
                        });
                    } else {
                        binding.tvApartmentName.setText("No Apartment Linked");
                    }
                } else {
                    binding.tvApartmentName.setText("User Profile Not Found");
                }
            }).addOnFailureListener(e -> {
                binding.tvApartmentName.setText("Error loading profile");
            });
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupRecyclerView() {
        adapter = new VisitorAdapter(visitorList, true, this);
        binding.rvVisitors.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVisitors.setAdapter(adapter);
    }

    private void fetchCurrentUser() {
        String phone = getIntent().getStringExtra("phone");
        if (phone == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            phone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        }
        
        if (phone == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            logoutUser();
            return;
        }
        
        db.collection("users").document(phone).get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentUser = documentSnapshot.toObject(User.class);
                    if (currentUser != null) {
                        binding.tvWelcome.setText("Welcome, " + currentUser.getName() + " (Flat " + currentUser.getFlatNumber() + ")");
                        listenForVisitors();
                    } else {
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForVisitors() {
        if (currentUser == null || currentUser.getFlatNumber() == null || currentUser.getApartmentId() == null) return;
        
        db.collection("visitors")
                .whereEqualTo("apartmentId", currentUser.getApartmentId())
                .whereEqualTo("flatNumber", currentUser.getFlatNumber())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading visitors: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                        // Sort by entryTime or createdAt manually if firestore order fails without index
                        visitorList.sort((v1, v2) -> {
                            Timestamp t1 = v1.getCreatedAt();
                            Timestamp t2 = v2.getCreatedAt();
                            if (t1 == null || t2 == null) return 0;
                            return t2.compareTo(t1);
                        });
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onApprove(Visitor visitor) {
        db.collection("visitors").document(visitor.getVisitorId())
                .update("status", "approved", "approvedAt", Timestamp.now())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Visitor Approved", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onReject(Visitor visitor) {
        db.collection("visitors").document(visitor.getVisitorId())
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Visitor Rejected", Toast.LENGTH_SHORT).show());
    }

    @Override public void onVerifyEntry(Visitor visitor) {}
    @Override public void onMarkExit(Visitor visitor) {}
}