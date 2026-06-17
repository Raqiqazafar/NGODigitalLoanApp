package com.example.ngodigitalloan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

// 🌟 FIREBASE FIRESTORE 🌟
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    Toolbar profileToolbar;
    TextView tvProfileName, tvProfileEmail, tvActiveLoansCount, tvInitialsAvatar; // Avatar TextView
    AutoCompleteTextView cityAutoComplete;
    MultiAutoCompleteTextView skillsMultiAutoComplete;

    Slider ageSlider;
    MaterialSwitch notificationSwitch;
    MaterialCheckBox dataSaverCheckbox;
    RatingBar appRatingBar;
    MaterialButton btnSaveProfile, logoutButton;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Firebase Init
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if(mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        // Views binding
        profileToolbar = findViewById(R.id.profileToolbar);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvActiveLoansCount = findViewById(R.id.tvActiveLoansCount);
        tvInitialsAvatar = findViewById(R.id.tvInitialsAvatar); // Bind Avatar

        cityAutoComplete = findViewById(R.id.cityAutoComplete);
        skillsMultiAutoComplete = findViewById(R.id.skillsMultiAutoComplete);
        ageSlider = findViewById(R.id.ageSlider);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        dataSaverCheckbox = findViewById(R.id.dataSaverCheckbox);
        appRatingBar = findViewById(R.id.appRatingBar);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        logoutButton = findViewById(R.id.logoutButton);

        // Toolbar
        setSupportActionBar(profileToolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        profileToolbar.setNavigationOnClickListener(v -> finish());

        // Setup Dropdowns
        String[] cities = {"Lahore", "Karachi", "Islamabad", "Faisalabad", "Multan", "Peshawar", "Quetta"};
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
        cityAutoComplete.setAdapter(cityAdapter);

        String[] skills = {"Business", "Agriculture", "Technology", "Education", "Healthcare", "E-Commerce"};
        ArrayAdapter<String> skillsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, skills);
        skillsMultiAutoComplete.setAdapter(skillsAdapter);
        skillsMultiAutoComplete.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        // Load Data
        if(currentUserId != null) {
            loadUserProfile();
            loadActiveLoansCount();
        }

        // Save Profile
        btnSaveProfile.setOnClickListener(v -> saveUserProfile());

        // Logout
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out securely", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // --- 🌟 LOAD DATA FROM FIRESTORE 🌟 ---
    private void loadUserProfile() {
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");

                        if(name != null && !name.isEmpty()) {
                            tvProfileName.setText(name);
                            // 🌟 SMART LOGIC: Naam ka pehla lafz nikal kar Avatar mein dalo
                            tvInitialsAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                        }

                        if(email != null) tvProfileEmail.setText(email);

                        // Load other settings
                        String city = documentSnapshot.getString("city");
                        String userSkills = documentSnapshot.getString("skills");
                        Double age = documentSnapshot.getDouble("age");
                        Boolean notifications = documentSnapshot.getBoolean("notificationsEnabled");
                        Boolean dataSaver = documentSnapshot.getBoolean("dataSaverEnabled");

                        if(city != null) cityAutoComplete.setText(city, false);
                        if(userSkills != null) skillsMultiAutoComplete.setText(userSkills);
                        if(age != null) ageSlider.setValue(age.floatValue());
                        if(notifications != null) notificationSwitch.setChecked(notifications);
                        if(dataSaver != null) dataSaverCheckbox.setChecked(dataSaver);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    private void loadActiveLoansCount() {
        db.collection("Loans")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("status", "Approved")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) tvActiveLoansCount.setText(String.valueOf(value.size()));
                });
    }

    private void saveUserProfile() {
        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("UPDATING CLOUD...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("city", cityAutoComplete.getText().toString());
        updates.put("skills", skillsMultiAutoComplete.getText().toString());
        updates.put("age", ageSlider.getValue());
        updates.put("notificationsEnabled", notificationSwitch.isChecked());
        updates.put("dataSaverEnabled", dataSaverCheckbox.isChecked());

        db.collection("Users").document(currentUserId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("SAVE CHANGES");
                    Toast.makeText(this, "Settings Synced to Cloud!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("SAVE CHANGES");
                    Toast.makeText(this, "Sync Failed", Toast.LENGTH_SHORT).show();
                });
    }
}