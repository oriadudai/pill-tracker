package com.dudai.pilltracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarms are wiped on reboot / app update, and shift when the clock changes. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        Scheduler.rescheduleAll(ctx);
    }
}
