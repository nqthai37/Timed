/**
 * Zoom Meeting Handler
 * Creates Zoom meetings using Server-to-Server OAuth
 */

const axios = require('axios');
const jwt = require('jsonwebtoken');

const ZOOM_API_BASE = 'https://api.zoom.us/v2';

/**
 * Get access token for Zoom Server-to-Server OAuth
 */
async function getZoomAccessToken(zoomClientId, zoomClientSecret, zoomAccountId) {
  try {
    const authHeader = Buffer.from(`${zoomClientId}:${zoomClientSecret}`).toString('base64');
    
    const response = await axios.post(
      `https://zoom.us/oauth/token?grant_type=account_credentials&account_id=${zoomAccountId}`,
      null,
      {
        headers: {
          Authorization: `Basic ${authHeader}`,
        },
      }
    );
    
    return response.data.access_token;
  } catch (error) {
    console.error('Error fetching Zoom access token:', error.response?.data || error.message);
    throw new Error('Failed to obtain Zoom access token');
  }
}

/**
 * Create a Zoom meeting
 * @param {Object} params - Meeting parameters
 * @param {string} params.summary - Meeting title
 * @param {string} params.startTime - Start time (ISO 8601)
 * @param {string} params.endTime - End time (ISO 8601)
 * @param {string} params.description - Meeting description
 * @param {Array} params.attendees - List of attendee emails
 * @param {string} params.zoomUserId - Zoom user ID for account owner
 * @param {Object} options - OAuth options
 * @param {string} options.zoomClientId - Zoom Client ID
 * @param {string} options.zoomClientSecret - Zoom Client Secret
 * @param {string} options.zoomAccountId - Zoom Account ID (optional)
 * @returns {Promise<Object>} Meeting details
 */
async function createZoomMeeting(params, options) {
  const {
    summary,
    startTime,
    endTime,
    description = '',
    attendees = [],
    zoomUserId = 'me',
  } = params;

  const {
    zoomClientId,
    zoomClientSecret,
    zoomAccountId,
  } = options;

  // Validate required parameters
  if (!summary || !startTime || !endTime) {
    throw new Error('Missing required parameters: summary, startTime, endTime');
  }

  if (!zoomClientId || !zoomClientSecret) {
    throw new Error('Missing Zoom OAuth credentials: zoomClientId and zoomClientSecret');
  }

  try {
    // Get Server-to-Server OAuth token
    if (!zoomAccountId) {
      throw new Error('Missing Zoom Account ID (zoomAccountId)');
    }
    const token = await getZoomAccessToken(zoomClientId, zoomClientSecret, zoomAccountId);

    // Prepare meeting request
    const startDate = new Date(startTime);
    const duration = Math.round((new Date(endTime) - startDate) / 60000); // Duration in minutes

    const meetingPayload = {
      topic: summary,
      type: 2, // Scheduled meeting
      start_time: startDate.toISOString(),
      duration: duration,
      timezone: 'UTC',
      agenda: description,
      settings: {
        host_video: true,
        participant_video: true,
        join_before_host: true,
        mute_upon_entry: false,
        waiting_room: false,
        auto_recording: 'none', // 'none', 'local', 'cloud'
        meeting_authentication: false,
      },
    };

    // Create the meeting
    const response = await axios.post(
      `${ZOOM_API_BASE}/users/${zoomUserId}/meetings`,
      meetingPayload,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      }
    );

    const meetingData = response.data;

    // Add attendees if provided
    if (attendees && attendees.length > 0) {
      try {
        await addZoomMeetingAttendees(meetingData.id, attendees, token);
      } catch (error) {
        console.error('Failed to add attendees:', error.message);
        // Continue even if attendees fail
      }
    }

    // Return standardized response
    return {
      success: true,
      meetingId: meetingData.id,
      meetingUrl: meetingData.join_url,
      startUrl: meetingData.start_url,
      summary: meetingData.topic,
      startTime: meetingData.start_time,
      duration: meetingData.duration,
      timezone: meetingData.timezone,
      description: meetingData.agenda,
      settings: meetingData.settings,
    };
  } catch (error) {
    console.error('Error creating Zoom meeting:', error.response?.data || error.message);
    throw new Error(
      `Failed to create Zoom meeting: ${error.response?.data?.message || error.message}`
    );
  }
}

/**
 * Add attendees to Zoom meeting (via email notification)
 */
async function addZoomMeetingAttendees(meetingId, attendees, token) {
  // Zoom doesn't have a direct "add attendees" API
  // Attendees are added by sending them the meeting invite link
  // This is handled by the client sending the meetingUrl to attendees

  console.log(
    `Meeting ${meetingId} can be shared with attendees via the join URL`
  );
  return {
    success: true,
    message: `${attendees.length} attendees can be invited via the meeting URL`,
  };
}

/**
 * Get Zoom meeting details
 */
async function getZoomMeeting(meetingId, options) {
  const { zoomClientId, zoomClientSecret, zoomAccountId, zoomUserId = 'me' } = options;

  if (!zoomClientId || !zoomClientSecret || !zoomAccountId) {
    throw new Error('Missing Zoom OAuth credentials or Account ID');
  }

  try {
    const token = await getZoomAccessToken(zoomClientId, zoomClientSecret, zoomAccountId);

    const response = await axios.get(
      `${ZOOM_API_BASE}/users/${zoomUserId}/meetings/${meetingId}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    return {
      success: true,
      data: response.data,
    };
  } catch (error) {
    console.error('Error getting Zoom meeting:', error.response?.data || error.message);
    throw new Error(
      `Failed to get Zoom meeting: ${error.response?.data?.message || error.message}`
    );
  }
}

/**
 * Update Zoom meeting
 */
async function updateZoomMeeting(meetingId, updates, options) {
  const { zoomClientId, zoomClientSecret, zoomAccountId, zoomUserId = 'me' } = options;

  if (!zoomClientId || !zoomClientSecret || !zoomAccountId) {
    throw new Error('Missing Zoom OAuth credentials or Account ID');
  }

  try {
    const token = await getZoomAccessToken(zoomClientId, zoomClientSecret, zoomAccountId);

    const response = await axios.patch(
      `${ZOOM_API_BASE}/users/${zoomUserId}/meetings/${meetingId}`,
      updates,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      }
    );

    return {
      success: true,
      message: 'Meeting updated successfully',
      data: response.data,
    };
  } catch (error) {
    console.error('Error updating Zoom meeting:', error.response?.data || error.message);
    throw new Error(
      `Failed to update Zoom meeting: ${error.response?.data?.message || error.message}`
    );
  }
}

/**
 * Delete Zoom meeting
 */
async function deleteZoomMeeting(meetingId, options) {
  const { zoomClientId, zoomClientSecret, zoomAccountId, zoomUserId = 'me' } = options;

  if (!zoomClientId || !zoomClientSecret || !zoomAccountId) {
    throw new Error('Missing Zoom OAuth credentials or Account ID');
  }

  try {
    const token = await getZoomAccessToken(zoomClientId, zoomClientSecret, zoomAccountId);

    await axios.delete(
      `${ZOOM_API_BASE}/users/${zoomUserId}/meetings/${meetingId}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    return {
      success: true,
      message: 'Meeting deleted successfully',
      meetingId: meetingId,
    };
  } catch (error) {
    console.error('Error deleting Zoom meeting:', error.response?.data || error.message);
    throw new Error(
      `Failed to delete Zoom meeting: ${error.response?.data?.message || error.message}`
    );
  }
}

module.exports = {
  createZoomMeeting,
  getZoomMeeting,
  updateZoomMeeting,
  deleteZoomMeeting,
};
