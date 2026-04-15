package com.timed.Setting.Profile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.timed.R;
import com.timed.repositories.AuthRepository;

public class ChangePasswordActivity extends AppCompatActivity {
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnUpdatePassword;
    private ImageView ivBack;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        authRepository = new AuthRepository();

        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        ivBack = findViewById(R.id.iv_back);

        setupClickListeners();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnUpdatePassword.setOnClickListener(v -> validateAndUpdatePassword());
    }

    private void validateAndUpdatePassword() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        tilCurrentPassword.setErrorEnabled(false);
        tilNewPassword.setErrorEnabled(false);
        tilConfirmPassword.setErrorEnabled(false);

        if (currentPass.isEmpty()) {
            tilCurrentPassword.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return;
        }

        boolean hasLength = newPass.length() >= 8;
        boolean hasUpper = newPass.matches(".*[A-Z].*");
        boolean hasLower = newPass.matches(".*[a-z].*");
        boolean hasNumber = newPass.matches(".*\\d.*");

        if (!hasLength || !hasUpper || !hasLower || !hasNumber) {
            tilNewPassword.setError("Must contain 8+ chars, 1 uppercase, 1 lowercase, and 1 number");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            tilConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (currentPass.equals(newPass)) {
            tilNewPassword.setError("New password must be different from current password");
            etNewPassword.requestFocus();
            return;
        }

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("Updating...");

        // 3. Call Firebase
        authRepository.updatePassword(currentPass, newPass).addOnCompleteListener(task -> {
            btnUpdatePassword.setEnabled(true);
            btnUpdatePassword.setText("Update Password");

            if (task.isSuccessful()) {
                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Exception e = task.getException();
                String errorMsg = e != null ? e.getMessage().toLowerCase() : "";
                if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ||
                        errorMsg.contains("incorrect") || errorMsg.contains("credential") || errorMsg.contains("mismatch")) {

                    tilCurrentPassword.setError("Incorrect current password");
                    etCurrentPassword.requestFocus();
                    etCurrentPassword.setText("");

                } else {
                    Toast.makeText(this, "Update failed. Please try again later.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
