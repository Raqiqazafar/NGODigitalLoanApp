package com.example.ngodigitalloan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// Firestore Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    FloatingActionButton fabAddLoan;
    BottomNavigationView bottomNavigationView;

    // UI Elements
    TextView userNameText, tvRemainingBalance, tvTotalDebt, greetingText, tvHeaderAvatar;
    MaterialCardView avatarCard, cardDebt, cardBalance;
    LinearLayout btnQuickApply, btnQuickRepay, btnQuickHistory;

    // Firebase Variables
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ListenerRegistration userListener, loansListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // UI Binding
        fabAddLoan = findViewById(R.id.fabAddLoan);
        bottomNavigationView = findViewById(R.id.bottomNav);
        userNameText = findViewById(R.id.userNameText);
        tvRemainingBalance = findViewById(R.id.tvRemainingBalance);
        tvTotalDebt = findViewById(R.id.tvTotalDebt);
        greetingText = findViewById(R.id.greetingText);

        // New Modern Elements
        tvHeaderAvatar = findViewById(R.id.tvHeaderAvatar);
        avatarCard = findViewById(R.id.avatarCard);
        cardDebt = findViewById(R.id.cardDebt);
        cardBalance = findViewById(R.id.cardBalance);

        btnQuickApply = findViewById(R.id.btnQuickApply);
        btnQuickRepay = findViewById(R.id.btnQuickRepay);
        btnQuickHistory = findViewById(R.id.btnQuickHistory);

        // Firebase Initialization
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup Functionalities
        setDynamicGreeting();
        loadUserData();

        // 🌟 CLICK LISTENERS 🌟

        // Avatar Click -> Jao Profile Par
        avatarCard.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));

        // Quick Actions
        btnQuickApply.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ApplyLoanActivity.class)));
        fabAddLoan.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ApplyLoanActivity.class)));

        btnQuickRepay.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PayInstallmentActivity.class)));
        cardDebt.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PayInstallmentActivity.class)));

        btnQuickHistory.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, RepaymentHistoryActivity.class)));
        cardBalance.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, RepaymentHistoryActivity.class)));

        // Bottom Navigation Logic
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(HomeActivity.this, LoansListActivity.class));
                overridePendingTransition(0, 0); // Smooth transition
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    // 🌟 SMART GREETING 🌟
    private void setDynamicGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if (timeOfDay >= 0 && timeOfDay < 12) {
            greetingText.setText("Good Morning ☀️,");
        } else if (timeOfDay >= 12 && timeOfDay < 16) {
            greetingText.setText("Good Afternoon 🌤️,");
        } else if (timeOfDay >= 16 && timeOfDay < 21) {
            greetingText.setText("Good Evening 🌇,");
        } else {
            greetingText.setText("Good Night 🌙,");
        }
    }

    // 🌟 LOAD REAL-TIME DATA 🌟
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        // 1. Fetch User Profile (Name & Balance)
        userListener = db.collection("Users").document(uid).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Toast.makeText(HomeActivity.this, "Network Sync Error", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                String name = snapshot.getString("name");
                String balance = snapshot.getString("balance");

                if (name != null && !name.isEmpty()) {
                    userNameText.setText(formatToTitleCase(name));
                    // 🌟 Set Avatar Initials 🌟
                    tvHeaderAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }
                if (balance != null) {
                    tvRemainingBalance.setText(balance);
                }
            }
        });

        // 2. Fetch Loans (Calculate Live Debt)
        loansListener = db.collection("Loans").whereEqualTo("userId", uid).addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                int totalOutstandingDebt = 0;

                for (QueryDocumentSnapshot doc : value) {
                    if ("Approved".equals(doc.getString("status"))) {
                        String approvedStr = doc.getString("approvedAmount");
                        String paidStr = doc.getString("paidAmount");

                        int approved = 0, paid = 0;
                        try { if (approvedStr != null) approved = Integer.parseInt(approvedStr); } catch (Exception e) {}
                        try { if (paidStr != null) paid = Integer.parseInt(paidStr); } catch (Exception e) {}

                        totalOutstandingDebt += (approved - paid);
                    }
                }

                // Format with commas (e.g., 50,000)
                String formattedDebt = NumberFormat.getNumberInstance(Locale.US).format(totalOutstandingDebt);
                tvTotalDebt.setText("Rs " + formattedDebt);
            }
        });
    }

    // Capitalize first letter of names
    private String formatToTitleCase(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else {
                c = Character.toLowerCase(c);
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
        if (loansListener != null) loansListener.remove();
    }
}