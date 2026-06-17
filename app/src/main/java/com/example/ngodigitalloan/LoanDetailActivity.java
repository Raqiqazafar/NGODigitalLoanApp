package com.example.ngodigitalloan;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

// 🌟 FIRESTORE IMPORTS 🌟
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

public class LoanDetailActivity extends AppCompatActivity {

    Toolbar toolbar;
    CollapsingToolbarLayout collapsingToolbar;
    TextView detailAmountText, detailStatusText, progressText, amountLabel;
    MaterialCardView statusBadgeCard;
    LinearProgressIndicator repaymentProgressBar;
    FloatingActionButton fabContact;

    // Smart Info Chips
    Chip chipCategory, chipPlan, chipDate;

    // Firebase
    FirebaseFirestore db;
    ListenerRegistration loanListener;
    String loanId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_detail);

        // UI Binding
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        detailAmountText = findViewById(R.id.detailAmountText);
        detailStatusText = findViewById(R.id.detailStatusText);
        progressText = findViewById(R.id.progressText);
        amountLabel = findViewById(R.id.amountLabel);
        statusBadgeCard = findViewById(R.id.statusBadgeCard);
        repaymentProgressBar = findViewById(R.id.repaymentProgressBar);
        fabContact = findViewById(R.id.fabContact);

        chipCategory = findViewById(R.id.chipCategory);
        chipPlan = findViewById(R.id.chipPlan);
        chipDate = findViewById(R.id.chipDate);

        // Firestore Init
        db = FirebaseFirestore.getInstance();

        // 1. Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // 🌟 2. REAL EMAIL INTENT 🌟
        fabContact.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@ehsan.ngo")); // Sirf email apps khulengi
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Query Regarding Loan ID: " + loanId);
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello Ehsan Digital Team,\n\nI need help regarding my loan application.\n\nRegards,");

            try {
                startActivity(emailIntent);
            } catch (Exception e) {
                Toast.makeText(this, "No Email app found on your phone!", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Fetch Data from Firestore
        loanId = getIntent().getStringExtra("LOAN_ID");

        if (loanId != null && !loanId.isEmpty()) {
            fetchLoanDataFromCloud();
        } else {
            Toast.makeText(this, "Error: Loan ID Missing!", Toast.LENGTH_SHORT).show();
            finish(); // ID nahi toh screen band kar do
        }
    }

    // --- 🌟 REAL FIRESTORE LOGIC 🌟 ---
    private void fetchLoanDataFromCloud() {
        loanListener = db.collection("Loans").document(loanId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String category = snapshot.getString("loanType"); // Apply screen mein 'loanType' rakha tha humne
                String status = snapshot.getString("status");
                String requestedAmtStr = snapshot.getString("requestedAmount");
                String approvedAmtStr = snapshot.getString("approvedAmount");
                String paidAmtStr = snapshot.getString("paidAmount");
                String plan = snapshot.getString("repaymentPlan");
                String date = snapshot.getString("requiredDate");

                // Title & Chips
                collapsingToolbar.setTitle(category != null ? category : "Loan Details");
                if(category != null) chipCategory.setText(category);
                if(plan != null) chipPlan.setText(plan.split("\\(")[0].trim()); // E.g., "Short Term"
                if(date != null) chipDate.setText(date);

                // Status Badge
                if (status != null) {
                    detailStatusText.setText(status.toUpperCase());
                    applyBadgeStyle(status);
                }

                // Amount Display Logic
                String displayAmount = requestedAmtStr;
                int approved = 0, paid = 0;

                try { if (approvedAmtStr != null) approved = Integer.parseInt(approvedAmtStr); } catch (Exception e) {}
                try { if (paidAmtStr != null) paid = Integer.parseInt(paidAmtStr); } catch (Exception e) {}

                if ("Approved".equalsIgnoreCase(status) && approved > 0) {
                    amountLabel.setText("Approved Amount");
                    displayAmount = String.valueOf(approved);
                } else if ("Rejected".equalsIgnoreCase(status)) {
                    amountLabel.setText("Requested Amount (Rejected)");
                } else {
                    amountLabel.setText("Requested Amount (Pending)");
                }

                // Format Amount (e.g., Rs 50,000)
                try {
                    int amtToDisplay = Integer.parseInt(displayAmount);
                    detailAmountText.setText("Rs " + NumberFormat.getNumberInstance(Locale.US).format(amtToDisplay));
                } catch (Exception e) {
                    detailAmountText.setText("Rs 0");
                }

                // 🌟 REAL PROGRESS BAR MATH 🌟
                if ("Approved".equalsIgnoreCase(status) && approved > 0) {
                    int percentage = (int) (((float) paid / approved) * 100);

                    if(percentage > 100) percentage = 100; // Cap at 100%

                    progressText.setText(percentage + "%");
                    repaymentProgressBar.setProgress(percentage, true); // Smooth animation
                } else {
                    progressText.setText("-");
                    repaymentProgressBar.setProgress(0);
                }
            }
        });
    }

    private void applyBadgeStyle(String status) {
        if (status.equalsIgnoreCase("Approved")) {
            statusBadgeCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            detailStatusText.setTextColor(Color.parseColor("#2E7D32"));
        } else if (status.equalsIgnoreCase("Rejected")) {
            statusBadgeCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            detailStatusText.setTextColor(Color.parseColor("#C62828"));
        } else {
            statusBadgeCard.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            detailStatusText.setTextColor(Color.parseColor("#E65100"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(loanListener != null) loanListener.remove(); // Stop listening when screen closes
    }
}