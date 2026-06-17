package com.example.ngodigitalloan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

// Firebase Imports
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText emailInput, passwordInput;
    MaterialCheckBox rememberMeCheck;
    MaterialButton loginButton, btnGoogleLogin; // 🌟 Google Button added
    TextView registerTextView, forgotPasswordText;

    // Firebase & Google Variables
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    GoogleSignInClient mGoogleSignInClient; // 🌟 Client added
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Initialization
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🌟 GOOGLE SIGN-IN SETUP 🌟
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Requires google-services.json
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Binding UI views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberMeCheck = findViewById(R.id.rememberMeCheck);
        loginButton = findViewById(R.id.loginButton);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin); // 🌟 Binding
        registerTextView = findViewById(R.id.registerTextView);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        // --- 1. EMAIL/PASSWORD LOGIN LOGIC ---
        loginButton.setOnClickListener(v -> {
            String email = (emailInput.getText() != null) ? emailInput.getText().toString().trim() : "";
            String password = (passwordInput.getText() != null) ? passwordInput.getText().toString().trim() : "";

            if (email.isEmpty()) {
                emailInput.setError("Email is required!");
                emailInput.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                passwordInput.setError("Password is required!");
                passwordInput.requestFocus();
                return;
            }

            loginButton.setEnabled(false);
            loginButton.setText("LOGGING IN...");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Fetch user info and pass to Gatekeeper
                            String userID = mAuth.getCurrentUser().getUid();
                            String userEmail = mAuth.getCurrentUser().getEmail();
                            checkUserRoleAndStatus(userID, userEmail, "Email User");
                        } else {
                            loginButton.setEnabled(true);
                            loginButton.setText("SECURE LOGIN");
                            Toast.makeText(LoginActivity.this, "Login Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown Error"), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // --- 2. GOOGLE LOGIN CLICK LISTENER 🌟 ---
        btnGoogleLogin.setOnClickListener(v -> {
            btnGoogleLogin.setEnabled(false);
            loginButton.setEnabled(false);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());
    }

    // 🌟 HANDLE GOOGLE POPUP RESULT 🌟
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                btnGoogleLogin.setEnabled(true);
                loginButton.setEnabled(true);
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 🌟 LINK GOOGLE ACCOUNT TO FIREBASE 🌟
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        String email = mAuth.getCurrentUser().getEmail();
                        String name = mAuth.getCurrentUser().getDisplayName();

                        // Pass to the Master Gatekeeper
                        checkUserRoleAndStatus(userId, email, name);
                    } else {
                        btnGoogleLogin.setEnabled(true);
                        loginButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 🌟 THE MASTER GATEKEEPER (Checks Status, Role & Auto-Registers Google Users) 🌟
    private void checkUserRoleAndStatus(String userID, String email, String nameFallback) {
        db.collection("Users").document(userID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        String status = documentSnapshot.getString("accountStatus");

                        // 🌟 GUARD: STATUS VALIDATION 🌟
                        if ("Archived".equalsIgnoreCase(status)) {
                            mAuth.signOut();
                            resetButtons();
                            Toast.makeText(LoginActivity.this, "Your account has been archived/deleted. Please contact support.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if ("Blocked".equalsIgnoreCase(status)) {
                            mAuth.signOut();
                            resetButtons();
                            Toast.makeText(LoginActivity.this, "Your account is temporarily blocked by the Admin.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // 🌟 ALL GOOD: Role Based Routing 🌟
                        if ("admin".equals(role)) {
                            Toast.makeText(LoginActivity.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                        } else {
                            Toast.makeText(LoginActivity.this, "Welcome to Ehsan Digital!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        }
                        finish();
                    } else {
                        // 🌟 AUTO-REGISTER NEW GOOGLE USER IN FIRESTORE 🌟
                        Map<String, Object> newUser = new HashMap<>();
                        newUser.put("name", nameFallback != null ? nameFallback : "Google User");
                        newUser.put("email", email);
                        newUser.put("phone", ""); // Optional
                        newUser.put("cnic", "");  // Optional
                        newUser.put("role", "user");
                        newUser.put("accountStatus", "Active");
                        newUser.put("balance", "Rs 0");

                        db.collection("Users").document(userID).set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(LoginActivity.this, "Google Account Registered Successfully!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                    finish();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    resetButtons();
                    Toast.makeText(LoginActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetButtons() {
        loginButton.setEnabled(true);
        loginButton.setText("SECURE LOGIN");
        btnGoogleLogin.setEnabled(true);
    }

    private void showForgotPasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_forgot_password, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextInputEditText resetEmailInput = dialogView.findViewById(R.id.resetEmailInput);
        MaterialButton btnSendResetLink = dialogView.findViewById(R.id.btnSendResetLink);

        btnSendResetLink.setOnClickListener(v -> {
            String email = (resetEmailInput.getText() != null) ? resetEmailInput.getText().toString().trim() : "";
            if (email.isEmpty()) {
                resetEmailInput.setError("Please enter your email!");
                resetEmailInput.requestFocus();
            } else {
                btnSendResetLink.setEnabled(false);
                btnSendResetLink.setText("SENDING...");

                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            } else {
                                btnSendResetLink.setEnabled(true);
                                btnSendResetLink.setText("SEND RESET LINK");
                                Toast.makeText(this, "Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown Error"), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        dialog.show();
    }
}