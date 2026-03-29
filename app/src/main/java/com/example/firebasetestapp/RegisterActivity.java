package com.example.firebasetestapp;

import android.content.Intent;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;

import com.example.firebasetestapp.repositories.AuthRepository;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etName, etEmail, etPassword;
    private MaterialButton btnRegister, btnGoogleSignUp;
    private TextView tvGoToLogin;

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

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> handleRegistration());

        tvGoToLogin.setOnClickListener(v -> {
            finish();
        });

        btnGoogleSignUp.setOnClickListener(v -> {
                btnRegister.setEnabled(false);
                CredentialManager credentialManager = CredentialManager.create(this);

                GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setAutoSelectEnabled(true)
                        .build();

                GetCredentialRequest request = new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

                credentialManager.getCredentialAsync(
                        this,
                        request,
                        new CancellationSignal(),
                        ContextCompat.getMainExecutor(this),
                        new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                            @Override
                            public void onResult(GetCredentialResponse result) {
                                Credential credential = result.getCredential();
                                if (credential instanceof CustomCredential && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                                    try {
                                        GoogleIdTokenCredential googleId = GoogleIdTokenCredential.createFrom(credential.getData());
                                        String idToken = googleId.getIdToken();

                                        firebaseAuthWithGoogle(idToken);
                                    } catch (Exception e) {
                                        Toast.makeText(RegisterActivity.this, "Token error " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        btnRegister.setEnabled(true);
                                    }
                                }
                            }

                            @Override
                            public void onError(GetCredentialException e) {
                                btnRegister.setEnabled(true);

                                String exactError = e.getMessage();
                                String errorType = e.getType();

                                Toast.makeText(RegisterActivity.this, "Failed: " + exactError, Toast.LENGTH_LONG).show();
                                android.util.Log.e("GoogleAuth", "Type: " + errorType + " | Message: " + exactError);
                            }
                        }
                );
            }
        );
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

        if (password.isEmpty() || password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.matches(".*[A-Z].*")) {
            etPassword.setError("Must contain at least one uppercase letter");
            etPassword.requestFocus();
            return;
        }

        if (!password.matches(".*[a-z].*")) {
            etPassword.setError("Must contain at least one lowercase letter");
            etPassword.requestFocus();
            return;
        }

        if (!password.matches(".*\\d.*")) {
            etPassword.setError("Must contain at least one number");
            etPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        // --- Backend Call ---
        authRepository.registerWithEmail(name, email, password)
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_LONG).show();

//                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);

                    Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);

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
