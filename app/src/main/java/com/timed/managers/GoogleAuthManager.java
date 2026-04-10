package com.timed.managers;

import android.app.Activity;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.timed.R;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public class GoogleAuthManager {
    private final Activity activity;
    private final CredentialManager credentialManager;
    private final AuthCallback callback;

    public interface AuthCallback {
        void onTokenReceived(String idToken);
        void onError(String errorMessage);
    }

    public GoogleAuthManager(Activity activity, AuthCallback callback) {
        this.activity = activity;
        this.callback = callback;
        credentialManager = CredentialManager.create(activity);
    }

    public void startGoogleSignIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activity.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                            try {
                                GoogleIdTokenCredential googleId = GoogleIdTokenCredential.createFrom(credential.getData());
                                String idToken = googleId.getIdToken();

                                callback.onTokenReceived(idToken);
                            } catch (Exception e) {
                                callback.onError("Error parsing token.");
                            }
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        String errorType = e.getClass().getSimpleName();
                        Log.e("GoogleAuth", "Failed: " + errorType + " - " + e.getMessage());

                        if (!errorType.contains("Cancellation")) {
                            callback.onError(e.getMessage());
                        } else {
                            callback.onError("cancelled");
                        }
                    }
                }
        );
    }
}
