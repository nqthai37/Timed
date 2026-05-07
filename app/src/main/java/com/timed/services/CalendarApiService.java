package com.timed.services;

import com.timed.models.*;
import retrofit2.Call;
import retrofit2.http.*;

/**
 * Retrofit service interface for Google Calendar API
 */
public interface CalendarApiService {

    @POST("api/calendar/create-event")
    Call<CalendarEventResponse> createCalendarEvent(@Body CreateEventRequest request);

    @PUT("api/calendar/update-event")
    Call<CalendarEventResponse> updateCalendarEvent(@Body UpdateEventRequest request);

    @DELETE("api/calendar/delete-event")
    Call<DeleteEventResponse> deleteCalendarEvent(@Body DeleteEventRequest request);

    @GET("api/health")
    Call<HealthCheckResponse> healthCheck();

    @POST("auth/exchange-code")
    Call<AuthResponse> exchangeAuthCode(@Body AuthCodeRequest request);

    @POST("auth/refresh-token")
    Call<AuthResponse> refreshAccessToken(@Body RefreshTokenRequest request);

    @POST("auth/verify-token")
    Call<AuthResponse> verifyRefreshToken(@Body VerifyTokenRequest request);
}
