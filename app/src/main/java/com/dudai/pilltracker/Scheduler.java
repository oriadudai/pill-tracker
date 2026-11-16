package com.dudai.pilltracker;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

/** One daily alarm per (medication, time of day). */
final class Scheduler {

    static final String EXTRA_MED = "med";
    static final String EXTRA_SLOT = "slot";
    static final String EXTRA_DAY = "day";

    private Scheduler() {}

    /** Stable id per (medication, time) — used for the alarm and the notification. */
    static int code(long medId, String slot) {
        return (int) (medId * 10000 + Integer.parseInt(slot.replace(":", "")));
    }

    static void rescheduleAll(Context ctx) {
        for (Med m : Db.get(ctx).activeMeds()) scheduleMed(ctx, m);
    }

    static void scheduleMed(Context ctx, Med m) {
        for (String slot : m.times) {
            if (m.remind && m.endDay == null) {
                scheduleSlot(ctx, m.id, slot);
            } else {
                cancelSlot(ctx, m.id, slot);
            }
        }
    }

    static void cancelSlots(Context ctx, long medId, List<String> slots) {
        for (String slot : slots) cancelSlot(ctx, medId, slot);
    }

    static void cancelSlot(Context ctx, long medId, String slot) {
        ctx.getSystemService(AlarmManager.class).cancel(alarmIntent(ctx, medId, slot));
        ctx.getSystemService(NotificationManager.class).cancel(code(medId, slot));
    }

    static void scheduleSlot(Context ctx, long medId, String slot) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        PendingIntent pi = alarmIntent(ctx, medId, slot);
        long at = next(slot);
        boolean exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    /** Next time this HH:mm comes around (at least a minute from now). */
    private static long next(String slot) {
        Calendar c = Calendar.getInstance();
        long limit = c.getTimeInMillis() + 60_000L;
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(slot.substring(0, 2)));
        c.set(Calendar.MINUTE, Integer.parseInt(slot.substring(3, 5)));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= limit) c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    private static PendingIntent alarmIntent(Context ctx, long medId, String slot) {
        Intent i = new Intent(ctx, DoseReceiver.class)
                .setAction(DoseReceiver.ACTION_REMIND)
                .putExtra(EXTRA_MED, medId)
                .putExtra(EXTRA_SLOT, slot);
        return PendingIntent.getBroadcast(ctx, code(medId, slot), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
