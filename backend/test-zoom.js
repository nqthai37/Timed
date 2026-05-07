const axios = require('axios');
require('dotenv').config();

async function testZoomAuth() {
  const zoomClientId = process.env.ZOOM_CLIENT_ID;
  const zoomClientSecret = process.env.ZOOM_CLIENT_SECRET;
  const zoomAccountId = process.env.ZOOM_ACCOUNT_ID;

  try {
    const authHeader = Buffer.from(`${zoomClientId}:${zoomClientSecret}`).toString('base64');
    
    console.log('Requesting token with account_id:', zoomAccountId);
    const response = await axios.post(
      `https://zoom.us/oauth/token?grant_type=account_credentials&account_id=${zoomAccountId}`,
      null,
      {
        headers: {
          Authorization: `Basic ${authHeader}`,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
      }
    );
    
    console.log('Success!', response.data);
  } catch (error) {
    console.error('Error fetching Zoom access token:');
    if (error.response) {
      console.error('Status:', error.response.status);
      console.error('Data:', error.response.data);
    } else {
      console.error(error.message);
    }
  }
}

testZoomAuth();
