package com.dudai.pilltracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** List of medications; tap to edit, button to add. */
public class MedsActivity extends Activity {

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
        LinearLayout root = Ui.page(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        Button add = Ui.button(this, "+ הוספת תרופה", true);
        add.setOnClickListener(v -> startActivity(new Intent(this, EditMedActivity.class)));
        root.addView(add);
    }

    @Override
    protected void onResume() {
        super.onResume();
        list.removeAllViews();
        for (Med m : Db.get(this).activeMeds()) list.addView(row(m));
        if (list.getChildCount() == 0) {
            TextView e = Ui.text(this, "עוד אין תרופות 🌷", 17, Ui.TEXT_SOFT, false);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 12));
            list.addView(e);
        }
    }

    private LinearLayout row(Med m) {
        LinearLayout row = Ui.card(this, Ui.WHITE);
        row.setBackground(Ui.ripple(Ui.rounded(Ui.WHITE, Ui.dp(this, 22))));
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView dot = new TextView(this);
        dot.setBackground(Ui.oval(m.colorValue(), 0, 0));
        int s = Ui.dp(this, 18);
        row.addView(dot, new LinearLayout.LayoutParams(s, s));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(Ui.dp(this, 14), 0, 0, 0);
        texts.addView(Ui.text(this, m.name, 18, Ui.TEXT, true));
        if (!m.dose.isEmpty()) texts.addView(Ui.text(this, m.dose, 14, Ui.TEXT_SOFT, false));
        String times = m.times.size() == 1 ? "פעם ביום" : m.times.size() + " פעמים ביום";
        texts.addView(Ui.text(this, times + ": " + String.join(" · ", m.times)
                + (m.remind ? "  🔔" : "  🔕"), 14, Ui.PINK_DARK, false));
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        row.setOnClickListener(v -> startActivity(new Intent(this, EditMedActivity.class)
                .putExtra(EditMedActivity.EXTRA_ID, m.id)));
        return row;
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
