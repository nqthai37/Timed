package com.timed.Setting.Themes;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.timed.R;

public class ThemeActivity extends AppCompatActivity {

    private LinearLayout llAppearanceLight, llAppearanceDark, llAppearanceSystem;
    private LinearLayout llColors;
    private SeekBar seekBarFontSize;
    private String selectedAppearance = "LIGHT";
    private String selectedColor = "BLUE";
    private int selectedFontSize = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme);

        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // Initialize Appearance buttons
        llAppearanceLight = findViewById(R.id.ll_appearance_light);
        llAppearanceDark = findViewById(R.id.ll_appearance_dark);
        llAppearanceSystem = findViewById(R.id.ll_appearance_system);

        // Appearance selection listeners
        llAppearanceLight.setOnClickListener(v -> selectAppearance("LIGHT", llAppearanceLight));
        llAppearanceDark.setOnClickListener(v -> selectAppearance("DARK", llAppearanceDark));
        llAppearanceSystem.setOnClickListener(v -> selectAppearance("SYSTEM", llAppearanceSystem));

        // Initialize Color selection
        initializeColorSelection();

        // Initialize Font Size SeekBar
        seekBarFontSize = findViewById(R.id.seekbar_font_size);
        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedFontSize = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void selectAppearance(String appearance, LinearLayout selected) {
        selectedAppearance = appearance;
        
        // Reset all backgrounds
        resetAppearanceButtons();
        
        // Set selected background
        selected.setBackground(getDrawable(R.drawable.bg_rounded_appearance_selected));
        
        Toast.makeText(this, "Appearance: " + appearance, Toast.LENGTH_SHORT).show();
        // TODO: Apply theme based on selectedAppearance
    }

    private void resetAppearanceButtons() {
        llAppearanceLight.setBackground(getDrawable(R.drawable.bg_rounded));
        llAppearanceDark.setBackground(getDrawable(R.drawable.bg_rounded));
        llAppearanceSystem.setBackground(getDrawable(R.drawable.bg_rounded));
    }

    private void initializeColorSelection() {
        int[] colorIds = {
            R.id.fl_color_blue,
            R.id.iv_color_purple,
            R.id.iv_color_pink,
            R.id.iv_color_red,
            R.id.iv_color_orange,
            R.id.iv_color_green
        };

        String[] colorCodes = {"BLUE", "PURPLE", "PINK", "RED", "ORANGE", "GREEN"};

        for (int i = 0; i < colorIds.length; i++) {
            final int colorIndex = i;
            findViewById(colorIds[i]).setOnClickListener(v -> {
                selectedColor = colorCodes[colorIndex];
                Toast.makeText(this, "Color: " + selectedColor, Toast.LENGTH_SHORT).show();
                // TODO: Apply color theme
            });
        }
    }
}