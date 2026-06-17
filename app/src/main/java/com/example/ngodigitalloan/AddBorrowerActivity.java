package com.example.ngodigitalloan;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

// Firestore Imports
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AddBorrowerActivity extends AppCompatActivity {

    Toolbar addBorrowerToolbar;
    TextInputEditText inputName, inputCnic, inputEmail, inputPhone;
    AutoCompleteTextView statusAutoComplete;
    MaterialButton btnSaveBorrower;

    // Firestore
    FirebaseFirestore db;

    // For CNIC Logic
    boolean isFormattingCnic = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_borrower);

        db = FirebaseFirestore.getInstance();

        // Binding UI
        addBorrowerToolbar = findViewById(R.id.addBorrowerToolbar);
        inputName = findViewById(R.id.inputName);
        inputCnic = findViewById(R.id.inputCnic);
        inputEmail = findViewById(R.id.inputEmail);
        inputPhone = findViewById(R.id.inputPhone);
        statusAutoComplete = findViewById(R.id.statusAutoComplete);
        btnSaveBorrower = findViewById(R.id.btnSaveBorrower);

        // Toolbar Back Button
        setSupportActionBar(addBorrowerToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        addBorrowerToolbar.setNavigationOnClickListener(v -> finish());

        // Dropdown Setup
        String[] statuses = {"Active", "Blocked", "Pending Verification"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statuses);
        statusAutoComplete.setAdapter(adapter);

        // 🌟 NAYA: LIVE CNIC FORMATTING (e.g. 35202-1234567-1) 🌟
        inputCnic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingCnic) return;
                isFormattingCnic = true;

                String str = s.toString().replaceAll("-", "");
                StringBuilder formatted = new StringBuilder();

                for (int i = 0; i < str.length(); i++) {
                    if (i == 5 || i == 12) {
                        formatted.append("-");
                    }
                    formatted.append(str.charAt(i));
                }

                inputCnic.setText(formatted.toString());
                inputCnic.setSelection(formatted.length()); // Cursor aakhir mein rakho
                isFormattingCnic = false;
            }
        });

        // Save Button Logic
        btnSaveBorrower.setOnClickListener(v -> {
            String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";
            String cnic = inputCnic.getText() != null ? inputCnic.getText().toString().trim() : "";
            String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim().toLowerCase() : "";
            String phone = inputPhone.getText() != null ? inputPhone.getText().toString().trim() : "";
            String status = statusAutoComplete.getText() != null ? statusAutoComplete.getText().toString().trim() : "";

            // 🌟 SMART VALIDATIONS 🌟
            if (name.isEmpty() || name.length() < 3) {
                inputName.setError("Enter a valid full name"); return;
            }
            if (cnic.length() < 15) { // 13 digits + 2 dashes
                inputCnic.setError("CNIC must be 13 digits"); return;
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inputEmail.setError("Enter a valid email address"); return;
            }
            if (phone.length() < 11) {
                inputPhone.setError("Enter valid phone number"); return;
            }
            if (status.isEmpty()) {
                statusAutoComplete.setError("Please select status"); return;
            }

            btnSaveBorrower.setEnabled(false);
            btnSaveBorrower.setText("SAVING TO CLOUD...");

            // Pehle check karo ke yeh Email pehle se mojood toh nahi?
            db.collection("Users").whereEqualTo("email", email).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Agar Email mil gayi
                            resetButton();
                            inputEmail.setError("This email is already registered!");
                            Toast.makeText(this, "A user with this email already exists.", Toast.LENGTH_LONG).show();
                        } else {
                            // Agar nahi mili toh Save karo
                            saveToFirestore(name, cnic, email, phone, status);
                        }
                    });
        });
    }

    private void saveToFirestore(String name, String cnic, String email, String phone, String status) {
        Map<String, Object> borrower = new HashMap<>();
        borrower.put("name", name);
        borrower.put("cnic", cnic);
        borrower.put("email", email);
        borrower.put("phone", phone);
        borrower.put("accountStatus", status);
        borrower.put("role", "user");
        borrower.put("balance", "Rs 0");
        borrower.put("addedByAdmin", true); // 🌟 Identity Flag
        borrower.put("timestamp", FieldValue.serverTimestamp());

        db.collection("Users").add(borrower)
                .addOnSuccessListener(documentReference -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Borrower Created")
                            .setMessage(name + " has been successfully registered into the NGO database.")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton("DONE", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    resetButton();
                    Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetButton() {
        btnSaveBorrower.setEnabled(true);
        btnSaveBorrower.setText("SAVE BORROWER");
    }
}