package com.forcefocus.app;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.util.*;

public final class Scheduler {
    public static final String CHANNEL="force_reminders";
    public static void ensureChannel(Context c){
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager nm=c.getSystemService(NotificationManager.class);
            NotificationChannel ch=new NotificationChannel(CHANNEL,"强制日程提醒",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("锁机前 5 分钟提醒"); ch.enableVibration(true); nm.createNotificationChannel(ch);
        }
    }
    public static void scheduleNext14Days(Context c){
        ensureChannel(c);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Calendar base=Calendar.getInstance();
        int request=1000;
        for(int add=0;add<14;add++){
            Calendar d=(Calendar)base.clone(); d.add(Calendar.DAY_OF_YEAR,add);
            int dow=d.get(Calendar.DAY_OF_WEEK); if(dow!=Calendar.SATURDAY&&dow!=Calendar.SUNDAY)continue;
            int[][] starts={{9,0},{13,30},{19,0}};
            for(int[] st:starts){
                Calendar start=(Calendar)d.clone(); start.set(Calendar.HOUR_OF_DAY,st[0]);start.set(Calendar.MINUTE,st[1]);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
                long pre=start.getTimeInMillis()-5*60_000L;
                if(pre<=System.currentTimeMillis())continue;
                Intent i=new Intent(c,AlarmReceiver.class).putExtra("kind","pre");
                PendingIntent pi=PendingIntent.getBroadcast(c,request++,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                set(am,pre,pi);
                Intent j=new Intent(c,AlarmReceiver.class).putExtra("kind","start");
                PendingIntent pj=PendingIntent.getBroadcast(c,request++,j,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                set(am,start.getTimeInMillis(),pj);
            }
        }
    }
    static void set(AlarmManager am,long when,PendingIntent pi){
        try{
            if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
        }catch(Exception e){ am.set(AlarmManager.RTC_WAKEUP,when,pi); }
    }
}
