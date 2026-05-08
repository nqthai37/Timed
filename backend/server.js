/**
 * Express.js Backend Server
 * REST API endpoints for Google Calendar and Zoom meeting management
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const {
  createCalendarEventWithMeet,
  updateCalendarEvent,
  deleteCalendarEvent,
} = require('./handlers/calendarEventHandler');
const {
  createZoomMeeting,
  getZoomMeeting,
  updateZoomMeeting,
  deleteZoomMeeting,
} = require('./handlers/zoomMeetingHandler');
const {
  exchangeAuthCodeForTokens,
  refreshAccessToken,
  verifyRefreshToken,
} = require('./handlers/authHandler');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// OAuth2 credentials from environment
const googleOAuthOptions = {
  clientId: process.env.GOOGLE_CLIENT_ID,
  clientSecret: process.env.GOOGLE_CLIENT_SECRET,
  redirectUrl: process.env.GOOGLE_REDIRECT_URL,
};

const zoomOAuthOptions = {
  zoomClientId: process.env.ZOOM_CLIENT_ID,
  zoomClientSecret: process.env.ZOOM_CLIENT_SECRET,
  zoomAccountId: process.env.ZOOM_ACCOUNT_ID,
};

/**
 * POST /auth/exchange-code
 * Exchange Google authorization code for refresh token
 */
