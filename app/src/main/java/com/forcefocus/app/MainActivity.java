package com.forcefocus.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    LinearLayout root; TextView status, escapes, next;
    int bg=Color.rgb(11,12,15), panel=Color.rgb(24,26,31), white=Color.rgb(244,244,245), muted=Color.rgb(161,161,170);
    @Override public void onCreate(Bundle b){ super.onCreate(b); build(); requestBasics(); Scheduler.scheduleNext14Days(this); }
    @Override public void onResume(){ super.onResume(); update(); }
    TextView text(String s,int sp,int color){ TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setPadding(0,8,0,8);return v; }
    Button btn(String s){ Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);return b; }
    void build(){
        ScrollView sc=new ScrollView(this); root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(34,32,34,48);root.setBackgroundColor(bg);sc.addView(root);setContentView(sc);
        root.addView(text("强制日程",14,muted)); root.addView(text("现在该做什么，就只让手机做什么。",25,white));
        status=text("",18,white);root.addView(status); escapes=text("",15,muted);root.addView(escapes); next=text("",15,muted);root.addView(next);
        Button access=btn("① 开启无障碍拦截权限"); access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));root.addView(access);
        Button alarm=btn("② 允许精确闹钟（用于准点提醒）"); alarm.setOnClickListener(v->{ if(Build.VERSION.SDK_INT>=31){try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}});root.addView(alarm);
        root.addView(text("\n手动开始",18,white));
        addMode(Mode.RESUME); addMode(Mode.JOB); addMode(Mode.EXAM); addMode(Mode.AUDIO);
        root.addView(text("\n固定周末",18,white));
        root.addView(text("周六、周日\n09:00–11:30\n13:30–17:00\n19:00–21:30\n\n自动进入【考公专业课】模式；提前 5 分钟声音提醒。",16,muted));
        root.addView(text("\n每自然周只有 2 次提前解除机会。锁定页需要长按 8 秒才会消耗一次。",15,muted));
        root.addView(text("\n说明：本 App 不联网，数据只保存在本机。普通安卓 App 无法阻止你卸载它或手动关闭无障碍权限。",13,Color.rgb(113,113,122)));
    }
    void addMode(Mode m){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(22,18,22,18);GradientDrawableCompat.bg(box,panel,16);
        box.addView(text(m.title,19,white));box.addView(text(m.desc,14,muted));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        for(int mins:new int[]{25,60,90}){Button b=btn(mins+" 分钟");b.setOnClickListener(v->{LockState.startManual(this,m,mins);Toast.makeText(this,"已开始："+m.title,Toast.LENGTH_SHORT).show();update();});row.addView(b,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));}
        box.addView(row); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,10,0,10);root.addView(box,lp);
    }
    void requestBasics(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);
    }
    void update(){
        LockState.Session s=LockState.current(this);
        status.setText(s==null?"当前：未锁定":"当前："+s.mode.title+" · "+LockState.timeLeft(s.end));
        escapes.setText("本周提前解除："+LockState.escapesLeft(this)+" / 2 次");
        next.setText("下一次周末自动锁机："+LockState.nextWeekendText());
    }
}
