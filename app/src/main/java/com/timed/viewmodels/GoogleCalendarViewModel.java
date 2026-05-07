package com.timed.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.timed.models.*;
import com.timed.repositories.GoogleCalendarRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for Google Calendar operations
 * Manages UI state and calendar event operations
 */
public class GoogleCalendarViewModel extends ViewModel {
    private static final String TAG = "GoogleCalendarViewModel";

    private final GoogleCalendarRepository repository;

    private final MutableLiveData<CalendarEventResponse> eventResponse = new MutableLiveData<>();
    private final MutableLiveData<DeleteEventResponse> deleteResponse = new MutableLiveData<>();
    private final MutableLiveData<AuthResponse> authResponse = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public GoogleCalendarViewModel() {
        repository = new GoogleCalendarRepository();
    }

    /**
     * Create a calendar event with Google Meet
     */
    public void createCalendarEvent(
            String summary,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String userRefreshToken,
            String description,
            List<String> attendees) {

        isLoading.setValue(true);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        String startTimeStr = startTime.format(formatter) + "Z";
        String endTimeStr = endTime.format(formatter) + "Z";

        Log.d(TAG, "Creating calendar event: " + summary);
        Log.d(TAG, "Start: " + startTimeStr);
        Log.d(TAG, "End: " + endTimeStr);

        repository.createCalendarEvent(
                summary,
                startTimeStr,
                endTimeStr,
                userRefreshToken,
                description != null ? description : "",
                "",
                attendees != null ? attendees : new java.util.ArrayList<>()
        ).enqueue(new Callback<CalendarEventResponse>() {
            @Override
            public void onResponse(Call<CalendarEventResponse> call, Response<CalendarEventResponse> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Event created successfully");
                    eventResponse.setValue(response.body());
                } else {
                    String error = "Failed to create event: " + response.code();
                    Log.e(TAG, error);
                    errorMessage.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<CalendarEventResponse> call, Throwable t) {
                isLoading.setValue(false);
                String error = "Error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error");
                Log.e(TAG, error, t);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * Exchange Google auth code for refresh token
     */
    public void exchangeAuthCode(String authCode) {
        isLoading.setValue(true);

        repository.exchangeAuthCode(authCode)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse authResult = response.body();
                            if (authResult.isSuccess()) {
                                Log.d(TAG, "Auth code exchanged successfully");
                                authResponse.setValue(authResult);
                                errorMessage.setValue(null);
                            } else {
                                String error = authResult.getError() != null ? authResult.getError() : "Exchange failed";
                                Log.e(TAG, error);
                                errorMessage.setValue(error);
                            }
                        } else {
                            String error = "Failed to exchange auth code: " + response.code();
                            Log.e(TAG, error);
                            errorMessage.setValue(error);
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        String error = "Error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error");
                        Log.e(TAG, error, t);
                        errorMessage.setValue(error);
                    }
                });
    }

    /**
     * Refresh access token using refresh token
     */
    public void refreshAccessToken(String refreshToken) {
        isLoading.setValue(true);

        repository.refreshAccessToken(refreshToken)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse authResponse = response.body();
                            if (authResponse.isSuccess()) {
                                Log.d(TAG, "Access token refreshed successfully");
                                errorMessage.setValue(null);
                            } else {
                                String error = authResponse.getError() != null ? authResponse.getError() : "Refresh failed";
                                Log.e(TAG, error);
                                errorMessage.setValue(error);
                            }
                        } else {
                            String error = "Failed to refresh access token: " + response.code();
                            Log.e(TAG, error);
                            errorMessage.setValue(error);
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        String error = "Error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error");
                        Log.e(TAG, error, t);
                        errorMessage.setValue(error);
                    }
                });
    }

    /**
     * Verify if refresh token is valid
     */
    public void verifyRefreshToken(String refreshToken) {
        isLoading.setValue(true);

        repository.verifyRefreshToken(refreshToken)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse authResponse = response.body();
                            if (authResponse.isSuccess()) {
                                Log.d(TAG, "Refresh token is valid");
                                errorMessage.setValue(null);
                            } else {
                                String error = authResponse.getError() != null ? authResponse.getError() : "Verification failed";
                                Log.e(TAG, error);
                                errorMessage.setValue(error);
                            }
                        } else {
                            String error = "Failed to verify refresh token: " + response.code();
                            Log.e(TAG, error);
                            errorMessage.setValue(error);
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        String error = "Error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error");
                        Log.e(TAG, error, t);
                        errorMessage.setValue(error);
                    }
                });
    }

    /**
     * Update a calendar event
     */
    public void updateCalendarEvent(
            String eventId,
            String userRefreshToken,
            String summary,
            String description,
            String location) {

        isLoading.setValue(true);

        EventUpdates updates = new EventUpdates(summary, description, location);

        Log.d(TAG, "Updating calendar event: " + eventId);

        repository.updateCalendarEvent(eventId, userRefreshToken, updates)
                .enqueue(new Callback<CalendarEventResponse>() {
                    @Override
                    public void onResponse(Call<CalendarEventResponse> call, Response<CalendarEventResponse> response) {
                        isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Event updated successfully");
                            eventResponse.setValue(response.body());
                        } else {
                            String error = "Failed to update event: " + response.code();
                            Log.e(TAG, error);
                            errorMessage.setValue(error);
                        }
                    }

                    @Override
                    public void onFailure(Call<CalendarEventResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        String error = "Error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error");
                        Log.e(TAG, error, t);
                        errorMessage.setValue(error);
                    }
                });
    }

    /**
     * Delete a calendar event
     */
    public void deleteCalendarEvent(
            String eventId,
            String userRefreshToken) {

        isLoading.setValue(true);

        Log.d(TAG, "Deleting calendar event: " + eventId);

        repository.deleteCalendarEvent(eventId, userRefreshToken)
                .enqueue(new Callback<DeleteEventResponse>() {
                    @Override
                    public void onResponse(Call<DeleteEventResponse> call, Response<DeleteEventResponse> response) {
                        isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Event deleted successfully");
                            deleteResponse.setValue(response.body());
                        } else {
                            String error = "Failed to delete event: " + response.code();
                            Log.e(TAG, error);
                            errorMessage.setValue(error);
                        }
                    }

                    @Override
                    public void onFailure(Call<DeleteEventResponse> call, Throwable t) {
                        isLoading.setValue(false);
                        String error = "Error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error");
                        Log.e(TAG, error, t);
                        errorMessage.setValue(error);
                    }
                });
    }

    /**
     * Health check
     */
    public void performHealthCheck() {
        Log.d(TAG, "Performing health check");

        repository.healthCheck()
                .enqueue(new Callback<HealthCheckResponse>() {
                    @Override
                    public void onResponse(Call<HealthCheckResponse> call, Response<HealthCheckResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Health check passed: " + response.body().getMessage());
                        } else {
                            Log.e(TAG, "Health check failed: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<HealthCheckResponse> call, Throwable t) {
                        Log.e(TAG, "Health check error: " + t.getMessage(), t);
                    }
                });
    }

    // LiveData getters
    public LiveData<CalendarEventResponse> getEventResponse() {
        return eventResponse;
    }

    public LiveData<DeleteEventResponse> getDeleteResponse() {
        return deleteResponse;
    }

    public LiveData<AuthResponse> getAuthResponse() {
        return authResponse;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
}
