package com.forcefocus.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Schedules each warning and start independently to avoid past-alarm loops. */
public final class WeekendScheduler {
    public static final int[][] SLOTS = {
            {9, 0, 11, 30},
            {13, 30, 17, 0},
            {19, 0, 21, 30}
    };

    private static final long FIVE_MINUTES = 5L * 60L * 1000L;

    private WeekendScheduler() {}

    public static void scheduleAll(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        long now = System.currentTimeMillis();
        for (int slot = 0; slot < SLOTS.length; slot++) {
            PendingIntent warning = warningIntent(context, slot);
            PendingIntent start = startIntent(context, slot, 0L);
            manager.cancel(warning);
            manager.cancel(start);

            Calendar warningOccurrence = nextWeekendStart(slot, -5, now);
            schedule(manager, warning, warningOccurrence.getTimeInMillis() - FIVE_MINUTES);

            Calendar startOccurrence = nextWeekendStart(slot, 0, now);
            Calendar endOccurrence = endFor(slot, startOccurrence);
            start = startIntent(context, slot, endOccurrence.getTimeInMillis());
            schedule(manager, start, startOccurrence.getTimeInMillis());
        }
    }

    public static void restoreCurrentWeekendSlot(Context context) {
        int slot = currentSlot();
        if (slot < 0 || FocusState.wasWeekendInstanceSkipped(context, slot)) return;
        if (FocusState.isActive(context)) return;

        Calendar now = Calendar.getInstance();
        Calendar end = (Calendar) now.clone();
        end.set(Calendar.HOUR_OF_DAY, SLOTS[slot][2]);
        end.set(Calendar.MINUTE, SLOTS[slot][3]);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);
        long endMillis = FocusState.start(
                context, FocusState.weekendTask(context, slot), end.getTimeInMillis(), slot);
        FocusAlarmReceiver.scheduleEnd(context, endMillis);
        MainActivity.requestUiRefresh();
    }

    public static int currentSlot() {
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK);
        if (day != Calendar.SATURDAY && day != Calendar.SUNDAY) return -1;
        int minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        for (int slot = 0; slot < SLOTS.length; slot++) {
            int start = SLOTS[slot][0] * 60 + SLOTS[slot][1];
            int end = SLOTS[slot][2] * 60 + SLOTS[slot][3];
            if (minuteOfDay >= start && minuteOfDay < end) return slot;
        }
        return -1;
    }

    public static String currentInstanceId(int slot) {
        String date = new SimpleDateFormat("yyyyMMdd", Locale.US).format(Calendar.getInstance().getTime());
        return date + "-" + slot;
    }

    public static boolean canScheduleExact(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || manager.canScheduleExactAlarms());
    }

    private static Calendar nextWeekendStart(int slot, int eventOffsetMinutes, long nowMillis) {
        Calendar date = Calendar.getInstance();
        for (int add = 0; add <= 14; add++) {
            Calendar start = (Calendar) date.clone();
            start.add(Calendar.DAY_OF_YEAR, add);
            start.set(Calendar.HOUR_OF_DAY, SLOTS[slot][0]);
            start.set(Calendar.MINUTE, SLOTS[slot][1]);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            int day = start.get(Calendar.DAY_OF_WEEK);
            long eventTime = start.getTimeInMillis() + eventOffsetMinutes * 60_000L;
            if ((day == Calendar.SATURDAY || day == Calendar.SUNDAY)
                    && eventTime > nowMillis + 500L) {
                return start;
            }
        }
        throw new IllegalStateException("Unable to find the next weekend occurrence");
    }

    private static Calendar endFor(int slot, Calendar start) {
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.HOUR_OF_DAY, SLOTS[slot][2]);
        end.set(Calendar.MINUTE, SLOTS[slot][3]);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);
        return end;
    }

    private static PendingIntent warningIntent(Context context, int slot) {
        Intent intent = new Intent(context, FocusAlarmReceiver.class)
                .setAction(FocusAlarmReceiver.ACTION_WEEKEND_WARN)
                .putExtra("slot", slot);
        return PendingIntent.getBroadcast(context, 4100 + slot, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent startIntent(Context context, int slot, long endMillis) {
        Intent intent = new Intent(context, FocusAlarmReceiver.class)
                .setAction(FocusAlarmReceiver.ACTION_WEEKEND_START)
                .putExtra("slot", slot)
                .putExtra("end", endMillis);
        return PendingIntent.getBroadcast(context, 4200 + slot, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(AlarmManager manager, PendingIntent operation, long when) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, operation);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, operation);
        } else {
            manager.setExact(AlarmManager.RTC_WAKEUP, when, operation);
        }
    }
}
