package com.example.ngodigitalloan;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

// Firestore Imports
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navView;
    Toolbar adminToolbar;
    ActionBarDrawerToggle toggle;

    // UI TextViews
    TextView tvActiveUsers, tvPendingReq, tvApproved, tvRecovery;
    MaterialButton btnReviewLoans, btnManageUsers;

    // Firebase Variables
    FirebaseFirestore db;
    ListenerRegistration usersListener, loansListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Binding UI
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        adminToolbar = findViewById(R.id.adminToolbar);

        tvActiveUsers = findViewById(R.id.tvActiveUsers);
        tvPendingReq = findViewById(R.id.tvPendingReq);
        tvApproved = findViewById(R.id.tvApproved);
        tvRecovery = findViewById(R.id.tvRecovery);

        btnReviewLoans = findViewById(R.id.btnReviewLoans);
        btnManageUsers = findViewById(R.id.btnManageUsers);

        // Firebase Initialization
        db = FirebaseFirestore.getInstance();

        // Toolbar setup
        setSupportActionBar(adminToolbar);

        // Drawer Toggle Setup
        toggle = new ActionBarDrawerToggle(this, drawerLayout, adminToolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(Color.WHITE);

        // Quick Actions Click Listeners
        btnReviewLoans.setOnClickListener(v -> startActivity(new Intent(this, LoanRequestsActivity.class)));
        btnManageUsers.setOnClickListener(v -> startActivity(new Intent(this, ManageUsersActivity.class)));

        // Menu clicks handle karna
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(AdminDashboardActivity.this, ManageUsersActivity.class));
            } else if (id == R.id.nav_requests) {
                startActivity(new Intent(AdminDashboardActivity.this, LoanRequestsActivity.class));
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(AdminDashboardActivity.this, ReportsActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(AdminDashboardActivity.this, AdminSettingsActivity.class));
            } else if (id == R.id.nav_logout) {
                Toast.makeText(AdminDashboardActivity.this, "Admin Logged Out Successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Load Real-time Data from Firestore
        fetchDashboardStats();

        // Modern "Back Press" system
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void fetchDashboardStats() {
        // 1. Get Total Users (EXCLUDING Admin)
        usersListener = db.collection("Users")
                .whereEqualTo("role", "user")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        tvActiveUsers.setText(String.valueOf(value.size()));
                    }
                });

        // 🌟 2. REAL MATH: Calculate Loans, Disbursed Amount & Recovery % 🌟
        loansListener = db.collection("Loans").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                int pendingCount = 0;
                long totalDisbursed = 0;
                long totalRecovered = 0;

                for (QueryDocumentSnapshot doc : value) {
                    String status = doc.getString("status");

                    if ("Pending".equals(status)) {
                        pendingCount++;
                    } else if ("Approved".equals(status)) {
                        String approvedStr = doc.getString("approvedAmount");
                        String paidStr = doc.getString("paidAmount");

                        long appAmt = 0, paidAmt = 0;
                        try { if (approvedStr != null) appAmt = Long.parseLong(approvedStr); } catch (Exception e) {}
                        try { if (paidStr != null) paidAmt = Long.parseLong(paidStr); } catch (Exception e) {}

                        totalDisbursed += appAmt;
                        totalRecovered += paidAmt;
                    }
                }

                // Update Pending Count
                tvPendingReq.setText(String.valueOf(pendingCount));

                // Update Total Disbursed Amount (Rs 500,000)
                String formattedDisbursed = NumberFormat.getNumberInstance(Locale.US).format(totalDisbursed);
                tvApproved.setText("Rs " + formattedDisbursed);

                // Calculate Recovery Percentage Health
                int recoveryPercent = 0;
                if (totalDisbursed > 0) {
                    recoveryPercent = (int) (((double) totalRecovered / totalDisbursed) * 100);
                }

                tvRecovery.setText(recoveryPercent + "%");

                // Color Code Recovery Health
                if(recoveryPercent >= 75) {
                    tvRecovery.setTextColor(Color.parseColor("#2E7D32")); // Green (Good Health)
                } else if(recoveryPercent >= 40) {
                    tvRecovery.setTextColor(Color.parseColor("#F57C00")); // Orange (Average)
                } else {
                    tvRecovery.setTextColor(Color.parseColor("#D32F2F")); // Red (Poor Health)
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersListener != null) usersListener.remove();
        if (loansListener != null) loansListener.remove();
    }
}