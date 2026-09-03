package com.forcefocus.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    LinearLayout root; TextView status, escapes, next;
    Typeface kai;
    int paper=Color.rgb(233,245,236), ink=Color.rgb(232,246,236), muted=Color.rgb(170,210,184);
    int stroke=Color.rgb(91,153,113), gold=Color.rgb(211,236,184);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        kai=Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        build();
        requestBasics();
        Scheduler.scheduleNext14Days(this);
    }

    @Override public void onResume(){ super.onResume(); update(); }

    TextView text(String s,int sp,int color){
        TextView v=new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setPadding(0,8,0,8); v.setTypeface(kai);
        return v;
    }

    Button btn(String s){
        Button b=new Button(this);
        b.setText(s); b.setAllCaps(false); b.setTextSize(16); b.setTypeface(kai); b.setPadding(22,18,22,18);
        GradientDrawableCompat.button(b, Color.rgb(34,89,59), Color.rgb(24,69,47), stroke, paper);
        return b;
    }

    LinearLayout.LayoutParams marginBottom(){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,10);
        return lp;
    }

    LinearLayout card(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(22,18,22,18);
        GradientDrawableCompat.panel(box, Color.argb(208,22,62,44), Color.argb(188,10,34,24), 24, stroke);
        return box;
    }

    TextView sectionTag(String s){
        TextView t=text("◈  "+s,18,gold); t.setPadding(0,12,0,8); return t;
    }

    void build(){
        FrameLayout frame=new FrameLayout(this);
        frame.addView(new JianghuDecorView(this),new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView sc=new ScrollView(this); sc.setFillViewport(true);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(34,42,34,54); root.setBackgroundColor(Color.TRANSPARENT);
        sc.addView(root);
        frame.addView(sc,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(frame);

        LinearLayout header=new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL); header.setPadding(24,22,24,22);
        GradientDrawableCompat.panel(header, Color.argb(210,26,71,49), Color.argb(195,12,39,28), 30, stroke);
        TextView brand=text("竹影江湖 · 强制日程",14,gold); brand.setLetterSpacing(0.08f); header.addView(brand);
        header.addView(text("风起竹林，先做眼前事。",28,ink));
        header.addView(text("锁住分心，给当下的任务一条清路。",15,muted));
        LinearLayout.LayoutParams hlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); hlp.setMargins(0,0,0,14); root.addView(header,hlp);

        LinearLayout hero=card();
        hero.addView(text("清笛一响，心无旁骛",13,gold));
        status=text("",20,ink); hero.addView(status);
        escapes=text("",15,muted); hero.addView(escapes);
        next=text("",15,muted); hero.addView(next);
        root.addView(hero,marginBottom());

        root.addView(sectionTag("开启权限"));
        Button access=btn("① 开启无障碍拦截权限");
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access,marginBottom());

        Button alarm=btn("② 允许精确闹钟（用于准点提醒）");
        alarm.setOnClickListener(v->{
            if(Build.VERSION.SDK_INT>=31){
                try{ startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName()))); }
                catch(Exception e){ startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            }
        });
        root.addView(alarm,marginBottom());

        root.addView(sectionTag("手动开始"));
        addMode(Mode.RESUME); addMode(Mode.JOB); addMode(Mode.EXAM); addMode(Mode.AUDIO);

        root.addView(sectionTag("固定周末"));
        LinearLayout weekend=card();
        weekend.addView(text("周六、周日",19,ink));
        weekend.addView(text("09:00–11:30\n13:30–17:00\n19:00–21:30",18,gold));
        weekend.addView(text("自动进入【考公专业课】模式，提前 5 分钟声音提醒。",15,muted));
        root.addView(weekend,marginBottom());

        LinearLayout note=card();
        note.addView(text("竹叶规矩",17,ink));
        note.addView(text("每自然周只有 2 次提前解除机会。锁定页需要长按 8 秒才会消耗一次。",15,muted));
        note.addView(text("说明：本 App 不联网，数据只保存在本机。普通安卓 App 无法阻止你卸载它或手动关闭无障碍权限。",13,Color.rgb(127,171,140)));
        root.addView(note,marginBottom());
    }

    void addMode(Mode m){
        LinearLayout box=card();
        box.addView(text(m.title,20,ink));
        box.addView(text(m.desc,14,muted));
        box.addView(text("竹纹 · 清心 · 断杂念",13,gold));
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for(int mins:new int[]{25,60,90}){
            Button b=btn(mins+" 分钟");
            b.setOnClickListener(v->{ LockState.startManual(this,m,mins); Toast.makeText(this,"已开始："+m.title,Toast.LENGTH_SHORT).show(); update(); });
            LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1); blp.setMargins(0,0,8,0);
            row.addView(b,blp);
        }
        box.addView(row);
        root.addView(box,marginBottom());
    }

    void requestBasics(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);
    }

    void update(){
        LockState.Session s=LockState.current(this);
        status.setText(s==null?"当前：未锁定":"当前："+s.mode.title+" · "+LockState.timeLeft(s.end));
        escapes.setText("本周提前解除："+LockState.escapesLeft(this)+" / 2 次");
        next.setText("下一次周末自动锁机："+LockState.nextWeekendText());
    }
}
