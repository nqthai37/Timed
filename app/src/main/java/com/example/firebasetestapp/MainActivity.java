package com.example.firebasetestapp;

import android.os.Bundle;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> testData = new HashMap<>();
        testData.put("test_message", "Connection successful!");
        testData.put("app_name", "TimeD");

        db.collection("connection_tests")
                .add(testData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FIREBASE_TEST", "SUCCESS! Document ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_TEST", "FAILED to add document", e);
                });
    }
}