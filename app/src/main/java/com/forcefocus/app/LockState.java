package com.forcefocus.app;

import android.content.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class LockState {
    private static final String P="lock_state";
    public static final String ACTION_REFRESH="com.forcefocus.app.REFRESH";
    private static final String[] SLOT_STARTS={"09:00","13:30","19:00"};
    private static final String[] SLOT_ENDS={"11:30","17:00","21:30"};

    public static class Session {
        public final Mode mode; public final long end; public final String source;
        Session(Mode mode,long end,String source){this.mode=mode;this.end=end;this.source=source;}
    }

    static SharedPreferences prefs(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE); }
    public static void startManual(Context c, Mode mode, int minutes){long end=System.currentTimeMillis()+minutes*60_000L;prefs(c).edit().putString("manual_mode",mode.name()).putLong("manual_end",end).putLong("bypass_until",0).apply();refresh(c);}
    public static void clearManual(Context c){prefs(c).edit().remove("manual_mode").remove("manual_end").apply();refresh(c);}
    public static Session current(Context c){long now=System.currentTimeMillis();long bypass=prefs(c).getLong("bypass_until",0);if(now<bypass)return null;long end=prefs(c).getLong("manual_end",0);String m=prefs(c).getString("manual_mode",null);if(m!=null&&now<end){try{return new Session(Mode.valueOf(m),end,"手动");}catch(Exception ignored){}}else if(m!=null)clearManual(c);return weekendSession(c,now);}
    public static Session weekendSession(Context c,long now){Calendar cal=Calendar.getInstance();cal.setTimeInMillis(now);int dow=cal.get(Calendar.DAY_OF_WEEK);if(dow!=Calendar.SATURDAY&&dow!=Calendar.SUNDAY)return null;int minutes=cal.get(Calendar.HOUR_OF_DAY)*60+cal.get(Calendar.MINUTE);int[][] slots={{9*60,11*60+30},{13*60+30,17*60},{19*60,21*60+30}};for(int idx=0;idx<slots.length;idx++){int[] s=slots[idx];if(minutes>=s[0]&&minutes<s[1]){Calendar e=(Calendar)cal.clone();e.set(Calendar.HOUR_OF_DAY,s[1]/60);e.set(Calendar.MINUTE,s[1]%60);e.set(Calendar.SECOND,0);e.set(Calendar.MILLISECOND,0);return new Session(weekendMode(c,idx),e.getTimeInMillis(),"周末");}}return null;}
    public static Mode weekendMode(Context c,int idx){String def=idx==0?Mode.EXAM.name():(idx==1?Mode.AUDIO.name():Mode.JOB.name());String raw=prefs(c).getString("weekend_slot_"+idx,def);try{return Mode.valueOf(raw);}catch(Exception e){return Mode.EXAM;}}
    public static void setWeekendMode(Context c,int idx,Mode mode){prefs(c).edit().putString("weekend_slot_"+idx,mode.name()).apply();refresh(c);}
    public static String slotStart(int idx){return SLOT_STARTS[idx];} public static String slotEnd(int idx){return SLOT_ENDS[idx];}
    public static int currentWeekendSlot(){Calendar cal=Calendar.getInstance();int dow=cal.get(Calendar.DAY_OF_WEEK);if(dow!=Calendar.SATURDAY&&dow!=Calendar.SUNDAY)return -1;int minutes=cal.get(Calendar.HOUR_OF_DAY)*60+cal.get(Calendar.MINUTE);int[][] slots={{9*60,11*60+30},{13*60+30,17*60},{19*60,21*60+30}};for(int i=0;i<slots.length;i++)if(minutes>=slots[i][0]&&minutes<slots[i][1])return i;return -1;}
    public static boolean slotFinished(int idx){Calendar cal=Calendar.getInstance();int dow=cal.get(Calendar.DAY_OF_WEEK);if(dow!=Calendar.SATURDAY&&dow!=Calendar.SUNDAY)return false;int minutes=cal.get(Calendar.HOUR_OF_DAY)*60+cal.get(Calendar.MINUTE);int[] end={11*60+30,17*60,21*60+30};return minutes>=end[idx];}
    public static String nextWeekendText(){Calendar c=Calendar.getInstance();long now=System.currentTimeMillis();for(int day=0;day<8;day++){Calendar d=(Calendar)c.clone();d.add(Calendar.DAY_OF_YEAR,day);int dow=d.get(Calendar.DAY_OF_WEEK);if(dow==Calendar.SATURDAY||dow==Calendar.SUNDAY){int[][] starts={{9,0},{13,30},{19,0}};for(int[] st:starts){Calendar x=(Calendar)d.clone();x.set(Calendar.HOUR_OF_DAY,st[0]);x.set(Calendar.MINUTE,st[1]);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);if(x.getTimeInMillis()>now)return new SimpleDateFormat("E HH:mm",Locale.CHINA).format(x.getTime());}}}return "--";}
    static String weekKey(){Calendar c=Calendar.getInstance();return c.getWeekYear()+"-"+c.get(Calendar.WEEK_OF_YEAR);}
    public static int escapesLeft(Context c){SharedPreferences p=prefs(c);String wk=weekKey();if(!wk.equals(p.getString("escape_week",""))){p.edit().putString("escape_week",wk).putInt("escapes",2).apply();return 2;}return p.getInt("escapes",2);}
    public static boolean useEscape(Context c,long sessionEnd){int n=escapesLeft(c);if(n<=0)return false;prefs(c).edit().putInt("escapes",n-1).putLong("bypass_until",sessionEnd).apply();refresh(c);return true;}
    public static void refresh(Context c){c.sendBroadcast(new Intent(ACTION_REFRESH).setPackage(c.getPackageName()));}
    public static String timeLeftShort(long end){long ms=Math.max(0,end-System.currentTimeMillis()),total=ms/1000,h=total/3600,m=(total%3600)/60,s=total%60;if(h>0)return String.format(Locale.CHINA,"%02d:%02d:%02d",h,m,s);return String.format(Locale.CHINA,"%02d:%02d",m,s);}
    public static String timeLeft(long end){long ms=Math.max(0,end-System.currentTimeMillis()),total=ms/1000,h=total/3600,m=(total%3600)/60,s=total%60;return String.format(Locale.CHINA,"%02d:%02d:%02d",h,m,s);}
}
