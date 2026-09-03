package com.forcefocus.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        WeekendScheduler.scheduleAll(context);
        if (FocusState.isActive(context)) {
            long end = context.getSharedPreferences(FocusState.PREFS, Context.MODE_PRIVATE)
                    .getLong("focus_end", 0L);
            if (end > System.currentTimeMillis()) FocusAlarmReceiver.scheduleEnd(context, end);
        }
    }
}
