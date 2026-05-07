# Android Integration Guide - Google Calendar with Meet

Guide for integrating the Timed Android app with the Google Calendar backend service.

## Overview

This guide explains how to integrate the Google Calendar event creation feature with automatic Google Meet link generation into the Timed Android application.

## Architecture

```
Android App (Mobile)
       ↓
   Retrofit/OkHttp
       ↓
Backend Server (Node.js)
       ↓
Google Calendar API
       ↓
Google Meet Conference
```

## Setup Steps

### 1. Add Dependencies to Android Project

In `app/build.gradle.kts`:

```kotlin
dependencies {
    // Retrofit and OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Google Sign-In (for obtaining refresh token)
    implementation("com.google.android.gms:play-services-auth:21.0.0")
}
```

### 2. Create Data Models

Create a new file `app/src/main/java/com/timed/models/CalendarModels.kt`:

```kotlin
package com.timed.models

import com.google.gson.annotations.SerializedName

// Request model for creating calendar event
data class CreateEventRequest(
    val summary: String,
    val startTime: String,      // ISO 8601 format
    val endTime: String,        // ISO 8601 format
    val userRefreshToken: String,
    val description: String = "",
    val location: String = "",
    val attendees: List<String> = emptyList()
)

// Response model for event creation
data class CalendarEventResponse(
    val success: Boolean,
    val eventId: String,
    val htmlLink: String,
    val hangoutLink: String?,
    val summary: String,
    val startTime: String,
    val endTime: String,
    val description: String = "",
    val conferenceData: ConferenceData? = null
)

data class ConferenceData(
    val conferenceId: String?,
    val entryPoints: List<EntryPoint> = emptyList()
)

data class EntryPoint(
    val entryPointType: String,
    val uri: String
)

// Request model for updating event
data class UpdateEventRequest(
    val eventId: String,
    val userRefreshToken: String,
    val updates: EventUpdates
)

data class EventUpdates(
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null
)

// Request model for deleting event
data class DeleteEventRequest(
    val eventId: String,
    val userRefreshToken: String
)

// Response for delete operation
data class DeleteEventResponse(
    val success: Boolean,
    val message: String,
    val eventId: String
)

// Error response
data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val message: String? = null
)
```

### 3. Create Retrofit Service

Create `app/src/main/java/com/timed/api/CalendarApiService.kt`:

```kotlin
package com.timed.api

import com.timed.models.*
import retrofit2.Call
import retrofit2.http.*

interface CalendarApiService {
    
    @POST("api/calendar/create-event")
    fun createCalendarEvent(@Body request: CreateEventRequest): Call<CalendarEventResponse>
    
    @PUT("api/calendar/update-event")
    fun updateCalendarEvent(@Body request: UpdateEventRequest): Call<CalendarEventResponse>
    
    @DELETE("api/calendar/delete-event")
    fun deleteCalendarEvent(@Body request: DeleteEventRequest): Call<DeleteEventResponse>
    
    @GET("api/health")
    fun healthCheck(): Call<HealthCheckResponse>
}

data class HealthCheckResponse(
    val status: String,
    val timestamp: String,
    val message: String
)
```

### 4. Create Retrofit Client

Create `app/src/main/java/com/timed/api/RetrofitClient.kt`:

```kotlin
package com.timed.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://your-backend-url:3000/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(httpClient)
        .build()
    
    val apiService: CalendarApiService = retrofit.create(CalendarApiService::class.java)
}
```

### 5. Create Calendar Repository

Create `app/src/main/java/com/timed/repositories/CalendarRepository.kt`:

```kotlin
package com.timed.repositories

import com.timed.api.RetrofitClient
import com.timed.models.*
import retrofit2.Call

class CalendarRepository {
    private val apiService = RetrofitClient.apiService
    
    fun createCalendarEvent(
        summary: String,
        startTime: String,
        endTime: String,
        userRefreshToken: String,
        description: String = "",
        location: String = "",
        attendees: List<String> = emptyList()
    ): Call<CalendarEventResponse> {
        val request = CreateEventRequest(
            summary = summary,
            startTime = startTime,
            endTime = endTime,
            userRefreshToken = userRefreshToken,
            description = description,
            location = location,
            attendees = attendees
        )
        return apiService.createCalendarEvent(request)
    }
    
    fun updateCalendarEvent(
        eventId: String,
        userRefreshToken: String,
        updates: EventUpdates
    ): Call<CalendarEventResponse> {
        val request = UpdateEventRequest(
            eventId = eventId,
            userRefreshToken = userRefreshToken,
            updates = updates
        )
        return apiService.updateCalendarEvent(request)
    }
    
    fun deleteCalendarEvent(
        eventId: String,
        userRefreshToken: String
    ): Call<DeleteEventResponse> {
        val request = DeleteEventRequest(
            eventId = eventId,
            userRefreshToken = userRefreshToken
        )
        return apiService.deleteCalendarEvent(request)
    }
    
    fun healthCheck(): Call<HealthCheckResponse> {
        return apiService.healthCheck()
    }
}
```

### 6. Create ViewModel

Create `app/src/main/java/com/timed/viewmodels/CalendarViewModel.kt`:

