package com.forcefocus.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public final class WeekendScheduler {
    private WeekendScheduler() {}
    private static final int[][] SLOTS = {{9,0,11,30},{13,30,17,0},{19,0,21,30}};

    public static void scheduleAll(Context c) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        for (int slot = 0; slot < 3; slot++) {
            Calendar start = nextWeekendTime(SLOTS[slot][0], SLOTS[slot][1]);
            Calendar end = (Calendar)start.clone();
            end.set(Calendar.HOUR_OF_DAY, SLOTS[slot][2]);
            end.set(Calendar.MINUTE, SLOTS[slot][3]);
            long startMs = start.getTimeInMillis();
            long endMs = end.getTimeInMillis();

            Intent warnI = new Intent(c, FocusAlarmReceiver.class)
                    .setAction(FocusAlarmReceiver.ACTION_WEEKEND_WARN).putExtra("slot", slot);
            PendingIntent warnPi = PendingIntent.getBroadcast(c, 4100 + slot, warnI,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            FocusAlarmReceiver.schedule(am, warnPi, startMs - 5 * 60 * 1000L);

            Intent startI = new Intent(c, FocusAlarmReceiver.class)
                    .setAction(FocusAlarmReceiver.ACTION_WEEKEND_START)
                    .putExtra("slot", slot).putExtra("end", endMs);
            PendingIntent startPi = PendingIntent.getBroadcast(c, 4200 + slot, startI,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            FocusAlarmReceiver.schedule(am, startPi, startMs);
        }
    }

    private static Calendar nextWeekendTime(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, minute);
        for (int add = 0; add <= 7; add++) {
            Calendar x = (Calendar)c.clone();
            x.add(Calendar.DAY_OF_YEAR, add);
            int dow = x.get(Calendar.DAY_OF_WEEK);
            if ((dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) &&
                    x.getTimeInMillis() > now.getTimeInMillis()) return x;
        }
        c.add(Calendar.DAY_OF_YEAR, 7);
        return c;
    }
}
