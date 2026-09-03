package com.forcefocus.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-registers exact/fallback alarms after reboot, update or clock changes. */
public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper.ensureChannel(context);
        WeekendScheduler.scheduleAll(context);
        if (FocusState.isActive(context)) {
            FocusAlarmReceiver.scheduleEnd(context, FocusState.focusEnd(context));
        } else {
            WeekendScheduler.restoreCurrentWeekendSlot(context);
        }
    }
}
