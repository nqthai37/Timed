package com.timed.Features.Analytics;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.timed.R;
import com.timed.Features.Analytics.AnalyticsReportAdapter;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsActivity extends AppCompatActivity {

    private RecyclerView rvAnalyticsReports;
    private AnalyticsReportAdapter reportAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);
        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        rvAnalyticsReports = findViewById(R.id.rv_analytics_reports);
        if (rvAnalyticsReports != null) {
            rvAnalyticsReports.setLayoutManager(new LinearLayoutManager(this));

            List<AnalyticsReportAdapter.AnalyticsReport> reportList = new ArrayList<>();
            reportList.add(new AnalyticsReportAdapter.AnalyticsReport("Total Hours", "80h", "Productive", "68%"));
            reportList.add(new AnalyticsReportAdapter.AnalyticsReport("Daily Average", "12.4h", "This Week", "64%"));
            reportList.add(new AnalyticsReportAdapter.AnalyticsReport("Peak Time", "2-4 PM", "Focused", "92%"));

            reportAdapter = new AnalyticsReportAdapter(reportList);
            rvAnalyticsReports.setAdapter(reportAdapter);
        }
    }
}