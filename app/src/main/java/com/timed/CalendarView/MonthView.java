package com.timed.CalendarView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.timed.R;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.activities.CreateEventActivity;
import com.timed.adapters.CalendarAdapter;
import com.timed.adapters.EventAdapter;
import com.timed.managers.EventSyncManager;
import com.timed.models.CalendarDay;
import com.timed.models.Event;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonthView extends Fragment implements CalendarAdapter.OnItemListener {

    private LocalDate selectedDate;
    private List<String> visibleCalendarIds;
    private EventSyncManager eventSyncManager;

    private RecyclerView rvCalendar;
    private TextView tvCurrentMonth;
    private RecyclerView rvEvents;
    private TextView tvUpcomingTitle;

    private EventAdapter eventAdapter;
    private List<Event> currentEvents;
    private List<CalendarDay> currentMonthDays;
    private CalendarAdapter calendarAdapter;
    private View rootView;

    public MonthView(LocalDate selectedDate, List<String> visibleCalendarIds) {
        this.selectedDate = selectedDate;
        this.visibleCalendarIds = visibleCalendarIds;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_month, container, false);
        eventSyncManager = new EventSyncManager(requireContext());

        rvCalendar = rootView.findViewById(R.id.rvCalendar);
        tvCurrentMonth = rootView.findViewById(R.id.tvCurrentMonth);
        rvEvents = rootView.findViewById(R.id.rvEvents);
        tvUpcomingTitle = rootView.findViewById(R.id.tvUpcomingTitle);

        ImageButton btnPrevMonth = rootView.findViewById(R.id.btnPrevMonth);
        ImageButton btnNextMonth = rootView.findViewById(R.id.btnNextMonth);

        rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));

        currentEvents = new ArrayList<>();
        eventAdapter = new EventAdapter(currentEvents, this::openEditEvent, true);
        rvEvents.setAdapter(eventAdapter);

        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                selectedDate = selectedDate.minusMonths(1);
                updateMonthYearText();
                setMonthView();
            });
        }
        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                selectedDate = selectedDate.plusMonths(1);
                updateMonthYearText();
                setMonthView();
            });
        }

        updateMonthYearText();
        setMonthView();
        updateEventsForDate(selectedDate);

        return rootView;
    }

    private void updateMonthYearText() {
        if (tvCurrentMonth != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            tvCurrentMonth.setText(selectedDate.format(formatter));
        }

        // Update the top title bar in the Activity
        if (getActivity() != null) {
            TextView tvTopTitle = getActivity().findViewById(R.id.tvTopTitle);
            if (tvTopTitle != null) {
                DateTimeFormatter titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
                tvTopTitle.setText(selectedDate.format(titleFormatter));
            }
        }
    }

    private void setMonthView() {
        currentMonthDays = daysInMonthArray(selectedDate);
        calendarAdapter = new CalendarAdapter(currentMonthDays, this);
        rvCalendar.setAdapter(calendarAdapter);
        loadMonthEventIndicators(selectedDate, currentMonthDays, calendarAdapter);
    }

    private List<CalendarDay> daysInMonthArray(LocalDate date) {
        List<CalendarDay> daysInMonthArray = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate firstOfMonth = date.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        int offset = (dayOfWeek == 7) ? 0 : dayOfWeek;

        for (int i = 1; i <= 42; i++) {
            if (i <= offset) {
                LocalDate prevMonthDate = firstOfMonth.minusDays(offset - i + 1);
                daysInMonthArray.add(new CalendarDay(prevMonthDate, false));
            } else if (i > daysInMonth + offset) {
                LocalDate nextMonthDate = firstOfMonth.plusDays(i - offset - 1);
                daysInMonthArray.add(new CalendarDay(nextMonthDate, false));
            } else {
                LocalDate currentMonthDate = firstOfMonth.plusDays(i - offset - 1);
                CalendarDay day = new CalendarDay(currentMonthDate, true);
                if (currentMonthDate != null) {
                    if (selectedDate != null && currentMonthDate.equals(selectedDate))
                        day.isSelected = true;
                    if (currentMonthDate.equals(LocalDate.now()))
                        day.isToday = true;
                }
                daysInMonthArray.add(day);
            }
        }
        return daysInMonthArray;
    }

    private void loadMonthEventIndicators(LocalDate month, List<CalendarDay> days, CalendarAdapter adapter) {
        if (visibleCalendarIds == null || visibleCalendarIds.isEmpty()) return;

        LocalDate startOfMonth = month.withDayOfMonth(1);
        LocalDate endOfMonth = month.withDayOfMonth(month.lengthOfMonth());

        eventSyncManager.fetchEventsForCalendars(startOfMonth, endOfMonth, visibleCalendarIds, events -> {
            if (getContext() == null) return;

            Map<LocalDate, Integer> counts = new HashMap<>();
            for (Event event : events) {
                if (event == null || event.getStartTime() == null) continue;
                LocalDate eventDate = toLocalDate(event.getStartTime());
                if (eventDate == null) continue;
                counts.put(eventDate, counts.getOrDefault(eventDate, 0) + 1);
            }

            for (CalendarDay day : days) {
                if (day != null && day.date != null) {
                    day.eventCount = counts.getOrDefault(day.date, 0);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onItemClick(int position, LocalDate date) {
        if (date != null) {
            YearMonth visibleMonth = YearMonth.from(selectedDate);
            selectedDate = date;
            if (YearMonth.from(date).equals(visibleMonth) && calendarAdapter != null) {
                calendarAdapter.setSelectedDate(date);
            } else {
                updateMonthYearText();
                setMonthView();
            }
            updateEventsForDate(date);
        }
    }

    private void updateEventsForDate(LocalDate date) {
        if (tvUpcomingTitle == null || eventAdapter == null) return;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");
        String formattedDate = date.format(formatter).toUpperCase();
        tvUpcomingTitle.setText("UPCOMING FOR " + formattedDate);

        if (visibleCalendarIds == null || visibleCalendarIds.isEmpty()) return;

        eventSyncManager.fetchEventsForCalendars(date, date, visibleCalendarIds, events -> {
            if (getContext() == null) return;
            currentEvents.clear();
            currentEvents.addAll(events);
            eventAdapter.notifyDataSetChanged();
        });
    }

    private void openEditEvent(Event event) {
        Intent intent = new Intent(requireContext(), CreateEventActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("eventId", event.getId());

        String fallbackCalendarId = event.getCalendarId();
        if ((fallbackCalendarId == null || fallbackCalendarId.isEmpty()) && !visibleCalendarIds.isEmpty()) {
            fallbackCalendarId = visibleCalendarIds.get(0);
        }

        intent.putExtra("calendarId", fallbackCalendarId);
        intent.putExtra("title", event.getTitle());
        intent.putExtra("description", event.getDescription());
        intent.putExtra("location", event.getLocation());
        intent.putExtra("startTime", event.getStartTime() != null ? event.getStartTime().toDate().getTime() : 0L);
        intent.putExtra("endTime", event.getEndTime() != null ? event.getEndTime().toDate().getTime() : 0L);
        intent.putExtra("allDay", event.getAllDay() != null && event.getAllDay());
        startActivity(intent);
    }

    private LocalDate toLocalDate(Timestamp timestamp) {
        if (timestamp == null) return null;
        java.time.ZoneId userZone = TimezoneHelper.getSelectedZoneId(requireContext());
        return timestamp.toDate().toInstant().atZone(userZone).toLocalDate();
    }
}
