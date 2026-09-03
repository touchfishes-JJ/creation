package com.forcefocus.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

public final class FocusState {
    public static final String PREFS = "forcefocus_native";
    private FocusState() {}

    public static void start(Context c, String task, long endTime) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("focus_active", true)
                .putString("focus_task", task == null ? "考公" : task)
                .putLong("focus_end", endTime)
                .apply();
    }

    public static void stop(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("focus_active", false)
                .remove("focus_end").apply();
    }

    public static boolean isActive(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean active = p.getBoolean("focus_active", false);
        long end = p.getLong("focus_end", 0);
        if (active && end > 0 && System.currentTimeMillis() >= end) { stop(c); return false; }
        return active;
    }

    public static String task(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("focus_task", "考公");
    }

    private static String weekKey() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) + "-W" + cal.get(Calendar.WEEK_OF_YEAR);
    }

    public static int getEarlyRemaining(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String wk = weekKey();
        if (!wk.equals(p.getString("early_week", ""))) {
            p.edit().putString("early_week", wk).putInt("early_used", 0).apply();
            return 2;
        }
        return Math.max(0, 2 - p.getInt("early_used", 0));
    }

    public static boolean consumeEarlyEnd(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int remaining = getEarlyRemaining(c);
        if (remaining <= 0) return false;
        p.edit().putInt("early_used", 3 - remaining).apply();
        return true;
    }
}
