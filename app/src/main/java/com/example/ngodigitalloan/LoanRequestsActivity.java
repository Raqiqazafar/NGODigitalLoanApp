package com.example.ngodigitalloan;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

// 🌟 FIRESTORE IMPORTS 🌟
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoanRequestsActivity extends AppCompatActivity {

    Toolbar requestsToolbar;
    TabLayout requestsTabLayout;
    ViewPager2 requestsViewPager;

    // Firebase Firestore Data Lists
    List<QueryDocumentSnapshot> pendingLoans = new ArrayList<>();
    List<QueryDocumentSnapshot> approvedLoans = new ArrayList<>();
    List<QueryDocumentSnapshot> rejectedLoans = new ArrayList<>();

    String[] tabTitles = new String[]{"Pending", "Approved", "Rejected"};
    ViewPagerAdapter viewPagerAdapter;

    FirebaseFirestore db;
    ListenerRegistration loansListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_requests);

        // Firestore Init
        db = FirebaseFirestore.getInstance();

        // Binding
        requestsToolbar = findViewById(R.id.requestsToolbar);
        requestsTabLayout = findViewById(R.id.requestsTabLayout);
        requestsViewPager = findViewById(R.id.requestsViewPager);

        // Toolbar
        setSupportActionBar(requestsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        requestsToolbar.setNavigationOnClickListener(v -> finish());

        // Setup ViewPager
        viewPagerAdapter = new ViewPagerAdapter();
        requestsViewPager.setAdapter(viewPagerAdapter);

        new TabLayoutMediator(requestsTabLayout, requestsViewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        // Load Data From Firestore
        fetchLoansFromFirestore();
    }

    private void fetchLoansFromFirestore() {
        loansListener = db.collection("Loans").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Network Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                pendingLoans.clear();
                approvedLoans.clear();
                rejectedLoans.clear();

                for (QueryDocumentSnapshot doc : value) {
                    String status = doc.getString("status");

                    if ("Pending".equals(status)) {
                        pendingLoans.add(doc);
                    } else if ("Approved".equals(status)) {
                        approvedLoans.add(doc);
                    } else if ("Rejected".equals(status)) {
                        rejectedLoans.add(doc);
                    }
                }

                // UI Refresh
                viewPagerAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loansListener != null) loansListener.remove(); // Memory leak prevention
    }

    // =========================================================
    // INNER CLASS 1: ViewPager Adapter (Tabs Manager)
    // =========================================================
    public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.PageViewHolder> {

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_request_list, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            List<QueryDocumentSnapshot> currentList;
            if (position == 0) currentList = pendingLoans;
            else if (position == 1) currentList = approvedLoans;
            else currentList = rejectedLoans;

            if (currentList.isEmpty()) {
                holder.emptyStateLayout.setVisibility(View.VISIBLE);
                holder.recyclerView.setVisibility(View.GONE);
            } else {
                holder.emptyStateLayout.setVisibility(View.GONE);
                holder.recyclerView.setVisibility(View.VISIBLE);

                LoansListAdapter listAdapter = new LoansListAdapter(currentList);
                holder.recyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
                holder.recyclerView.setAdapter(listAdapter);
            }
        }

        @Override
        public int getItemCount() {
            return tabTitles.length;
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            RecyclerView recyclerView;
            LinearLayout emptyStateLayout; // 🌟 Updated Empty State

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                recyclerView = itemView.findViewById(R.id.recyclerViewRequests);
                emptyStateLayout = itemView.findViewById(R.id.emptyStateLayout);
            }
        }
    }

    // =========================================================
    // INNER CLASS 2: RecyclerView Adapter (Premium Card Designer)
    // =========================================================
    public class LoansListAdapter extends RecyclerView.Adapter<LoansListAdapter.LoanViewHolder> {

        List<QueryDocumentSnapshot> loanList;

        public LoansListAdapter(List<QueryDocumentSnapshot> loanList) {
            this.loanList = loanList;
        }

        @NonNull
        @Override
        public LoanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 🌟 NAYA: Yahan ab hum apni banayi hui 'item_loan_request.xml' use kar rahay hain 🌟
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loan_request, parent, false);
            return new LoanViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LoanViewHolder holder, int position) {
            QueryDocumentSnapshot doc = loanList.get(position);

            // Fetch Fields from Apply Loan Form
            String category = doc.getString("loanType");
            String date = doc.getString("requiredDate");
            String requestedAmountStr = doc.getString("requestedAmount");

            // Setup Text
            holder.tvCategory.setText(category != null ? category : "Loan Application");
            holder.tvDate.setText("Required By: " + (date != null ? date : "N/A"));

            // Setup Text 2 (Amount formatting)
            try {
                if (requestedAmountStr != null) {
                    int amt = Integer.parseInt(requestedAmountStr);
                    String formatted = NumberFormat.getNumberInstance(Locale.US).format(amt);
                    holder.tvAmount.setText("Rs " + formatted);
                }
            } catch (Exception e) {
                holder.tvAmount.setText("Rs " + requestedAmountStr);
            }

            // Click -> Go to Review Activity
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(LoanRequestsActivity.this, LoanReviewActivity.class);
                intent.putExtra("LOAN_ID", doc.getId()); // Admin ko doc Id mil jayegi approve karne ke liye
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return loanList.size();
        }

        class LoanViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategory, tvDate, tvAmount;

            public LoanViewHolder(@NonNull View itemView) {
                super(itemView);
                // 🌟 NAYA: Binding custom XML IDs 🌟
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvAmount = itemView.findViewById(R.id.tvAmount);
            }
        }
    }
}