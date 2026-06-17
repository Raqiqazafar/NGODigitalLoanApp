package com.example.ngodigitalloan;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

// 🌟 FIRESTORE IMPORTS 🌟
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    Toolbar reportsToolbar;
    ProgressBar recoveryCircle;
    ProgressBar fundsProgressBar;
    MaterialCheckBox exportPdfCheckbox; // Changed to Modern Checkbox
    MaterialButton btnExportReport;

    // Naye Textviews Data dikhane ke liye
    TextView tvApprovalRate, tvFundsDisbursedText;
    TextView tvEduCount, tvBizCount, tvMedCount;

    // Firebase
    FirebaseFirestore db;
    ListenerRegistration loansListener;

    // Report Limits
    final double TARGET_FUNDS = 3000000.0; // 3 Million Rupees Target

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // Binding Views
        reportsToolbar = findViewById(R.id.reportsToolbar);
        recoveryCircle = findViewById(R.id.recoveryCircle);
        fundsProgressBar = findViewById(R.id.fundsProgressBar);
        exportPdfCheckbox = findViewById(R.id.exportPdfCheckbox);
        btnExportReport = findViewById(R.id.btnExportReport);

        tvApprovalRate = findViewById(R.id.tvApprovalRate);
        tvFundsDisbursedText = findViewById(R.id.tvFundsDisbursedText);
        tvEduCount = findViewById(R.id.tvEduCount);
        tvBizCount = findViewById(R.id.tvBizCount);
        tvMedCount = findViewById(R.id.tvMedCount);

        // Firebase Init (Firestore)
        db = FirebaseFirestore.getInstance();

        // Toolbar Setup
        setSupportActionBar(reportsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        reportsToolbar.setNavigationOnClickListener(v -> finish());

        // Export Button Demo Logic
        btnExportReport.setOnClickListener(v -> {
            String msg = exportPdfCheckbox.isChecked() ?
                    "Generating Detailed PDF Report with Charts..." :
                    "Generating Summary Excel Report (Data Only)...";
            Toast.makeText(ReportsActivity.this, msg, Toast.LENGTH_LONG).show();
        });

        // 🌟 Fetch Live Data and Run Calculations
        generateLiveFirestoreReport();
    }

    private void generateLiveFirestoreReport() {
        loansListener = db.collection("Loans").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading report: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                int totalLoansCount = 0;
                int approvedLoansCount = 0;
                double totalDisbursedAmount = 0.0;

                int eduCount = 0;
                int bizCount = 0;
                int medCount = 0;

                // Loop through all loans in Firestore
                for (QueryDocumentSnapshot doc : value) {
                    totalLoansCount++;

                    String status = doc.getString("status");
                    String category = doc.getString("loanType"); // Ensure this matches your dropdown (e.g. "Education Loan")

                    // 1. Category Counting
                    if (category != null) {
                        String lowerCat = category.toLowerCase();
                        if (lowerCat.contains("education")) {
                            eduCount++;
                        } else if (lowerCat.contains("business")) {
                            bizCount++;
                        } else {
                            medCount++; // Medical or Others
                        }
                    }

                    // 2. Disbursed Amount & Approval Rate Calculation
                    if ("Approved".equalsIgnoreCase(status)) {
                        approvedLoansCount++;
                        String appAmtStr = doc.getString("approvedAmount");
                        if (appAmtStr != null) {
                            try {
                                totalDisbursedAmount += Double.parseDouble(appAmtStr);
                            } catch (Exception e) {}
                        }
                    }
                }

                // --- 🌟 UPDATE UI WITH LIVE CALCULATED DATA 🌟 ---

                // Categories Update
                tvEduCount.setText(String.valueOf(eduCount));
                tvBizCount.setText(String.valueOf(bizCount));
                tvMedCount.setText(String.valueOf(medCount));

                // Approval Rate Circle Math (Total Approved / Total Applications)
                int approvalRate = 0;
                if (totalLoansCount > 0) {
                    approvalRate = (int) (((double) approvedLoansCount / totalLoansCount) * 100);
                }
                tvApprovalRate.setText(approvalRate + "%");
                animateProgress(recoveryCircle, 0, approvalRate);

                // Funds Disbursed Bar Math
                int fundsPercentage = (int) ((totalDisbursedAmount / TARGET_FUNDS) * 100);
                if (fundsPercentage > 100) fundsPercentage = 100; // Limit bar visual to 100% max

                String formattedDisbursed = NumberFormat.getNumberInstance(Locale.US).format(totalDisbursedAmount);
                tvFundsDisbursedText.setText("Rs " + formattedDisbursed + " out of Rs 3.0M limit");
                animateProgress(fundsProgressBar, 0, fundsPercentage);
            }
        });
    }

    // Custom smooth animation for progress bars
    private void animateProgress(ProgressBar progressBar, int start, int end) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", start, end);
        animation.setDuration(1500); // 1.5 seconds smooth transition
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loansListener != null) loansListener.remove(); // Stop listening to save RAM
    }
}