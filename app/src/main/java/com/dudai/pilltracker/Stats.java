package com.dudai.pilltracker;

import java.util.List;
import java.util.Map;

final class Stats {

    static final int NONE = 0, MISSED = 1, PARTIAL = 2, FULL = 3;

    private Stats() {}

    /** @return {taken, expected} for that day. */
    static int[] progress(List<Med> meds, Map<Long, Integer> counts, String day) {
        int expected = 0, taken = 0;
        for (Med m : meds) {
            if (!m.activeOn(day)) continue;
            int n = m.times.size();
            expected += n;
            Integer c = counts == null ? null : counts.get(m.id);
            if (c != null) taken += Math.min(c, n);
        }
        return new int[]{taken, expected};
    }

    static int status(int[] p) {
        if (p[1] == 0) return NONE;
        if (p[0] == 0) return MISSED;
        return p[0] >= p[1] ? FULL : PARTIAL;
    }

    /** Days in a row with everything taken (today counts only once it's complete). */
    static int streak(Db db) {
        String today = Days.today();
        Map<String, Map<Long, Integer>> counts = db.counts(Days.add(today, -400), today);
        List<Med> all = db.allMeds();
        int s = status(progress(all, counts.get(today), today)) == FULL ? 1 : 0;
        String d = Days.add(today, -1);
        for (int i = 0; i < 400; i++) {
            if (status(progress(all, counts.get(d), d)) != FULL) break;
            s++;
            d = Days.add(d, -1);
        }
        return s;
    }
}
