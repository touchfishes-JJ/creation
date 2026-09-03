package com.forcefocus.app;

import android.app.*;import android.content.*;import android.net.Uri;import android.os.Build;import android.provider.Settings;import android.webkit.JavascriptInterface;import java.util.*;

public class WebBridge{
 private final MainActivity a; WebBridge(MainActivity a){this.a=a;}
 private Mode mode(String s){if("岗位调研".equals(s))return Mode.JOB;if("考公".equals(s))return Mode.EXAM;if("磨耳朵".equals(s))return Mode.AUDIO;return Mode.RESUME;}
 @JavascriptInterface public void openAccessibilitySettings(){a.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
 @JavascriptInterface public void openExactAlarmSettings(){try{if(Build.VERSION.SDK_INT>=31)a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+a.getPackageName())));else a.startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception e){a.startActivity(new Intent(Settings.ACTION_SETTINGS));}}
 @JavascriptInterface public void startFocus(String task,int seconds){LockState.startManual(a,mode(task),Math.max(1,(seconds+59)/60));Scheduler.scheduleNext14Days(a);}
 @JavascriptInterface public int endFocusEarly(){LockState.Session s=LockState.current(a);if(s==null)return LockState.escapesLeft(a);if(!LockState.useEscape(a,s.end))return -1;LockState.clearManual(a);return LockState.escapesLeft(a);}
 @JavascriptInterface public void finishFocus(){LockState.clearManual(a);}
 @JavascriptInterface public int getEarlyRemaining(){return LockState.escapesLeft(a);}
 @JavascriptInterface public boolean isFocusActive(){return LockState.current(a)!=null;}
 @JavascriptInterface public long getFocusEnd(){LockState.Session s=LockState.current(a);return s==null?0:s.end;}
 @JavascriptInterface public String getFocusTask(){LockState.Session s=LockState.current(a);if(s==null)return "简历";switch(s.mode){case JOB:return "岗位调研";case EXAM:return "考公";case AUDIO:return "磨耳朵";default:return "简历";}}
 @JavascriptInterface public void setWeekendTask(int slot,String task){LockState.setWeekendMode(a,slot,mode(task));Scheduler.scheduleNext14Days(a);}
 @JavascriptInterface public void rescheduleWeekend(){Scheduler.scheduleNext14Days(a);}
 @JavascriptInterface public void launchApp(String label){String[] p;if("WPS".equals(label))p=new String[]{"cn.wps.moffice_eng","cn.wps.moffice"};else if("小红书".equals(label))p=new String[]{"com.xingin.xhs"};else if("粉笔".equals(label))p=new String[]{"com.fenbi.android.servant"};else p=new String[]{"com.android.soundrecorder","com.miui.voicerecorder","com.huawei.soundrecorder","com.coloros.soundrecorder","com.oplus.soundrecorder","com.vivo.soundrecorder","com.sec.android.app.voicenote"};for(String x:p){Intent i=a.getPackageManager().getLaunchIntentForPackage(x);if(i!=null){a.startActivity(i);return;}}}
}