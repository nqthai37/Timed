package com.mobile.timed.utils;

import android.util.Log;

import com.mobile.timed.data.models.EventModel;
import com.mobile.timed.managers.EventManager;
import com.mobile.timed.data.repository.EventRepository;

import java.util.List;

/**
 * Event Integration Service - Tích hợp backend EventManager vào UI
 */
public class EventIntegrationService {
    private static final String TAG = "EventIntegration";
    private final EventManager eventManager;
    private final FirebaseInitializer firebaseInitializer;
    private final EventModelAdapter adapter;

    public interface EventLoadListener {
        void onEventsLoaded(List<EventModel> events);
        void onError(String errorMessage);
    }

    public interface EventSaveListener {
        void onSuccess(String eventId);
        void onError(String errorMessage);
    }

    public interface EventDetailListener {
        void onEventLoaded(EventModel event);
        void onError(String errorMessage);
    }

    public EventIntegrationService() {
        this.eventManager = new EventManager();
        this.firebaseInitializer = FirebaseInitializer.getInstance();
        this.adapter = new EventModelAdapter();
    }

    /**
     * Lấy tất cả events của lịch
     */
    public void getEventsForCalendar(String calendarId, EventLoadListener listener) {
        eventManager.getCalendarEvents(calendarId, new EventRepository.OnEventListener<List<EventModel>>() {
            @Override
            public void onSuccess(List<EventModel> events) {
                Log.d(TAG, "Events loaded: " + events.size());
                listener.onEventsLoaded(events);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error loading events: " + errorMessage);
                listener.onError(errorMessage);
            }
        });
    }

    /**
     * Lấy events trong khoảng thời gian
     */
    public void getEventsInDateRange(String calendarId, long startTime, long endTime, EventLoadListener listener) {
        eventManager.getEventsInDateRange(calendarId, startTime, endTime, new EventRepository.OnEventListener<List<EventModel>>() {
            @Override
            public void onSuccess(List<EventModel> events) {
                Log.d(TAG, "Events in range loaded: " + events.size());
                listener.onEventsLoaded(events);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error loading events in range: " + errorMessage);
                listener.onError(errorMessage);
            }
        });
    }

    /**
     * Lấy chi tiết một event theo id
     */
    public void getEventById(String eventId, EventDetailListener listener) {
        eventManager.getEvent(eventId, new EventRepository.OnEventListener<EventModel>() {
            @Override
            public void onSuccess(EventModel event) {
                listener.onEventLoaded(event);
            }

            @Override
            public void onFailure(String errorMessage) {
                listener.onError(errorMessage);
            }
        });
    }

    /**
     * Tạo sự kiện đơn lẻ
     */
    public void createSingleEvent(String calendarId, String title, long startTime, long endTime,
                                  String description, String location, boolean isAllDay, EventSaveListener listener) {
        String userId = firebaseInitializer.getCurrentUserId();

        if (userId == null) {
            listener.onError("User not logged in");
            return;
        }

        eventManager.createSingleEvent(calendarId, title, startTime, endTime, description, location, isAllDay,
            new EventRepository.OnEventListener<String>() {
                @Override
                public void onSuccess(String eventId) {
                    Log.d(TAG, "Event created: " + eventId);
                    listener.onSuccess(eventId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error creating event: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Tạo sự kiện định kỳ
     */
    public void createRecurringEvent(String calendarId, String title, long startTime, long endTime,
                                     String recurrenceRule, String description, String location,
                                     boolean isAllDay, EventSaveListener listener) {
        String userId = firebaseInitializer.getCurrentUserId();

        if (userId == null) {
            listener.onError("User not logged in");
            return;
        }

        eventManager.createRecurringEvent(calendarId, title, startTime, endTime, recurrenceRule,
            description, location, isAllDay,
            new EventRepository.OnEventListener<String>() {
                @Override
                public void onSuccess(String eventId) {
                    Log.d(TAG, "Recurring event created: " + eventId);
                    listener.onSuccess(eventId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error creating recurring event: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Cập nhật sự kiện
     */
    public void updateEvent(String eventId, EventModel updatedEvent, EventSaveListener listener) {
        eventManager.updateEvent(eventId, updatedEvent,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Event updated: " + eventId);
                    listener.onSuccess(eventId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error updating event: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Xóa sự kiện
     */
    public void deleteEvent(String eventId, EventSaveListener listener) {
        eventManager.deleteEvent(eventId,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Event deleted: " + eventId);
                    listener.onSuccess(eventId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error deleting event: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Thêm reminder vào event
     */
    public void addReminder(String eventId, int minutesBefore, EventSaveListener listener) {
        eventManager.addReminder(eventId, minutesBefore,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Reminder added to event: " + eventId);
                    listener.onSuccess(eventId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error adding reminder: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Thêm tệp đính kèm vào event
     */
    public void addAttachment(String eventId, String fileName, String fileUrl, String fileType,
                             long fileSize, EventSaveListener listener) {
        eventManager.addAttachment(eventId, fileName, fileUrl, fileType, fileSize,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Attachment added to event: " + eventId);
                    listener.onSuccess(eventId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error adding attachment: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Thêm ghi chú vào event
     */
    public void addNotes(String eventId, String notes, EventSaveListener listener) {
        eventManager.addNotes(eventId, notes,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Notes added to event: " + eventId);
                    listener.onSuccess(eventId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error adding notes: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Thêm tham gia viên vào event
     */
    public void addParticipant(String eventId, String participantUid, EventSaveListener listener) {
        eventManager.addParticipant(eventId, participantUid, "pending",
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Participant added to event: " + eventId);
                    listener.onSuccess(eventId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error adding participant: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }

    /**
     * Cập nhật trạng thái tham gia viên
     */
    public void updateParticipantStatus(String eventId, String participantUid, String status, EventSaveListener listener) {
        eventManager.updateParticipantStatus(eventId, participantUid, status,
            new EventRepository.OnEventListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Participant status updated: " + eventId);
                    listener.onSuccess(eventId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error updating participant status: " + errorMessage);
                    listener.onError(errorMessage);
                }
            });
    }
}

