package com.example.ngodigitalloan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

// 🌟 FIRESTORE IMPORTS 🌟
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminSettingsActivity extends AppCompatActivity {

    Toolbar settingsToolbar;
    TextView adminNameText, tvAdminAvatar, tvMaxReqValue;
    MaterialSwitch switchNotifications, switchMaintenance;
    AutoCompleteTextView regionAutoComplete;
    AutoCompleteTextView roleAutoComplete;
    Slider maxRequestsSlider;
    TextInputEditText logDateInput;
    MaterialButton btnSaveSettings, btnLogout;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String currentUserId;
    String selectedDateStr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        // Firebase Init (Firestore)
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        // Binding UI
        settingsToolbar = findViewById(R.id.settingsToolbar);
        adminNameText = findViewById(R.id.adminNameText);
        tvAdminAvatar = findViewById(R.id.tvAdminAvatar);
        tvMaxReqValue = findViewById(R.id.tvMaxReqValue);

        switchNotifications = findViewById(R.id.switchNotifications);
        switchMaintenance = findViewById(R.id.switchMaintenance);
        regionAutoComplete = findViewById(R.id.regionAutoComplete);
        roleAutoComplete = findViewById(R.id.roleAutoComplete);
        maxRequestsSlider = findViewById(R.id.maxRequestsSlider);
        logDateInput = findViewById(R.id.logDateInput);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnLogout = findViewById(R.id.btnLogout);

        // Toolbar Setup
        setSupportActionBar(settingsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        settingsToolbar.setNavigationOnClickListener(v -> finish());

        // Setup Dropdowns
        String[] regions = {"Punjab", "Sindh", "KPK", "Balochistan", "Islamabad", "Gilgit-Baltistan"};
        ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, regions);
        regionAutoComplete.setAdapter(regionAdapter);

        String[] roles = {"Super Admin", "Regional Manager", "Loan Reviewer", "Auditor"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        roleAutoComplete.setAdapter(roleAdapter);

        // Slider Listener (Live Value Update)
        maxRequestsSlider.addOnChangeListener((slider, value, fromUser) -> {
            tvMaxReqValue.setText(String.valueOf((int) value));
        });

        // 🌟 CALENDAR POPUP LOGIC 🌟
        logDateInput.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date for Logs")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
                selectedDateStr = sdf.format(new Date(selection));
                logDateInput.setText(selectedDateStr);
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        // Load Data from Firestore
        if (currentUserId != null) {
            loadAdminData();
        }

        // Save Settings
        btnSaveSettings.setOnClickListener(v -> saveSettingsToFirebase());

        // Secure Logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(AdminSettingsActivity.this, "Logged out securely!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminSettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // --- FIRESTORE Fetch Logic ---
    private void loadAdminData() {
        // Fetch Name & Set Avatar
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            adminNameText.setText(name);
                            tvAdminAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase()); // Initials Avatar!
                        }
                    }
                });

        // Fetch saved settings
        db.collection("AdminSettings").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean notif = documentSnapshot.getBoolean("notificationsEnabled");
                        Boolean maintenance = documentSnapshot.getBoolean("maintenanceMode");
                        String region = documentSnapshot.getString("region");
                        String role = documentSnapshot.getString("role");
                        Double maxReq = documentSnapshot.getDouble("maxAutoApprovals");
                        String dateStr = documentSnapshot.getString("logFilterDate");

                        if (notif != null) switchNotifications.setChecked(notif);
                        if (maintenance != null) switchMaintenance.setChecked(maintenance);
                        if (region != null) regionAutoComplete.setText(region, false);
                        if (role != null) roleAutoComplete.setText(role, false);
                        if (dateStr != null) {
                            selectedDateStr = dateStr;
                            logDateInput.setText(dateStr);
                        }
                        if (maxReq != null) {
                            maxRequestsSlider.setValue(maxReq.floatValue());
                            tvMaxReqValue.setText(String.valueOf(maxReq.intValue()));
                        }
                    }
                });
    }

    // --- FIRESTORE Save Logic ---
    private void saveSettingsToFirebase() {
        if (currentUserId == null) return;

        btnSaveSettings.setEnabled(false);
        btnSaveSettings.setText("SAVING...");

        Map<String, Object> settingsMap = new HashMap<>();
        settingsMap.put("notificationsEnabled", switchNotifications.isChecked());
        settingsMap.put("maintenanceMode", switchMaintenance.isChecked());
        settingsMap.put("region", regionAutoComplete.getText().toString());
        settingsMap.put("role", roleAutoComplete.getText().toString());
        settingsMap.put("maxAutoApprovals", (int) maxRequestsSlider.getValue());
        settingsMap.put("logFilterDate", selectedDateStr);

        db.collection("AdminSettings").document(currentUserId).set(settingsMap)
                .addOnSuccessListener(aVoid -> {
                    btnSaveSettings.setEnabled(true);
                    btnSaveSettings.setText("SAVE SETTINGS");
                    Toast.makeText(AdminSettingsActivity.this, "Settings Synced to Cloud!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSaveSettings.setEnabled(true);
                    btnSaveSettings.setText("SAVE SETTINGS");
                    Toast.makeText(AdminSettingsActivity.this, "Sync Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}