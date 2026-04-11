package com.mobile.timed.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mobile.timed.R;
import com.mobile.timed.data.models.CalendarModel;
import com.mobile.timed.data.models.EventModel;
import com.mobile.timed.utils.CalendarIntegrationService;
import com.mobile.timed.utils.EventIntegrationService;
import com.mobile.timed.utils.FirebaseInitializer;

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

public class ImportExportActivity extends AppCompatActivity {

    private final SimpleDateFormat icsDateTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
    private final SimpleDateFormat icsDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);

    private EventIntegrationService eventIntegrationService;
    private CalendarIntegrationService calendarIntegrationService;
    private FirebaseInitializer firebaseInitializer;
    private ActivityResultLauncher<String[]> openDocumentLauncher;

    private CheckBox cbExportPersonal;
    private CheckBox cbExportWork;
    private CheckBox cbExportShared;

    private String calendarId = "default_calendar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_import_export);
        setupInsets();

        firebaseInitializer = FirebaseInitializer.getInstance();
        firebaseInitializer.initialize(this);
        eventIntegrationService = new EventIntegrationService();
        calendarIntegrationService = new CalendarIntegrationService();

        icsDateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String incomingCalendarId = getIntent().getStringExtra("calendarId");
        if (incomingCalendarId != null && !incomingCalendarId.isEmpty()) {
            calendarId = incomingCalendarId;
        }

        cbExportPersonal = findViewById(R.id.cbExportPersonal);
        cbExportWork = findViewById(R.id.cbExportWork);
        cbExportShared = findViewById(R.id.cbExportShared);

        openDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFileSelected);

        ImageButton btnBackImport = findViewById(R.id.btnBackImport);
        Button btnChooseFile = findViewById(R.id.btnChooseFile);
        Button btnSyncFromUrl = findViewById(R.id.btnSyncFromUrl);
        Button btnExportSelected = findViewById(R.id.btnExportSelected);
        Button btnExportAll = findViewById(R.id.btnExportAll);

        btnBackImport.setOnClickListener(v -> finish());
        btnChooseFile.setOnClickListener(v -> openDocumentLauncher.launch(new String[]{"text/calendar", "text/csv", "text/plain", "application/octet-stream"}));
        btnSyncFromUrl.setOnClickListener(v -> promptSyncUrl());
        btnExportSelected.setOnClickListener(v -> exportCalendars(false));
        btnExportAll.setOnClickListener(v -> exportCalendars(true));
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
                runOnUiThread(() -> Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                            runOnUiThread(() -> Toast.makeText(this, "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

        ensureCalendarReady(() -> importSequential(items, 0, 0, 0));
    }

    private void importSequential(List<ImportEventItem> items, int index, int successCount, int failureCount) {
        if (index >= items.size()) {
            Toast.makeText(this, "Import done: " + successCount + " success, " + failureCount + " failed", Toast.LENGTH_LONG).show();
            return;
        }

        ImportEventItem item = items.get(index);
        EventIntegrationService.EventSaveListener listener = new EventIntegrationService.EventSaveListener() {
            @Override
            public void onSuccess(String eventId) {
                importSequential(items, index + 1, successCount + 1, failureCount);
            }

            @Override
            public void onError(String errorMessage) {
                importSequential(items, index + 1, successCount, failureCount + 1);
            }
        };

        if (item.recurrenceRule != null && !item.recurrenceRule.isEmpty()) {
            eventIntegrationService.createRecurringEvent(
                    calendarId,
                    item.title,
                    item.startTime,
                    item.endTime,
                    item.recurrenceRule,
                    item.description,
                    item.location,
                    item.allDay,
                    listener
            );
        } else {
            eventIntegrationService.createSingleEvent(
                    calendarId,
                    item.title,
                    item.startTime,
                    item.endTime,
                    item.description,
                    item.location,
                    item.allDay,
                    listener
            );
        }
    }

    private void exportCalendars(boolean exportAll) {
        calendarIntegrationService.getUserCalendars(new CalendarIntegrationService.CalendarLoadListener() {
            @Override
            public void onCalendarsLoaded(List<CalendarModel> calendars) {
                List<CalendarModel> selected = filterCalendars(calendars, exportAll);
                if (selected.isEmpty()) {
                    Toast.makeText(ImportExportActivity.this, "No calendars selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                fetchEventsForExport(selected, 0, new HashMap<>());
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ImportExportActivity.this, "Cannot load calendars: " + errorMessage, Toast.LENGTH_SHORT).show();
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

    private void fetchEventsForExport(List<CalendarModel> calendars, int index, Map<String, List<EventModel>> bucket) {
        if (index >= calendars.size()) {
            showExportFormatChooser(bucket);
            return;
        }

        CalendarModel calendar = calendars.get(index);
        String id = calendar.getId();
        if (id == null || id.isEmpty()) {
            fetchEventsForExport(calendars, index + 1, bucket);
            return;
        }

        eventIntegrationService.getEventsForCalendar(id, new EventIntegrationService.EventLoadListener() {
            @Override
            public void onEventsLoaded(List<EventModel> events) {
                String key = calendar.getName() == null || calendar.getName().isEmpty() ? id : calendar.getName();
                bucket.put(key, events == null ? new ArrayList<>() : events);
                fetchEventsForExport(calendars, index + 1, bucket);
            }

            @Override
            public void onError(String errorMessage) {
                fetchEventsForExport(calendars, index + 1, bucket);
            }
        });
    }

    private void showExportFormatChooser(Map<String, List<EventModel>> data) {
        if (data.isEmpty()) {
            Toast.makeText(this, "No events to export", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] formats = new String[] {"ICS", "CSV"};
        new AlertDialog.Builder(this)
                .setTitle("Choose export format")
                .setItems(formats, (dialog, which) -> {
                    if (which == 0) {
                        shareExport("timed-calendar.ics", buildIcs(data));
                    } else {
                        shareExport("timed-calendar.csv", buildCsv(data));
                    }
                })
                .show();
    }

    private void shareExport(String fileName, String content) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(shareIntent, "Share export"));
    }

    private void ensureCalendarReady(Runnable onReady) {
        if (calendarId != null && !calendarId.isEmpty() && !"default_calendar".equals(calendarId)) {
            onReady.run();
            return;
        }

        calendarIntegrationService.getUserCalendars(new CalendarIntegrationService.CalendarLoadListener() {
            @Override
            public void onCalendarsLoaded(List<CalendarModel> calendars) {
                if (calendars != null && !calendars.isEmpty()) {
                    CalendarModel first = calendars.get(0);
                    if (first.getId() != null && !first.getId().isEmpty()) {
                        calendarId = first.getId();
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
                                Toast.makeText(ImportExportActivity.this, "Cannot prepare calendar: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ImportExportActivity.this, "Cannot load calendars: " + errorMessage, Toast.LENGTH_SHORT).show();
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

            if (i == 0 && line.toLowerCase(Locale.ROOT).contains("title") && line.toLowerCase(Locale.ROOT).contains("start")) {
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

    private String buildIcs(Map<String, List<EventModel>> data) {
        StringBuilder builder = new StringBuilder();
        builder.append("BEGIN:VCALENDAR\n");
        builder.append("VERSION:2.0\n");
        builder.append("PRODID:-//Timed//Calendar Export//EN\n");

        for (Map.Entry<String, List<EventModel>> entry : data.entrySet()) {
            String calendarName = entry.getKey();
            List<EventModel> events = entry.getValue();
            if (events == null) {
                continue;
            }

            for (EventModel event : events) {
                builder.append("BEGIN:VEVENT\n");
                String uid = event.getId() == null || event.getId().isEmpty() ? String.valueOf(System.nanoTime()) : event.getId();
                builder.append("UID:").append(uid).append("@timed\n");
                builder.append("DTSTAMP:").append(icsDateTimeFormat.format(new Date())).append("\n");

                if (event.isAllDay()) {
                    builder.append("DTSTART;VALUE=DATE:").append(icsDateFormat.format(new Date(event.getStartTime()))).append("\n");
                    long endDate = event.getEndTime() > event.getStartTime() ? event.getEndTime() : event.getStartTime() + (24 * 60 * 60 * 1000L);
                    builder.append("DTEND;VALUE=DATE:").append(icsDateFormat.format(new Date(endDate))).append("\n");
                } else {
                    builder.append("DTSTART:").append(icsDateTimeFormat.format(new Date(event.getStartTime()))).append("\n");
                    builder.append("DTEND:").append(icsDateTimeFormat.format(new Date(event.getEndTime()))).append("\n");
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

    private String buildCsv(Map<String, List<EventModel>> data) {
        StringBuilder builder = new StringBuilder();
        builder.append("title,start,end,description,location,allDay,rrule,calendar\n");

        for (Map.Entry<String, List<EventModel>> entry : data.entrySet()) {
            String calendarName = entry.getKey();
            List<EventModel> events = entry.getValue();
            if (events == null) {
                continue;
            }

            for (EventModel event : events) {
                builder.append(csvValue(event.getTitle())).append(',')
                        .append(csvValue(String.valueOf(event.getStartTime()))).append(',')
                        .append(csvValue(String.valueOf(event.getEndTime()))).append(',')
                        .append(csvValue(event.getDescription())).append(',')
                        .append(csvValue(event.getLocation())).append(',')
                        .append(csvValue(String.valueOf(event.isAllDay()))).append(',')
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
