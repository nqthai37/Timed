package com.timed.repositories;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.timed.Features.AI.AiPromptTemplate;

import java.util.ArrayList;
import java.util.List;

public class TemplateRepository {
    private final FirebaseFirestore db;

    public TemplateRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void getUserTemplates(String userId, RepositoryCallback<List<AiPromptTemplate>> callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("User ID is required");
            return;
        }

        db.collection("users").document(userId).collection("quick_templates")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AiPromptTemplate> templates = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AiPromptTemplate template = doc.toObject(AiPromptTemplate.class);
                        template.setId(doc.getId()); // Attach the Firestore document ID
                        templates.add(template);
                    }
                    callback.onSuccess(templates);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void addTemplate(String userId, AiPromptTemplate template, RepositoryCallback<String> callback) {
        db.collection("users").document(userId).collection("quick_templates")
                .add(template)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteTemplate(String userId, String templateId, RepositoryCallback<Void> callback) {
        db.collection("users").document(userId).collection("quick_templates")
                .document(templateId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
