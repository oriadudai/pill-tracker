package com.dudai.pilltracker;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small helpers to build the (pink, round, cheerful) UI in code. */
final class Ui {

    static final int PINK = 0xFFEC4899;
    static final int PINK_DARK = 0xFFBE185D;
    static final int PINK_SOFT = 0xFFFCE7F3;
    static final int PURPLE = 0xFFA855F7;
    static final int TEXT = 0xFF3B0A2A;
    static final int TEXT_SOFT = 0xFF9D5C82;
    static final int WHITE = 0xFFFFFFFF;

    static final int FULL = 0xFFF472B6;      // all taken
    static final int PARTIAL = 0xFFFBCFE8;   // some taken
    static final int MISSED = 0xFFE5E7EB;    // nothing taken
    static final int TAKEN_BG = 0xFFFDF2F8;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static GradientDrawable oval(int fill, int strokeColor, int strokePx) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        if (strokePx > 0) g.setStroke(strokePx, strokeColor);
        return g;
    }

    static Drawable ripple(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x33EC4899), content, null);
    }

    /** Hebrew app: always right-to-left, whatever the phone's language. Returns the page's root. */
    static LinearLayout page(Activity a) {
        a.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(a, 16);
        root.setPadding(p, p, p, dp(a, 32));
        sv.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        a.setContentView(sv);
        return root;
    }

    static LinearLayout card(Context c, int color) {
        LinearLayout l = new LinearLayout(c);
        l.setBackground(rounded(color, dp(c, 22)));
        l.setElevation(dp(c, 2));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        l.setLayoutParams(lp);
        return l;
    }

    static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static TextView label(Context c, String s) {
        TextView t = text(c, s, 15, PINK_DARK, true);
        t.setPadding(dp(c, 4), dp(c, 14), dp(c, 4), dp(c, 6));
        return t;
    }

    static Button button(Context c, String s, boolean primary) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(primary ? WHITE : PINK_DARK);
        b.setBackground(ripple(rounded(primary ? PINK : PINK_SOFT, dp(c, 26))));
        b.setStateListAnimator(null);
        b.setMinHeight(dp(c, 52));
        b.setPadding(dp(c, 20), 0, dp(c, 20), 0);
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 10);
        b.setLayoutParams(lp);
        return b;
    }

    static View space(Context c, int dpHeight) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dpHeight)));
        return v;
    }
}
