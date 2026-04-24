package com.example.visitsafe.utils;

import android.content.Context;
import android.content.Intent;

import com.example.visitsafe.activities.AdminDashboardActivity;
import com.example.visitsafe.activities.ResidentDashboardActivity;
import com.example.visitsafe.activities.SecurityDashboardActivity;

import java.util.Locale;

public final class RoleNavigator {

    private RoleNavigator() {
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toLowerCase(Locale.US);
    }

    public static Intent dashboardIntent(Context context, String role) {
        String normalizedRole = normalizeRole(role);
        if ("admin".equals(normalizedRole)) {
            return new Intent(context, AdminDashboardActivity.class);
        }
        if ("security".equals(normalizedRole)) {
            return new Intent(context, SecurityDashboardActivity.class);
        }
        if ("resident".equals(normalizedRole)) {
            return new Intent(context, ResidentDashboardActivity.class);
        }
        return null;
    }
}