```kotlin
package com.timed.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.timed.models.*
import com.timed.repositories.CalendarRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CalendarViewModel : ViewModel() {
    private val repository = CalendarRepository()
    
    val eventResponse = MutableLiveData<CalendarEventResponse>()
    val deleteResponse = MutableLiveData<DeleteEventResponse>()
    val errorMessage = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>(false)
    
    fun createCalendarEvent(
        summary: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        userRefreshToken: String,
        description: String = "",
        attendees: List<String> = emptyList()
    ) {
        isLoading.value = true
        
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val startTimeStr = startTime.format(formatter) + "Z"
        val endTimeStr = endTime.format(formatter) + "Z"
        
        repository.createCalendarEvent(
            summary = summary,
            startTime = startTimeStr,
            endTime = endTimeStr,
            userRefreshToken = userRefreshToken,
            description = description,
            attendees = attendees
        ).enqueue(object : Callback<CalendarEventResponse> {
            override fun onResponse(
                call: Call<CalendarEventResponse>,
                response: Response<CalendarEventResponse>
            ) {
                isLoading.value = false
                if (response.isSuccessful) {
                    eventResponse.value = response.body()
                } else {
                    errorMessage.value = "Failed to create event: ${response.code()}"
                }
            }
            
            override fun onFailure(call: Call<CalendarEventResponse>, t: Throwable) {
                isLoading.value = false
                errorMessage.value = "Error: ${t.message}"
            }
        })
    }
    
    fun deleteCalendarEvent(
        eventId: String,
        userRefreshToken: String
    ) {
        isLoading.value = true
        
        repository.deleteCalendarEvent(eventId, userRefreshToken)
            .enqueue(object : Callback<DeleteEventResponse> {
                override fun onResponse(
                    call: Call<DeleteEventResponse>,
                    response: Response<DeleteEventResponse>
                ) {
                    isLoading.value = false
                    if (response.isSuccessful) {
                        deleteResponse.value = response.body()
                    } else {
                        errorMessage.value = "Failed to delete event: ${response.code()}"
                    }
                }
                
                override fun onFailure(call: Call<DeleteEventResponse>, t: Throwable) {
                    isLoading.value = false
                    errorMessage.value = "Error: ${t.message}"
                }
            })
    }
}
```

### 7. Update Activity

Modify `app/src/main/java/com/timed/activities/TasksListActivity.java`:

```java
// Add to imports
import androidx.lifecycle.ViewModelProvider;
import com.timed.viewmodels.CalendarViewModel;
import com.timed.models.CalendarEventResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TasksListActivity extends AppCompatActivity {
    // ... existing code ...
    
    private CalendarViewModel calendarViewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_list);
        
        calendarViewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
        
        // Observe live data
        observeCalendarViewModel();
    }
    
    private void observeCalendarViewModel() {
        calendarViewModel.getEventResponse().observe(this, event -> {
            if (event != null && event.isSuccess()) {
                // Event created successfully
                String meetLink = event.getHangoutLink();
                String eventLink = event.getHtmlLink();
                
                Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();
                openMeetLink(meetLink);
            }
        });
        
        calendarViewModel.getErrorMessage().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
        
        calendarViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                // Show loading indicator
            } else {
                // Hide loading indicator
            }
        });
    }
    
    public void createTimedEventWithMeet(
        String summary,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String userRefreshToken
    ) {
        calendarViewModel.createCalendarEvent(
            summary,
            startTime,
            endTime,
            userRefreshToken,
            "Timed meeting created from mobile app",
            new ArrayList<>()
        );
    }
    
    private void openMeetLink(String meetLink) {
        if (meetLink != null && !meetLink.isEmpty()) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(meetLink));
            startActivity(browserIntent);
        }
    }
}
```

## Getting User Refresh Token

To get the user's Google refresh token:

```kotlin
private fun signInWithGoogle() {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestScopes(Scope(Scopes.CALENDAR))
        .requestServerAuthCode("your_backend_client_id")
        .build()
    
    val googleSignInClient = GoogleSignIn.getClient(this, gso)
    val signInIntent = googleSignInClient.getSignInIntent()
    startActivityForResult(signInIntent, RC_SIGN_IN)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    
    if (requestCode == RC_SIGN_IN) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val authCode = account?.serverAuthCode // Use this to get refresh token
            sendAuthCodeToBackend(authCode)
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Sign in failed", e)
        }
    }
}
```

## Testing

Test the integration:

```kotlin
@Test
fun testCreateCalendarEvent() {
    val startTime = LocalDateTime.now().plusHours(1)
    val endTime = startTime.plus(1, ChronoUnit.HOURS)
    
    calendarViewModel.createCalendarEvent(
        summary = "Test Event",
        startTime = startTime,
        endTime = endTime,
        userRefreshToken = "test_token",
        description = "Testing calendar integration"
    )
    
    // Verify response
    val eventResponse = calendarViewModel.getEventResponse().value
    assertNotNull(eventResponse)
    assertNotNull(eventResponse.getHangoutLink())
}
```

## Common Issues

1. **Backend URL Configuration**
   - Update `BASE_URL` in `RetrofitClient.kt` to match your backend server
   - Use `http://10.0.2.2:3000/` for Android emulator

2. **OAuth Issues**
   - Ensure refresh token is valid
   - Verify backend has correct OAuth credentials

3. **Network Issues**
   - Check firewall and network configuration
   - Enable network debugging in Android Studio

## Support

For more information, see the [Backend README](../backend/README.md).
