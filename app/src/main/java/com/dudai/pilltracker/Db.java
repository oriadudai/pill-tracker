package com.dudai.pilltracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Everything is stored only on the phone. */
final class Db extends SQLiteOpenHelper {

    private static final String COLS = "_id,name,dose,times,remind,color,start_day,end_day";
    private static Db instance;

    static synchronized Db get(Context ctx) {
        if (instance == null) instance = new Db(ctx.getApplicationContext());
        return instance;
    }

    private Db(Context ctx) {
        super(ctx, "pills.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE meds (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, "
                + "dose TEXT, times TEXT NOT NULL, remind INTEGER NOT NULL DEFAULT 1, "
                + "color INTEGER NOT NULL DEFAULT 0, start_day TEXT NOT NULL, end_day TEXT)");
        db.execSQL("CREATE TABLE intakes (med_id INTEGER NOT NULL, day TEXT NOT NULL, "
                + "slot TEXT NOT NULL, taken_at INTEGER NOT NULL, PRIMARY KEY (med_id, day, slot))");

        // A first example, so the app isn't empty on first launch (can be edited or deleted).
        ContentValues v = new ContentValues();
        v.put("name", "אומגה 3");
        v.put("dose", "עוזרת לברכיים 🦵💪");
        v.put("times", "09:00");
        v.put("remind", 1);
        v.put("color", 2);
        v.put("start_day", Days.today());
        db.insert("meds", null, v);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}

    // ---- medications ----

    private List<Med> query(String where, String[] args) {
        List<Med> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM meds"
                + (where == null ? "" : " WHERE " + where) + " ORDER BY name COLLATE NOCASE", args)) {
            while (c.moveToNext()) {
                Med m = new Med();
                m.id = c.getLong(0);
                m.name = c.getString(1);
                m.dose = c.isNull(2) ? "" : c.getString(2);
                m.times = Med.parseTimes(c.getString(3));
                m.remind = c.getInt(4) != 0;
                m.color = c.getInt(5);
                m.startDay = c.getString(6);
                m.endDay = c.isNull(7) ? null : c.getString(7);
                out.add(m);
            }
        }
        return out;
    }

    List<Med> activeMeds() {
        return query("end_day IS NULL", null);
    }

    List<Med> allMeds() {
        return query(null, null);
    }

    List<Med> medsForDay(String day) {
        List<Med> out = new ArrayList<>();
        for (Med m : allMeds()) if (m.activeOn(day)) out.add(m);
        return out;
    }

    Med getMed(long id) {
        List<Med> l = query("_id=?", new String[]{String.valueOf(id)});
        return l.isEmpty() ? null : l.get(0);
    }

    void save(Med m) {
        ContentValues v = new ContentValues();
        v.put("name", m.name);
        v.put("dose", m.dose);
        v.put("times", m.timesString());
        v.put("remind", m.remind ? 1 : 0);
        v.put("color", m.color);
        v.put("start_day", m.startDay);
        if (m.endDay == null) v.putNull("end_day"); else v.put("end_day", m.endDay);
        if (m.id == 0) {
            m.id = getWritableDatabase().insert("meds", null, v);
        } else {
            getWritableDatabase().update("meds", v, "_id=?", new String[]{String.valueOf(m.id)});
        }
    }

    /** Removes the medication from the list from that day on, but keeps its history. */
    void end(Med m, String day) {
        m.endDay = day;
        save(m);
    }

    // ---- intakes ----

    static String key(long medId, String slot) {
        return medId + "|" + slot;
    }

    /** key(med, slot) -> time it was marked. */
    Map<String, Long> intakes(String day) {
        Map<String, Long> out = new HashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT med_id, slot, taken_at FROM intakes WHERE day=?", new String[]{day})) {
            while (c.moveToNext()) out.put(key(c.getLong(0), c.getString(1)), c.getLong(2));
        }
        return out;
    }

    boolean isTaken(long medId, String day, String slot) {
        return DatabaseUtils.queryNumEntries(getReadableDatabase(), "intakes",
                "med_id=? AND day=? AND slot=?", new String[]{String.valueOf(medId), day, slot}) > 0;
    }

    void setTaken(long medId, String day, String slot, boolean taken) {
        if (taken) {
            ContentValues v = new ContentValues();
            v.put("med_id", medId);
            v.put("day", day);
            v.put("slot", slot);
            v.put("taken_at", System.currentTimeMillis());
            getWritableDatabase().insertWithOnConflict("intakes", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        } else {
            getWritableDatabase().delete("intakes", "med_id=? AND day=? AND slot=?",
                    new String[]{String.valueOf(medId), day, slot});
        }
    }

    /** day -> (med id -> number of doses marked). */
    Map<String, Map<Long, Integer>> counts(String from, String to) {
        Map<String, Map<Long, Integer>> out = new HashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT day, med_id, COUNT(*) FROM intakes WHERE day>=? AND day<=? GROUP BY day, med_id",
                new String[]{from, to})) {
            while (c.moveToNext()) {
                Map<Long, Integer> m = out.get(c.getString(0));
                if (m == null) {
                    m = new HashMap<>();
                    out.put(c.getString(0), m);
                }
                m.put(c.getLong(1), c.getInt(2));
            }
        }
        return out;
    }
}
