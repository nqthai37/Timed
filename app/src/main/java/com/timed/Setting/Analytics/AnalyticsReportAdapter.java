package com.timed.Setting.Analytics;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;

import java.util.List;

public class AnalyticsReportAdapter extends RecyclerView.Adapter<AnalyticsReportAdapter.ReportViewHolder> {

    private List<AnalyticsReport> reportList;

    public AnalyticsReportAdapter(List<AnalyticsReport> reportList) {
        this.reportList = reportList;
    }

    @Override
    public ReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ReportViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_analytics_report, parent, false));
    }

    @Override
    public void onBindViewHolder(ReportViewHolder holder, int position) {
        AnalyticsReport report = reportList.get(position);
        holder.tvTotalHoursLabel.setText(report.label1);
        holder.tvTotalHoursValue.setText(report.value1);
        holder.tvProductiveLabel.setText(report.label2);
        holder.tvProductiveValue.setText(report.value2);
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvTotalHoursLabel, tvTotalHoursValue;
        TextView tvProductiveLabel, tvProductiveValue;

        public ReportViewHolder(android.view.View itemView) {
            super(itemView);
            tvTotalHoursLabel = itemView.findViewById(R.id.tv_total_hours_label);
            tvTotalHoursValue = itemView.findViewById(R.id.tv_total_hours_value);
            tvProductiveLabel = itemView.findViewById(R.id.tv_productive_label);
            tvProductiveValue = itemView.findViewById(R.id.tv_productive_value);
        }
    }

    public static class AnalyticsReport {
        public String label1;
        public String value1;
        public String label2;
        public String value2;

        public AnalyticsReport(String label1, String value1, String label2, String value2) {
            this.label1 = label1;
            this.value1 = value1;
            this.label2 = label2;
            this.value2 = value2;
        }
    }
}

