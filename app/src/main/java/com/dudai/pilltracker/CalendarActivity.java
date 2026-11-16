package com.dudai.pilltracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

/** Month view: colour per day by how much was taken. Tap a day to see / fix it. */
public class CalendarActivity extends Activity {

    private static final String[] SHORT_DAYS = {"א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳"};

    private int monthOffset = 0;
    private TextView monthTitle, summary;
    private LinearLayout grid;
    private Button nextMonth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
        LinearLayout root = Ui.page(this);

        LinearLayout card = Ui.card(this, Ui.WHITE);
        card.setOrientation(LinearLayout.VERTICAL);
        root.addView(card);

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        Button prevMonth = small("חודש קודם");
        nextMonth = small("חודש הבא");
        monthTitle = Ui.text(this, "", 20, Ui.PINK_DARK, true);
        monthTitle.setGravity(Gravity.CENTER);
        nav.addView(prevMonth);
        nav.addView(monthTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        nav.addView(nextMonth);
        card.addView(nav);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(grid);

        prevMonth.setOnClickListener(v -> {
            monthOffset--;
            refresh();
        });
        nextMonth.setOnClickListener(v -> {
            if (monthOffset < 0) monthOffset++;
            refresh();
        });

        LinearLayout legend = Ui.card(this, Ui.WHITE);
        legend.setOrientation(LinearLayout.VERTICAL);
        legend.addView(legendRow(Ui.FULL, "הכל נלקח"));
        legend.addView(legendRow(Ui.PARTIAL, "חלק נלקח"));
        legend.addView(legendRow(Ui.MISSED, "לא סומן כלום"));
        summary = Ui.text(this, "", 16, Ui.TEXT, true);
        summary.setPadding(0, Ui.dp(this, 10), 0, 0);
        legend.addView(summary);
        root.addView(legend);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private Button small(String s) {
        Button b = Ui.button(this, s, false);
        b.setTextSize(13);
        b.setMinHeight(Ui.dp(this, 36));
        b.setMinimumHeight(Ui.dp(this, 36));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return b;
    }

    private LinearLayout legendRow(int color, String s) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
        TextView dot = new TextView(this);
        dot.setBackground(Ui.oval(color, 0, 0));
        int sz = Ui.dp(this, 18);
        row.addView(dot, new LinearLayout.LayoutParams(sz, sz));
        TextView t = Ui.text(this, s, 15, Ui.TEXT_SOFT, false);
        t.setPadding(Ui.dp(this, 10), 0, 0, 0);
        row.addView(t);
        return row;
    }

    private void refresh() {
        Calendar first = Calendar.getInstance();
        first.set(Calendar.DAY_OF_MONTH, 1);
        first.add(Calendar.MONTH, monthOffset);
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int lead = first.get(Calendar.DAY_OF_WEEK) - 1; // Sunday first
        monthTitle.setText(Days.MONTHS[first.get(Calendar.MONTH)] + " " + first.get(Calendar.YEAR));
        nextMonth.setEnabled(monthOffset < 0);
        nextMonth.setAlpha(monthOffset < 0 ? 1f : 0.35f);

        String from = Days.of(first);
        Calendar last = (Calendar) first.clone();
        last.set(Calendar.DAY_OF_MONTH, daysInMonth);
        String to = Days.of(last);
        String today = Days.today();

        Db db = Db.get(this);
        Map<String, Map<Long, Integer>> counts = db.counts(from, to);
        List<Med> all = db.allMeds();

        grid.removeAllViews();
        LinearLayout head = new LinearLayout(this);
        for (String s : SHORT_DAYS) {
            TextView t = Ui.text(this, s, 13, Ui.TEXT_SOFT, true);
            t.setGravity(Gravity.CENTER);
            head.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        grid.addView(head);

        int fullDays = 0, countedDays = 0, missedDoses = 0;
        int cells = lead + daysInMonth;
        int rows = (cells + 6) / 7;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(this);
            for (int col = 0; col < 7; col++) {
                int idx = r * 7 + col;
                FrameLayout cell = new FrameLayout(this);
                row.addView(cell, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
                int dayNum = idx - lead + 1;
                if (dayNum < 1 || dayNum > daysInMonth) continue;

                Calendar c = (Calendar) first.clone();
                c.set(Calendar.DAY_OF_MONTH, dayNum);
                String d = Days.of(c);
                boolean future = d.compareTo(today) > 0;

                int bg = 0x00000000;
                int fg = future ? 0xFFD1D5DB : Ui.TEXT;
                if (!future) {
                    int[] p = Stats.progress(all, counts.get(d), d);
                    int st = Stats.status(p);
                    if (st != Stats.NONE) {
                        bg = st == Stats.FULL ? Ui.FULL : st == Stats.PARTIAL ? Ui.PARTIAL : Ui.MISSED;
                        if (st == Stats.FULL) fg = Ui.WHITE;
                        // Today isn't over yet, so it doesn't count against her
                        if (!d.equals(today)) {
                            countedDays++;
                            if (st == Stats.FULL) fullDays++;
                            missedDoses += p[1] - p[0];
                        }
                    }
                }

                TextView t = Ui.text(this, String.valueOf(dayNum), 15, fg, d.equals(today));
                t.setGravity(Gravity.CENTER);
                t.setBackground(Ui.oval(bg, Ui.PINK_DARK, d.equals(today) ? Ui.dp(this, 2) : 0));
                int s = Ui.dp(this, 40);
                cell.addView(t, new FrameLayout.LayoutParams(s, s, Gravity.CENTER));

                if (!future) {
                    t.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)
                            .putExtra(MainActivity.EXTRA_DAY, d)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)));
                }
            }
            grid.addView(row);
        }

        if (countedDays == 0) {
            summary.setText("");
        } else {
            String s = "🌸 " + fullDays + " ימים מלאים מתוך " + countedDays;
            if (missedDoses > 0) s += "\nפוספסו " + missedDoses + " מנות";
            summary.setText(s);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
