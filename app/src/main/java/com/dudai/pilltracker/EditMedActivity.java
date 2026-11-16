package com.dudai.pilltracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Add / edit a medication: name, dose note, times per day, reminder, colour. */
public class EditMedActivity extends Activity {

    static final String EXTRA_ID = "id";

    private Med med;
    private final List<String> oldTimes = new ArrayList<>();
    private final List<String> times = new ArrayList<>();
    private int color;

    private EditText name, dose;
    private LinearLayout timesBox, colorRow;
    private Switch remind;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        long id = getIntent().getLongExtra(EXTRA_ID, 0);
        Db db = Db.get(this);
        if (id == 0) {
            med = new Med();
            med.times.add("08:00");
            med.color = db.allMeds().size();
            setTitle("תרופה חדשה");
        } else {
            med = db.getMed(id);
            if (med == null) {
                finish();
                return;
            }
            oldTimes.addAll(med.times);
            setTitle(med.name);
        }
        times.addAll(med.times);
        color = med.color;

        LinearLayout root = Ui.page(this);
        LinearLayout form = Ui.card(this, Ui.WHITE);
        form.setOrientation(LinearLayout.VERTICAL);
        root.addView(form);

        form.addView(Ui.label(this, "שם התרופה"));
        name = field("למשל: אומגה 3");
        name.setText(med.name);
        form.addView(name);

        form.addView(Ui.label(this, "מינון / הערה (לא חובה)"));
        dose = field("למשל: כדור אחד אחרי האוכל");
        dose.setText(med.dose);
        form.addView(dose);

        form.addView(Ui.label(this, "מתי לוקחים?"));
        timesBox = new LinearLayout(this);
        timesBox.setOrientation(LinearLayout.VERTICAL);
        form.addView(timesBox);
        Button addTime = Ui.button(this, "+ עוד פעם ביום", false);
        addTime.setOnClickListener(v -> pickTime(-1));
        form.addView(addTime);

        remind = new Switch(this);
        remind.setText("🔔 תזכורת בהתראה בשעות האלה");
        remind.setTextSize(16);
        remind.setTextColor(Ui.TEXT);
        remind.setChecked(med.remind);
        remind.setPadding(Ui.dp(this, 4), Ui.dp(this, 18), Ui.dp(this, 4), Ui.dp(this, 6));
        form.addView(remind);

        form.addView(Ui.label(this, "צבע"));
        colorRow = new LinearLayout(this);
        colorRow.setGravity(Gravity.CENTER_VERTICAL);
        form.addView(colorRow);

        Button save = Ui.button(this, "שמירה 💖", true);
        save.setOnClickListener(v -> save());
        root.addView(save);

        if (med.id != 0) {
            Button del = Ui.button(this, "הסרת התרופה", false);
            del.setTextColor(0xFFDC2626);
            del.setOnClickListener(v -> confirmDelete());
            root.addView(del);
        }

        renderTimes();
        renderColors();
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(17);
        e.setTextColor(Ui.TEXT);
        e.setHintTextColor(0xFFD8A7C4);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setBackground(Ui.rounded(Ui.PINK_SOFT, Ui.dp(this, 14)));
        int p = Ui.dp(this, 12);
        e.setPadding(p, p, p, p);
        return e;
    }

    private void renderTimes() {
        Collections.sort(times);
        timesBox.removeAllViews();
        for (int i = 0; i < times.size(); i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));

            TextView t = Ui.text(this, "⏰  " + times.get(i), 20, Ui.PINK_DARK, true);
            t.setBackground(Ui.ripple(Ui.rounded(Ui.PINK_SOFT, Ui.dp(this, 16))));
            t.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
            t.setOnClickListener(v -> pickTime(idx));
            row.addView(t);

            View spacer = new View(this);
            row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

            if (times.size() > 1) {
                TextView x = Ui.text(this, "הסרה ✕", 14, Ui.TEXT_SOFT, false);
                x.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
                x.setOnClickListener(v -> {
                    times.remove(idx);
                    renderTimes();
                });
                row.addView(x);
            }
            timesBox.addView(row);
        }
    }

    /** idx = -1 adds a new time, otherwise changes that one. */
    private void pickTime(int idx) {
        int h = 20, m = 0;
        if (idx >= 0) {
            h = Integer.parseInt(times.get(idx).substring(0, 2));
            m = Integer.parseInt(times.get(idx).substring(3, 5));
        } else if (times.isEmpty()) {
            h = 8;
        } else if (times.size() >= 2) {
            h = 14;
        }
        new TimePickerDialog(this, (view, hour, minute) -> {
            String s = Days.slot(hour, minute);
            if (idx >= 0) times.remove(idx);
            if (!times.contains(s)) times.add(s);
            renderTimes();
        }, h, m, true).show();
    }

    private void renderColors() {
        colorRow.removeAllViews();
        for (int i = 0; i < Med.COLORS.length; i++) {
            final int c = i;
            TextView dot = Ui.text(this, i == color ? "✓" : "", 16, Ui.WHITE, true);
            dot.setGravity(Gravity.CENTER);
            dot.setBackground(Ui.oval(Med.COLORS[i], i == color ? Ui.TEXT : 0, i == color ? Ui.dp(this, 2) : 0));
            int s = Ui.dp(this, 36);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
            lp.setMarginEnd(Ui.dp(this, 10));
            dot.setOnClickListener(v -> {
                color = c;
                renderColors();
            });
            colorRow.addView(dot, lp);
        }
    }

    private void save() {
        String n = name.getText().toString().trim();
        if (n.isEmpty()) {
            Toast.makeText(this, "צריך לכתוב שם לתרופה", Toast.LENGTH_SHORT).show();
            return;
        }
        if (times.isEmpty()) {
            Toast.makeText(this, "צריך לפחות שעה אחת ביום", Toast.LENGTH_SHORT).show();
            return;
        }
        med.name = n;
        med.dose = dose.getText().toString().trim();
        med.times = new ArrayList<>(times);
        Collections.sort(med.times);
        med.remind = remind.isChecked();
        med.color = color;
        if (med.startDay == null) med.startDay = Days.today();
        Db.get(this).save(med);
        Scheduler.cancelSlots(this, med.id, oldTimes);
        Scheduler.scheduleMed(this, med);
        Toast.makeText(this, "נשמר ✨", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("להסיר את " + med.name + "?")
                .setMessage("התרופה תרד מהרשימה מהיום. מה שכבר סומן יישאר בלוח השנה.")
                .setPositiveButton("הסרה", (d, w) -> {
                    Db.get(this).end(med, Days.today());
                    Scheduler.cancelSlots(this, med.id, oldTimes);
                    finish();
                })
                .setNegativeButton("ביטול", null)
                .show();
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
