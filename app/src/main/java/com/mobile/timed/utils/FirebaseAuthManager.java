package com.mobile.timed.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Firebase Authentication Manager
 * Quản lý các tác vụ liên quan đến xác thực người dùng
 */
public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private final FirebaseAuth mAuth;

    /**
     * Interface callback cho kết quả authentication
     */
    public interface AuthCallback {
        void onSuccess(String userId);
        void onFailure(String errorMessage);
    }

    public FirebaseAuthManager() {
        this.mAuth = FirebaseInitializer.getInstance().getAuth();
    }

    /**
     * Đăng ký người dùng mới
     *
     * @param email Email của người dùng
     * @param password Mật khẩu (tối thiểu 6 ký tự)
     * @param callback Callback khi hoàn thành
     */
    public void registerUser(String email, String password, AuthCallback callback) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            callback.onFailure("Email và password không được để trống");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Log.d(TAG, "User registered successfully: " + userId);
                        callback.onSuccess(userId);
                    } else {
                        String errorMessage = "Đăng ký thất bại";
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "Email đã được sử dụng";
                        } else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                            errorMessage = "Mật khẩu quá yếu (tối thiểu 6 ký tự)";
                        } else if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Log.e(TAG, "Registration failed: " + errorMessage);
                        callback.onFailure(errorMessage);
                    }
                });
    }

    /**
     * Đăng nhập người dùng
     *
     * @param email Email của người dùng
     * @param password Mật khẩu
     * @param callback Callback khi hoàn thành
     */
    public void loginUser(String email, String password, AuthCallback callback) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            callback.onFailure("Email và password không được để trống");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Log.d(TAG, "User logged in successfully: " + userId);
                        callback.onSuccess(userId);
                    } else {
                        String errorMessage = "Đăng nhập thất bại";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Log.e(TAG, "Login failed: " + errorMessage);
                        callback.onFailure(errorMessage);
                    }
                });
    }

    /**
     * Đăng xuất người dùng
     */
    public void logoutUser() {
        mAuth.signOut();
        Log.d(TAG, "User logged out");
    }

    /**
     * Kiểm tra xem người dùng đã đăng nhập hay không
     */
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Lấy UID của người dùng hiện tại
     */
    public String getCurrentUserId() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }
        return null;
    }

    /**
     * Lấy email của người dùng hiện tại
     */
    public String getCurrentUserEmail() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getEmail();
        }
        return null;
    }

    /**
     * Gửi email đặt lại mật khẩu
     *
     * @param email Email của người dùng
     * @param callback Callback khi hoàn thành
     */
    public void sendPasswordResetEmail(String email, AuthCallback callback) {
        if (email == null || email.isEmpty()) {
            callback.onFailure("Email không được để trống");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent to: " + email);
                        callback.onSuccess(email);
                    } else {
                        String errorMessage = "Gửi email thất bại";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Log.e(TAG, "Send password reset email failed: " + errorMessage);
                        callback.onFailure(errorMessage);
                    }
                });
    }

    /**
     * Cập nhật mật khẩu người dùng hiện tại
     *
     * @param newPassword Mật khẩu mới
     * @param callback Callback khi hoàn thành
     */
    public void updatePassword(String newPassword, AuthCallback callback) {
        if (newPassword == null || newPassword.isEmpty()) {
            callback.onFailure("Mật khẩu không được để trống");
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        mAuth.getCurrentUser().updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password updated successfully");
                        callback.onSuccess("Cập nhật mật khẩu thành công");
                    } else {
                        String errorMessage = "Cập nhật mật khẩu thất bại";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Log.e(TAG, "Update password failed: " + errorMessage);
                        callback.onFailure(errorMessage);
                    }
                });
    }
}

