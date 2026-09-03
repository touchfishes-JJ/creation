package com.forcefocus.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.*;

public final class StudyStats {
    private static final String P="study_stats";
    private static final String M="study_marks";
    private static long lastTick=0L;
    private static String key(long time){return new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).format(new Date(time));}
    public static synchronized void tick(Context c){
        long now=System.currentTimeMillis(); LockState.Session s=LockState.current(c);
        if(s==null){lastTick=now;return;}
        if(lastTick==0L||now-lastTick>5000L){lastTick=now;return;}
        long delta=Math.max(0,Math.min(2000L,now-lastTick));lastTick=now;
        String k=key(now);SharedPreferences p=c.getSharedPreferences(P,Context.MODE_PRIVATE);p.edit().putLong(k,p.getLong(k,0L)+delta).apply();
    }
    public static long millisFor(Context c,int year,int monthZero,int day){Calendar cal=Calendar.getInstance();cal.set(year,monthZero,day);return c.getSharedPreferences(P,Context.MODE_PRIVATE).getLong(key(cal.getTimeInMillis()),0L);}
    public static boolean marked(Context c,int year,int monthZero,int day){Calendar cal=Calendar.getInstance();cal.set(year,monthZero,day);return c.getSharedPreferences(M,Context.MODE_PRIVATE).getBoolean(key(cal.getTimeInMillis()),false);}
    public static void toggleMark(Context c,int year,int monthZero,int day){Calendar cal=Calendar.getInstance();cal.set(year,monthZero,day);String k=key(cal.getTimeInMillis());SharedPreferences p=c.getSharedPreferences(M,Context.MODE_PRIVATE);p.edit().putBoolean(k,!p.getBoolean(k,false)).apply();}
}
