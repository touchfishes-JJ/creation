package com.forcefocus.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class WeekendPrefs {
    private static final String P="weekend_prefs";
    private static final String KEY="enabled";
    public static boolean enabled(Context c){
        return c.getSharedPreferences(P,Context.MODE_PRIVATE).getBoolean(KEY,true);
    }
    public static void setEnabled(Context c,boolean enabled){
        c.getSharedPreferences(P,Context.MODE_PRIVATE).edit().putBoolean(KEY,enabled).apply();
        LockState.refresh(c);
        Scheduler.scheduleNext14Days(c);
    }
}
