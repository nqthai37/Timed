package com.timed.Setting.Themes;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
import com.timed.R;
import com.timed.utils.ThemeManager;
import android.widget.ImageView;

public class ThemeActivity extends AppCompatActivity {

    private LinearLayout llAppearanceLight, llAppearanceDark, llAppearanceSystem;
    private ImageView ivCheckBlue, ivCheckEmerald, ivCheckSunset;
    private SeekBar seekBarFontSize;
    private String selectedAppearance = ThemeManager.APPEARANCE_SYSTEM;
    private String selectedColor = ThemeManager.PALETTE_BLUE;
    private int selectedFontSize = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme);

        // Back button
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        View btnReset = findViewById(R.id.btnResetTheme);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> resetToDefault());
        }

        // Initialize Appearance buttons
        llAppearanceLight = findViewById(R.id.ll_appearance_light);
        llAppearanceDark = findViewById(R.id.ll_appearance_dark);
        llAppearanceSystem = findViewById(R.id.ll_appearance_system);

        ivCheckBlue = findViewById(R.id.iv_check_blue);
        ivCheckEmerald = findViewById(R.id.iv_check_emerald);
        ivCheckSunset = findViewById(R.id.iv_check_sunset);

        // Appearance selection listeners
        llAppearanceLight.setOnClickListener(v -> selectAppearance(ThemeManager.APPEARANCE_LIGHT, llAppearanceLight));
        llAppearanceDark.setOnClickListener(v -> selectAppearance(ThemeManager.APPEARANCE_DARK, llAppearanceDark));
        llAppearanceSystem
                .setOnClickListener(v -> selectAppearance(ThemeManager.APPEARANCE_SYSTEM, llAppearanceSystem));

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
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        loadCurrentSelections();
    }

    private void selectAppearance(String appearance, LinearLayout selected) {
        if (appearance.equals(selectedAppearance)) {
            return;
        }
        selectedAppearance = appearance;
        ThemeManager.setAppearance(this, appearance);
        ThemeManager.applyNightMode(this);
        resetAppearanceButtons();
        selected.setBackground(getDrawable(R.drawable.bg_rounded_appearance_selected));
        recreate();
    }

    private void resetAppearanceButtons() {
        llAppearanceLight.setBackground(getDrawable(R.drawable.bg_rounded));
        llAppearanceDark.setBackground(getDrawable(R.drawable.bg_rounded));
        llAppearanceSystem.setBackground(getDrawable(R.drawable.bg_rounded));
    }

    private void initializeColorSelection() {
        findViewById(R.id.fl_color_blue).setOnClickListener(v -> selectPalette(ThemeManager.PALETTE_BLUE));
        findViewById(R.id.fl_color_emerald).setOnClickListener(v -> selectPalette(ThemeManager.PALETTE_EMERALD));
        findViewById(R.id.fl_color_sunset).setOnClickListener(v -> selectPalette(ThemeManager.PALETTE_SUNSET));
    }

    private void selectPalette(String palette) {
        if (palette.equals(selectedColor)) {
            return;
        }
        selectedColor = palette;
        ThemeManager.setPalette(this, palette);
        applyPaletteSelection();
        recreate();
    }

    private void loadCurrentSelections() {
        selectedAppearance = ThemeManager.getAppearance(this);
        selectedColor = ThemeManager.getPalette(this);
        applyAppearanceSelection();
        applyPaletteSelection();
    }

    private void applyAppearanceSelection() {
        resetAppearanceButtons();
        if (ThemeManager.APPEARANCE_LIGHT.equals(selectedAppearance)) {
            llAppearanceLight.setBackground(getDrawable(R.drawable.bg_rounded_appearance_selected));
        } else if (ThemeManager.APPEARANCE_DARK.equals(selectedAppearance)) {
            llAppearanceDark.setBackground(getDrawable(R.drawable.bg_rounded_appearance_selected));
        } else {
            llAppearanceSystem.setBackground(getDrawable(R.drawable.bg_rounded_appearance_selected));
        }
    }

    private void applyPaletteSelection() {
        if (ivCheckBlue != null) {
            ivCheckBlue.setVisibility(ThemeManager.PALETTE_BLUE.equals(selectedColor) ? View.VISIBLE : View.GONE);
        }
        if (ivCheckEmerald != null) {
            ivCheckEmerald.setVisibility(ThemeManager.PALETTE_EMERALD.equals(selectedColor) ? View.VISIBLE : View.GONE);
        }
        if (ivCheckSunset != null) {
            ivCheckSunset.setVisibility(ThemeManager.PALETTE_SUNSET.equals(selectedColor) ? View.VISIBLE : View.GONE);
        }
    }

    private void resetToDefault() {
        selectedAppearance = ThemeManager.APPEARANCE_SYSTEM;
        selectedColor = ThemeManager.PALETTE_BLUE;
        ThemeManager.setAppearance(this, selectedAppearance);
        ThemeManager.setPalette(this, selectedColor);
        ThemeManager.applyNightMode(this);
        applyAppearanceSelection();
        applyPaletteSelection();
        recreate();
    }
}