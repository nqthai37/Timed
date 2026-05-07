package com.timed.repositories;

import android.util.Log;

import com.timed.models.*;
import com.timed.services.CalendarApiService;
import com.timed.services.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

/**
 * Repository for Google Calendar API operations
 * Handles event creation, updates, and deletion with Google Meet integration
 */
public class GoogleCalendarRepository {
    private static final String TAG = "GoogleCalendarRepository";
    private final CalendarApiService apiService;

    public GoogleCalendarRepository() {
        this.apiService = RetrofitClient.getRetrofitInstance()
                .create(CalendarApiService.class);
    }

    /**
     * Create a calendar event with Google Meet
     */
    public Call<CalendarEventResponse> createCalendarEvent(
            String summary,
            String startTime,
            String endTime,
            String userRefreshToken) {
        return createCalendarEvent(summary, startTime, endTime, userRefreshToken, "", "", new ArrayList<>());
    }

    /**
     * Create a calendar event with Google Meet (full options)
     */
    public Call<CalendarEventResponse> createCalendarEvent(
            String summary,
            String startTime,
            String endTime,
            String userRefreshToken,
            String description,
            String location,
            List<String> attendees) {

        Log.d(TAG, "Creating calendar event: " + summary);

        CreateEventRequest request = new CreateEventRequest(
                summary,
                startTime,
                endTime,
                userRefreshToken,
                description,
                location,
                attendees
        );

        return apiService.createCalendarEvent(request);
    }

    /**
     * Update a calendar event
     */
    public Call<CalendarEventResponse> updateCalendarEvent(
            String eventId,
            String userRefreshToken,
            EventUpdates updates) {

        Log.d(TAG, "Updating calendar event: " + eventId);

        UpdateEventRequest request = new UpdateEventRequest(eventId, userRefreshToken, updates);
        return apiService.updateCalendarEvent(request);
    }

    /**
     * Exchange Google server auth code for OAuth tokens
     */
    public Call<AuthResponse> exchangeAuthCode(String authCode) {
        Log.d(TAG, "Exchanging auth code");
        AuthCodeRequest request = new AuthCodeRequest(authCode);
        return apiService.exchangeAuthCode(request);
    }

    /**
     * Refresh an expired access token using the Google refresh token
     */
    public Call<AuthResponse> refreshAccessToken(String refreshToken) {
        Log.d(TAG, "Refreshing access token");
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        return apiService.refreshAccessToken(request);
    }

    /**
     * Verify refresh token validity
     */
    public Call<AuthResponse> verifyRefreshToken(String refreshToken) {
        Log.d(TAG, "Verifying refresh token");
        VerifyTokenRequest request = new VerifyTokenRequest(refreshToken);
        return apiService.verifyRefreshToken(request);
    }

    /**
     * Delete a calendar event
     */
    public Call<DeleteEventResponse> deleteCalendarEvent(
            String eventId,
            String userRefreshToken) {

        Log.d(TAG, "Deleting calendar event: " + eventId);

        DeleteEventRequest request = new DeleteEventRequest(eventId, userRefreshToken);
        return apiService.deleteCalendarEvent(request);
    }

    /**
     * Health check
     */
    public Call<HealthCheckResponse> healthCheck() {
        Log.d(TAG, "Performing health check");
        return apiService.healthCheck();
    }
}
