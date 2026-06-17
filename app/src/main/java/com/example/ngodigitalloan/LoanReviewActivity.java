package com.example.ngodigitalloan;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

public class LoanReviewActivity extends AppCompatActivity {

    Toolbar reviewToolbar;
    MaterialButtonToggleGroup actionToggleGroup;
    TextInputEditText adminRemarksInput;
    MaterialButton btnSubmitAction;
    Slider amountSlider;

    TextView tvApplicantName, tvLoanPurpose, tvDateReq, tvRequestedAmount;
    TextView tvRepaymentPlan, tvMonthlyIncome, tvDetailedReason;
    Chip chipPhone, chipCnic;

    FirebaseFirestore db;
    String loanId = "";
    String applicantUserId = "";
    int requestedAmountInt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_review);

        reviewToolbar = findViewById(R.id.reviewToolbar);
        actionToggleGroup = findViewById(R.id.actionToggleGroup);
        adminRemarksInput = findViewById(R.id.adminRemarksInput);
        btnSubmitAction = findViewById(R.id.btnSubmitAction);
        amountSlider = findViewById(R.id.amountSlider);

        tvApplicantName = findViewById(R.id.tvApplicantName);
        tvLoanPurpose = findViewById(R.id.tvLoanPurpose);
        tvDateReq = findViewById(R.id.tvDateReq);
        tvRequestedAmount = findViewById(R.id.tvRequestedAmount);

        tvRepaymentPlan = findViewById(R.id.tvRepaymentPlan);
        tvMonthlyIncome = findViewById(R.id.tvMonthlyIncome);
        tvDetailedReason = findViewById(R.id.tvDetailedReason);

        chipPhone = findViewById(R.id.chipPhone);
        chipCnic = findViewById(R.id.chipCnic);

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(reviewToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        reviewToolbar.setNavigationOnClickListener(v -> finish());

        loanId = getIntent().getStringExtra("LOAN_ID");

        if (loanId != null && !loanId.isEmpty()) {
            fetchLoanDetailsFromFirestore(loanId);
        } else {
            Toast.makeText(this, "Error: Loan ID not found!", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSubmitAction.setOnClickListener(v -> {
            int checkedId = actionToggleGroup.getCheckedButtonId();
            String remarks = adminRemarksInput.getText() != null ? adminRemarksInput.getText().toString().trim() : "";
            boolean isApproved = (checkedId == R.id.btnApprove);
            String actionName = isApproved ? "Approve" : "Reject";

            if (!isApproved && remarks.isEmpty()) {
                adminRemarksInput.setError("Remarks required for rejection!");
                adminRemarksInput.requestFocus();
                return;
            }

            new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
                    .setTitle("Confirm Decision")
                    .setMessage("Are you sure you want to " + actionName.toUpperCase() + " this loan application?")
                    .setPositiveButton("YES, " + actionName.toUpperCase(), (dialog, which) -> {
                        executeDecisionInFirestore(isApproved, remarks);
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        });
    }

    private void fetchLoanDetailsFromFirestore(String lId) {
        db.collection("Loans").document(lId).get()
                .addOnSuccessListener(loanDoc -> {
                    if (loanDoc.exists()) {
                        applicantUserId = loanDoc.getString("userId");
                        String category = loanDoc.getString("loanType");
                        String date = loanDoc.getString("requiredDate");
                        String amountStr = loanDoc.getString("requestedAmount");
                        String currentStatus = loanDoc.getString("status");

                        String plan = loanDoc.getString("repaymentPlan");
                        String income = loanDoc.getString("monthlyIncome");
                        String description = loanDoc.getString("description");

                        tvLoanPurpose.setText(category != null ? category : "N/A");
                        tvDateReq.setText(date != null ? date : "N/A");

                        tvRepaymentPlan.setText(plan != null ? plan : "Standard Plan");
                        tvMonthlyIncome.setText(income != null ? "Rs " + income : "Not Provided by User");
                        tvDetailedReason.setText(description != null ? description : "Applicant requested funds for " + (category != null ? category.toLowerCase() : "their needs") + ". Standard verification applies.");

                        // 🌟 REAL SLIDER LIMIT LOCK FOR ADMIN 🌟
                        try {
                            requestedAmountInt = Integer.parseInt(amountStr);
                            String formatted = NumberFormat.getNumberInstance(Locale.US).format(requestedAmountInt);
                            tvRequestedAmount.setText("Rs " + formatted);

                            // Set Max Limit to Requested Amount (safely cast to float)
                            amountSlider.setValueTo((float) requestedAmountInt);
                            amountSlider.setValue((float) requestedAmountInt);
                        } catch (Exception e) {
                            tvRequestedAmount.setText("Rs " + amountStr);
                        }

                        if (currentStatus != null && !currentStatus.equalsIgnoreCase("Pending")) {
                            lockUIForProcessedLoan(currentStatus);
                        }

                        if (applicantUserId != null) {
                            db.collection("Users").document(applicantUserId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String name = userDoc.getString("name");
                                            String phone = userDoc.getString("phone");
                                            String cnic = userDoc.getString("cnic");

                                            tvApplicantName.setText(name != null ? name : "Unknown User");
                                            if(phone != null) chipPhone.setText(phone);
                                            if(cnic != null) chipCnic.setText(cnic);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load data.", Toast.LENGTH_SHORT).show());
    }

    private void lockUIForProcessedLoan(String status) {
        btnSubmitAction.setEnabled(false);
        btnSubmitAction.setText("ALREADY " + status.toUpperCase());
        btnSubmitAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

        amountSlider.setEnabled(false);
        adminRemarksInput.setEnabled(false);
        adminRemarksInput.setHint("Decision already made");

        for (int i = 0; i < actionToggleGroup.getChildCount(); i++) {
            actionToggleGroup.getChildAt(i).setEnabled(false);
        }
    }

    private void executeDecisionInFirestore(boolean isApproved, String remarks) {
        btnSubmitAction.setEnabled(false);
        btnSubmitAction.setText("PROCESSING...");

        String finalStatus = isApproved ? "Approved" : "Rejected";
        int finalApprovedAmount = isApproved ? (int) amountSlider.getValue() : 0;

        db.collection("Loans").document(loanId)
                .update(
                        "status", finalStatus,
                        "adminRemarks", remarks,
                        "approvedAmount", String.valueOf(finalApprovedAmount)
                )
                .addOnSuccessListener(aVoid -> {
                    if (isApproved && applicantUserId != null) {
                        db.collection("Users").document(applicantUserId).get()
                                .addOnSuccessListener(userDoc -> {
                                    String currentBalanceStr = userDoc.getString("balance");
                                    int currentBalance = 0;
                                    if (currentBalanceStr != null && currentBalanceStr.startsWith("Rs ")) {
                                        try {
                                            currentBalanceStr = currentBalanceStr.replace("Rs ", "").replace(",", "").trim();
                                            currentBalance = Integer.parseInt(currentBalanceStr);
                                        } catch (Exception e) {}
                                    }
                                    int newBalance = currentBalance + finalApprovedAmount;
                                    String formattedNewBalance = "Rs " + NumberFormat.getNumberInstance(Locale.US).format(newBalance);

                                    db.collection("Users").document(applicantUserId)
                                            .update("balance", formattedNewBalance)
                                            .addOnSuccessListener(aVoid1 -> {
                                                Toast.makeText(this, "Approved & Funds Transferred!", Toast.LENGTH_LONG).show();
                                                finish();
                                            });
                                });
                    } else {
                        Toast.makeText(this, "Loan Application Rejected.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }
}