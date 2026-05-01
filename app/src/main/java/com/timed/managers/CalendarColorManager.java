package com.timed.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides predefined calendar colors used across create/edit flows.
 */
public final class CalendarColorManager {
    public static final class CalendarColor {
        private final String name;
        private final String hex;

        public CalendarColor(String name, String hex) {
            this.name = name;
            this.hex = hex;
        }

        public String getName() {
            return name;
        }

        public String getHex() {
            return hex;
        }
    }

    private static final List<CalendarColor> PRESET_COLORS;
    private static final Map<String, String> NAME_TO_HEX;
    private static final Map<String, String> HEX_TO_NAME;

    static {
        List<CalendarColor> colors = new ArrayList<>();
        colors.add(new CalendarColor("blue", "#2B78E4"));
        colors.add(new CalendarColor("green", "#34A853"));
        colors.add(new CalendarColor("red", "#EA4335"));
        colors.add(new CalendarColor("yellow", "#FBBC04"));
        colors.add(new CalendarColor("orange", "#EA8600"));
        colors.add(new CalendarColor("purple", "#AB47BC"));
        colors.add(new CalendarColor("cyan", "#00ACC1"));
        colors.add(new CalendarColor("deep_orange", "#FF6D00"));
        colors.add(new CalendarColor("teal", "#00897B"));
        colors.add(new CalendarColor("gray", "#757575"));
        colors.add(new CalendarColor("deep_purple", "#9C27B0"));
        colors.add(new CalendarColor("dark_blue", "#1976D2"));
        colors.add(new CalendarColor("light_purple", "#B39DDB"));
        colors.add(new CalendarColor("light_cyan", "#80DEEA"));
        colors.add(new CalendarColor("light_green", "#C8E6C9"));
        PRESET_COLORS = Collections.unmodifiableList(colors);

        Map<String, String> nameToHex = new LinkedHashMap<>();
        Map<String, String> hexToName = new HashMap<>();
        for (CalendarColor color : PRESET_COLORS) {
            nameToHex.put(color.getName(), color.getHex());
            hexToName.put(color.getHex().toLowerCase(Locale.US), color.getName());
        }
        NAME_TO_HEX = Collections.unmodifiableMap(nameToHex);
        HEX_TO_NAME = Collections.unmodifiableMap(hexToName);
    }

    private CalendarColorManager() {
    }

    public static List<CalendarColor> getPresetColors() {
        return PRESET_COLORS;
    }

    public static String getColorByName(String colorName) {
        if (colorName == null || colorName.trim().isEmpty()) {
            return PRESET_COLORS.get(0).getHex();
        }
        String hex = NAME_TO_HEX.get(colorName);
        return hex != null ? hex : PRESET_COLORS.get(0).getHex();
    }

    public static String getNameByColor(String hexColor) {
        if (hexColor == null || hexColor.trim().isEmpty()) {
            return PRESET_COLORS.get(0).getName();
        }
        String name = HEX_TO_NAME.get(hexColor.toLowerCase(Locale.US));
        return name != null ? name : PRESET_COLORS.get(0).getName();
    }

    public static boolean isValidHexColor(String hexColor) {
        return hexColor != null && hexColor.matches("^#[0-9A-Fa-f]{6}$");
    }
}
