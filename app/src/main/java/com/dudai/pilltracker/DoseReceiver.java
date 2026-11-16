package com.dudai.pilltracker;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles the daily alarm and the "I took it" button on the notification. */
public class DoseReceiver extends BroadcastReceiver {

    static final String ACTION_REMIND = "com.dudai.pilltracker.REMIND";
    static final String ACTION_TAKEN = "com.dudai.pilltracker.TAKEN";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        long medId = intent.getLongExtra(Scheduler.EXTRA_MED, 0);
        String slot = intent.getStringExtra(Scheduler.EXTRA_SLOT);
        if (slot == null) return;
        Db db = Db.get(ctx);

        if (ACTION_TAKEN.equals(intent.getAction())) {
            String day = intent.getStringExtra(Scheduler.EXTRA_DAY);
            if (day == null) day = Days.today();
            db.setTaken(medId, day, slot, true);
            ctx.getSystemService(NotificationManager.class).cancel(Scheduler.code(medId, slot));
            return;
        }

        // ACTION_REMIND
        Med m = db.getMed(medId);
        if (m == null || m.endDay != null || !m.remind || !m.times.contains(slot)) return;
        Scheduler.scheduleSlot(ctx, medId, slot); // tomorrow, same time
        String today = Days.today();
        if (!db.isTaken(medId, today, slot)) Notifs.show(ctx, m, slot, today);
    }
}
