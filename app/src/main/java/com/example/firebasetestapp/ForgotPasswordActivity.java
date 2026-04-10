package com.example.firebasetestapp;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.firebasetestapp.repositories.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPasswordActivity extends AppCompatActivity {
    private TextInputEditText etEmail;
    private TextInputLayout tilEmail;
    private MaterialButton btnResetPassword;
    private TextView tvBackToLogin;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authRepository = new AuthRepository();

        etEmail = findViewById(R.id.etEmail);
        tilEmail = findViewById(R.id.tilEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        setupClickListeners();
    }

    private void setupClickListeners() {
        tvBackToLogin.setOnClickListener(v -> {
            finish();
        });

        btnResetPassword.setOnClickListener(v -> handlePasswordReset());
    }

    private void handlePasswordReset() {
        String email = etEmail.getText().toString().trim();

        tilEmail.setErrorEnabled(false);
        tilEmail.setError(null);

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setErrorEnabled(true);
            tilEmail.setError("Please enter a valid registered email address");
            etEmail.requestFocus();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Sending...");

        authRepository.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent! Check your inbox.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText("Reset Password");
                    Toast.makeText(ForgotPasswordActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
