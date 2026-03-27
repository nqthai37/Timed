package com.timed.Setting.Security;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.timed.R;

public class SecurityActivity extends AppCompatActivity {

    private RecyclerView rvSecurityOptions;
    private SecurityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        // Nút Back
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        rvSecurityOptions = findViewById(R.id.rv_security_options);
        if (rvSecurityOptions != null) {
            rvSecurityOptions.setLayoutManager(new LinearLayoutManager(this));

            // Dữ liệu giống hệt thiết kế
            List<SecurityOption> securityList = new ArrayList<>();
            securityList.add(new SecurityOption("Two-Factor Authentication", R.drawable.ic_two_step_auth, SecurityOption.TYPE_SWITCH, true));
            securityList.add(new SecurityOption("Biometric Login", R.drawable.ic_lock, SecurityOption.TYPE_SWITCH, false));
            securityList.add(new SecurityOption("Change Password", R.drawable.ic_key, SecurityOption.TYPE_ARROW, false));

            adapter = new SecurityAdapter(securityList, new SecurityAdapter.OnItemClickListener() {
                @Override
                public void onClick(SecurityOption option) {
                    // Click vào các mục có mũi tên (như Change PIN)
                    Toast.makeText(SecurityActivity.this, "Mở màn hình: " + option.getTitle(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSwitchChange(SecurityOption option, boolean isChecked) {
                    // Khi gạt nút bật/tắt
                    String status = isChecked ? "Bật" : "Tắt";
                    Toast.makeText(SecurityActivity.this, option.getTitle() + " - " + status, Toast.LENGTH_SHORT).show();
                }
            });
            
            rvSecurityOptions.setAdapter(adapter);
        }
    }
}