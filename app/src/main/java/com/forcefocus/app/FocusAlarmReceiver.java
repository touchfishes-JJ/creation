package com.forcefocus.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles focus completion plus the three weekend warning/start alarms. */
public final class FocusAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_END = "com.forcefocus.app.FOCUS_END";
    public static final String ACTION_WEEKEND_WARN = "com.forcefocus.app.WEEKEND_WARN";
    public static final String ACTION_WEEKEND_START = "com.forcefocus.app.WEEKEND_START";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_END.equals(action)) {
            if (FocusState.finishNaturally(context)) {
                NotificationHelper.show(
                        context, 3001, "本次专注完成", "可以休息一下了。", true);
            }
            MainActivity.requestUiRefresh();
        } else if (ACTION_WEEKEND_WARN.equals(action)) {
            int slot = intent.getIntExtra("slot", 0);
            NotificationHelper.show(
                    context,
                    3100 + slot,
                    "5 分钟后进入周末专注",
                    slotTime(slot) + " · 请保存当前内容",
                    true);
        } else if (ACTION_WEEKEND_START.equals(action)) {
            int slot = intent.getIntExtra("slot", -1);
            long end = intent.getLongExtra("end", 0L);
            if (slot >= 0 && slot < 3 && end > System.currentTimeMillis()
                    && !FocusState.wasWeekendInstanceSkipped(context, slot)) {
                String task = FocusState.weekendTask(context, slot);
                long safeEnd = FocusState.start(context, task, end, slot);
                scheduleEnd(context, safeEnd);
                NotificationHelper.show(
                        context,
                        3200 + slot,
                        "周末专注已开始",
                        task + " · 仅允许对应白名单 App",
                        true);
                FocusAccessibilityService.bringForceFocusToFront();
                MainActivity.bringToFront(context);
                MainActivity.requestUiRefresh();
            }
        }
        WeekendScheduler.scheduleAll(context);
    }

    private static String slotTime(int slot) {
        if (slot == 1) return "13:30–17:00";
        if (slot == 2) return "19:00–21:30";
        return "09:00–11:30";
    }

    public static void scheduleEnd(Context context, long when) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        PendingIntent operation = endIntent(context);
        manager.cancel(operation);
        WeekendScheduler.schedule(manager, operation, when);
    }

    public static void cancelEnd(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(endIntent(context));
    }

    private static PendingIntent endIntent(Context context) {
        Intent intent = new Intent(context, FocusAlarmReceiver.class).setAction(ACTION_END);
        return PendingIntent.getBroadcast(context, 2001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
