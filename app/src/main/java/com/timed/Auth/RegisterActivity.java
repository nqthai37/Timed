package com.timed.Auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.timed.ProfileActivity;
import com.timed.R;
import com.timed.managers.GoogleAuthManager;
import com.timed.repositories.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etName, etEmail, etPassword;
    private MaterialButton btnRegister, btnGoogleSignUp;
    private TextView tvGoToLogin;
    private TextInputLayout tilPassword;
    private TextView tvReqLength, tvReqUppercase, tvReqLowercase, tvReqNumber;
    private MaterialCheckBox cbTerms;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository();

        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        tilPassword = findViewById(R.id.tilPassword);
        cbTerms = findViewById(R.id.cbTerms);
        tvReqLength = findViewById(R.id.tvReqLength);
        tvReqUppercase = findViewById(R.id.tvReqUppercase);
        tvReqLowercase = findViewById(R.id.tvReqLowercase);
        tvReqNumber = findViewById(R.id.tvReqNumber);

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> handleRegistration());

        tvGoToLogin.setOnClickListener(v -> {
            finish();
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();

                updateRequirementState(tvReqLength, password.length() >= 8);
                updateRequirementState(tvReqUppercase, password.matches(".*[A-Z].*"));
                updateRequirementState(tvReqLowercase, password.matches(".*[a-z].*"));
                updateRequirementState(tvReqNumber, password.matches(".*\\d.*"));
            }
        });

        GoogleAuthManager googleAuthManager = new GoogleAuthManager(this, new GoogleAuthManager.AuthCallback() {
            @Override
            public void onTokenReceived(String idToken) {
                firebaseAuthWithGoogle(idToken);
            }

            @Override
            public void onError(String errorMessage) {
                btnRegister.setEnabled(true);
                if (!errorMessage.equals("cancelled")) {
                    Toast.makeText(RegisterActivity.this, "Google UI Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });

        btnGoogleSignUp.setOnClickListener(v -> {
            btnRegister.setEnabled(false);
            googleAuthManager.startGoogleSignIn();
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        authRepository.signInWithGoogle(idToken)
                .addOnSuccessListener(user -> {
                    Toast.makeText(this, "Welcome, " + user.getName() + "!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Auth Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateRequirementState(TextView tv, boolean isMet) {
        int color = isMet ? android.graphics.Color.parseColor("#10B981") : android.graphics.Color.parseColor("#94A3B8");

        tv.setTextColor(color);

        if (tv.getCompoundDrawablesRelative()[0] != null) {
            tv.getCompoundDrawablesRelative()[0].setTint(color);
        }
    }

    private void handleRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            etEmail.requestFocus();
            return;
        }

        tilPassword.setErrorEnabled(false);
        tilPassword.setError(null);

        boolean hasLength = password.length() >= 8;
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*\\d.*");

        if (!hasLength || !hasUpper || !hasLower || !hasNumber) {
            tilPassword.setErrorEnabled(true);
            tilPassword.setError("Please meet all password requirements below.");
            etPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "You must agree to the Terms & Conditions to register.", Toast.LENGTH_LONG).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        authRepository.registerWithEmail(name, email, password)
                .addOnSuccessListener(aVoid -> {

                    authRepository.sendVerificationEmail();

                    Intent intent = new Intent(RegisterActivity.this, EmailVerificationActivity.class);

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Sign Up");

                    Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
