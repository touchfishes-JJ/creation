package com.forcefocus.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class FocusAlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "forcefocus_focus";
    public static final String ACTION_END = "com.forcefocus.app.FOCUS_END";
    public static final String ACTION_WEEKEND_WARN = "com.forcefocus.app.WEEKEND_WARN";
    public static final String ACTION_WEEKEND_START = "com.forcefocus.app.WEEKEND_START";

    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (ACTION_END.equals(a)) {
            FocusState.stop(context);
            notify(context, "本次专注完成", "可以休息一下了。", 3001);
        } else if (ACTION_WEEKEND_WARN.equals(a)) {
            notify(context, "5 分钟后进入周末专注", "请保存当前内容，准备切换到白名单应用。", 3002 + intent.getIntExtra("slot", 0));
        } else if (ACTION_WEEKEND_START.equals(a)) {
            int slot = intent.getIntExtra("slot", 0);
            long end = intent.getLongExtra("end", System.currentTimeMillis() + 60 * 60 * 1000L);
            String task = context.getSharedPreferences(FocusState.PREFS, Context.MODE_PRIVATE)
                    .getString("weekend_task_" + slot, "考公");
            FocusState.start(context, task, end);
            scheduleEnd(context, end);
            notify(context, "周末专注已开始", task + " · 仅允许对应白名单应用", 3010 + slot);
        }
        WeekendScheduler.scheduleAll(context);
    }

    private static void notify(Context c, String title, String text, int id) {
        Intent open = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE);
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, b.build());
    }

    public static void scheduleEnd(Context c, long when) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, FocusAlarmReceiver.class).setAction(ACTION_END);
        PendingIntent pi = PendingIntent.getBroadcast(c, 2001, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        schedule(am, pi, when);
    }

    public static void cancelEnd(Context c) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, FocusAlarmReceiver.class).setAction(ACTION_END);
        PendingIntent pi = PendingIntent.getBroadcast(c, 2001, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    public static void schedule(AlarmManager am, PendingIntent pi, long when) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } else am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
    }
}