app.post('/auth/exchange-code', async (req, res) => {
  try {
    const { code } = req.body;

    if (!code) {
      return res.status(400).json({
        success: false,
        error: 'Missing authorization code',
      });
    }

    if (!googleOAuthOptions.clientId || !googleOAuthOptions.clientSecret) {
      return res.status(500).json({
        success: false,
        error: 'Server configuration error: Missing Google OAuth credentials',
      });
    }

    const result = await exchangeAuthCodeForTokens(code, googleOAuthOptions);
    res.json(result);
  } catch (error) {
    console.error('Error exchanging auth code:', error.message);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

/**
 * POST /auth/refresh-token
 * Refresh an expired access token
 */
app.post('/auth/refresh-token', async (req, res) => {
  try {
    const { refreshToken } = req.body;

    if (!refreshToken) {
      return res.status(400).json({
        success: false,
        error: 'Missing refresh token',
      });
    }

    if (!googleOAuthOptions.clientId || !googleOAuthOptions.clientSecret) {
      return res.status(500).json({
        success: false,
        error: 'Server configuration error: Missing Google OAuth credentials',
      });
    }

    const result = await refreshAccessToken(refreshToken, googleOAuthOptions);
    res.json(result);
  } catch (error) {
    console.error('Error refreshing access token:', error.message);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

/**
 * POST /auth/verify-token
 * Verify if a refresh token is still valid
 */
app.post('/auth/verify-token', async (req, res) => {
  try {
    const { refreshToken } = req.body;

    if (!refreshToken) {
      return res.status(400).json({
        success: false,
        error: 'Missing refresh token',
      });
    }

    const result = await verifyRefreshToken(refreshToken, googleOAuthOptions);
    res.json(result);
  } catch (error) {
    console.error('Error verifying token:', error.message);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});
app.post(['/api/calendar/create-event', '/api/meeting-rooms/create'], async (req, res) => {
  try {
    console.log('Creating meeting room...', req.body);
    
    // Support both the original payload format and the new Android App payload format
    const summary = req.body.summary || req.body.event_title;
    const startTime = req.body.startTime || req.body.start_time;
    const endTime = req.body.endTime || req.body.end_time;
    const userRefreshToken = req.body.userRefreshToken || req.body.refresh_token; 
    const description = req.body.description || req.body.event_description;
    const location = req.body.location || '';
    const attendees = req.body.attendees || [];
    
    const meetingType = req.body.meeting_type || 'GOOGLE_MEET';

    // Validate core required fields
    if (!summary || !startTime || !endTime) {
      return res.status(400).json({
        success: false,
        error: 'Missing required parameters: summary (event_title), startTime (start_time), endTime (end_time)',
      });
    }

    // ---------------------------------------------------------
    // 1. ZOOM MEETING LOGIC - REAL IMPLEMENTATION
    // ---------------------------------------------------------
    if (meetingType === 'ZOOM_MEETING') {
      // Check if Zoom credentials are configured
      if (!zoomOAuthOptions.zoomClientId || !zoomOAuthOptions.zoomClientSecret) {
        return res.status(500).json({
          success: false,
          error: 'Server configuration error: Missing Zoom OAuth credentials (ZOOM_CLIENT_ID, ZOOM_CLIENT_SECRET)',
        });
      }

      try {
        // Create real Zoom meeting
        const result = await createZoomMeeting(
          {
            summary,
            startTime,
            endTime,
            description: description || '',
            attendees: attendees || [],
            zoomUserId: 'me',
          },
          zoomOAuthOptions
        );

        return res.json({
          success: true,
          meetingId: result.meetingId,
          meetingUrl: result.meetingUrl,
          startUrl: result.startUrl,
          summary: result.summary,
          startTime: result.startTime,
          duration: result.duration,
          description: result.description,
        });
      } catch (error) {
        console.error('Zoom meeting creation failed:', error.message);
        return res.status(500).json({
          success: false,
          error: error.message,
        });
      }
    }
    
    // ---------------------------------------------------------
    // 2. GOOGLE MEET LOGIC - REAL IMPLEMENTATION
    // ---------------------------------------------------------
    if (meetingType === 'GOOGLE_MEET') {
      // Check if user has provided refresh token
      if (!userRefreshToken) {
        return res.status(400).json({
          success: false,
          error: 'Missing required parameter: userRefreshToken for Google Calendar access',
        });
      }

      // Validate OAuth credentials
      if (!googleOAuthOptions.clientId || !googleOAuthOptions.clientSecret || !googleOAuthOptions.redirectUrl) {
        return res.status(500).json({
          success: false,
          error: 'Server configuration error: Missing Google OAuth credentials',
        });
      }

      try {
        // Call the Google Calendar API handler
        const result = await createCalendarEventWithMeet(
          {
            summary,
            startTime,
            endTime,
            userRefreshToken,
            description: description || '',
            location: location || '',
            attendees: attendees || [],
          },
          googleOAuthOptions
        );

        // Return standardized response
        return res.json({
          success: true,
          eventId: result.eventId,
          meetingUrl: result.hangoutLink,
          htmlLink: result.htmlLink,
          summary: result.summary,
          startTime: result.startTime,
          endTime: result.endTime,
          description: result.description,
          conferenceData: result.conferenceData,
        });
      } catch (error) {
        console.error('Google Meet creation failed:', error.message);
        return res.status(500).json({
          success: false,
          error: error.message,
        });
      }
    }

    // Fallback for invalid types
    return res.status(400).json({ 
      success: false, 
      error: 'Invalid meeting_type. Supported types: ZOOM_MEETING, GOOGLE_MEET' 
    });

  } catch (error) {
    console.error('Error creating meeting:', error);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

/**
 * PUT /api/calendar/update-event
 * Update an existing meeting (Google Calendar event or Zoom meeting)
 */
app.put('/api/calendar/update-event', async (req, res) => {
  try {
    const { eventId, userRefreshToken, updates, meeting_type = 'GOOGLE_MEET' } = req.body;

    if (!eventId || !updates) {
      return res.status(400).json({
        success: false,
        error: 'Missing required parameters: eventId, updates',
      });
    }

    // ---------------------------------------------------------
    // UPDATE ZOOM MEETING
    // ---------------------------------------------------------
    if (meeting_type === 'ZOOM_MEETING') {
      if (!zoomOAuthOptions.zoomClientId || !zoomOAuthOptions.zoomClientSecret) {
        return res.status(500).json({
          success: false,
          error: 'Server configuration error: Missing Zoom OAuth credentials',
        });
      }

      try {
        const result = await updateZoomMeeting(eventId, updates, zoomOAuthOptions);
        return res.json(result);
      } catch (error) {
        console.error('Zoom meeting update failed:', error.message);
        return res.status(500).json({
          success: false,
          error: error.message,
        });
      }
    }

    // ---------------------------------------------------------
    // UPDATE GOOGLE CALENDAR EVENT
    // ---------------------------------------------------------
    if (meeting_type === 'GOOGLE_MEET') {
      if (!userRefreshToken) {
        return res.status(400).json({
          success: false,
          error: 'Missing required parameter: userRefreshToken for Google Calendar access',
        });
      }

      try {
        const result = await updateCalendarEvent(
          {
            eventId,
            userRefreshToken,
            updates,
          },
          googleOAuthOptions
        );
        return res.json(result);
      } catch (error) {
        console.error('Google Calendar update failed:', error.message);
        return res.status(500).json({
          success: false,
          error: error.message,
        });
      }
    }

    return res.status(400).json({ 
      success: false, 
      error: 'Invalid meeting_type. Supported types: ZOOM_MEETING, GOOGLE_MEET' 
    });

  } catch (error) {
    console.error('Error updating event:', error);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

/**
 * DELETE /api/calendar/delete-event
 * Delete a meeting (Google Calendar event or Zoom meeting)
 */
app.delete('/api/calendar/delete-event', async (req, res) => {
  try {
    const { eventId, userRefreshToken, meeting_type = 'GOOGLE_MEET' } = req.body;

    if (!eventId) {
      return res.status(400).json({
        success: false,
        error: 'Missing required parameter: eventId',
      });
    }

    // ---------------------------------------------------------
    // DELETE ZOOM MEETING
    // ---------------------------------------------------------
    if (meeting_type === 'ZOOM_MEETING') {
      if (!zoomOAuthOptions.zoomClientId || !zoomOAuthOptions.zoomClientSecret) {
        return res.status(500).json({
          success: false,
          error: 'Server configuration error: Missing Zoom OAuth credentials',
        });
      }

      try {
        const result = await deleteZoomMeeting(eventId, zoomOAuthOptions);
        return res.json(result);
      } catch (error) {
        console.error('Zoom meeting deletion failed:', error.message);
        return res.status(500).json({
          success: false,
          error: error.message,
        });
      }
    }

    // ---------------------------------------------------------
    // DELETE GOOGLE CALENDAR EVENT
    // ---------------------------------------------------------
    if (meeting_type === 'GOOGLE_MEET') {
      if (!userRefreshToken) {
        return res.status(400).json({
          success: false,
          error: 'Missing required parameter: userRefreshToken for Google Calendar access',
        });
      }

      try {
        const result = await deleteCalendarEvent(
          {
            eventId,
            userRefreshToken,
          },
          googleOAuthOptions
        );
        return res.json(result);
      } catch (error) {
        console.error('Google Calendar deletion failed:', error.message);
        return res.status(500).json({
          success: false,
          error: error.message,
        });
      }
    }

    return res.status(400).json({ 
      success: false, 
      error: 'Invalid meeting_type. Supported types: ZOOM_MEETING, GOOGLE_MEET' 
    });

  } catch (error) {
    console.error('Error deleting event:', error);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

/**
 * GET /api/health
 * Health check endpoint
 */
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    message: 'Timed Calendar Backend Server is running',
  });
});

/**
 * Error handler middleware
 */
app.use((err, req, res, next) => {
  console.error('Server error:', err);
  res.status(500).json({
    success: false,
    error: 'Internal server error',
    message: process.env.NODE_ENV === 'development' ? err.message : undefined,
  });
});

/**
 * 404 handler
 */
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: 'Endpoint not found',
    path: req.path,
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`✓ Server is running on http://localhost:${PORT}`);
  console.log(`✓ Health check: http://localhost:${PORT}/api/health`);
  console.log(`✓ Create event endpoint: POST http://localhost:${PORT}/api/calendar/create-event`);
  console.log(`✓ Update event endpoint: PUT http://localhost:${PORT}/api/calendar/update-event`);
  console.log(`✓ Delete event endpoint: DELETE http://localhost:${PORT}/api/calendar/delete-event`);
});

module.exports = app;
