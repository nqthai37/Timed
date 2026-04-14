package com.timed.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.timed.R;
import com.timed.models.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeekEventAdapter extends RecyclerView.Adapter<WeekEventAdapter.WeekEventViewHolder> {

    private final List<Event> eventList;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public WeekEventAdapter(List<Event> eventList) {
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public WeekEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_week_event, parent, false);
        return new WeekEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekEventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.tvEventTime.setText(formatTime(event.getStartTime()));
        holder.tvEventTitle.setText(event.getTitle());
        holder.tvEventSubtitle.setText(buildDetails(event));

        // Tùy biến màu thẻ (Ví dụ: Thẻ chẵn màu trắng viền, thẻ lẻ màu tím đặc)
        if (position % 2 == 0) {
            holder.layoutEventCard.setBackgroundResource(R.drawable.bg_day_event_light);
            holder.tvEventTitle.setTextColor(android.graphics.Color.parseColor("#0f172a"));
            holder.tvEventSubtitle.setTextColor(android.graphics.Color.parseColor("#64748b"));
        } else {
            holder.layoutEventCard.setBackgroundResource(R.drawable.bg_day_selected);
            holder.tvEventTitle.setTextColor(android.graphics.Color.WHITE);
            holder.tvEventSubtitle.setTextColor(android.graphics.Color.parseColor("#E6FFFFFF"));
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    private String buildDetails(Event event) {
        String description = event.getDescription() != null ? event.getDescription().trim() : "";
        String location = event.getLocation() != null ? event.getLocation().trim() : "";

        if (!description.isEmpty() && !location.isEmpty()) {
            return description + " • " + location;
        }
        if (!description.isEmpty()) {
            return description;
        }
        return location;
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        Date date = timestamp.toDate();
        return timeFormat.format(date);
    }

    static class WeekEventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTime, tvEventTitle, tvEventSubtitle;
        View layoutEventCard;

        public WeekEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTime = itemView.findViewById(R.id.tvEventTime);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventSubtitle = itemView.findViewById(R.id.tvEventSubtitle);
            layoutEventCard = itemView.findViewById(R.id.layoutEventCard);
        }
    }
}
