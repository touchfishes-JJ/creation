package com.forcefocus.app;

import android.app.*;
import android.content.*;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i){
        Scheduler.ensureChannel(c);
        String kind=i.getStringExtra("kind");
        if("pre".equals(kind)){
            Notification.Builder b = android.os.Build.VERSION.SDK_INT>=26 ? new Notification.Builder(c,Scheduler.CHANNEL) : new Notification.Builder(c);
            b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
             .setContentTitle("5 分钟后进入专注")
             .setContentText("周末时段即将开始")
             .setAutoCancel(true).setPriority(Notification.PRIORITY_MAX).setDefaults(Notification.DEFAULT_ALL);
            c.getSystemService(NotificationManager.class).notify((int)(System.currentTimeMillis()%100000),b.build());
        } else { LockState.refresh(c); }
        Scheduler.scheduleNext14Days(c);
    }
}
