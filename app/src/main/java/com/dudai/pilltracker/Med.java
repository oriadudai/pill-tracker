package com.dudai.pilltracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Med {

    /** Pastel-but-bright palette for the medication colour dots. */
    static final int[] COLORS = {
            0xFFEC4899, // pink
            0xFFA855F7, // purple
            0xFFF97316, // orange
            0xFF14B8A6, // teal
            0xFF3B82F6, // blue
            0xFFEAB308, // yellow
    };

    long id;
    String name = "";
    String dose = "";
    List<String> times = new ArrayList<>();
    boolean remind = true;
    int color;
    String startDay;
    String endDay; // null = still active

    int colorValue() {
        return COLORS[Math.floorMod(color, COLORS.length)];
    }

    /** Was this medication on the list on that day? */
    boolean activeOn(String day) {
        return startDay.compareTo(day) <= 0 && (endDay == null || endDay.compareTo(day) > 0);
    }

    String timesString() {
        return String.join(",", times);
    }

    static List<String> parseTimes(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        for (String t : s.split(",")) {
            t = t.trim();
            if (t.length() == 5 && !out.contains(t)) out.add(t);
        }
        Collections.sort(out);
        return out;
    }
}
