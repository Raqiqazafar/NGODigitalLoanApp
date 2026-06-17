package com.example.ngodigitalloan;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputEditText;

// 🌟 FIRESTORE IMPORTS (Realtime DB hata diya hai) 🌟
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RepaymentHistoryActivity extends AppCompatActivity {

    ImageView backButton;
    TextInputEditText searchInput;
    RecyclerView ledgerRecyclerView;
    SwipeRefreshLayout swipeRefreshLedger;
    TextView tvTotalPaid, tvTotalRemaining;

    LedgerAdapter adapter;
    List<LedgerItem> allTransactions = new ArrayList<>();

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db; // 🌟 Firestore variable
    String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repayment_history);

        // Binding UI
        backButton = findViewById(R.id.backButton);
        searchInput = findViewById(R.id.searchInput);
        ledgerRecyclerView = findViewById(R.id.ledgerRecyclerView);
        swipeRefreshLedger = findViewById(R.id.swipeRefreshLedger);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvTotalRemaining = findViewById(R.id.tvTotalRemaining);

        // Firebase Init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // 🌟 Init Firestore

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        backButton.setOnClickListener(v -> finish());

        // Recycler View Setup
        ledgerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LedgerAdapter(allTransactions);
        ledgerRecyclerView.setAdapter(adapter);

        // Swipe Refresh Logic
        swipeRefreshLedger.setColorSchemeColors(Color.parseColor("#00897B"));
        swipeRefreshLedger.setOnRefreshListener(this::fetchLiveLedgerData);

        // Fetch Live Data from Firestore
        if (currentUserId != null) {
            fetchLiveLedgerData();
        }

        // Search Filter Logic
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // --- 🌟 REAL FIRESTORE DATA LOGIC 🌟 ---
    private void fetchLiveLedgerData() {
        swipeRefreshLedger.setRefreshing(true);

        db.collection("Loans")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTransactions.clear();
                    int totalPaidSum = 0;
                    int totalRemainingSum = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String loanType = doc.getString("loanType");
                        String approvedStr = doc.getString("approvedAmount");
                        String paidStr = doc.getString("paidAmount");

                        int approved = 0, paid = 0;
                        try { if (approvedStr != null) approved = Integer.parseInt(approvedStr); } catch (Exception e) {}
                        try { if (paidStr != null) paid = Integer.parseInt(paidStr); } catch (Exception e) {}

                        int remaining = approved - paid;

                        // Grand Totals Calculation
                        totalPaidSum += paid;
                        totalRemainingSum += remaining;

                        // Create actual list item
                        String formattedTotal = NumberFormat.getNumberInstance(Locale.US).format(approved);
                        String formattedPaid = NumberFormat.getNumberInstance(Locale.US).format(paid);

                        String displayStatus = (remaining <= 0) ? "Fully Paid" : "Active Debt";

                        allTransactions.add(new LedgerItem(
                                displayStatus,
                                loanType != null ? loanType : "Loan",
                                "Rs " + formattedTotal,
                                "Paid: Rs " + formattedPaid,
                                remaining <= 0
                        ));
                    }

                    adapter.updateList(allTransactions);

                    // Update Top Summary Cards
                    String finalPaid = NumberFormat.getNumberInstance(Locale.US).format(totalPaidSum);
                    String finalRemaining = NumberFormat.getNumberInstance(Locale.US).format(totalRemainingSum);

                    tvTotalPaid.setText("Rs " + finalPaid);
                    tvTotalRemaining.setText("Rs " + finalRemaining);

                    swipeRefreshLedger.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    swipeRefreshLedger.setRefreshing(false);
                    Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- Search Filter Logic ---
    private void filter(String text) {
        List<LedgerItem> filteredList = new ArrayList<>();
        for (LedgerItem item : allTransactions) {
            if (item.method.toLowerCase().contains(text.toLowerCase()) ||
                    item.amount.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
    }

    // --- Data Model ---
    class LedgerItem {
        String dateStatus, method, amount, paidBadge;
        boolean isFullyPaid;

        public LedgerItem(String dateStatus, String method, String amount, String paidBadge, boolean isFullyPaid) {
            this.dateStatus = dateStatus;
            this.method = method;
            this.amount = amount;
            this.paidBadge = paidBadge;
            this.isFullyPaid = isFullyPaid;
        }
    }

    // --- RECYCLERVIEW ADAPTER ---
    class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder> {
        List<LedgerItem> items;

        public LedgerAdapter(List<LedgerItem> items) {
            this.items = items;
        }

        public void updateList(List<LedgerItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LedgerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ledger, parent, false);
            return new LedgerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LedgerViewHolder holder, int position) {
            LedgerItem item = items.get(position);

            holder.methodText.setText(item.method);
            holder.dateText.setText(item.dateStatus);
            holder.amountText.setText(item.amount);
            holder.statusBadge.setText(item.paidBadge);

            // Dynamic Styling based on payment status
            if(item.isFullyPaid) {
                holder.dateText.setTextColor(Color.parseColor("#2E7D32")); // Green status
                holder.statusBadge.setTextColor(Color.parseColor("#2E7D32")); // Green badge
            } else {
                holder.dateText.setTextColor(Color.parseColor("#757575")); // Grey status
                holder.statusBadge.setTextColor(Color.parseColor("#F57C00")); // Orange badge
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class LedgerViewHolder extends RecyclerView.ViewHolder {
            TextView dateText, methodText, amountText, statusBadge;
            public LedgerViewHolder(@NonNull View itemView) {
                super(itemView);
                dateText = itemView.findViewById(R.id.ledgerDate);
                methodText = itemView.findViewById(R.id.ledgerMethod);
                amountText = itemView.findViewById(R.id.ledgerAmount);
                statusBadge = itemView.findViewById(R.id.ledgerStatusBadge);
            }
        }
    }
}