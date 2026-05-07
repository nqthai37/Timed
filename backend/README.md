# Timed Calendar Backend

Backend service for creating Google Calendar events with automatic Google Meet link generation for the Timed mobile application.

## Features

✅ Create Google Calendar events with automatic Google Meet conference links  
✅ Update existing calendar events  
✅ Delete calendar events  
✅ Support for multiple attendees  
✅ Batch event creation  
✅ Express.js REST API  
✅ OAuth2 authentication  

## Prerequisites

- Node.js >= 14.0.0
- npm or yarn
- Google Cloud Project with Calendar API enabled
- Google OAuth2 credentials (Client ID, Client Secret)

## Setup Instructions

### 1. Install Dependencies

```bash
cd backend
npm install
```

### 2. Google Cloud Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project
3. Enable the **Google Calendar API**:
   - Navigate to "APIs & Services" → "Library"
   - Search for "Google Calendar API"
   - Click "Enable"
4. Create OAuth2 credentials:
   - Go to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "OAuth 2.0 Client ID"
   - Select "Web Application"
   - Add authorized redirect URIs:
     - `http://localhost:3000/auth/callback` (for development)
     - `https://yourdomain.com/auth/callback` (for production)
   - Copy the Client ID and Client Secret

### 3. Configure Environment Variables

1. Copy the example environment file:
```bash
cp .env.example .env
```

2. Edit `.env` and add your Google OAuth credentials:
```env
NODE_ENV=development
PORT=3000
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
GOOGLE_REDIRECT_URL=http://localhost:3000/auth/callback
```

## Usage

### Start the Server

```bash
npm start
```

For development with auto-reload:
```bash
npm run dev
```

### API Endpoints

#### 1. Create Calendar Event with Google Meet

**POST** `/api/calendar/create-event`

Request body:
```json
{
  "summary": "Team Meeting",
  "startTime": "2026-05-15T10:00:00Z",
  "endTime": "2026-05-15T11:00:00Z",
  "userRefreshToken": "your_user_refresh_token",
  "description": "Weekly team sync",
  "location": "Online",
  "attendees": ["team@example.com", "manager@example.com"]
}
```

Response:
```json
{
  "success": true,
  "eventId": "abc123xyz",
  "htmlLink": "https://calendar.google.com/calendar/event?eid=abc123xyz",
  "hangoutLink": "https://meet.google.com/xxx-yyyy-zzz",
  "summary": "Team Meeting",
  "startTime": "2026-05-15T10:00:00Z",
  "endTime": "2026-05-15T11:00:00Z",
  "conferenceData": {
    "conferenceId": "meet_conference_id",
    "entryPoints": [
      {
        "entryPointType": "video",
        "uri": "https://meet.google.com/xxx-yyyy-zzz"
      }
    ]
  }
}
```

#### 2. Update Calendar Event

**PUT** `/api/calendar/update-event`

Request body:
```json
{
  "eventId": "abc123xyz",
  "userRefreshToken": "your_user_refresh_token",
  "updates": {
    "summary": "Updated Meeting Title",
    "description": "Updated description",
    "location": "Conference Room A"
  }
}
```

#### 3. Delete Calendar Event

**DELETE** `/api/calendar/delete-event`

Request body:
```json
{
  "eventId": "abc123xyz",
  "userRefreshToken": "your_user_refresh_token"
}
```

#### 4. Health Check

**GET** `/api/health`

Response:
```json
{
  "status": "ok",
  "timestamp": "2026-05-08T12:00:00.000Z",
  "message": "Timed Calendar Backend Server is running"
}
```

## Using the Handler in Your Code

### Direct Handler Usage

