package com.example.ngodigitalloan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

// 🌟 FIRESTORE IMPORTS 🌟
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ManageUsersActivity extends AppCompatActivity {

    Toolbar usersToolbar;
    TextInputEditText searchUsersInput;
    MaterialCheckBox cbShowArchived; // 🌟 NAYA CHECKBOX
    SwipeRefreshLayout swipeRefreshUsers;
    RecyclerView usersRecyclerView;
    FloatingActionButton fabAddUser;
    LinearLayout emptyStateLayout;

    List<UserModel> userList = new ArrayList<>();
    UserAdapter adapter;

    // Firebase
    FirebaseFirestore db;
    ListenerRegistration usersListener;

    boolean isShowingArchived = false; // 🌟 Logic Control

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        // Binding UI
        usersToolbar = findViewById(R.id.usersToolbar);
        searchUsersInput = findViewById(R.id.searchUsersInput);
        cbShowArchived = findViewById(R.id.cbShowArchived); // 🌟 Bind CheckBox
        swipeRefreshUsers = findViewById(R.id.swipeRefreshUsers);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        fabAddUser = findViewById(R.id.fabAddUser);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        // Firebase Init
        db = FirebaseFirestore.getInstance();

        // Toolbar
        setSupportActionBar(usersToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        usersToolbar.setNavigationOnClickListener(v -> finish());

        // Set Adapter
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(userList);
        usersRecyclerView.setAdapter(adapter);

        // 🌟 CHECKBOX LISTENER 🌟
        if (cbShowArchived != null) {
            cbShowArchived.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isShowingArchived = isChecked;
                listenToUsers(); // Checkbox badalte hi list update hogi
            });
        }

        // Start Live Listening
        listenToUsers();

        // Add User FAB
        fabAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(ManageUsersActivity.this, AddBorrowerActivity.class);
            startActivity(intent);
        });

        // Swipe Refresh
        swipeRefreshUsers.setColorSchemeColors(Color.parseColor("#00897B"));
        swipeRefreshUsers.setOnRefreshListener(() -> {
            listenToUsers();
            swipeRefreshUsers.setRefreshing(false);
        });

        // Search Logic
        searchUsersInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // --- 🌟 FIRESTORE REALTIME SYNC 🌟 ---
    private void listenToUsers() {
        if (usersListener != null) usersListener.remove();

        usersListener = db.collection("Users").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(ManageUsersActivity.this, "Sync Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                userList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    String uid = doc.getId();
                    String role = doc.getString("role");
                    String status = doc.getString("accountStatus");

                    if (status == null) status = "Active";

                    // 🌟 1. Admin hide karo
                    if ("admin".equalsIgnoreCase(role)) continue;

                    // 🌟 2. Agar checkbox OFF hai, toh Archived hide karo
                    if (!isShowingArchived && "Archived".equalsIgnoreCase(status)) {
                        continue;
                    }

                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String balance = doc.getString("balance");

                    String cnic = doc.getString("cnic");
                    String phone = doc.getString("phone");
                    String address = doc.getString("address");

                    if (balance == null) balance = "Rs 0";

                    userList.add(new UserModel(uid, name, email, status, balance, cnic, phone, address));
                }

                adapter.updateData(userList);
                checkEmptyState(userList.size());
            }
        });
    }

    private void checkEmptyState(int size) {
        if (size == 0) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            usersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            usersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersListener != null) usersListener.remove();
    }

    // --- 🌟 CUSTOM DIALOG FOR EDITING USER 🌟 ---
    @SuppressLint("SetTextI18n")
    private void showEditDialog(UserModel user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);

        TextInputEditText editNameInput = dialogView.findViewById(R.id.editNameInput);
        TextInputEditText editPhoneInput = dialogView.findViewById(R.id.editPhoneInput);
        TextInputEditText editCnicInput = dialogView.findViewById(R.id.editCnicInput);
        TextInputEditText editAddressInput = dialogView.findViewById(R.id.editAddressInput);
        AutoCompleteTextView statusAutoComplete = dialogView.findViewById(R.id.statusAutoComplete);
        MaterialButton btnSaveEdit = dialogView.findViewById(R.id.btnSaveEdit);

        editNameInput.setText(user.name);
        if (user.phone != null) editPhoneInput.setText(user.phone);
        if (user.cnic != null) editCnicInput.setText(user.cnic);
        if (user.address != null) editAddressInput.setText(user.address);

        // 🌟 DROPDOWN MEIN ARCHIVED BHI ADD KAR DIYA 🌟
        String[] statuses = {"Active", "Blocked", "Pending Verification", "Archived"};
        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statuses);
        statusAutoComplete.setAdapter(dropdownAdapter);
        statusAutoComplete.setText(user.status, false);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnSaveEdit.setOnClickListener(v -> {
            String newName = Objects.requireNonNull(editNameInput.getText()).toString().trim();
            String newPhone = Objects.requireNonNull(editPhoneInput.getText()).toString().trim();
            String newCnic = Objects.requireNonNull(editCnicInput.getText()).toString().trim();
            String newAddress = Objects.requireNonNull(editAddressInput.getText()).toString().trim();
            String newStatus = statusAutoComplete.getText().toString().trim();

            if(newName.isEmpty()) {
                editNameInput.setError("Name required");
                return;
            }

            btnSaveEdit.setEnabled(false);
            btnSaveEdit.setText("UPDATING...");

            db.collection("Users").document(user.uid)
                    .update(
                            "name", newName,
                            "accountStatus", newStatus,
                            "phone", newPhone,
                            "cnic", newCnic,
                            "address", newAddress
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ManageUsersActivity.this, "Status Updated Successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        btnSaveEdit.setEnabled(true);
                        btnSaveEdit.setText("SAVE CHANGES");
                        Toast.makeText(ManageUsersActivity.this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }


    // --- 🌟 DATA MODEL 🌟 ---
    public static class UserModel {
        String uid, name, email, status, balance;
        String cnic, phone, address;

        public UserModel(String uid, String name, String email, String status, String balance, String cnic, String phone, String address) {
            this.uid = uid; this.name = name; this.email = email; this.status = status; this.balance = balance;
            this.cnic = cnic; this.phone = phone; this.address = address;
        }
    }

    // --- PREMIUM ADAPTER ---
    public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<UserModel> list;
        private List<UserModel> originalList;

        public UserAdapter(List<UserModel> list) {
            this.list = list;
            this.originalList = new ArrayList<>(list);
        }

        @SuppressLint("NotifyDataSetChanged")
        public void updateData(List<UserModel> newData) {
            this.list = newData;
            this.originalList = new ArrayList<>(newData);
            notifyDataSetChanged();
        }

        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    List<UserModel> filtered = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        filtered.addAll(originalList);
                    } else {
                        String pattern = constraint.toString().toLowerCase().trim();
                        for (UserModel item : originalList) {
                            if ((item.name != null && item.name.toLowerCase().contains(pattern)) ||
                                    (item.email != null && item.email.toLowerCase().contains(pattern))) {
                                filtered.add(item);
                            }
                        }
                    }
                    FilterResults results = new FilterResults();
                    results.values = filtered;
                    return results;
                }
                @Override
                @SuppressWarnings("unchecked")
                @SuppressLint("NotifyDataSetChanged")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    list = (List<UserModel>) results.values;
                    notifyDataSetChanged();
                    checkEmptyState(list.size());
                }
            };
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(v);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserModel user = list.get(position);

            holder.userName.setText(user.name != null ? user.name : "Unknown User");
            holder.userEmail.setText(user.email != null ? user.email : "No Email");
            holder.userStatus.setText(user.status.toUpperCase());
            holder.userBalance.setText(user.balance);

            if(user.name != null && !user.name.isEmpty()){
                holder.userAvatarInitials.setText(String.valueOf(user.name.charAt(0)).toUpperCase());
            }

            // 🌟 COLORS (Archived ke liye Grey Color add kiya) 🌟
            if(user.status.equalsIgnoreCase("Blocked")) {
                holder.statusBadgeCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                holder.userStatus.setTextColor(Color.parseColor("#D32F2F"));
            } else if (user.status.equalsIgnoreCase("Pending Verification")) {
                holder.statusBadgeCard.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
                holder.userStatus.setTextColor(Color.parseColor("#E65100"));
            } else if (user.status.equalsIgnoreCase("Archived")) {
                holder.statusBadgeCard.setCardBackgroundColor(Color.parseColor("#EEEEEE")); // Grey
                holder.userStatus.setTextColor(Color.parseColor("#9E9E9E")); // Grey Text
            } else {
                holder.statusBadgeCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                holder.userStatus.setTextColor(Color.parseColor("#2E7D32"));
            }

            // REAL EDIT CLICK
            holder.btnEditUser.setOnClickListener(v -> showEditDialog(user));

            // ULTIMATE BULLETPROOF DELETE CLICK
            holder.btnDeleteUser.setOnClickListener(v -> {
                holder.btnDeleteUser.setEnabled(false);

                db.collection("Loans")
                        .whereEqualTo("userId", user.uid)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            holder.btnDeleteUser.setEnabled(true);

                            if (queryDocumentSnapshots.isEmpty()) {
                                new MaterialAlertDialogBuilder(v.getContext())
                                        .setTitle("Permanently Delete User?")
                                        .setMessage("This user has no financial history. They will be permanently removed from the system.")
                                        .setIcon(android.R.drawable.ic_delete)
                                        .setPositiveButton("DELETE", (dialog, which) -> {
                                            db.collection("Users").document(user.uid).delete()
                                                    .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "User deleted permanently!", Toast.LENGTH_SHORT).show());
                                        })
                                        .setNegativeButton("CANCEL", null)
                                        .show();
                            } else {
                                boolean hasActiveDebt = false;
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    String status = doc.getString("status");
                                    if ("Pending".equalsIgnoreCase(status)) {
                                        hasActiveDebt = true;
                                        break;
                                    } else if ("Approved".equalsIgnoreCase(status)) {
                                        String appStr = doc.getString("approvedAmount");
                                        String paidStr = doc.getString("paidAmount");

                                        int approved = 0, paid = 0;
                                        try { if (appStr != null) approved = Integer.parseInt(appStr); } catch (Exception e){}
                                        try { if (paidStr != null) paid = Integer.parseInt(paidStr); } catch (Exception e){}

                                        if (paid < approved) {
                                            hasActiveDebt = true;
                                            break;
                                        }
                                    }
                                }

                                if (hasActiveDebt) {
                                    new MaterialAlertDialogBuilder(v.getContext())
                                            .setTitle("Action Denied!")
                                            .setMessage(user.name + " has an active loan or a pending request. You cannot delete this user.\n\nTip: You can change their status to 'Blocked' from the Edit menu instead.")
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton("OK UNDERSTOOD", null)
                                            .show();
                                } else {
                                    new MaterialAlertDialogBuilder(v.getContext())
                                            .setTitle("Archive User?")
                                            .setMessage("This user has completed their loans. Hard deleting will ruin the NGO's financial reports.\n\nDo you want to securely 'Archive' them instead?")
                                            .setIcon(android.R.drawable.ic_menu_save)
                                            .setPositiveButton("ARCHIVE SECURELY", (dialog, which) -> {
                                                db.collection("Users").document(user.uid)
                                                        .update("accountStatus", "Archived")
                                                        .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "User Archived successfully!", Toast.LENGTH_SHORT).show());
                                            })
                                            .setNegativeButton("CANCEL", null)
                                            .show();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            holder.btnDeleteUser.setEnabled(true);
                            Toast.makeText(v.getContext(), "Error checking database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        public class UserViewHolder extends RecyclerView.ViewHolder {
            TextView userName, userEmail, userStatus, userBalance, userAvatarInitials;
            ImageView btnEditUser, btnDeleteUser;
            MaterialCardView statusBadgeCard;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                userName = itemView.findViewById(R.id.userName);
                userEmail = itemView.findViewById(R.id.userEmail);
                userStatus = itemView.findViewById(R.id.userStatus);
                userBalance = itemView.findViewById(R.id.userBalance);
                userAvatarInitials = itemView.findViewById(R.id.userAvatarInitials);
                statusBadgeCard = itemView.findViewById(R.id.statusBadgeCard);
                btnEditUser = itemView.findViewById(R.id.btnEditUser);
                btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
            }
        }
    }
}