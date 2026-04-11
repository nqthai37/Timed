/**
 * Integration Guide: Adding Events System to MainActivity
 * 
 * Copy these code snippets to your MainActivity to integrate the Events system
 */

// ===== STEP 1: Add imports to MainActivity =====
import com.timed.managers.EventsManager;
import com.timed.models.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;

// ===== STEP 2: Add to onCreate() method =====
// Ideally in your Application.java or MainActivity.onCreate()
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // ... existing code ...
    
    // Schedule reminders for events when app starts
    String userId = getCurrentUserId();
    if (userId != null) {
        EventsManager.getInstance(this).rescheduleAllReminders(userId);
        Log.d("MainActivity", "Event reminders rescheduled on app start");
    }
}

// ===== STEP 3: Add helper method to get current user ID =====
private String getCurrentUserId() {
    try {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    } catch (Exception e) {
        Log.e("MainActivity", "Error getting user ID", e);
        return null;
    }
}

// ===== STEP 4: Load events when displaying calendar =====
private void loadCalendarEvents(String calendarId) {
    EventsManager eventsManager = EventsManager.getInstance(this);
    
    eventsManager.getEventsByCalendarId(calendarId)
            .addOnSuccessListener(events -> {
                Log.d("MainActivity", "Loaded " + events.size() + " events");
                
                // Update your calendar adapter with events
                if (eventAdapter != null) {
                    eventAdapter.setEvents(events);
                    eventAdapter.notifyDataSetChanged();
                }
                
                // Or if you need events grouped by date
                Map<String, List<Event>> eventsByDate = groupEventsByDate(events);
                updateCalendarView(eventsByDate);
            })
            .addOnFailureListener(e -> {
                Log.e("MainActivity", "Error loading events", e);
                Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
            });
}

// ===== STEP 5: Helper to group events by date =====
private Map<String, List<Event>> groupEventsByDate(List<Event> events) {
    Map<String, List<Event>> grouped = new HashMap<>();
    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
    
    for (Event event : events) {
        String dateKey = dateFormat.format(event.getStartTime().toDate());
        grouped.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(event);
    }
    
    return grouped;
}

// ===== STEP 6: Load events for a specific date =====
private void loadEventsForDate(java.util.Date targetDate, String calendarId) {
    Calendar calendar = Calendar.getInstance();
    
    // Start of day
    calendar.setTime(targetDate);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    Timestamp startOfDay = new Timestamp(calendar.getTime());
    
    // End of day
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    Timestamp endOfDay = new Timestamp(calendar.getTime());
    
    EventsManager eventsManager = EventsManager.getInstance(this);
    eventsManager.getEventsByDateRange(calendarId, startOfDay, endOfDay)
            .addOnSuccessListener(events -> {
                Log.d("MainActivity", "Events for " + targetDate + ": " + events.size());
                
                // Update UI with today's events
                displayDayEvents(events);
            })
            .addOnFailureListener(e -> {
                Log.e("MainActivity", "Error loading events for date", e);
            });
}

// ===== STEP 7: Update the "upcoming events" section =====
private void updateUpcomingEvents() {
    String userId = getCurrentUserId();
    if (userId == null) return;
    
    EventsManager eventsManager = EventsManager.getInstance(this);
    
    eventsManager.getUpcomingEventsForUser(userId)
            .addOnSuccessListener(events -> {
                if (events.isEmpty()) {
                    tvUpcomingTitle.setText("No upcoming events");
                    return;
                }
                
                // Display next 3-5 upcoming events
                List<Event> nextEvents = events.subList(0, Math.min(5, events.size()));
                
                // Update your upcoming events list/adapter
                // upcomingEventsAdapter.setEvents(nextEvents);
                // upcomingEventsAdapter.notifyDataSetChanged();
                
                tvUpcomingTitle.setText("Upcoming Events (" + events.size() + ")");
            })
            .addOnFailureListener(e -> {
                Log.e("MainActivity", "Error loading upcoming events", e);
            });
}

