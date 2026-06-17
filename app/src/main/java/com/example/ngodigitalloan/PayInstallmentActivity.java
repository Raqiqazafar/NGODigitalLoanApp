package com.example.ngodigitalloan;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// Firestore Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PayInstallmentActivity extends AppCompatActivity {

    Toolbar payToolbar;
    AutoCompleteTextView loanSelector;
    TextView tvRemainingDebt, tvWalletBalance; // Naya: Wallet Balance dikhane ke liye
    TextInputEditText amountInput;
    MaterialButton btnPayNow;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String currentUserId;

    List<String> loanIds = new ArrayList<>();
    String selectedLoanId = "";
    int currentDebt = 0;
    int userWalletBalance = 0; // 🌟 NAYA: User ka asli wallet balance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_installment);

        // Binding UI
        payToolbar = findViewById(R.id.payToolbar);
        loanSelector = findViewById(R.id.loanSelector);
        tvRemainingDebt = findViewById(R.id.tvRemainingDebt);
        amountInput = findViewById(R.id.amountInput);
        btnPayNow = findViewById(R.id.btnPayNow);

        // Agar XML mein Wallet Balance dikhane ki jagah nahi hai toh isay ignore kar saktay hain,
        // lekin logic zaroor kaam karegi.

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        // Toolbar
        setSupportActionBar(payToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        payToolbar.setNavigationOnClickListener(v -> finish());

        // Load Data
        fetchUserWalletBalance();
        fetchActiveLoans();

        // Payment Logic
        btnPayNow.setOnClickListener(v -> processRealPayment());
    }

    // 🌟 1. PEHLE USER KA ASLI WALLET BALANCE CHECK KARO 🌟
    private void fetchUserWalletBalance() {
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String balanceStr = doc.getString("balance");
                        if (balanceStr != null && balanceStr.startsWith("Rs ")) {
                            try {
                                balanceStr = balanceStr.replace("Rs ", "").replace(",", "").trim();
                                userWalletBalance = Integer.parseInt(balanceStr);
                            } catch (Exception e) {}
                        }
                    }
                });
    }

    private void fetchActiveLoans() {
        if (currentUserId == null) return;

        db.collection("Loans")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> displayNames = new ArrayList<>();
                    loanIds.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("loanType");
                        String approvedStr = doc.getString("approvedAmount");
                        String paidStr = doc.getString("paidAmount");

                        int approved = 0, paid = 0;
                        try { if (approvedStr != null) approved = Integer.parseInt(approvedStr); } catch (Exception e) {}
                        try { if (paidStr != null) paid = Integer.parseInt(paidStr); } catch (Exception e) {}

                        int remaining = approved - paid;

                        if (remaining > 0) {
                            displayNames.add((type != null ? type : "Loan") + " (Debt: Rs " + remaining + ")");
                            loanIds.add(doc.getId());
                        }
                    }

                    if (displayNames.isEmpty()) {
                        Toast.makeText(this, "No active debts found!", Toast.LENGTH_SHORT).show();
                        btnPayNow.setEnabled(false);
                    } else {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, displayNames);
                        loanSelector.setAdapter(adapter);

                        loanSelector.setOnItemClickListener((parent, view, position, id) -> {
                            selectedLoanId = loanIds.get(position);
                            fetchSpecificLoanDebt(selectedLoanId);
                        });
                    }
                });
    }

    private void fetchSpecificLoanDebt(String loanId) {
        db.collection("Loans").document(loanId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String approvedStr = doc.getString("approvedAmount");
                        String paidStr = doc.getString("paidAmount");

                        int approved = 0, paid = 0;
                        try { if (approvedStr != null) approved = Integer.parseInt(approvedStr); } catch (Exception e) {}
                        try { if (paidStr != null) paid = Integer.parseInt(paidStr); } catch (Exception e) {}

                        currentDebt = approved - paid;
                        String formatted = NumberFormat.getNumberInstance(Locale.US).format(currentDebt);
                        tvRemainingDebt.setText("Rs " + formatted);
                    }
                });
    }

    // 🌟 2. ASLI PAYMENT LOGIC (WALLET DEDUCTION KE SATH) 🌟
    private void processRealPayment() {
        if (selectedLoanId.isEmpty()) {
            Toast.makeText(this, "Please select a loan first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String inputAmtStr = amountInput.getText().toString().trim();
        if (inputAmtStr.isEmpty()) {
            amountInput.setError("Enter payment amount");
            return;
        }

        int payAmount = Integer.parseInt(inputAmtStr);

        // Validations
        if (payAmount <= 0) {
            amountInput.setError("Amount must be greater than 0");
            return;
        }
        if (payAmount > currentDebt) {
            amountInput.setError("Cannot pay more than remaining debt (Rs " + currentDebt + ")");
            return;
        }

        // 🌟 REAL WALLET CHECK 🌟
        if (payAmount > userWalletBalance) {
            amountInput.setError("Insufficient Wallet Balance!");
            Toast.makeText(this, "You only have Rs " + userWalletBalance + " in your Digital Wallet.", Toast.LENGTH_LONG).show();
            return;
        }

        btnPayNow.setEnabled(false);
        btnPayNow.setText("PROCESSING PAYMENT...");

        // Pehle purana paidAmount nikalo
        db.collection("Loans").document(selectedLoanId).get()
                .addOnSuccessListener(doc -> {
                    String oldPaidStr = doc.getString("paidAmount");
                    int oldPaid = 0;
                    try { if (oldPaidStr != null) oldPaid = Integer.parseInt(oldPaidStr); } catch (Exception e) {}

                    int newPaidTotal = oldPaid + payAmount;

                    // 🌟 Step 1: Update Loan Debt 🌟
                    db.collection("Loans").document(selectedLoanId)
                            .update("paidAmount", String.valueOf(newPaidTotal))
                            .addOnSuccessListener(aVoid -> {

                                // 🌟 Step 2: Deduct from User Wallet Balance 🌟
                                int newWalletBalance = userWalletBalance - payAmount;
                                String formattedNewBalance = "Rs " + NumberFormat.getNumberInstance(Locale.US).format(newWalletBalance);

                                db.collection("Users").document(currentUserId)
                                        .update("balance", formattedNewBalance)
                                        .addOnSuccessListener(aVoid2 -> {
                                            Toast.makeText(this, "Payment Successful! Wallet Updated.", Toast.LENGTH_SHORT).show();
                                            finish(); // Wapis Dashboard par
                                        });
                            })
                            .addOnFailureListener(e -> {
                                btnPayNow.setEnabled(true);
                                btnPayNow.setText("PAY SECURELY");
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }
}