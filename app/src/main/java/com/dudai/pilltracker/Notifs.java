package com.dudai.pilltracker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;

final class Notifs {

    static final String CHANNEL_ID = "doses";

    private static final String[] TITLES = {
            "הגיע הזמן ל%s 💊",
            "פסססט… %s מחכה לך ✨",
            "תזכורת קטנה: %s 💖",
            "זמן %s! 🌸",
    };

    private Notifs() {}

    static void ensureChannel(Context ctx) {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                ctx.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH);
        ctx.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    static void show(Context ctx, Med m, String slot, String day) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ensureChannel(ctx);
        int code = Scheduler.code(m.id, slot);

        PendingIntent open = PendingIntent.getActivity(ctx, code,
                new Intent(ctx, MainActivity.class).putExtra(MainActivity.EXTRA_DAY, day),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent taken = new Intent(ctx, DoseReceiver.class)
                .setAction(DoseReceiver.ACTION_TAKEN)
                .putExtra(Scheduler.EXTRA_MED, m.id)
                .putExtra(Scheduler.EXTRA_SLOT, slot)
                .putExtra(Scheduler.EXTRA_DAY, day);
        PendingIntent takenPi = PendingIntent.getBroadcast(ctx, code, taken,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = String.format(TITLES[(int) Math.floorMod(code, TITLES.length)], m.name);
        String text = slot + (m.dose.isEmpty() ? "" : " · " + m.dose);

        Notification n = new Notification.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(m.colorValue())
                .setContentTitle(title)
                .setContentText(text)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(ctx, R.drawable.ic_notification),
                        "✓ לקחתי", takenPi).build())
                .build();
        ctx.getSystemService(NotificationManager.class).notify(code, n);
    }
}
