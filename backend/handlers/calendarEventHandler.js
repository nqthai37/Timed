/**
 * Google Calendar Event Handler
 * Creates calendar events with automatic Google Meet link generation
 */

const { google } = require('googleapis');

/**
 * Creates a Google Calendar event with automatic Google Meet link
 * @param {Object} params - Handler parameters
 * @param {string} params.summary - Event title/summary
 * @param {string} params.startTime - Event start time (ISO 8601 format or Date object)
 * @param {string} params.endTime - Event end time (ISO 8601 format or Date object)
 * @param {string} params.userRefreshToken - User's Google OAuth refresh token
 * @param {Object} options - Additional options
 * @param {string} options.clientId - Google OAuth Client ID
 * @param {string} options.clientSecret - Google OAuth Client Secret
 * @param {string} options.redirectUrl - Google OAuth Redirect URL
 * @param {string} options.description - Event description (optional)
 * @param {string} options.location - Event location (optional)
 * @param {Array} options.attendees - Array of attendee emails (optional)
 * @returns {Promise<Object>} Event details containing htmlLink and hangoutLink
 */
async function createCalendarEventWithMeet(params, options = {}) {
  const {
    summary,
    startTime,
    endTime,
    userRefreshToken,
  } = params;

  const {
    clientId,
    clientSecret,
    redirectUrl,
    description = '',
    location = '',
    attendees = [],
  } = options;

  // Validate required parameters
  if (!summary || !startTime || !endTime || !userRefreshToken) {
    throw new Error(
      'Missing required parameters: summary, startTime, endTime, and userRefreshToken are required'
    );
  }

  if (!clientId || !clientSecret || !redirectUrl) {
    throw new Error(
      'Missing OAuth credentials: clientId, clientSecret, and redirectUrl are required'
    );
  }

  try {
    // Initialize OAuth2 client
    const oauth2Client = new google.auth.OAuth2(
      clientId,
      clientSecret,
      redirectUrl
    );

    // Set refresh token to get access token
    oauth2Client.setCredentials({
      refresh_token: userRefreshToken,
    });

    // Get the calendar API instance
    const calendar = google.calendar({ version: 'v3', auth: oauth2Client });

    // Prepare event object
    const event = {
      summary: summary,
      description: description,
      location: location,
      start: {
        dateTime: new Date(startTime).toISOString(),
        timeZone: 'UTC',
      },
      end: {
        dateTime: new Date(endTime).toISOString(),
        timeZone: 'UTC',
      },
      // Configure conference data for Google Meet
      conferenceData: {
        createRequest: {
          requestId: `meet-${Date.now()}`,
          conferenceSolutionKey: {
            key: 'hangoutsMeet',
          },
        },
      },
      // Add attendees if provided
      ...(attendees.length > 0 && {
        attendees: attendees.map((email) => ({
          email: email,
          responseStatus: 'needsAction',
        })),
      }),
    };

    // Insert event with conferenceDataVersion=1
    const response = await calendar.events.insert({
      calendarId: 'primary',
      resource: event,
      conferenceDataVersion: 1,
      maxAttendees: attendees.length > 0 ? attendees.length + 1 : 1,
      sendUpdates: attendees.length > 0 ? 'all' : 'none',
    });

    // Extract relevant data for mobile client
    const eventData = response.data;
    const result = {
      success: true,
      eventId: eventData.id,
      htmlLink: eventData.htmlLink, // Web link to the event
      hangoutLink: eventData.conferenceData?.entryPoints?.find(
        (entry) => entry.entryPointType === 'video'
      )?.uri || eventData.hangoutLink || null, // Google Meet link
      summary: eventData.summary,
      startTime: eventData.start.dateTime,
      endTime: eventData.end.dateTime,
      description: eventData.description,
      conferenceData: {
        conferenceId: eventData.conferenceData?.conferenceId,
        entryPoints: eventData.conferenceData?.entryPoints || [],
      },
    };

    return result;
  } catch (error) {
    console.error('Error creating calendar event:', error);
    throw new Error(
      `Failed to create calendar event: ${error.message}`
    );
  }
}

/**
 * Batch create multiple calendar events with Google Meet
 * @param {Array} eventsList - Array of event parameters
 * @param {Object} options - OAuth and additional options
 * @returns {Promise<Array>} Array of created event results
 */
async function createMultipleCalendarEvents(eventsList, options) {
  const results = [];
  const errors = [];

  for (const eventParams of eventsList) {
    try {
      const result = await createCalendarEventWithMeet(eventParams, options);
      results.push(result);
    } catch (error) {
      errors.push({
        event: eventParams.summary,
        error: error.message,
      });
    }
  }

  return {
    successful: results,
    failed: errors,
    totalProcessed: eventsList.length,
    successCount: results.length,
    failureCount: errors.length,
  };
}

/**
 * Update an existing calendar event
 * @param {Object} params - Update parameters
 * @param {string} params.eventId - Google Calendar event ID
 * @param {string} params.userRefreshToken - User's refresh token
 * @param {Object} params.updates - Fields to update
 * @param {Object} options - OAuth credentials
 * @returns {Promise<Object>} Updated event details
 */
async function updateCalendarEvent(params, options) {
  const {
    eventId,
    userRefreshToken,
    updates,
  } = params;

  const {
    clientId,
    clientSecret,
    redirectUrl,
  } = options;

  if (!eventId || !userRefreshToken || !updates) {
    throw new Error('Missing required parameters: eventId, userRefreshToken, and updates');
  }

  try {
    const oauth2Client = new google.auth.OAuth2(
      clientId,
      clientSecret,
      redirectUrl
    );

    oauth2Client.setCredentials({
      refresh_token: userRefreshToken,
    });

    const calendar = google.calendar({ version: 'v3', auth: oauth2Client });

    const response = await calendar.events.update({
      calendarId: 'primary',
      eventId: eventId,
      resource: updates,
      conferenceDataVersion: 1,
    });

    return {
      success: true,
      eventId: response.data.id,
      htmlLink: response.data.htmlLink,
      hangoutLink: response.data.conferenceData?.entryPoints?.find(
        (entry) => entry.entryPointType === 'video'
      )?.uri || null,
    };
  } catch (error) {
    throw new Error(`Failed to update calendar event: ${error.message}`);
  }
}

/**
 * Delete a calendar event
 * @param {Object} params - Delete parameters
 * @param {string} params.eventId - Google Calendar event ID
 * @param {string} params.userRefreshToken - User's refresh token
 * @param {Object} options - OAuth credentials
 * @returns {Promise<Object>} Deletion confirmation
 */
async function deleteCalendarEvent(params, options) {
  const {
    eventId,
    userRefreshToken,
  } = params;

  const {
    clientId,
    clientSecret,
    redirectUrl,
  } = options;

  if (!eventId || !userRefreshToken) {
    throw new Error('Missing required parameters: eventId and userRefreshToken');
  }

  try {
    const oauth2Client = new google.auth.OAuth2(
      clientId,
      clientSecret,
      redirectUrl
    );

    oauth2Client.setCredentials({
      refresh_token: userRefreshToken,
    });

    const calendar = google.calendar({ version: 'v3', auth: oauth2Client });

    await calendar.events.delete({
      calendarId: 'primary',
      eventId: eventId,
    });

    return {
      success: true,
      message: `Event ${eventId} deleted successfully`,
      eventId: eventId,
    };
  } catch (error) {
    throw new Error(`Failed to delete calendar event: ${error.message}`);
  }
}

module.exports = {
  createCalendarEventWithMeet,
  createMultipleCalendarEvents,
  updateCalendarEvent,
  deleteCalendarEvent,
};
