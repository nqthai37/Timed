package com.timed.Setting.Security;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;

import com.timed.R;
import com.timed.Setting.Main.GenericSettingAdapter;

public class SecurityActivity extends AppCompatActivity {

    private RecyclerView rvSecurityOptions;
    private GenericSettingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        rvSecurityOptions = findViewById(R.id.rv_security_options);
        if (rvSecurityOptions != null) {
            rvSecurityOptions.setLayoutManager(new LinearLayoutManager(this));

            List<GenericSettingAdapter.SettingItemModel> securityList = new ArrayList<>();
            securityList.add(new GenericSettingAdapter.SettingItemModel("Two-Factor Auth", "Enable 2FA", "sec_1"));
            securityList.add(new GenericSettingAdapter.SettingItemModel("Password", "Change password", "sec_2"));
            securityList.add(new GenericSettingAdapter.SettingItemModel("Session", "Manage sessions", "sec_3"));

            adapter = new GenericSettingAdapter(securityList, item -> {
                // Handle security option selection
            });
            rvSecurityOptions.setAdapter(adapter);
        }
    }
}
