package com.example.firebasetestapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import com.example.firebasetestapp.managers.GoogleAuthManager;
import com.example.firebasetestapp.repositories.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView tvGoToRegister, tvForgotPassword;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        GoogleAuthManager googleAuthManager = new GoogleAuthManager(this, new GoogleAuthManager.AuthCallback() {
            @Override
            public void onTokenReceived(String idToken) {
                firebaseAuthWithGoogle(idToken);
            }

            @Override
            public void onError(String errorMessage) {
                btnGoogleSignIn.setEnabled(true);
                if (!errorMessage.equals("cancelled")) {
                    Toast.makeText(LoginActivity.this, "Google UI Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });

        btnGoogleSignIn.setOnClickListener(v -> {
                btnGoogleSignIn.setEnabled(false);
                googleAuthManager.startGoogleSignIn();
            }
        );
    }

    private void firebaseAuthWithGoogle(String idToken) {
        authRepository.signInWithGoogle(idToken)
                .addOnSuccessListener(user -> {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnGoogleSignIn.setEnabled(true);
                    Toast.makeText(this, "Auth Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging In...");

        // --- Backend Call ---
        authRepository.loginWithEmail(email, password)
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_LONG).show();

//                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);

                    Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Log In");

                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
