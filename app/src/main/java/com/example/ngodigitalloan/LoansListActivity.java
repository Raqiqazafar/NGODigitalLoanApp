package com.example.ngodigitalloan;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

// 🌟 FIRESTORE IMPORTS 🌟
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoansListActivity extends AppCompatActivity {

    ImageView backButton;
    SwipeRefreshLayout swipeRefresh;
    RecyclerView loansRecyclerView;
    ChipGroup filterChipGroup;
    LinearLayout emptyStateLayout;

    List<LoanModel> allLoansList = new ArrayList<>();
    SimpleLoanAdapter adapter;

    // Firebase Variables
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ListenerRegistration loansListener;

    String currentUserId;
    int currentFilterId = R.id.chipAll; // Default filter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loans_list);

        // Firebase Initialization
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if(mAuth.getCurrentUser() != null){
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        // Binding UI
        backButton = findViewById(R.id.backButton);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        loansRecyclerView = findViewById(R.id.loansRecyclerView);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        emptyStateLayout = findViewById(R.id.emptyStateLayout); // Naya Empty State

        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        loansRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleLoanAdapter(new ArrayList<>()); // Shuru mein khali
        loansRecyclerView.setAdapter(adapter);

        // Fetch Data from Firestore
        if(currentUserId != null){
            fetchLoansFromFirestore();
        }

        // Modern Refresh Indicator
        swipeRefresh.setColorSchemeColors(Color.parseColor("#00897B"));
        swipeRefresh.setOnRefreshListener(this::fetchLoansFromFirestore);

        // 🌟 CHIP FILTER LOGIC 🌟
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                currentFilterId = checkedIds.get(0);
                applyFilter();
            }
        });
    }

    // --- 🌟 FIRESTORE LOGIC 🌟 ---
    private void fetchLoansFromFirestore() {
        swipeRefresh.setRefreshing(true);

        loansListener = db.collection("Loans")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    swipeRefresh.setRefreshing(false);

                    if (error != null) {
                        Toast.makeText(this, "Network Sync Error", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        allLoansList.clear(); // Purana data saaf karo

                        for(QueryDocumentSnapshot doc : value){
                            String id = doc.getId();
                            String category = doc.getString("loanType"); // Apply form mein humne 'loanType' rakha tha
                            String status = doc.getString("status");
                            String requestedAmount = doc.getString("requestedAmount");
                            String date = doc.getString("requiredDate");

                            // Amount formatting (Rs 50,000)
                            String displayAmount = requestedAmount;
                            try {
                                if (requestedAmount != null) {
                                    int amt = Integer.parseInt(requestedAmount);
                                    displayAmount = "Rs " + NumberFormat.getNumberInstance(Locale.US).format(amt);
                                }
                            } catch (Exception e) {}

                            allLoansList.add(new LoanModel(id, category, status, displayAmount, date));
                        }

                        // Data aane ke baad automatically current filter apply karo
                        applyFilter();
                    }
                });
    }

    // --- 🌟 FILTERING LOGIC FIX 🌟 ---
    private void applyFilter() {
        List<LoanModel> filteredList = new ArrayList<>();

        if (currentFilterId == R.id.chipAll) {
            filteredList.addAll(allLoansList);
        } else if (currentFilterId == R.id.chipPending) {
            for (LoanModel loan : allLoansList) {
                if ("Pending".equalsIgnoreCase(loan.status)) {
                    filteredList.add(loan);
                }
            }
        } else if (currentFilterId == R.id.chipApproved) {
            for (LoanModel loan : allLoansList) {
                if ("Approved".equalsIgnoreCase(loan.status)) {
                    filteredList.add(loan);
                }
            }
        }

        // Agar list khali hai toh khubsoorat Empty State dikhao
        if (filteredList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            loansRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            loansRecyclerView.setVisibility(View.VISIBLE);
            adapter.updateList(filteredList);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loansListener != null) loansListener.remove(); // Memory leak rokne ke liye
    }

    // --- Custom Data Model Class ---
    public class LoanModel {
        String id, title, status, amount, date;
        public LoanModel(String id, String title, String status, String amount, String date) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.amount = amount;
            this.date = date;
        }
    }

    // --- Modern RecyclerView Adapter Class ---
    public class SimpleLoanAdapter extends RecyclerView.Adapter<SimpleLoanAdapter.LoanViewHolder> {
        private List<LoanModel> loansList;

        public SimpleLoanAdapter(List<LoanModel> loansList) {
            this.loansList = loansList;
        }

        public void updateList(List<LoanModel> filteredList) {
            this.loansList = filteredList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LoanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loan, parent, false);
            return new LoanViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LoanViewHolder holder, int position) {
            LoanModel currentLoan = loansList.get(position);

            holder.titleText.setText(currentLoan.title != null ? currentLoan.title : "Loan Application");
            holder.amountText.setText(currentLoan.amount != null ? currentLoan.amount : "Rs 0");
            holder.dateText.setText(currentLoan.date != null ? "Needed by: " + currentLoan.date : "");

            String stat = currentLoan.status != null ? currentLoan.status : "Pending";
            holder.statusText.setText(stat.toUpperCase());

            // Premium Status Badges Logic
            if(stat.equalsIgnoreCase("Approved")) {
                holder.statusBadgeCard.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // Light Green Bg
                holder.statusText.setTextColor(Color.parseColor("#2E7D32")); // Dark Green Text
            } else if(stat.equalsIgnoreCase("Rejected")) {
                holder.statusBadgeCard.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Light Red Bg
                holder.statusText.setTextColor(Color.parseColor("#C62828")); // Dark Red Text
            } else {
                holder.statusBadgeCard.setCardBackgroundColor(Color.parseColor("#FFF3E0")); // Light Orange Bg
                holder.statusText.setTextColor(Color.parseColor("#E65100")); // Dark Orange Text
            }

            // Click listener: Pass the specific LOAN_ID to LoanDetailActivity
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), LoanDetailActivity.class);
                intent.putExtra("LOAN_ID", currentLoan.id); // Passing Firebase ID
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return loansList.size(); }

        public class LoanViewHolder extends RecyclerView.ViewHolder {
            TextView titleText, dateText, statusText, amountText;
            MaterialCardView statusBadgeCard;

            public LoanViewHolder(@NonNull View itemView) {
                super(itemView);
                // Make sure your 'item_loan.xml' matches these IDs
                titleText = itemView.findViewById(R.id.loanTitleText);
                dateText = itemView.findViewById(R.id.loanDateText);
                statusText = itemView.findViewById(R.id.loanStatusText);
                amountText = itemView.findViewById(R.id.loanAmountText);
                statusBadgeCard = itemView.findViewById(R.id.statusBadgeCard);
            }
        }
    }
}