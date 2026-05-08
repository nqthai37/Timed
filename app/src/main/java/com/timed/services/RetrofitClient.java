package com.timed.services;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.timed.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client service for API communication
 * Provides singleton pattern for consistent API access
 */
public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    private static Retrofit retrofit = null;
    private static final Object lock = new Object();

    /**
     * Get Retrofit instance with configured base URL and converters
     */
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            synchronized (lock) {
                if (retrofit == null) {
                    String baseUrl = BuildConfig.BACKEND_URL;
                    Log.d(TAG, "Initializing Retrofit with base URL: " + baseUrl);

                    // Configure HTTP logging for debugging
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(
                        BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.BASIC
                    );

                    // Configure OkHttp client
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(loggingInterceptor)
                            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                            .build();

                    // Configure Gson for JSON serialization
                    Gson gson = new GsonBuilder()
                            .setLenient()
                            .setPrettyPrinting()
                            .create();

                    // Build Retrofit instance
                    retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
                }
            }
        }
        return retrofit;
    }

    /**
     * Get MeetingRoomAPI service instance
     */
    public static MeetingRoomAPI getMeetingRoomAPI() {
        return getRetrofitInstance().create(MeetingRoomAPI.class);
    }

    /**
     * Get CalendarApiService service instance
     */
    public static CalendarApiService getCalendarApiService() {
        return getRetrofitInstance().create(CalendarApiService.class);
    }

    /**
     * Reset retrofit instance (useful for testing or changing base URL)
     */
    public static void resetRetrofit() {
        synchronized (lock) {
            retrofit = null;
        }
    }
}
