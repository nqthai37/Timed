//package com.timed.examples;
//
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Toast;
//
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.lifecycle.ViewModelProvider;
//
//import com.google.android.gms.auth.api.signin.GoogleSignIn;
//import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
//import com.google.android.gms.auth.api.signin.GoogleSignInClient;
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
//import com.google.android.gms.common.Scopes;
//import com.google.android.gms.common.api.ApiException;
//import com.google.android.gms.common.api.Scope;
//import com.timed.models.AuthResponse;
//import com.timed.models.CalendarEventResponse;
//import com.timed.models.DeleteEventResponse;
//import com.timed.viewmodels.GoogleCalendarViewModel;
//
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.ArrayList;
//import java.util.Collections;
//
///**
// * Example Activity for Google Calendar + Google Meet integration
// */
//public class CalendarActivityExample extends AppCompatActivity {
//    private static final String TAG = "CalendarExample";
//    private static final int RC_SIGN_IN = 1000;
//    private static final String PREFS_NAME = "auth_prefs";
//    private static final String KEY_REFRESH_TOKEN = "user_refresh_token";
//    private static final String GOOGLE_CLIENT_ID =
//            "343178520285-8nj0h58mg4mtmrdm7kvj112ehcehqpam.apps.googleusercontent.com";
//
//    private GoogleCalendarViewModel calendarViewModel;
//    private GoogleSignInClient googleSignInClient;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        initializeCalendarUI();
//    }
//
//    private void initializeCalendarUI() {
//        calendarViewModel = new ViewModelProvider(this).get(GoogleCalendarViewModel.class);
//
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestScopes(new Scope(Scopes.CALENDAR_EVENTS), new Scope("https://www.googleapis.com/auth/calendar.events"))
//                .requestServerAuthCode(GOOGLE_CLIENT_ID)
//                .build();
//
//        googleSignInClient = GoogleSignIn.getClient(this, gso);
//
//        observeCalendarViewModel();
//        calendarViewModel.performHealthCheck();
//    }
//
//    private void observeCalendarViewModel() {
//        calendarViewModel.getEventResponse().observe(this, event -> {
//            if (event != null && event.isSuccess()) {
//                Log.d(TAG, "Event created successfully");
//                Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();
//                if (event.getHangoutLink() != null && !event.getHangoutLink().isEmpty()) {
//                    openMeetLink(event.getHangoutLink());
//                }
//            }
//        });
//
//        calendarViewModel.getDeleteResponse().observe(this, response -> {
//            if (response != null && response.isSuccess()) {
//                Log.d(TAG, "Event deleted: " + response.getMessage());
//                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        calendarViewModel.getAuthResponse().observe(this, authResponse -> {
//            if (authResponse != null && authResponse.isSuccess()) {
//                String refreshToken = authResponse.getRefreshToken();
//                if (refreshToken != null && !refreshToken.isEmpty()) {
//                    saveRefreshToken(refreshToken);
//                    Toast.makeText(this, "Google sign-in succeeded", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//
//        calendarViewModel.getErrorMessage().observe(this, error -> {
//            if (error != null && !error.isEmpty()) {
//                Log.e(TAG, "Error: " + error);
//                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
//            }
//        });
//    }
//
//    public void signInWithGoogle() {
//        Intent signInIntent = googleSignInClient.getSignInIntent();
//        startActivityForResult(signInIntent, RC_SIGN_IN);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == RC_SIGN_IN && data != null) {
//            try {
//                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
//                        .getResult(ApiException.class);
//                if (account != null) {
//                    String authCode = account.getServerAuthCode();
//                    if (authCode != null) {
//                        calendarViewModel.exchangeAuthCode(authCode);
//                    } else {
//                        Toast.makeText(this, "Server auth code not returned", Toast.LENGTH_LONG).show();
//                    }
//                }
//            } catch (ApiException e) {
//                Log.e(TAG, "Google sign-in failed", e);
//                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        }
//    }
//
//    private void createSampleEvent() {
//        String refreshToken = getStoredRefreshToken();
//        if (refreshToken == null || refreshToken.isEmpty()) {
//            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
//            signInWithGoogle();
//            return;
//        }
//
//        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
//        LocalDateTime endTime = startTime.plus(1, ChronoUnit.HOURS);
//        calendarViewModel.createCalendarEvent(
//                "Team Meeting",
//                startTime,
//                endTime,
//                refreshToken,
//                "Weekly sync",
//                Collections.emptyList()
//        );
//    }
//
//    private void openMeetLink(String meetLink) {
//        try {
//            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(meetLink));
//            startActivity(browserIntent);
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to open Meet link", e);
//            Toast.makeText(this, "Could not open Meet link", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void saveRefreshToken(String refreshToken) {
//        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//        prefs.edit()
//                .putString(KEY_REFRESH_TOKEN, refreshToken)
//                .apply();
//    }
//
//    private String getStoredRefreshToken() {
//        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//        return prefs.getString(KEY_REFRESH_TOKEN, null);
//    }
//}
//
