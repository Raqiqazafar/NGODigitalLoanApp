package com.example.ngodigitalloan;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

// Firebase
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    Toolbar resetToolbar;
    TextInputEditText resetEmailInput;
    MaterialButton btnSendResetLink;
    TextView tvBackToLogin;

    // Firebase Auth
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // UI Binding
        resetToolbar = findViewById(R.id.resetToolbar);
        resetEmailInput = findViewById(R.id.resetEmailInput);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Toolbar Setup
        setSupportActionBar(resetToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(
                    androidx.appcompat.R.drawable.abc_ic_ab_back_material
            );
        }

        resetToolbar.setNavigationOnClickListener(v -> finish());

        // Back to login
        tvBackToLogin.setOnClickListener(v -> finish());

        // Reset Button Click
        btnSendResetLink.setOnClickListener(v -> {

            String email = resetEmailInput.getText().toString().trim();

            if (email.isEmpty()) {
                resetEmailInput.setError("Email is required!");
                resetEmailInput.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                resetEmailInput.setError("Enter valid email!");
                resetEmailInput.requestFocus();
                return;
            }

            btnSendResetLink.setEnabled(false);
            btnSendResetLink.setText("VERIFYING EMAIL...");

            // First Check Email Exists
            mAuth.fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            boolean isNewUser =
                                    task.getResult().getSignInMethods().isEmpty();

                            if (isNewUser) {

                                // Email Not Exist
                                btnSendResetLink.setEnabled(true);
                                btnSendResetLink.setText("SEND RESET LINK");

                                resetEmailInput.setError("Email not registered!");
                                resetEmailInput.requestFocus();

                                Toast.makeText(
                                        ForgotPasswordActivity.this,
                                        "This email does not exist",
                                        Toast.LENGTH_LONG
                                ).show();

                            } else {

                                // Email Exist -> Send Reset Link
                                sendResetEmail(email);

                            }

                        } else {

                            btnSendResetLink.setEnabled(true);
                            btnSendResetLink.setText("SEND RESET LINK");

                            Toast.makeText(
                                    ForgotPasswordActivity.this,
                                    "Network Error",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    });

        });

    }


    // Send Reset Email
    private void sendResetEmail(String email) {

        btnSendResetLink.setText("SENDING...");

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {

                    btnSendResetLink.setText("EMAIL SENT");

                    showSuccessDialog(email);

                })
                .addOnFailureListener(e -> {

                    btnSendResetLink.setEnabled(true);
                    btnSendResetLink.setText("SEND RESET LINK");

                    Toast.makeText(
                            ForgotPasswordActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });

    }


    // Success Dialog
    private void showSuccessDialog(String email) {

        new MaterialAlertDialogBuilder(
                this,
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered
        )
                .setTitle("Secure Link Sent")
                .setMessage(
                        "We have sent a password reset link to:\n\n"
                                + email
                                + "\n\nCheck your email to reset password."
                )
                .setIcon(android.R.drawable.ic_dialog_email)
                .setPositiveButton("BACK TO LOGIN", (dialog, which) -> finish())
                .setCancelable(false)
                .show();

    }

}