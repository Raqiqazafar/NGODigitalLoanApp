package com.example.ngodigitalloan;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// Firebase Auth & Firestore Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    ImageView btnBack;
    TextInputEditText nameInput, cnicInput, phoneInput, emailInput, passwordInput;
    MaterialButton btnSubmitRegister;
    TextView backToLoginText;

    // Firebase Variables
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // 🌟 STRONG PASSWORD REGEX 🌟
    // Kam az kam 1 chota lafz, 1 number, 1 special character aur total 6 ki length
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         // at least 1 digit
                    "(?=.*[a-zA-Z])" +      // any letter
                    "(?=.*[@#$%^&+=!])" +   // at least 1 special character
                    "(?=\\S+$)" +           // no white spaces
                    ".{6,}" +               // at least 6 characters
                    "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase Initialize
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Binding UI Views
        btnBack = findViewById(R.id.btnBack);
        nameInput = findViewById(R.id.nameInput);
        cnicInput = findViewById(R.id.cnicInput);
        phoneInput = findViewById(R.id.phoneInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        backToLoginText = findViewById(R.id.backToLoginText);

        // Back Button Logic
        btnBack.setOnClickListener(v -> finish());
        backToLoginText.setOnClickListener(v -> finish());

        // Register Action Logic
        btnSubmitRegister.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String cnic = cnicInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // 1. Basic Validations
            if (name.isEmpty() || cnic.length() < 13 || phone.length() < 11 || email.isEmpty()) {
                Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Enter a valid email address!");
                emailInput.requestFocus();
                return;
            }

            // 🌟 2. Real App Password Validation 🌟
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                passwordInput.setError("Password must contain at least 1 number, 1 letter, and 1 special character (@#$%^&+=!)");
                passwordInput.requestFocus();
                return;
            }

            btnSubmitRegister.setEnabled(false);
            btnSubmitRegister.setText("VERIFYING DETAILS...");

            // 🌟 3. Check CNIC in Firestore 🌟
            db.collection("Users").whereEqualTo("cnic", cnic).get()
                    .addOnCompleteListener(cnicTask -> {
                        if (cnicTask.isSuccessful() && !cnicTask.getResult().isEmpty()) {
                            // CNIC pehle se majood hai!
                            cnicInput.setError("This CNIC is already registered!");
                            cnicInput.requestFocus();
                            resetButton();
                        } else {

                            // 🌟 4. Check Phone in Firestore 🌟
                            db.collection("Users").whereEqualTo("phone", phone).get()
                                    .addOnCompleteListener(phoneTask -> {
                                        if (phoneTask.isSuccessful() && !phoneTask.getResult().isEmpty()) {
                                            // Phone Number pehle se majood hai!
                                            phoneInput.setError("This phone number is already registered!");
                                            phoneInput.requestFocus();
                                            resetButton();
                                        } else {

                                            // Agar CNIC aur Phone dono naye hain, toh account banao!
                                            createFirebaseAccount(name, cnic, phone, email, password);
                                        }
                                    });
                        }
                    });
        });
    }

    // Alag function bana diya taake code saaf rahay
    private void createFirebaseAccount(String name, String cnic, String phone, String email, String password) {
        btnSubmitRegister.setText("CREATING ACCOUNT...");

        // Step 1: Auth mein Account banana
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userID = mAuth.getCurrentUser().getUid();

                        // Step 2: Data tayar karna
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("cnic", cnic);
                        user.put("phone", phone);
                        user.put("email", email);
                        user.put("role", "user");
                        user.put("balance", "Rs 0");
                        user.put("accountStatus", "Active");

                        // Step 3: Cloud Firestore mein Data save karna
                        db.collection("Users").document(userID).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(RegisterActivity.this, "Account Created Successfully!", Toast.LENGTH_LONG).show();
                                    finish(); // Wapis login par bhej do
                                })
                                .addOnFailureListener(e -> {
                                    resetButton();
                                    Toast.makeText(RegisterActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        resetButton();
                        // 🌟 5. Real App Email Validation Catch 🌟
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            emailInput.setError("This email is already registered!");
                            emailInput.requestFocus();
                            Toast.makeText(RegisterActivity.this, "Account already exists with this email.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Button ko wapis normal karne ka helper method
    private void resetButton() {
        btnSubmitRegister.setEnabled(true);
        btnSubmitRegister.setText("CREATE ACCOUNT");
    }
}