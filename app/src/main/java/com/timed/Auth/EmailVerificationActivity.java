package com.timed.Auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.timed.ProfileActivity;
import com.timed.R;
import com.timed.repositories.AuthRepository;
import com.google.android.material.button.MaterialButton;

public class EmailVerificationActivity extends AppCompatActivity {
    private MaterialButton btnCheckVerification, btnResendEmail;
    private TextView tvBackToLogin;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        authRepository = new AuthRepository();

        btnCheckVerification = findViewById(R.id.btnCheckVerification);
        btnResendEmail = findViewById(R.id.btnResendEmail);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnCheckVerification.setOnClickListener(v -> {
            btnCheckVerification.setEnabled(false);
            btnCheckVerification.setText("Checking...");

            authRepository.reloadUser().addOnCompleteListener(task -> {
                if (task.isSuccessful() && authRepository.isCurrentUserVerified()) {

                    authRepository.markEmailAsVerifiedInFirestore().addOnCompleteListener(updateTask -> {
                        Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(EmailVerificationActivity.this, ProfileActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });

                } else {
                    btnCheckVerification.setEnabled(true);
                    btnCheckVerification.setText("I've verified my email");
                    Toast.makeText(this, "Not verified yet. Please check your inbox or spam folder.", Toast.LENGTH_LONG).show();
                }
            });
        });

        btnResendEmail.setOnClickListener(v -> {
            btnResendEmail.setEnabled(false);
            authRepository.sendVerificationEmail().addOnCompleteListener(task -> {
                Toast.makeText(this, "Verification email resent!", Toast.LENGTH_SHORT).show();
                // Re-enable after 10 seconds to prevent spam clicking
                btnResendEmail.postDelayed(() -> btnResendEmail.setEnabled(true), 10000);
            });
        });

        tvBackToLogin.setOnClickListener(v -> {
            authRepository.logout();
            Intent intent = new Intent(EmailVerificationActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
