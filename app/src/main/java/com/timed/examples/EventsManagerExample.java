package com.timed.examples;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.timed.managers.EventsManager;
import com.timed.models.Event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Example class showing how to use EventsManager
 * Copy relevant code to your Activities
 */
public class EventsManagerExample {
    private static final String TAG = "EventsExample";
    private EventsManager eventsManager;
    private Context context;
    private String userId;

    public EventsManagerExample(Context context) {
        this.context = context;
        this.eventsManager = EventsManager.getInstance(context);
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    // ===== EXAMPLE 1: Create a new event =====
    public void createEventExample() {
        Event event = new Event();
        event.setTitle("Team Meeting");
        event.setDescription("Quarterly review and planning");
        event.setLocation("Conference Room A");
        
        // Set start and end time
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1); // Tomorrow
        calendar.set(Calendar.HOUR_OF_DAY, 14);
        calendar.set(Calendar.MINUTE, 0);
        
        Date startDate = calendar.getTime();
        event.setStartTime(new Timestamp(startDate));
        
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        Date endDate = calendar.getTime();
        event.setEndTime(new Timestamp(endDate));
        
        // Set metadata
        event.setAllDay(false);
        event.setTimezone("Asia/Ho_Chi_Minh");
        event.setCreatedBy(userId);
        event.setCalendarId("hxRs66vHwA1AnUPLr3sH"); // Replace with actual calendar ID
        event.setVisibility("public");
        
        // Add reminders
        List<Event.EventReminder> reminders = new ArrayList<>();
        reminders.add(new Event.EventReminder(10L, "push"));    // 10 minutes before
        reminders.add(new Event.EventReminder(30L, "push"));    // 30 minutes before
        event.setReminders(reminders);
        
        // Add participants
        event.getParticipantId().add(userId);
        event.getParticipantStatus().put(userId, "accepted");
        event.getParticipantId().add("participant2_id");
        event.getParticipantStatus().put("participant2_id", "pending");
        
        // Add attachments
        List<Event.EventAttachment> attachments = new ArrayList<>();
        attachments.add(new Event.EventAttachment("agenda.pdf", "https://example.com/agenda.pdf"));
        event.setAttachments(attachments);
        
        // Save to Firebase
        eventsManager.createEvent(event)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Event created with ID: " + docRef.getId());
                    Toast.makeText(context, "Event created successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating event", e);
                    Toast.makeText(context, "Error creating event", Toast.LENGTH_SHORT).show();
                });
    }

    // ===== EXAMPLE 2: Get all events for a calendar =====
    public void getCalendarEventsExample(String calendarId) {
        eventsManager.getEventsByCalendarId(calendarId)
                .addOnSuccessListener(events -> {
                    Log.d(TAG, "Retrieved " + events.size() + " events");
                    for (Event event : events) {
                        Log.d(TAG, "Event: " + event.getTitle() + " at " + event.getStartTime());
                    }
                    // Update your UI adapter here
                    // eventAdapter.setEvents(events);
                    // eventAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching events", e);
                });
    }

    // ===== EXAMPLE 3: Get events in date range (useful for calendar view) =====
    public void getEventsInDateRangeExample(String calendarId, Date fromDate, Date toDate) {
        Timestamp startDate = new Timestamp(fromDate);
        Timestamp endDate = new Timestamp(toDate);
        
        eventsManager.getEventsByDateRange(calendarId, startDate, endDate)
                .addOnSuccessListener(events -> {
                    Log.d(TAG, "Retrieved " + events.size() + " events for date range");
                    
                    // Example: Group events by date
                    for (Event event : events) {
                        Log.d(TAG, "Event: " + event.getTitle() + 
                                " starts: " + event.getStartTime().toDate());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching events", e);
                });
    }

    // ===== EXAMPLE 4: Get upcoming events for current user =====
    public void getUpcomingEventsExample() {
        eventsManager.getUpcomingEventsForUser(userId)
                .addOnSuccessListener(events -> {
                    Log.d(TAG, "Retrieved " + events.size() + " upcoming events");
                    
                    if (events.isEmpty()) {
                        Toast.makeText(context, "No upcoming events", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Event nextEvent = events.get(0);
                    Log.d(TAG, "Next event: " + nextEvent.getTitle() + 
                            " at " + nextEvent.getStartTime().toDate());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching upcoming events", e);
                });
    }

    // ===== EXAMPLE 5: Get a specific event =====
    public void getEventByIdExample(String eventId) {
        eventsManager.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    if (event != null) {
                        Log.d(TAG, "Retrieved event: " + event.getTitle());
                        Log.d(TAG, "Participants: " + event.getParticipantId());
                        Log.d(TAG, "Reminders: " + event.getReminders().size());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching event", e);
                });
    }

    // ===== EXAMPLE 6: Update an event =====
    public void updateEventExample(String eventId) {
        eventsManager.getEventById(eventId)
                .addOnSuccessListener(event -> {
                    if (event != null) {
                        // Modify event
                        event.setTitle("Updated Meeting Title");
                        event.setDescription("Updated description with new details");
                        
                        // Add a new reminder
                        event.getReminders().add(new Event.EventReminder(5L, "push"));
                        
                        // Save changes
                        eventsManager.updateEvent(eventId, event)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Event updated successfully");
                                    Toast.makeText(context, "Event updated!", Toast.LENGTH_SHORT).show();
                                    // Reminders will be automatically rescheduled
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating event", e);
                                });
                    }
                });
    }

    // ===== EXAMPLE 7: Delete an event =====
    public void deleteEventExample(String eventId) {
        eventsManager.deleteEvent(eventId)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event deleted successfully");
                    Toast.makeText(context, "Event deleted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting event", e);
                });
    }

    // ===== EXAMPLE 8: Update participant status =====
    public void updateParticipantStatusExample(String eventId, String participantId) {
        // Accept event
        eventsManager.updateParticipantStatus(eventId, participantId, "accepted")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Status updated to: accepted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating status", e);
                });
    }

    // ===== EXAMPLE 9: Add a new participant =====
    public void addParticipantExample(String eventId, String newParticipantId) {
        eventsManager.addParticipant(eventId, newParticipantId, "pending")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Participant added");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding participant", e);
                });
    }

    // ===== EXAMPLE 10: Schedule reminders on app startup =====
    public void scheduleRemindersOnAppStart() {
        eventsManager.rescheduleAllReminders(userId);
        Log.d(TAG, "Reminders rescheduled on app startup");
    }

    // ===== EXAMPLE 11: Recurring events =====
    public void createRecurringEventExample() {
        Event event = new Event();
        event.setTitle("Weekly Team Standup");
        event.setDescription("Daily standup meeting");
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        
        event.setStartTime(new Timestamp(calendar.getTime()));
        
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 15);
        event.setEndTime(new Timestamp(calendar.getTime()));
        
        event.setCreatedBy(userId);
        event.setCalendarId("calendar_id");
        
        // Set recurrence: Weekly on Monday until end of 2026
        event.setRecurrenceRule("FREQ=WEEKLY;INTERVAL=1;BYDAY=MO;UNTIL=20261231T000000Z");
        
        // Add exceptions (dates to skip)
        List<String> exceptions = new ArrayList<>();
        exceptions.add("2026-06-01");
        exceptions.add("2026-12-25");
        event.setRecurrenceExceptions(exceptions);
        
        // Add reminders
        List<Event.EventReminder> reminders = new ArrayList<>();
        reminders.add(new Event.EventReminder(15L, "push"));
        event.setReminders(reminders);
        
        eventsManager.createEvent(event)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Recurring event created");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating recurring event", e);
                });
    }

    // ===== EXAMPLE 12: All-day event =====
    public void createAllDayEventExample() {
        Event event = new Event();
        event.setTitle("Company Holiday");
        event.setAllDay(true);
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.MONTH, 0); // January
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        
        event.setStartTime(new Timestamp(calendar.getTime()));
        event.setEndTime(new Timestamp(calendar.getTime()));
        
        event.setCreatedBy(userId);
        event.setCalendarId("calendar_id");
        
        // All-day events might not need reminders
        event.setReminders(new ArrayList<>());
        
        eventsManager.createEvent(event)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "All-day event created");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating all-day event", e);
                });
    }

    // ===== HELPER: Format date for display =====
    public static String formatEventTime(Event event) {
        if (event.getAllDay() != null && event.getAllDay()) {
            return "All Day";
        }
        
        Date startDate = event.getStartTime().toDate();
        Date endDate = event.getEndTime().toDate();
        
        // Format as: "HH:mm - HH:mm"
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return timeFormat.format(startDate) + " - " + timeFormat.format(endDate);
    }

    // ===== HELPER: Display event details =====
    public static void logEventDetails(Event event) {
        Log.d("EventDetails", "=== Event Details ===");
        Log.d("EventDetails", "Title: " + event.getTitle());
        Log.d("EventDetails", "Description: " + event.getDescription());
        Log.d("EventDetails", "Location: " + event.getLocation());
        Log.d("EventDetails", "Start Time: " + event.getStartTime().toDate());
        Log.d("EventDetails", "End Time: " + event.getEndTime().toDate());
        Log.d("EventDetails", "All Day: " + event.getAllDay());
        Log.d("EventDetails", "Timezone: " + event.getTimezone());
        Log.d("EventDetails", "Participants: " + event.getParticipantId());
        Log.d("EventDetails", "Reminders: " + event.getReminders().size());
        Log.d("EventDetails", "Visibility: " + event.getVisibility());
        Log.d("EventDetails", "Created By: " + event.getCreatedBy());
    }
}
