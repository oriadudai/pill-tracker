package com.dudai.pilltracker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.app.NotificationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The day screen: every dose of the day, tap to mark it as taken. */
public class MainActivity extends Activity {

    static final String EXTRA_DAY = "day";

    private static final String[] CHEERS = {
            "לקחת הכל! את מלכה 👑",
            "הכל נלקח, כל הכבוד! 🎉",
            "וואו, יום מושלם 💖",
            "סיימת להיום! ✨",
    };

    private String day;
    private boolean followToday = true;

    private LinearLayout list;
    private TextView title, subtitle, progressText, streakText;
    private ProgressBar bar;
    private Button prevBtn, nextBtn, todayBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Notifs.ensureChannel(this);
        buildUi();
        handleIntent(getIntent());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        refresh();
    }

    private void handleIntent(Intent i) {
        String d = i.getStringExtra(EXTRA_DAY);
        if (d != null) {
            day = d;
            followToday = d.equals(Days.today());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (followToday || day == null) day = Days.today();
        refresh();
        Scheduler.rescheduleAll(this);
    }

    private void buildUi() {
        LinearLayout root = Ui.page(this);

        // Header: gradient card with the day, progress and streak
        LinearLayout header = Ui.card(this, Ui.PINK);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TR_BL,
                new int[]{Ui.PINK, Ui.PURPLE});
        g.setCornerRadius(Ui.dp(this, 26));
        header.setBackground(g);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        prevBtn = navButton("יום קודם");
        nextBtn = navButton("יום הבא");
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER);
        title = Ui.text(this, "", 26, Ui.WHITE, true);
        title.setGravity(Gravity.CENTER);
        subtitle = Ui.text(this, "", 14, 0xEEFFFFFF, false);
        subtitle.setGravity(Gravity.CENTER);
        titles.addView(title);
        titles.addView(subtitle);
        nav.addView(prevBtn);
        nav.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        nav.addView(nextBtn);
        header.addView(nav);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setProgressTintList(ColorStateList.valueOf(Ui.WHITE));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0x66FFFFFF));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 10));
        blp.topMargin = Ui.dp(this, 14);
        header.addView(bar, blp);

        progressText = Ui.text(this, "", 16, Ui.WHITE, true);
        progressText.setGravity(Gravity.CENTER);
        progressText.setPadding(0, Ui.dp(this, 8), 0, 0);
        header.addView(progressText);
        streakText = Ui.text(this, "", 14, Ui.WHITE, false);
        streakText.setGravity(Gravity.CENTER);
        header.addView(streakText);
        root.addView(header);

        todayBtn = Ui.button(this, "חזרה להיום", false);
        todayBtn.setOnClickListener(v -> {
            followToday = true;
            day = Days.today();
            refresh();
        });
        root.addView(todayBtn);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, Ui.dp(this, 8), 0, 0);
        root.addView(list);

        prevBtn.setOnClickListener(v -> {
            day = Days.add(day, -1);
            followToday = false;
            refresh();
        });
        nextBtn.setOnClickListener(v -> {
            if (day.compareTo(Days.today()) >= 0) return;
            day = Days.add(day, 1);
            followToday = day.equals(Days.today());
            refresh();
        });
    }

    private Button navButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(Ui.WHITE);
        b.setBackground(Ui.ripple(Ui.rounded(0x33FFFFFF, Ui.dp(this, 18))));
        b.setStateListAnimator(null);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(Ui.dp(this, 36));
        b.setMinimumHeight(Ui.dp(this, 36));
        b.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        return b;
    }

    private static final class Dose {
        final Med med;
        final String slot;
        Dose(Med med, String slot) { this.med = med; this.slot = slot; }
    }

    private void refresh() {
        Db db = Db.get(this);
        String today = Days.today();
        boolean isToday = day.equals(today);
        List<Med> meds = db.medsForDay(day);
        Map<String, Long> taken = db.intakes(day);

        title.setText(Days.title(day));
        subtitle.setText(Days.subtitle(day));
        boolean canNext = day.compareTo(today) < 0;
        nextBtn.setEnabled(canNext);
        nextBtn.setAlpha(canNext ? 1f : 0.3f);
        todayBtn.setVisibility(isToday ? View.GONE : View.VISIBLE);

        List<Dose> doses = new ArrayList<>();
        for (Med m : meds) for (String s : m.times) doses.add(new Dose(m, s));
        doses.sort((a, b) -> {
            int c = a.slot.compareTo(b.slot);
            return c != 0 ? c : a.med.name.compareToIgnoreCase(b.med.name);
        });

        list.removeAllViews();
        if (doses.isEmpty()) {
            boolean none = db.activeMeds().isEmpty();
            LinearLayout empty = Ui.card(this, Ui.WHITE);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            TextView e = Ui.text(this, none ? "🌷\nעוד אין תרופות ברשימה" : "אין תרופות ליום הזה 🌷",
                    17, Ui.TEXT_SOFT, false);
            e.setGravity(Gravity.CENTER);
            empty.addView(e);
            if (none) {
                Button add = Ui.button(this, "+ הוספת תרופה", true);
                add.setOnClickListener(v -> startActivity(new Intent(this, EditMedActivity.class)));
                empty.addView(add);
            }
            list.addView(empty);
        }

        int done = 0;
        int nowMin = Days.nowMinutes();
        for (Dose d : doses) {
            Long at = taken.get(Db.key(d.med.id, d.slot));
            if (at != null) done++;
            boolean late = at == null && (day.compareTo(today) < 0
                    || (isToday && Days.minutes(d.slot) < nowMin));
            list.addView(doseRow(d, at, late));
        }

        int n = doses.size();
        bar.setMax(Math.max(1, n));
        bar.setProgress(done);
        bar.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
        if (n == 0) {
            progressText.setText("");
        } else if (done == n) {
            progressText.setText(CHEERS[Math.floorMod(day.hashCode(), CHEERS.length)]);
        } else {
            progressText.setText("סימנת " + done + " מתוך " + n);
        }

        int streak = isToday ? Stats.streak(db) : 0;
        streakText.setVisibility(streak >= 2 ? View.VISIBLE : View.GONE);
        streakText.setText("🔥 " + streak + " ימים ברצף");
    }

    private View doseRow(Dose d, Long takenAt, boolean late) {
        boolean taken = takenAt != null;
        int color = d.med.colorValue();
        LinearLayout row = Ui.card(this, taken ? Ui.TAKEN_BG : Ui.WHITE);
        row.setBackground(Ui.ripple(Ui.rounded(taken ? Ui.TAKEN_BG : Ui.WHITE, Ui.dp(this, 22))));
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView check = Ui.text(this, taken ? "✓" : "", 22, Ui.WHITE, true);
        check.setGravity(Gravity.CENTER);
        check.setBackground(taken ? Ui.oval(color, 0, 0) : Ui.oval(0x00000000, color, Ui.dp(this, 3)));
        int size = Ui.dp(this, 46);
        row.addView(check, new LinearLayout.LayoutParams(size, size));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 8), 0);
        texts.addView(Ui.text(this, d.med.name, 18, Ui.TEXT, true));
        if (!d.med.dose.isEmpty()) texts.addView(Ui.text(this, d.med.dose, 14, Ui.TEXT_SOFT, false));
        if (taken) {
            texts.addView(Ui.text(this, "נלקח ב-" + Days.clock(takenAt) + " 💖", 14, Ui.PINK_DARK, true));
        } else if (late) {
            texts.addView(Ui.text(this, "עוד לא סומן", 14, 0xFFF97316, false));
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView time = Ui.text(this, d.slot, 17, Ui.PINK_DARK, true);
        time.setBackground(Ui.rounded(Ui.PINK_SOFT, Ui.dp(this, 14)));
        time.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4));
        row.addView(time);

        row.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Db.get(this).setTaken(d.med.id, day, d.slot, !taken);
            getSystemService(NotificationManager.class).cancel(Scheduler.code(d.med.id, d.slot));
            refresh();
        });
        return row;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "📅 לוח").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 2, 0, "💊 תרופות").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            startActivity(new Intent(this, CalendarActivity.class));
            return true;
        }
        if (item.getItemId() == 2) {
            startActivity(new Intent(this, MedsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
