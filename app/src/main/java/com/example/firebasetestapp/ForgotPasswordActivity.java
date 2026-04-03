package com.example.firebasetestapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.firebasetestapp.repositories.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {
    private TextInputEditText etEmail;
    private MaterialButton btnResetPassword;
    private TextView tvBackToLogin;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authRepository = new AuthRepository();

        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        setupClickListeners();
    }

    private void setupClickListeners() {
        tvBackToLogin.setOnClickListener(v -> {
            finish();
        });

//        btnResetPassword.setOnClickListener(v -> handlePasswordReset());
    }
}
