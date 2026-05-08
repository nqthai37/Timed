/**
 * Example usage of the Calendar Event Handler
 * Shows how to create Google Calendar events with Meet links
 */

const {
  createCalendarEventWithMeet,
  createMultipleCalendarEvents,
  updateCalendarEvent,
  deleteCalendarEvent,
} = require('./calendarEventHandler');

// OAuth2 credentials (store these securely in environment variables)
const oauthOptions = {
  clientId: process.env.GOOGLE_CLIENT_ID,
  clientSecret: process.env.GOOGLE_CLIENT_SECRET,
  redirectUrl: process.env.GOOGLE_REDIRECT_URL,
};

/**
 * Example 1: Create a single event with Google Meet
 */
async function exampleCreateEvent() {
  try {
    const result = await createCalendarEventWithMeet(
      {
        summary: 'Team Meeting',
        startTime: new Date(Date.now() + 3600000).toISOString(), // 1 hour from now
        endTime: new Date(Date.now() + 5400000).toISOString(),   // 1.5 hours from now
        userRefreshToken: 'YOUR_USER_REFRESH_TOKEN',
        description: 'Weekly team sync',
        location: 'Online',
        attendees: ['team@example.com', 'manager@example.com'],
      },
      oauthOptions
    );

    console.log('Event created successfully:');
    console.log({
      eventId: result.eventId,
      title: result.summary,
      htmlLink: result.htmlLink,
      googleMeetLink: result.hangoutLink,
      startTime: result.startTime,
      endTime: result.endTime,
      conferenceId: result.conferenceData.conferenceId,
    });

    return result;
  } catch (error) {
    console.error('Error creating event:', error.message);
  }
}

/**
 * Example 2: Create multiple events
 */
async function exampleCreateMultipleEvents() {
  try {
    const events = [
      {
        summary: 'Sprint Planning',
        startTime: new Date(Date.now() + 3600000).toISOString(),
        endTime: new Date(Date.now() + 7200000).toISOString(),
        userRefreshToken: 'USER_REFRESH_TOKEN_1',
      },
      {
        summary: 'Design Review',
        startTime: new Date(Date.now() + 86400000).toISOString(),
        endTime: new Date(Date.now() + 90000000).toISOString(),
        userRefreshToken: 'USER_REFRESH_TOKEN_1',
      },
    ];

    const result = await createMultipleCalendarEvents(events, oauthOptions);

    console.log('Batch creation results:');
    console.log({
      totalProcessed: result.totalProcessed,
      successCount: result.successCount,
      failureCount: result.failureCount,
      successful: result.successful,
      failed: result.failed,
    });

    return result;
  } catch (error) {
    console.error('Error creating multiple events:', error.message);
  }
}

/**
 * Example 3: Update an existing event
 */
async function exampleUpdateEvent() {
  try {
    const result = await updateCalendarEvent(
      {
        eventId: 'EVENT_ID_FROM_CREATION',
        userRefreshToken: 'YOUR_USER_REFRESH_TOKEN',
        updates: {
          summary: 'Updated Team Meeting',
          description: 'Updated meeting description',
          location: 'Conference Room B',
        },
      },
      oauthOptions
    );

    console.log('Event updated successfully:', result);
    return result;
  } catch (error) {
    console.error('Error updating event:', error.message);
  }
}

/**
 * Example 4: Delete an event
 */
async function exampleDeleteEvent() {
  try {
    const result = await deleteCalendarEvent(
      {
        eventId: 'EVENT_ID_TO_DELETE',
        userRefreshToken: 'YOUR_USER_REFRESH_TOKEN',
      },
      oauthOptions
    );

    console.log('Event deleted:', result);
    return result;
  } catch (error) {
    console.error('Error deleting event:', error.message);
  }
}

// Export examples
module.exports = {
  exampleCreateEvent,
  exampleCreateMultipleEvents,
  exampleUpdateEvent,
  exampleDeleteEvent,
};

// For testing: Uncomment to run examples
// exampleCreateEvent();
// exampleCreateMultipleEvents();
// exampleUpdateEvent();
// exampleDeleteEvent();
