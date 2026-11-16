package com.dudai.pilltracker;

import java.util.Calendar;
import java.util.Locale;

/** Days are stored as "yyyy-MM-dd" strings, which sort correctly as text. */
final class Days {

    static final String[] WEEKDAYS = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};
    static final String[] MONTHS = {"ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני",
            "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר"};

    private Days() {}

    static String of(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static String today() {
        return of(Calendar.getInstance());
    }

    static Calendar parse(String d) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(d.substring(0, 4)),
                Integer.parseInt(d.substring(5, 7)) - 1,
                Integer.parseInt(d.substring(8, 10)));
        return c;
    }

    static String add(String d, int days) {
        Calendar c = parse(d);
        c.add(Calendar.DAY_OF_MONTH, days);
        return of(c);
    }

    static String title(String d) {
        String today = today();
        if (d.equals(today)) return "היום";
        if (d.equals(add(today, -1))) return "אתמול";
        return "יום " + WEEKDAYS[parse(d).get(Calendar.DAY_OF_WEEK) - 1];
    }

    static String subtitle(String d) {
        Calendar c = parse(d);
        return "יום " + WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1] + ", "
                + c.get(Calendar.DAY_OF_MONTH) + " ב" + MONTHS[c.get(Calendar.MONTH)];
    }

    static String clock(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    static String slot(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    /** Minutes since midnight, now. */
    static int nowMinutes() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    static int minutes(String slot) {
        return Integer.parseInt(slot.substring(0, 2)) * 60 + Integer.parseInt(slot.substring(3, 5));
    }
}
