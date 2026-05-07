package com.timed.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.android.material.color.MaterialColors;
import com.timed.R;
import com.timed.Setting.Timezone.TimezoneHelper;
import com.timed.models.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeekEventAdapter extends RecyclerView.Adapter<WeekEventAdapter.WeekEventViewHolder> {

    private final List<Event> eventList;

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
        holder.tvEventTime.setText(formatTime(holder.itemView.getContext(), event.getStartTime()));
        holder.tvEventTitle.setText(event.getTitle());
        holder.tvEventSubtitle.setText(buildDetails(event));

        // Tùy biến màu thẻ (Ví dụ: Thẻ chẵn màu trắng viền, thẻ lẻ màu tím đặc)
        if (position % 2 == 0) {
            holder.layoutEventCard.setBackgroundResource(R.drawable.bg_day_event_light);
            int onSurface = MaterialColors.getColor(holder.itemView,
                    com.google.android.material.R.attr.colorOnSurface);
            int onSurfaceVariant = MaterialColors.getColor(holder.itemView,
                    com.google.android.material.R.attr.colorOnSurfaceVariant);
            holder.tvEventTitle.setTextColor(onSurface);
            holder.tvEventSubtitle.setTextColor(onSurfaceVariant);
            holder.tvEventSubtitle.setAlpha(1.0f);
        } else {
            holder.layoutEventCard.setBackgroundResource(R.drawable.bg_day_selected);
            int onPrimary = MaterialColors.getColor(holder.itemView,
                    com.google.android.material.R.attr.colorOnPrimary);
            holder.tvEventTitle.setTextColor(onPrimary);
            holder.tvEventSubtitle.setTextColor(onPrimary);
            holder.tvEventSubtitle.setAlpha(0.85f);
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

    private String formatTime(android.content.Context context, Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return TimezoneHelper.formatTime24h(context, timestamp.toDate());
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