```javascript
const { createCalendarEventWithMeet } = require('./handlers/calendarEventHandler');

const result = await createCalendarEventWithMeet(
  {
    summary: 'Team Meeting',
    startTime: '2026-05-15T10:00:00Z',
    endTime: '2026-05-15T11:00:00Z',
    userRefreshToken: 'user_refresh_token',
    description: 'Weekly sync',
    attendees: ['team@example.com']
  },
  {
    clientId: process.env.GOOGLE_CLIENT_ID,
    clientSecret: process.env.GOOGLE_CLIENT_SECRET,
    redirectUrl: process.env.GOOGLE_REDIRECT_URL
  }
);

console.log('Google Meet Link:', result.hangoutLink);
console.log('Event Link:', result.htmlLink);
```

### Via REST API from Mobile

**Kotlin/Java (Android):**
```kotlin
// Using Retrofit or OkHttp
val request = CreateEventRequest(
    summary = "Team Meeting",
    startTime = "2026-05-15T10:00:00Z",
    endTime = "2026-05-15T11:00:00Z",
    userRefreshToken = userToken,
    description = "Weekly sync",
    attendees = listOf("team@example.com")
)

val response = apiService.createCalendarEvent(request)
val googleMeetLink = response.hangoutLink
val eventLink = response.htmlLink
```

**JavaScript/TypeScript:**
```typescript
const response = await fetch('http://localhost:3000/api/calendar/create-event', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    summary: 'Team Meeting',
    startTime: '2026-05-15T10:00:00Z',
    endTime: '2026-05-15T11:00:00Z',
    userRefreshToken: userToken,
    description: 'Weekly sync',
    attendees: ['team@example.com']
  })
});

const result = await response.json();
console.log('Google Meet Link:', result.hangoutLink);
```

## Implementation Details

### Key Features

1. **Google Meet Integration**
   - Uses `conferenceData` with `createRequest`
   - Sets `conferenceSolutionKey` to `'hangoutsMeet'`
   - Includes `conferenceDataVersion: 1` parameter

2. **User Refresh Token**
   - Used to obtain access tokens without requiring manual login
   - Securely stored on your backend
   - Never exposed to the mobile client

3. **Response Data**
   - `htmlLink`: Direct link to the calendar event
   - `hangoutLink`: Direct link to the Google Meet conference
   - Conference entry points and details

4. **Error Handling**
   - Comprehensive error messages
   - Validation of required parameters
   - OAuth credential verification

## Security Best Practices

1. **Never expose refresh tokens on the client side**
   - Store refresh tokens securely on the backend
   - Always authenticate users before creating events

2. **Use environment variables for credentials**
   - Never commit `.env` file to version control
   - Use `.env.example` as a template

3. **Validate all inputs**
   - Check event times (end time must be after start time)
   - Validate email addresses
   - Sanitize user input

4. **Use HTTPS in production**
   - Always use HTTPS for production deployments
   - Keep OAuth credentials confidential

## Deployment

### Heroku

1. Create a Heroku app:
```bash
heroku create your-app-name
```

2. Set environment variables:
```bash
heroku config:set GOOGLE_CLIENT_ID=your_client_id
heroku config:set GOOGLE_CLIENT_SECRET=your_client_secret
heroku config:set GOOGLE_REDIRECT_URL=https://your-app-name.herokuapp.com/auth/callback
```

3. Deploy:
```bash
git push heroku main
```

### Docker

1. Create a `Dockerfile`:
```dockerfile
FROM node:18
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 3000
CMD ["npm", "start"]
```

2. Build and run:
```bash
docker build -t timed-calendar-backend .
docker run -p 3000:3000 --env-file .env timed-calendar-backend
```

## Troubleshooting

### "Invalid refresh token"
- Ensure the refresh token is valid and not expired
- Request a new refresh token from the user

### "Calendar API not enabled"
- Go to Google Cloud Console
- Enable the Google Calendar API
- Wait a few minutes for changes to propagate

### "Unauthorized redirect URL"
- Add the redirect URL to your OAuth2 credentials in Google Cloud Console
- Include the full URL with protocol and port

## Documentation

- [Google Calendar API Docs](https://developers.google.com/calendar/api)
- [Google Meet API Integration](https://developers.google.com/calendar/api/guides/create-events#conference_data)
- [OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)

## License

MIT

## Support

For issues or questions, please create an issue in the repository.
