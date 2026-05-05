package com.timed.Features.AI;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.timed.R;

public class AiSchedulingActivity extends AppCompatActivity {

    private RecyclerView rvSchedules;
    private AiScheduleAdapter adapter;
    private List<AiSchedule> scheduleList;
    private EditText etAiInput;
    private ImageView ivSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_scheduling);

        // Nút quay lại
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        etAiInput = findViewById(R.id.et_ai_input);
        ivSend = findViewById(R.id.iv_send);
        rvSchedules = findViewById(R.id.rv_ai_schedules);

        // Sự kiện gửi lệnh AI
        ivSend.setOnClickListener(v -> {
            String prompt = etAiInput.getText().toString().trim();
            if (!prompt.isEmpty()) {
                Toast.makeText(this, "Processing: " + prompt, Toast.LENGTH_SHORT).show();
                // Logic gửi lên API của bạn ở đây...
                etAiInput.setText(""); // Xoá trắng ô nhập
            }
        });

        // Setup danh sách lịch sử gần đây
        if (rvSchedules != null) {
            rvSchedules.setLayoutManager(new LinearLayoutManager(this));

            scheduleList = new ArrayList<>();
            scheduleList.add(new AiSchedule("Team sync tomorrow at 10 AM", "Scheduled successfully", true));
            scheduleList.add(new AiSchedule("Doctor appointment on Friday", "Need confirmation", false));
            scheduleList.add(new AiSchedule("Gym every Monday 7am", "Recurring event added", true));

            adapter = new AiScheduleAdapter(scheduleList);
            rvSchedules.setAdapter(adapter);
        }
    }
}