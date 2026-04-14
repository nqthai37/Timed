package com.timed.repositories;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.timed.Setting.FocusMode.FocusPreset;
import com.timed.managers.UserManager;

import java.util.ArrayList;
import java.util.List;

public class FocusPresetRepository {
    private FirebaseFirestore db;

    public FocusPresetRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnPresetsLoadedListener {
        void onSuccess(List<FocusPreset> presets);
        void onFailure(Exception e);
    }

    public interface OnPresetAddedListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnPresetDeletedListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void addPreset(FocusPreset preset, OnPresetAddedListener listener) {
        String userId = UserManager.getInstance().getCurrentUser().getUid();

        if (userId == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        db.collection("users").document(userId).collection("focus_presets")
                .add(preset)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void deletePreset(String presetId, OnPresetDeletedListener listener) {
        String userId = UserManager.getInstance().getCurrentUser().getUid();

        if (userId == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        db.collection("users").document(userId).collection("focus_presets")
                .document(presetId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void fetchUserPresets(OnPresetsLoadedListener listener) {
        String userId = UserManager.getInstance().getCurrentUser().getUid();

        if (userId == null) {
            listener.onFailure(new Exception("User not logged in"));
            return;
        }

        db.collection("users").document(userId).collection("focus_presets")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FocusPreset> loadedPresets = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        FocusPreset preset = document.toObject(FocusPreset.class);
                        if (preset != null) {
                            preset.setId(document.getId());
                            loadedPresets.add(preset);
                        }
                    }

                    listener.onSuccess(loadedPresets);
                })
                .addOnFailureListener(listener::onFailure);
    }
}
