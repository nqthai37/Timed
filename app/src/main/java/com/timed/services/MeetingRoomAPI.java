package com.timed.services;

import com.timed.models.CreateMeetingRoomRequest;
import com.timed.models.CreateMeetingRoomResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit API interface for meeting room management
 */
public interface MeetingRoomAPI {
    /**
     * Create a meeting room for an event
     * POST /api/meeting-rooms/create
     *
     * @param request The meeting room creation request containing event and organizer details
     * @return Call with response containing meeting room details (URL, ID, etc.)
     */
    @POST("meeting-rooms/create")
    Call<CreateMeetingRoomResponse> createMeetingRoom(@Body CreateMeetingRoomRequest request);
}
