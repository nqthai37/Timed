package com.timed.utils;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Firebase Helper - Hỗ trợ các tác vụ phổ biến với Firestore
 */
public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private final FirebaseFirestore db;

    /**
     * Interface callback cho các tác vụ
     */
    public interface FirebaseCallback<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }

    public FirebaseHelper() {
        // Updated to use standard Firestore initialization if FirebaseInitializer is missing
        this.db = FirebaseFirestore.getInstance();
    }

    private void logDetailedError(String context, Exception e) {
        try {
            Log.e(TAG, context + " -> " + e.toString());
            // Log stacktrace manually as BuildConfig may be unavailable or in a different package
            Log.e(TAG, context + " -> stacktrace: " + Log.getStackTraceString(e));
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
                Log.e(TAG, context + " -> firestore code: " + ffe.getCode());
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to log detailed error", ex);
        }
    }

    /**
     * Kiểm tra kết nối Firebase
     */
    public void checkConnection(FirebaseCallback<Boolean> callback) {
        db.collection("_connectionTest")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> callback.onSuccess(true))
                        .addOnFailureListener(e -> {
                            logDetailedError("checkConnection failed", e);
                            callback.onFailure("Không thể kết nối đến Firebase: " + e.toString());
                        });
    }

    /**
     * Kiểm tra xem user có quyền truy cập collection hay không
     */
    public void checkUserPermission(String collection, FirebaseCallback<Boolean> callback) {
        db.collection(collection)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> callback.onSuccess(true))
                .addOnFailureListener(e -> {
                    logDetailedError("checkUserPermission failed for: " + collection, e);
                    callback.onFailure("Không có quyền truy cập: " + collection + ": " + e.toString());
                });
    }

    /**
     * Lấy giá trị từ Firestore Document
     */
    public void getDocument(String collection, String documentId, FirebaseCallback<DocumentSnapshot> callback) {
        db.collection(collection)
                .document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess(documentSnapshot);
                    } else {
                        callback.onFailure("Tài liệu không tồn tại");
                    }
                })
                .addOnFailureListener(e -> {
                    logDetailedError("getDocument failed for: " + collection + "/" + documentId, e);
                    callback.onFailure("Lỗi khi lấy dữ liệu: " + e.toString());
                });
    }

    /**
     * Lấy tất cả document từ collection
     */
    public void getCollection(String collection, FirebaseCallback<QuerySnapshot> callback) {
        db.collection(collection)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> {
                    logDetailedError("getCollection failed for: " + collection, e);
                    callback.onFailure("Lỗi khi lấy dữ liệu: " + e.toString());
                });
    }

    /**
     * Lưu hoặc cập nhật document
     */
    public void setDocument(String collection, String documentId, Object data, FirebaseCallback<Void> callback) {
        db.collection(collection)
                .document(documentId)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> {
                    logDetailedError("setDocument failed for: " + collection + "/" + documentId, e);
                    callback.onFailure("Lỗi khi lưu dữ liệu: " + e.toString());
                });
    }

    /**
     * Thêm document mới (Firebase sẽ tạo ID)
     */
    public void addDocument(String collection, Object data, FirebaseCallback<String> callback) {
        db.collection(collection)
                .add(data)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(e -> {
                    logDetailedError("addDocument failed for: " + collection, e);
                    callback.onFailure("Lỗi khi thêm dữ liệu: " + e.toString());
                });
    }

    /**
     * Xóa document
     */
    public void deleteDocument(String collection, String documentId, FirebaseCallback<Void> callback) {
        db.collection(collection)
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> {
                    logDetailedError("deleteDocument failed for: " + collection + "/" + documentId, e);
                    callback.onFailure("Lỗi khi xóa dữ liệu: " + e.toString());
                });
    }

    /**
     * Lấy Firestore instance (nếu cần)
     */
    public FirebaseFirestore getDb() {
        return db;
    }
}
