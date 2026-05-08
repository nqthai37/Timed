package com.timed.activities;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.timed.BuildConfig;
import com.timed.R;
import com.timed.models.CalendarModel;
import com.google.firebase.Timestamp;
import com.timed.managers.EventsManager;
import com.timed.managers.UserManager;
import com.timed.models.Event;
import com.timed.utils.CalendarIntegrationService;
import com.timed.utils.CalendarPermissionUtils;
import com.timed.utils.FirebaseInitializer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

public class ImportExportActivity extends AppCompatActivity {
    private static final String TAG = "ImportExportActivity";

    private final SimpleDateFormat icsDateTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
    private final SimpleDateFormat icsDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);

    private EventsManager eventsManager;
    private CalendarIntegrationService calendarIntegrationService;
    private FirebaseInitializer firebaseInitializer;
    private ActivityResultLauncher<String[]> openDocumentLauncher;
    private final OkHttpClient httpClient = new OkHttpClient();

    private CheckBox cbExportPersonal;
    private CheckBox cbExportWork;
    private CheckBox cbExportShared;
    private LinearLayout layoutExportCalendars;
    private final Map<CheckBox, CalendarModel> exportCalendarOptions = new HashMap<>();

    private String calendarId;
    private String importCalendarName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_import_export);
        setupInsets();

        firebaseInitializer = FirebaseInitializer.getInstance();
        firebaseInitializer.initialize(this);
        eventsManager = EventsManager.getInstance(this);
        calendarIntegrationService = new CalendarIntegrationService();

        icsDateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String incomingCalendarId = getIntent().getStringExtra("calendarId");
        if (incomingCalendarId != null && !incomingCalendarId.isEmpty()) {
            calendarId = incomingCalendarId;
        } else {
            String cachedId = calendarIntegrationService.getCachedDefaultCalendarId(this);
            if (cachedId != null && !cachedId.isEmpty()) {
                calendarId = cachedId;
            }
        }

        cbExportPersonal = findViewById(R.id.cbExportPersonal);
        cbExportWork = findViewById(R.id.cbExportWork);
        cbExportShared = findViewById(R.id.cbExportShared);
        layoutExportCalendars = findViewById(R.id.layoutExportCalendars);

        openDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                this::onFileSelected);

        ImageButton btnBackImport = findViewById(R.id.btnBackImport);
        Button btnChooseFile = findViewById(R.id.btnChooseFile);
        Button btnSyncFromUrl = findViewById(R.id.btnSyncFromUrl);
        Button btnExportSelected = findViewById(R.id.btnExportSelected);
        Button btnExportAll = findViewById(R.id.btnExportAll);
        Button btnUploadExport = findViewById(R.id.btnUploadExport);

        btnBackImport.setOnClickListener(v -> finish());
        btnChooseFile.setOnClickListener(v -> openDocumentLauncher
                .launch(new String[] { "text/calendar", "text/csv", "text/plain", "application/octet-stream" }));
        btnSyncFromUrl.setOnClickListener(v -> promptSyncUrl());
        btnExportSelected.setOnClickListener(v -> exportCalendars(false, false));
        btnExportAll.setOnClickListener(v -> exportCalendars(true, false));
        btnUploadExport.setOnClickListener(v -> exportCalendars(false, true));
        loadExportCalendarOptions();
    }

    private void setupInsets() {
        View root = findViewById(R.id.rootImportExport);
        View topBar = findViewById(R.id.topBar);
        if (root == null || topBar == null) {
            return;
        }

        final int baseTopBarHeight = dpToPx(56);
        final int baseTopPadding = topBar.getPaddingTop();
        final int baseBottomPadding = topBar.getPaddingBottom();
        final int baseLeftPadding = topBar.getPaddingLeft();
        final int baseRightPadding = topBar.getPaddingRight();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            topBar.setPadding(baseLeftPadding, baseTopPadding + bars.top, baseRightPadding, baseBottomPadding);
            ViewGroup.LayoutParams lp = topBar.getLayoutParams();
            lp.height = baseTopBarHeight + bars.top;
            topBar.setLayoutParams(lp);

            v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void onFileSelected(Uri uri) {
        if (uri == null) {
            return;
        }

        Toast.makeText(this, "Importing file...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String content = readUriAsText(uri);
                List<ImportEventItem> items = parseImportContent(content);
                runOnUiThread(() -> importItems(items));
            } catch (Exception e) {
                runOnUiThread(
                        () -> Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void promptSyncUrl() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://example.com/calendar.ics");

        new AlertDialog.Builder(this)
                .setTitle("Sync from URL")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sync", (dialog, which) -> {
                    String url = input.getText() == null ? "" : input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(this, "URL is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        try {
                            String content = fetchUrl(url);
                            List<ImportEventItem> items = parseImportContent(content);
                            runOnUiThread(() -> importItems(items));
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast
                                    .makeText(this, "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .show();
    }

    private void importItems(List<ImportEventItem> items) {
        if (items == null || items.isEmpty()) {
            Toast.makeText(this, "No events found to import", Toast.LENGTH_SHORT).show();
            return;
        }

        prepareImportedCalendar(() -> importSequential(items, 0, 0, 0));
    }

    private void importSequential(List<ImportEventItem> items, int index, int successCount, int failureCount) {
        if (index >= items.size()) {
            Toast.makeText(this, "Import done: " + successCount + " events imported, " + failureCount + " events failed",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ImportEventItem item = items.get(index);
        Event event = new Event();
        event.setCalendarId(calendarId);
        event.setCalendarName(importCalendarName);
        event.setColor("#741ce9");
        event.setTitle(item.title);
        event.setDescription(item.description);
        event.setLocation(item.location);
        event.setAllDay(item.allDay);
        event.setRecurrenceRule(item.recurrenceRule);
        event.setStartTime(new Timestamp(new Date(item.startTime)));
        event.setEndTime(new Timestamp(new Date(item.endTime)));
        applyImportOwnership(event);

        eventsManager.createEvent(event)
                .addOnSuccessListener(docRef -> importSequential(items, index + 1, successCount + 1, failureCount))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to import event: " + item.title, e);
                    importSequential(items, index + 1, successCount, failureCount + 1);
                });
    }

    private void applyImportOwnership(Event event) {
        String userId = firebaseInitializer.getCurrentUserId();
        if ((userId == null || userId.isEmpty()) && UserManager.getInstance().getCurrentUser() != null) {
            userId = UserManager.getInstance().getCurrentUser().getUid();
        }

        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Importing event without current user ownership");
            return;
        }

        event.setCreatedBy(userId);
        if (!event.getParticipantId().contains(userId)) {
            event.getParticipantId().add(userId);
        }
        event.getParticipantStatus().put(userId, "accepted");
    }

    private void prepareImportedCalendar(Runnable onReady) {
        calendarIntegrationService.getUserCalendars(new CalendarIntegrationService.CalendarLoadListener() {
            @Override
            public void onCalendarsLoaded(List<CalendarModel> calendars) {
                createImportedCalendar(nextImportedCalendarName(calendars), onReady);
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Could not load calendars before import: " + errorMessage);
                createImportedCalendar("Imported Calendar 1", onReady);
            }
        });
    }

    private void createImportedCalendar(String name, Runnable onReady) {
        calendarIntegrationService.createCalendar(name, "Imported events", "#741ce9", false,
                new CalendarIntegrationService.CalendarSaveListener() {
                    @Override
                    public void onSuccess(String createdCalendarId) {
                        calendarId = createdCalendarId;
                        importCalendarName = name;
                        calendarIntegrationService.setCalendarVisibility(ImportExportActivity.this, createdCalendarId,
                                true);
                        onReady.run();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(ImportExportActivity.this, "Cannot create import calendar: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String nextImportedCalendarName(List<CalendarModel> calendars) {
        int maxNumber = 0;
        if (calendars != null) {
            for (CalendarModel calendar : calendars) {
                String name = calendar == null ? null : calendar.getName();
                int number = importedCalendarNumber(name);
                if (number > maxNumber) {
                    maxNumber = number;
                }
            }
        }
        return "Imported Calendar " + (maxNumber + 1);
    }

    private int importedCalendarNumber(String name) {
        if (name == null) {
            return 0;
        }

        String prefix = "Imported Calendar ";
        if (!name.startsWith(prefix)) {
            return 0;
        }

        try {
            return Integer.parseInt(name.substring(prefix.length()).trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void exportCalendars(boolean exportAll, boolean uploadToCloud) {
        calendarIntegrationService.getUserCalendars(new CalendarIntegrationService.CalendarLoadListener() {
            @Override
            public void onCalendarsLoaded(List<CalendarModel> calendars) {
                List<CalendarModel> selected = filterCalendars(calendars, exportAll);
                if (selected.isEmpty()) {
                    Toast.makeText(ImportExportActivity.this, "No calendars selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                fetchEventsForExport(selected, 0, new HashMap<>(), uploadToCloud);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ImportExportActivity.this, "Cannot load calendars: " + errorMessage, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private List<CalendarModel> filterCalendars(List<CalendarModel> calendars, boolean exportAll) {
        List<CalendarModel> result = new ArrayList<>();
        if (calendars == null) {
            return result;
        }

        if (exportAll) {
            result.addAll(calendars);
            return result;
        }

        if (!exportCalendarOptions.isEmpty()) {
            for (Map.Entry<CheckBox, CalendarModel> entry : exportCalendarOptions.entrySet()) {
                if (entry.getKey().isChecked()) {
                    result.add(entry.getValue());
                }
            }
            return result;
        }

        for (CalendarModel calendar : calendars) {
            String name = calendar.getName() == null ? "" : calendar.getName().toLowerCase(Locale.ROOT);
            if (cbExportPersonal.isChecked() && name.contains("personal")) {
                result.add(calendar);
                continue;
            }
            if (cbExportWork.isChecked() && (name.contains("work") || name.contains("meeting"))) {
                result.add(calendar);
                continue;
            }
            if (cbExportShared.isChecked() && (name.contains("shared") || name.contains("family"))) {
                result.add(calendar);
            }
        }

        if (result.isEmpty()) {
            for (CalendarModel calendar : calendars) {
                if (calendar.getId() != null && calendar.getId().equals(calendarId)) {
                    result.add(calendar);
                    break;
                }
            }
        }

        return result;
    }

    private void fetchEventsForExport(List<CalendarModel> calendars, int index, Map<String, List<Event>> bucket,
            boolean uploadToCloud) {
        if (index >= calendars.size()) {
            showExportFormatChooser(bucket, uploadToCloud);
            return;
        }

        CalendarModel calendar = calendars.get(index);
        String id = calendar.getId();
        if (id == null || id.isEmpty()) {
            fetchEventsForExport(calendars, index + 1, bucket, uploadToCloud);
            return;
        }

        eventsManager.getEventsByCalendarId(id)
                .addOnSuccessListener(events -> {
                    String key = calendar.getName() == null || calendar.getName().isEmpty() ? id : calendar.getName();
                    bucket.put(key, events == null ? new ArrayList<>() : events);
                    fetchEventsForExport(calendars, index + 1, bucket, uploadToCloud);
                })
                .addOnFailureListener(e -> fetchEventsForExport(calendars, index + 1, bucket, uploadToCloud));
    }

    private void showExportFormatChooser(Map<String, List<Event>> data, boolean uploadToCloud) {
        if (data.isEmpty()) {
            Toast.makeText(this, "No events to export", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] formats = new String[] { "ICS", "CSV" };
        new AlertDialog.Builder(this)
                .setTitle("Choose export format")
                .setItems(formats, (dialog, which) -> {
                    if (which == 0) {
                        handleExportResult("timed-calendar.ics", buildIcs(data), "text/calendar", uploadToCloud);
                    } else {
                        handleExportResult("timed-calendar.csv", buildCsv(data), "text/csv", uploadToCloud);
                    }
                })
                .show();
    }

    private void handleExportResult(String fileName, String content, String mimeType, boolean uploadToCloud) {
        if (uploadToCloud) {
            uploadExportToCloud(fileName, content, mimeType);
        } else {
            shareExport(fileName, content);
        }
    }

    private void shareExport(String fileName, String content) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(shareIntent, "Share export"));
    }

    private void uploadExportToCloud(String fileName, String content, String mimeType) {
        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isEmpty() || BuildConfig.CLOUDINARY_UPLOAD_PRESET.isEmpty()) {
            Toast.makeText(this, "Cloudinary upload preset is not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading export...", Toast.LENGTH_SHORT).show();
        RequestBody fileBody = RequestBody.create(content.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                MediaType.parse(mimeType));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                .addFormDataPart("resource_type", "raw")
                .build();

        String uploadUrl = "https://api.cloudinary.com/v1_1/" + BuildConfig.CLOUDINARY_CLOUD_NAME + "/raw/upload";
        Request request = new Request.Builder().url(uploadUrl).post(requestBody).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                runOnUiThread(() -> Toast.makeText(ImportExportActivity.this,
                        "Cloud upload failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ImportExportActivity.this,
                            "Cloud upload failed", Toast.LENGTH_SHORT).show());
                    return;
                }
                try {
                    String url = new JSONObject(body).optString("secure_url");
                    runOnUiThread(() -> showCloudExportUrl(url));
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ImportExportActivity.this,
                            "Invalid cloud upload response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showCloudExportUrl(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Cloud upload did not return a URL", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cloud export ready")
                .setMessage(url)
                .setPositiveButton("Copy URL", (dialog, which) -> copyToClipboard(url))
                .setNegativeButton("Share", (dialog, which) -> shareExportUrl(url))
                .setNeutralButton("Import URL", (dialog, which) -> importFromUrl(url))
                .show();
    }

    private void copyToClipboard(String url) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Timed calendar export", url));
            Toast.makeText(this, "URL copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareExportUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(intent, "Share export URL"));
    }

    private void importFromUrl(String url) {
        Toast.makeText(this, "Importing from cloud URL...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String content = fetchUrl(url);
                List<ImportEventItem> items = parseImportContent(content);
                runOnUiThread(() -> importItems(items));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void ensureCalendarReady(Runnable onReady) {
        if (calendarId != null && !calendarId.isEmpty()) {
            calendarIntegrationService.getCalendar(calendarId,
                    new CalendarIntegrationService.CalendarLoadDetailListener() {
                        @Override
                        public void onCalendarLoaded(CalendarModel calendar) {
                            if (CalendarPermissionUtils.canWrite(calendar, firebaseInitializer.getCurrentUserId())) {
                                onReady.run();
                            } else {
                                calendarId = null;
                                ensureCalendarReady(onReady);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            calendarId = null;
                            ensureCalendarReady(onReady);
                        }
                    });
            return;
        }

        calendarIntegrationService.getUserCalendars(new CalendarIntegrationService.CalendarLoadListener() {
            @Override
            public void onCalendarsLoaded(List<CalendarModel> calendars) {
                if (calendars != null && !calendars.isEmpty()) {
                    String userId = firebaseInitializer.getCurrentUserId();
                    for (CalendarModel calendar : calendars) {
                        if (CalendarPermissionUtils.canWrite(calendar, userId)
                                && calendar.getId() != null && !calendar.getId().isEmpty()) {
                            calendarId = calendar.getId();
                            break;
                        }
                    }
                    if (calendarId == null || calendarId.isEmpty()) {
                        Toast.makeText(ImportExportActivity.this, "No editable calendars available",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    onReady.run();
                    return;
                }

                calendarIntegrationService.createCalendar("My Calendar", "Default personal calendar", "#741ce9", false,
                        new CalendarIntegrationService.CalendarSaveListener() {
                            @Override
                            public void onSuccess(String createdCalendarId) {
                                calendarId = createdCalendarId;
                                onReady.run();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(ImportExportActivity.this, "Cannot prepare calendar: " + errorMessage,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ImportExportActivity.this, "Cannot load calendars: " + errorMessage, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private String readUriAsText(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IllegalStateException("Cannot open file");
        }

        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        reader.close();
        inputStream.close();
        return builder.toString();
    }

    private String fetchUrl(String rawUrl) throws Exception {
        URL url = new URL(rawUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.connect();

        if (connection.getResponseCode() >= 400) {
            throw new IllegalStateException("HTTP " + connection.getResponseCode());
        }

        InputStream inputStream = connection.getInputStream();
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        reader.close();
        inputStream.close();
        connection.disconnect();
        return builder.toString();
    }

    private List<ImportEventItem> parseImportContent(String content) {
        if (content == null) {
            return new ArrayList<>();
        }

        String trimmed = content.trim();
        if (trimmed.startsWith("BEGIN:VCALENDAR") || trimmed.contains("BEGIN:VEVENT")) {
            return parseIcs(trimmed);
        }
        return parseCsv(trimmed);
    }

    private List<ImportEventItem> parseIcs(String content) {
        List<ImportEventItem> items = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");
        ImportEventItem current = null;

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.equals("BEGIN:VEVENT")) {
                current = new ImportEventItem();
                continue;
            }
            if (line.equals("END:VEVENT")) {
                if (current != null && current.title != null && !current.title.isEmpty()) {
                    if (current.startTime <= 0) {
                        current.startTime = System.currentTimeMillis();
                    }
                    if (current.endTime <= current.startTime) {
                        current.endTime = current.startTime + (30 * 60 * 1000L);
                    }
                    items.add(current);
                }
                current = null;
                continue;
            }

            if (current == null) {
                continue;
            }

            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }

            String key = line.substring(0, separator);
            String value = line.substring(separator + 1).trim();

            if (key.startsWith("SUMMARY")) {
                current.title = unescapeIcs(value);
            } else if (key.startsWith("DESCRIPTION")) {
                current.description = unescapeIcs(value);
            } else if (key.startsWith("LOCATION")) {
                current.location = unescapeIcs(value);
            } else if (key.startsWith("RRULE")) {
                current.recurrenceRule = value;
            } else if (key.startsWith("DTSTART")) {
                ParsedTime parsedTime = parseIcsTime(value);
                current.startTime = parsedTime.timeMillis;
                current.allDay = parsedTime.allDay;
            } else if (key.startsWith("DTEND")) {
                ParsedTime parsedTime = parseIcsTime(value);
                current.endTime = parsedTime.timeMillis;
            }
        }

        return items;
    }

    private List<ImportEventItem> parseCsv(String content) {
        List<ImportEventItem> items = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            if (i == 0 && line.toLowerCase(Locale.ROOT).contains("title")
                    && line.toLowerCase(Locale.ROOT).contains("start")) {
                continue;
            }

            String[] parts = splitCsvLine(line);
            if (parts.length < 3) {
                continue;
            }

            ImportEventItem item = new ImportEventItem();
            item.title = safeCsv(parts, 0);
            item.startTime = parseFlexibleTime(safeCsv(parts, 1));
            item.endTime = parseFlexibleTime(safeCsv(parts, 2));
            item.description = safeCsv(parts, 3);
            item.location = safeCsv(parts, 4);
            item.allDay = Boolean.parseBoolean(safeCsv(parts, 5));
            item.recurrenceRule = safeCsv(parts, 6);

            if (item.title == null || item.title.isEmpty()) {
                continue;
            }
            if (item.startTime <= 0) {
                item.startTime = System.currentTimeMillis();
            }
            if (item.endTime <= item.startTime) {
                item.endTime = item.startTime + (30 * 60 * 1000L);
            }

            items.add(item);
        }

        return items;
    }

    private String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (c == ',' && !inQuotes) {
                tokens.add(token.toString().trim());
                token.setLength(0);
                continue;
            }

            token.append(c);
        }
        tokens.add(token.toString().trim());

        return tokens.toArray(new String[0]);
    }

    private String safeCsv(String[] parts, int index) {
        if (index < 0 || index >= parts.length) {
            return "";
        }
        return parts[index] == null ? "" : parts[index].trim();
    }

    private ParsedTime parseIcsTime(String value) {
        ParsedTime parsedTime = new ParsedTime();
        if (value == null || value.isEmpty()) {
            return parsedTime;
        }

        try {
            if (value.length() == 8) {
                Date date = icsDateFormat.parse(value);
                parsedTime.timeMillis = date == null ? 0 : date.getTime();
                parsedTime.allDay = true;
                return parsedTime;
            }

            Date date = icsDateTimeFormat.parse(value);
            parsedTime.timeMillis = date == null ? 0 : date.getTime();
            parsedTime.allDay = false;
        } catch (ParseException ignored) {
            parsedTime.timeMillis = parseFlexibleTime(value);
        }

        return parsedTime;
    }

    private long parseFlexibleTime(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        try {
            return Long.parseLong(input);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Instant.parse(input).toEpochMilli();
        } catch (Exception ignored) {
        }

        String[] patterns = new String[] {
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm",
                "dd/MM/yyyy HH:mm",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
                Date parsed = format.parse(input);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (ParseException ignored) {
            }
        }

        return 0;
    }

    private String buildIcs(Map<String, List<Event>> data) {
        StringBuilder builder = new StringBuilder();
        builder.append("BEGIN:VCALENDAR\n");
        builder.append("VERSION:2.0\n");
        builder.append("PRODID:-//Timed//Calendar Export//EN\n");

        for (Map.Entry<String, List<Event>> entry : data.entrySet()) {
            String calendarName = entry.getKey();
            List<Event> events = entry.getValue();
            if (events == null) {
                continue;
            }

            for (Event event : events) {
                builder.append("BEGIN:VEVENT\n");
                String uid = event.getId() == null || event.getId().isEmpty() ? String.valueOf(System.nanoTime())
                        : event.getId();
                builder.append("UID:").append(uid).append("@timed\n");
                builder.append("DTSTAMP:").append(icsDateTimeFormat.format(new Date())).append("\n");

                boolean allDay = event.getAllDay() != null && event.getAllDay();
                long startMillis = event.getStartTime() != null ? event.getStartTime().toDate().getTime() : 0L;
                long endMillis = event.getEndTime() != null ? event.getEndTime().toDate().getTime() : startMillis;

                if (allDay) {
                    builder.append("DTSTART;VALUE=DATE:").append(icsDateFormat.format(new Date(startMillis)))
                            .append("\n");
                    long endDate = endMillis > startMillis ? endMillis : startMillis + (24 * 60 * 60 * 1000L);
                    builder.append("DTEND;VALUE=DATE:").append(icsDateFormat.format(new Date(endDate))).append("\n");
                } else {
                    builder.append("DTSTART:").append(icsDateTimeFormat.format(new Date(startMillis))).append("\n");
                    builder.append("DTEND:").append(icsDateTimeFormat.format(new Date(endMillis))).append("\n");
                }

                builder.append("SUMMARY:").append(escapeIcs(event.getTitle())).append("\n");
                if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                    builder.append("DESCRIPTION:").append(escapeIcs(event.getDescription())).append("\n");
                }
                if (event.getLocation() != null && !event.getLocation().isEmpty()) {
                    builder.append("LOCATION:").append(escapeIcs(event.getLocation())).append("\n");
                }
                if (event.getRecurrenceRule() != null && !event.getRecurrenceRule().isEmpty()) {
                    builder.append("RRULE:").append(event.getRecurrenceRule()).append("\n");
                }
                builder.append("CATEGORIES:").append(escapeIcs(calendarName)).append("\n");
                builder.append("END:VEVENT\n");
            }
        }

        builder.append("END:VCALENDAR\n");
        return builder.toString();
    }

    private String buildCsv(Map<String, List<Event>> data) {
        StringBuilder builder = new StringBuilder();
        builder.append("title,start,end,description,location,allDay,rrule,calendar\n");

        for (Map.Entry<String, List<Event>> entry : data.entrySet()) {
            String calendarName = entry.getKey();
            List<Event> events = entry.getValue();
            if (events == null) {
                continue;
            }

            for (Event event : events) {
                long startMillis = event.getStartTime() != null ? event.getStartTime().toDate().getTime() : 0L;
                long endMillis = event.getEndTime() != null ? event.getEndTime().toDate().getTime() : 0L;
                builder.append(csvValue(event.getTitle())).append(',')
                        .append(csvValue(String.valueOf(startMillis))).append(',')
                        .append(csvValue(String.valueOf(endMillis))).append(',')
                        .append(csvValue(event.getDescription())).append(',')
                        .append(csvValue(event.getLocation())).append(',')
                        .append(csvValue(String.valueOf(event.getAllDay() != null && event.getAllDay()))).append(',')
                        .append(csvValue(event.getRecurrenceRule())).append(',')
                        .append(csvValue(calendarName))
                        .append("\n");
            }
        }

        return builder.toString();
    }

    private String escapeIcs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }

    private String unescapeIcs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\n", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }

    private String csvValue(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private void loadExportCalendarOptions() {
        calendarIntegrationService.getUserCalendars(new CalendarIntegrationService.CalendarLoadListener() {
            @Override
            public void onCalendarsLoaded(List<CalendarModel> calendars) {
                renderExportCalendarOptions(calendars);
            }

            @Override
            public void onError(String errorMessage) {
            }
        });
    }

    private void renderExportCalendarOptions(List<CalendarModel> calendars) {
        if (layoutExportCalendars == null || calendars == null || calendars.isEmpty()) {
            return;
        }
        exportCalendarOptions.clear();
        layoutExportCalendars.removeAllViews();
        layoutExportCalendars.setVisibility(View.VISIBLE);
        cbExportPersonal.setVisibility(View.GONE);
        cbExportWork.setVisibility(View.GONE);
        cbExportShared.setVisibility(View.GONE);

        for (CalendarModel calendar : calendars) {
            if (calendar == null || calendar.getId() == null || calendar.getId().isEmpty()) {
                continue;
            }
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(calendar.getName() == null || calendar.getName().trim().isEmpty()
                    ? "Calendar" : calendar.getName().trim());
            checkBox.setTextColor(getColor(R.color.slate_900));
            checkBox.setChecked(true);
            layoutExportCalendars.addView(checkBox);
            exportCalendarOptions.put(checkBox, calendar);
        }
    }

    private static class ParsedTime {
        long timeMillis;
        boolean allDay;
    }

    private static class ImportEventItem {
        String title = "";
        String description = "";
        String location = "";
        String recurrenceRule = "";
        long startTime;
        long endTime;
        boolean allDay;
    }
}
