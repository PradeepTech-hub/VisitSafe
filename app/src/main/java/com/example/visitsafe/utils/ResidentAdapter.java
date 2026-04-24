package com.example.visitsafe.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.visitsafe.R;
import com.example.visitsafe.models.User;

import java.util.List;

public class ResidentAdapter extends RecyclerView.Adapter<ResidentAdapter.ResidentViewHolder> {

    private List<User> residentList;
    private OnResidentActionListener listener;

    public interface OnResidentActionListener {
        void onEdit(User user);
        void onDelete(User user);
    }

    public ResidentAdapter(List<User> residentList, OnResidentActionListener listener) {
        this.residentList = residentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResidentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_resident, parent, false);
        return new ResidentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResidentViewHolder holder, int position) {
        User user = residentList.get(position);
        holder.tvName.setText(user.getName());
        holder.tvPhone.setText(user.getPhone());
        holder.tvFlat.setText("Flat: " + user.getFlatNumber());
        
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(user));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(user));
    }

    @Override
    public int getItemCount() {
        return residentList.size();
    }

    public void updateList(List<User> newList) {
        this.residentList = newList;
        notifyDataSetChanged();
    }

    static class ResidentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvFlat;
        ImageButton btnEdit, btnDelete;

        ResidentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvResidentName);
            tvPhone = itemView.findViewById(R.id.tvResidentPhone);
            tvFlat = itemView.findViewById(R.id.tvResidentFlat);
            btnEdit = itemView.findViewById(R.id.btnEditResident);
            btnDelete = itemView.findViewById(R.id.btnDeleteResident);
        }
    }
}