// ===== STEP 8: Handle event click - update participant status =====
private void handleEventClick(Event event) {
    String userId = getCurrentUserId();
    if (userId == null) return;
    
    // Check if user is a participant
    if (!event.getParticipantId().contains(userId)) {
        // Not a participant, just display event details
        showEventDetails(event);
        return;
    }
    
    // User is a participant - show acceptance options
    String currentStatus = event.getParticipantStatus().get(userId);
    
    new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(event.getTitle())
            .setMessage("Update your response for this event?")
            .setPositiveButton("Accept", (dialog, which) -> {
                EventsManager manager = EventsManager.getInstance(MainActivity.this);
                manager.updateParticipantStatus(event.getId(), userId, "accepted")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(MainActivity.this, "Response updated", Toast.LENGTH_SHORT).show();
                        });
            })
            .setNegativeButton("Decline", (dialog, which) -> {
                EventsManager manager = EventsManager.getInstance(MainActivity.this);
                manager.updateParticipantStatus(event.getId(), userId, "declined")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(MainActivity.this, "Response updated", Toast.LENGTH_SHORT).show();
                        });
            })
            .setNeutralButton("Cancel", null)
            .show();
}

// ===== STEP 9: Refresh events =====
private void refreshEvents() {
    loadCalendarEvents("your_calendar_id");
    updateUpcomingEvents();
}

// ===== STEP 10: Handle onResume to refresh =====
@Override
protected void onResume() {
    super.onResume();
    
    // Refresh events when returning to MainActivity
    refreshEvents();
}

/**
 * ADAPTER EXAMPLE: How to display events in RecyclerView
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private List<Event> events = new ArrayList<>();
    private Context context;
    private OnEventClickListener listener;
    
    public interface OnEventClickListener {
        void onEventClick(Event event);
    }
    
    public EventAdapter(Context context, OnEventClickListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Event event = events.get(position);
        
        holder.text1.setText(event.getTitle());
        
        String timeText = formatEventTime(event);
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            timeText += " - " + event.getLocation();
        }
        holder.text2.setText(timeText);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return events.size();
    }
    
    private String formatEventTime(Event event) {
        if (event.getAllDay() != null && event.getAllDay()) {
            return "All Day";
        }
        
        java.text.SimpleDateFormat timeFormat = 
                new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        Date startDate = event.getStartTime().toDate();
        Date endDate = event.getEndTime().toDate();
        
        return timeFormat.format(startDate) + " - " + timeFormat.format(endDate);
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        
        public ViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}

/**
 * USAGE IN FRAGMENT/ACTIVITY:
 */
// In your Fragment or Activity
private void setupEventsList() {
    RecyclerView rvEvents = findViewById(R.id.rv_events);
    rvEvents.setLayoutManager(new LinearLayoutManager(this));
    
    EventAdapter eventAdapter = new EventAdapter(this, event -> {
        handleEventClick(event);
    });
    
    rvEvents.setAdapter(eventAdapter);
    
    // Load events
    String calendarId = "your_calendar_id";
    EventsManager.getInstance(this).getEventsByCalendarId(calendarId)
            .addOnSuccessListener(events -> {
                eventAdapter.setEvents(events);
            });
}

/**
 * FILES CREATED:
 * - /models/Event.java - Event model
 * - /repositories/EventsRepository.java - Firebase queries
 * - /managers/EventsManager.java - Main manager (USE THIS)
 * - /managers/EventsNotificationManager.java - Notification scheduling
 * - /services/EventNotificationReceiver.java - Notification handler
 * - /examples/EventsManagerExample.java - Usage examples
 * - EVENTS_USAGE_GUIDE.md - Complete documentation
 * 
 * START HERE:
 * 1. Read EVENTS_USAGE_GUIDE.md for full documentation
 * 2. Check EventsManagerExample.java for code examples
 * 3. Copy relevant code snippets from this file to your Activities
 * 4. Call EventsManager.getInstance(context) to start using
 */
