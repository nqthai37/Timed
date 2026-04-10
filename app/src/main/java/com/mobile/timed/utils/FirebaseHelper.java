package com.mobile.timed.utils;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
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
        this.db = FirebaseInitializer.getInstance().getFirestore();
    }

    /**
     * Kiểm tra kết nối Firebase
     */
    public void checkConnection(FirebaseCallback<Boolean> callback) {
        db.collection("_connectionTest")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Firebase connection successful");
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase connection failed: " + e.getMessage(), e);
                    callback.onFailure("Không thể kết nối đến Firebase: " + e.getMessage());
                });
    }

    /**
     * Kiểm tra xem user có quyền truy cập collection hay không
     */
    public void checkUserPermission(String collection, FirebaseCallback<Boolean> callback) {
        db.collection(collection)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "User has permission to access: " + collection);
                    callback.onSuccess(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Permission denied for collection: " + collection);
                    callback.onFailure("Không có quyền truy cập: " + collection);
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
                        Log.d(TAG, "Document found: " + documentId);
                        callback.onSuccess(documentSnapshot);
                    } else {
                        Log.w(TAG, "Document not found: " + documentId);
                        callback.onFailure("Tài liệu không tồn tại");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting document: " + e.getMessage(), e);
                    callback.onFailure("Lỗi khi lấy dữ liệu: " + e.getMessage());
                });
    }

    /**
     * Lấy tất cả document từ collection
     */
    public void getCollection(String collection, FirebaseCallback<QuerySnapshot> callback) {
        db.collection(collection)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Collection retrieved: " + collection + " (" + queryDocumentSnapshots.size() + " documents)");
                    callback.onSuccess(queryDocumentSnapshots);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting collection: " + e.getMessage(), e);
                    callback.onFailure("Lỗi khi lấy dữ liệu: " + e.getMessage());
                });
    }

    /**
     * Lưu hoặc cập nhật document
     */
    public void setDocument(String collection, String documentId, Object data, FirebaseCallback<Void> callback) {
        db.collection(collection)
                .document(documentId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Document saved/updated: " + documentId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving document: " + e.getMessage(), e);
                    callback.onFailure("Lỗi khi lưu dữ liệu: " + e.getMessage());
                });
    }

    /**
     * Thêm document mới (Firebase sẽ tạo ID)
     */
    public void addDocument(String collection, Object data, FirebaseCallback<String> callback) {
        db.collection(collection)
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    Log.d(TAG, "Document added with ID: " + docId);
                    callback.onSuccess(docId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding document: " + e.getMessage(), e);
                    callback.onFailure("Lỗi khi thêm dữ liệu: " + e.getMessage());
                });
    }

    /**
     * Xóa document
     */
    public void deleteDocument(String collection, String documentId, FirebaseCallback<Void> callback) {
        db.collection(collection)
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Document deleted: " + documentId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting document: " + e.getMessage(), e);
                    callback.onFailure("Lỗi khi xóa dữ liệu: " + e.getMessage());
                });
    }

    /**
     * Lấy Firestore instance (nếu cần)
     */
    public FirebaseFirestore getDb() {
        return db;
    }
}

