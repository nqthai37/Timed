package com.example.firebasetestapp.Auth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import com.example.firebasetestapp.ProfileActivity;
import com.example.firebasetestapp.R;
import com.example.firebasetestapp.managers.GoogleAuthManager;
import com.example.firebasetestapp.repositories.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView tvGoToRegister, tvForgotPassword;
    private MaterialCheckBox cbRememberMe;
    private SharedPreferences prefs;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        prefs = getSharedPreferences("TimedAppPrefs", MODE_PRIVATE);

        Runnable clearErrors = () -> {
            tilEmail.setErrorEnabled(false);
            tilEmail.setError(null);
            tilPassword.setErrorEnabled(false);
            tilPassword.setError(null);
        };

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus)
                clearErrors.run();
        };

        etEmail.setOnFocusChangeListener(focusListener);
        etPassword.setOnFocusChangeListener(focusListener);

        TextWatcher typingListener = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearErrors.run();
            }
        };

        etEmail.addTextChangedListener(typingListener);
        etPassword.addTextChangedListener(typingListener);

        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean wantsToBeRemembered = prefs.getBoolean("REMEMBER_ME", false);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            if (wantsToBeRemembered) {
                btnLogin.setEnabled(false);
                btnLogin.setText("Restoring Session...");

                authRepository.restoreSession().addOnSuccessListener(user -> {
                   Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }).addOnFailureListener(e -> {
                    authRepository.logout();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Sign in");
                });
            } else {
                authRepository.logout();
            }
        }
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
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        authRepository.signInWithGoogle(idToken)
                .addOnSuccessListener(user -> {
                    prefs.edit().putBoolean("REMEMBER_ME", cbRememberMe.isChecked()).apply();
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

        tilEmail.setErrorEnabled(false);
        tilEmail.setError(null);
        tilPassword.setErrorEnabled(false);
        tilPassword.setError(null);

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setErrorEnabled(true);
            tilEmail.setError("Valid email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 8) {
            tilPassword.setErrorEnabled(true);
            tilPassword.setError("Password must be at least 8 characters");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging In...");

        // --- Backend Call ---
        authRepository.loginWithEmail(email, password)
                .addOnSuccessListener(user -> {

                    prefs.edit().putBoolean("REMEMBER_ME", cbRememberMe.isChecked()).apply();

                    Toast.makeText(LoginActivity.this, "Welcome back, " + user.getName() + "!", Toast.LENGTH_LONG)
                            .show();

                    // Intent intent = new Intent(RegisterActivity.this, MainActivity.class);

                    Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Log In");

                    if (e.getMessage() != null && e.getMessage().contains("Please verify your email")) {

                        Toast.makeText(this, "Please verify your email before login", Toast.LENGTH_SHORT).show();
                        authRepository.sendVerificationEmail();

                        Intent intent = new Intent(LoginActivity.this, EmailVerificationActivity.class);
                        startActivity(intent);
                    } else if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                        etPassword.requestFocus();

                        tilEmail.setErrorEnabled(true);
                        tilEmail.setError("Incorrect email or password");

                        tilPassword.setErrorEnabled(true);
                        tilPassword.setError("Incorrect email or password");

                    } else {
                        Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
