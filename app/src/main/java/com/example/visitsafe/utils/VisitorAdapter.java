package com.example.visitsafe.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.visitsafe.databinding.ItemVisitorBinding;
import com.example.visitsafe.models.Visitor;
import java.util.List;

public class VisitorAdapter extends RecyclerView.Adapter<VisitorAdapter.VisitorViewHolder> {

    private List<Visitor> visitorList;
    private OnVisitorActionListener listener;
    private boolean isResident;

    public VisitorAdapter(List<Visitor> visitorList, boolean isResident, OnVisitorActionListener listener) {
        this.visitorList = visitorList;
        this.isResident = isResident;
        this.listener = listener;
    }

    public interface OnVisitorActionListener {
        void onApprove(Visitor visitor);
        void onReject(Visitor visitor);
        void onVerifyEntry(Visitor visitor);
        void onMarkExit(Visitor visitor);
    }

    public void updateList(List<Visitor> newList) {
        this.visitorList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VisitorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVisitorBinding binding = ItemVisitorBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VisitorViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VisitorViewHolder holder, int position) {
        Visitor visitor = visitorList.get(position);
        holder.bind(visitor);
    }

    @Override
    public int getItemCount() {
        return visitorList.size();
    }

    class VisitorViewHolder extends RecyclerView.ViewHolder {
        ItemVisitorBinding binding;

        VisitorViewHolder(ItemVisitorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Visitor visitor) {
            String name = visitor.getName() == null ? "N/A" : visitor.getName();
            String phone = visitor.getPhone() == null ? "N/A" : visitor.getPhone();
            String purpose = visitor.getPurpose() == null ? "N/A" : visitor.getPurpose();
            String flat = visitor.getFlatNumber() == null ? "N/A" : visitor.getFlatNumber();
            String status = visitor.getStatus() == null ? "UNKNOWN" : visitor.getStatus();

            binding.tvVisitorName.setText(name);
            binding.tvVisitorPhone.setText(phone);
            binding.tvPurpose.setText("Purpose: " + purpose);
            binding.tvFlat.setText("Flat: " + flat);
            binding.tvStatus.setText(status.toUpperCase());

            // Display times
            if (visitor.getEntryTime() != null || visitor.getExitTime() != null) {
                binding.llTimes.setVisibility(View.VISIBLE);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
                
                if (visitor.getEntryTime() != null) {
                    binding.tvEntryTime.setText("Entry: " + sdf.format(visitor.getEntryTime().toDate()));
                    binding.tvEntryTime.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEntryTime.setVisibility(View.GONE);
                }

                if (visitor.getExitTime() != null) {
                    binding.tvExitTime.setText("Exit: " + sdf.format(visitor.getExitTime().toDate()));
                    binding.tvExitTime.setVisibility(View.VISIBLE);
                } else {
                    binding.tvExitTime.setVisibility(View.GONE);
                }
            } else {
                binding.llTimes.setVisibility(View.GONE);
            }

            // Handle visibility and colors based on status
            updateStatusUI(visitor);

            if (isResident && "pending".equalsIgnoreCase(status)) {
                binding.llActions.setVisibility(View.VISIBLE);
                binding.btnApprove.setOnClickListener(v -> listener.onApprove(visitor));
                binding.btnReject.setOnClickListener(v -> listener.onReject(visitor));
            } else {
                binding.llActions.setVisibility(View.GONE);
            }

            // Exit button visibility for security
            if (!isResident && "entered".equalsIgnoreCase(status)) {
                binding.btnExit.setVisibility(View.VISIBLE);
                binding.btnExit.setOnClickListener(v -> listener.onMarkExit(visitor));
            } else {
                binding.btnExit.setVisibility(View.GONE);
            }

            // Security specific actions could be added here (e.g., Verify OTP)
            if (!isResident && "approved".equalsIgnoreCase(status)) {
                binding.tvOtp.setVisibility(View.GONE);
                binding.getRoot().setOnClickListener(v -> listener.onVerifyEntry(visitor));
            } else if (!isResident && "entered".equalsIgnoreCase(status)) {
                binding.tvOtp.setVisibility(View.GONE);
                binding.getRoot().setOnClickListener(null);
            } else {
                binding.tvOtp.setVisibility(View.GONE);
                binding.getRoot().setOnClickListener(null);
            }
        }

        private void updateStatusUI(Visitor visitor) {
            String status = visitor.getStatus() == null ? "" : visitor.getStatus().toLowerCase();
            switch (status) {
                case "pending":
                    binding.tvStatus.setBackgroundColor(0xFFFFE0B2); // Orange light
                    binding.tvStatus.setTextColor(0xFFF57C00);
                    break;
                case "approved":
                    binding.tvStatus.setBackgroundColor(0xFFC8E6C9); // Green light
                    binding.tvStatus.setTextColor(0xFF388E3C);
                    break;
                case "rejected":
                    binding.tvStatus.setBackgroundColor(0xFFFFCDD2); // Red light
                    binding.tvStatus.setTextColor(0xFFD32F2F);
                    break;
                case "entered":
                    binding.tvStatus.setBackgroundColor(0xFFBBDEFB); // Blue light
                    binding.tvStatus.setTextColor(0xFF1976D2);
                    break;
                case "exited":
                    binding.tvStatus.setBackgroundColor(0xFFF5F5F5); // Grey light
                    binding.tvStatus.setTextColor(0xFF616161);
                    break;
                default:
                    binding.tvStatus.setBackgroundColor(0xFFE0E0E0);
                    binding.tvStatus.setTextColor(0xFF424242);
                    break;
            }
        }
    }
}