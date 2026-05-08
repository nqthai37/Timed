/**
 * Authentication Handler
 * Exchanges Google authorization code for refresh token
 */

const { google } = require('googleapis');
const axios = require('axios');

/**
 * Exchange Google authorization code for tokens
 * @param {string} code - Authorization code from Google OAuth
 * @param {Object} options - OAuth options
 * @param {string} options.clientId - Google Client ID
 * @param {string} options.clientSecret - Google Client Secret
 * @param {string} options.redirectUrl - Redirect URL
 * @returns {Promise<Object>} Tokens including refresh_token
 */
async function exchangeAuthCodeForTokens(code, options) {
  const { clientId, clientSecret, redirectUrl } = options;

  if (!code) {
    throw new Error('Missing authorization code');
  }

  if (!clientId || !clientSecret || !redirectUrl) {
    throw new Error('Missing OAuth configuration');
  }

  try {
    // Create OAuth2 client
    // For Android Server Auth Code flow, the redirect URI must be empty string
    const oauth2Client = new google.auth.OAuth2(clientId, clientSecret, "");

    let tokens;
    try {
      // Try with empty redirect URI (Android standard)
      const response = await oauth2Client.getToken(code);
      tokens = response.tokens;
    } catch (err) {
      if (err.message && err.message.includes('redirect_uri_mismatch')) {
        // Fallback to configured redirectUrl if it was from a web client
        const fallbackClient = new google.auth.OAuth2(clientId, clientSecret, redirectUrl);
        const fallbackResponse = await fallbackClient.getToken(code);
        tokens = fallbackResponse.tokens;
      } else {
        throw err;
      }
    }

    console.log('Tokens received:', {
      hasAccessToken: !!tokens.access_token,
      hasRefreshToken: !!tokens.refresh_token,
      expiresIn: tokens.expiry_date,
    });

    return {
      success: true,
      accessToken: tokens.access_token,
      refreshToken: tokens.refresh_token,
      expiresIn: tokens.expiry_date,
      tokenType: tokens.token_type,
      scope: tokens.scope,
    };
  } catch (error) {
    console.error('Error exchanging auth code for tokens:', error.message);
    throw new Error(`Failed to exchange authorization code: ${error.message}`);
  }
}

/**
 * Refresh an expired access token using refresh token
 * @param {string} refreshToken - Refresh token
 * @param {Object} options - OAuth options
 * @returns {Promise<Object>} New access token
 */
async function refreshAccessToken(refreshToken, options) {
  const { clientId, clientSecret, redirectUrl } = options;

  if (!refreshToken) {
    throw new Error('Missing refresh token');
  }

  if (!clientId || !clientSecret) {
    throw new Error('Missing OAuth configuration');
  }

  try {
    const oauth2Client = new google.auth.OAuth2(clientId, clientSecret, redirectUrl);

    // Set refresh token
    oauth2Client.setCredentials({
      refresh_token: refreshToken,
    });

    // Refresh the access token
    const { credentials } = await oauth2Client.refreshAccessToken();

    console.log('Access token refreshed');

    return {
      success: true,
      accessToken: credentials.access_token,
      expiresIn: credentials.expiry_date,
    };
  } catch (error) {
    console.error('Error refreshing access token:', error.message);
    throw new Error(`Failed to refresh access token: ${error.message}`);
  }
}

/**
 * Verify and get info about a refresh token
 * @param {string} refreshToken - Refresh token to verify
 * @param {Object} options - OAuth options
 * @returns {Promise<Object>} Token info
 */
async function verifyRefreshToken(refreshToken, options) {
  const { clientId, clientSecret, redirectUrl } = options;

  try {
    const oauth2Client = new google.auth.OAuth2(clientId, clientSecret, redirectUrl);

    oauth2Client.setCredentials({
      refresh_token: refreshToken,
    });

    // Try to refresh to verify it's valid
    const { credentials } = await oauth2Client.refreshAccessToken();

    return {
      success: true,
      valid: true,
      expiresIn: credentials.expiry_date,
      message: 'Refresh token is valid',
    };
  } catch (error) {
    console.error('Error verifying refresh token:', error.message);
    return {
      success: false,
      valid: false,
      message: error.message,
    };
  }
}

module.exports = {
  exchangeAuthCodeForTokens,
  refreshAccessToken,
  verifyRefreshToken,
};
