package com.example.ngodigitalloan;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// Firebase Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ApplyLoanActivity extends AppCompatActivity {

    ImageView btnBack;
    AutoCompleteTextView categoryAutoComplete;
    TextInputLayout otherCategoryLayout;
    TextInputEditText otherCategoryInput;
    TextInputEditText incomeInput;
    TextInputEditText reasonInput;
    MaterialButtonToggleGroup loanTypeToggleGroup;
    Slider amountSlider;
    TextView amountLabelTextView;
    TextInputEditText dateInput;
    MaterialSwitch termsSwitch;
    MaterialButton submitLoanButton;

    // Firebase Variables
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    String selectedDateStr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_loan);

        // Firebase Initialize
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Binding Views
        btnBack = findViewById(R.id.btnBack);
        categoryAutoComplete = findViewById(R.id.categoryAutoComplete);
        otherCategoryLayout = findViewById(R.id.otherCategoryLayout);
        otherCategoryInput = findViewById(R.id.otherCategoryInput);
        incomeInput = findViewById(R.id.incomeInput);
        reasonInput = findViewById(R.id.reasonInput);
        loanTypeToggleGroup = findViewById(R.id.loanTypeToggleGroup);
        amountSlider = findViewById(R.id.amountSlider);
        amountLabelTextView = findViewById(R.id.amountLabelTextView);
        dateInput = findViewById(R.id.dateInput);
        termsSwitch = findViewById(R.id.termsSwitch);
        submitLoanButton = findViewById(R.id.submitLoanButton);

        // Back Button
        btnBack.setOnClickListener(v -> finish());

        // Setup Dropdown Category
        String[] categories = {"Education Loan", "Small Business Loan", "Medical Emergency", "Housing/Construction", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        categoryAutoComplete.setAdapter(adapter);

        // "OTHER" LOGIC
        categoryAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            if (selectedItem.equals("Other")) {
                otherCategoryLayout.setVisibility(View.VISIBLE);
            } else {
                otherCategoryLayout.setVisibility(View.GONE);
                otherCategoryInput.setText("");
            }
        });

        // Setup Slider Logic
        amountSlider.addOnChangeListener((slider, value, fromUser) -> {
            String formattedAmount = NumberFormat.getNumberInstance(Locale.US).format((int) value);
            amountLabelTextView.setText("Rs " + formattedAmount);
        });

        // 🌟 REAL FINTECH DYNAMIC LIMIT LOGIC 🌟
        loanTypeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnShortTerm) {
                    // Safety Check: Avoid crash if value is higher than new limit
                    if (amountSlider.getValue() > 500000f) {
                        amountSlider.setValue(500000f);
                    }
                    amountSlider.setValueTo(500000f);
                    Toast.makeText(this, "Short Term limit is up to Rs 5 Lakhs", Toast.LENGTH_SHORT).show();
                } else if (checkedId == R.id.btnLongTerm) {
                    amountSlider.setValueTo(2000000f);
                    Toast.makeText(this, "Long Term limit unlocked up to Rs 20 Lakhs", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Date Picker Logic
        dateInput.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date for Funds")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
                selectedDateStr = sdf.format(new Date(selection));
                dateInput.setText(selectedDateStr);
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        // Submit Button Logic
        submitLoanButton.setOnClickListener(v -> submitApplicationToCloud());
    }

    private void submitApplicationToCloud() {
        String selectedCategory = categoryAutoComplete.getText().toString().trim();
        String monthlyIncome = Objects.requireNonNull(incomeInput.getText()).toString().trim();
        String description = Objects.requireNonNull(reasonInput.getText()).toString().trim();

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please select a loan purpose", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory.equals("Other")) {
            selectedCategory = Objects.requireNonNull(otherCategoryInput.getText()).toString().trim();
            if (selectedCategory.isEmpty()) {
                otherCategoryInput.setError("Please specify the loan category");
                otherCategoryInput.requestFocus();
                return;
            }
        }

        if (monthlyIncome.isEmpty()) {
            incomeInput.setError("Income is required for risk assessment");
            incomeInput.requestFocus();
            return;
        }

        if (selectedDateStr.isEmpty()) {
            Toast.makeText(this, "Please select when you need the funds", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            reasonInput.setError("Please provide a detailed reason");
            reasonInput.requestFocus();
            return;
        }

        if (!termsSwitch.isChecked()) {
            Toast.makeText(this, "Please agree to the NGO's terms & conditions!", Toast.LENGTH_SHORT).show();
            return;
        }

        submitLoanButton.setEnabled(false);
        submitLoanButton.setText("SUBMITTING TO SERVER...");

        String requestedAmount = String.valueOf((int) amountSlider.getValue());
        String repaymentPlan = (loanTypeToggleGroup.getCheckedButtonId() == R.id.btnLongTerm) ? "Long Term (12-24 Months)" : "Short Term (6 Months)";
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> loanData = new HashMap<>();
        loanData.put("userId", userId);
        loanData.put("loanType", selectedCategory);
        loanData.put("monthlyIncome", monthlyIncome);
        loanData.put("description", description);
        loanData.put("requestedAmount", requestedAmount);
        loanData.put("approvedAmount", "0");
        loanData.put("paidAmount", "0");
        loanData.put("repaymentPlan", repaymentPlan);
        loanData.put("requiredDate", selectedDateStr);
        loanData.put("status", "Pending");
        loanData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("Loans").add(loanData)
                .addOnSuccessListener(documentReference -> showSuccessDialog())
                .addOnFailureListener(e -> {
                    submitLoanButton.setEnabled(true);
                    submitLoanButton.setText("SUBMIT APPLICATION");
                    Toast.makeText(this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
                .setTitle("Application Sent!")
                .setMessage("Your loan request has been successfully submitted to our review team. You will be notified shortly.")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("BACK TO DASHBOARD", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}